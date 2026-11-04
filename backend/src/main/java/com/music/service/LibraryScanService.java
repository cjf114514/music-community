package com.music.service;

import com.music.entity.Song;
import com.music.repository.SongRepository;
import com.music.util.Id3TagReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * 本地音乐库扫描
 *
 * ========== 它解决什么问题？==========
 * 通过浏览器上传 148 首歌要 20~40 分钟（串行 HTTP 上传，每首 3~4MB），
 * 而音乐本来就在硬盘上。这里让后端直接读目录：
 *   · 不复制文件（500MB 不重复占空间），只把目录挂成 URL /library/**
 *   · 支持增量：已入库的跳过，新增的才插入
 *   · 顺便检测「库里有记录但文件已消失」的失效条目
 *
 * ========== 配置 ===========
 *   LIBRARY_AUDIO_DIR=E:/音乐        (必填，扫描根目录，同时挂在 /library/**)
 *   LIBRARY_LYRIC_DIR=E:/音乐歌词     (可选，歌词只会被读进数据库，不对外暴露)
 * 不配置时接口返回“未配置”，不影响其他功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryScanService {

    private final SongRepository songRepository;
    private final SongService songService;

    @Value("${app.library.audio-dir:}")
    private String audioDirConfig;

    @Value("${app.library.lyric-dir:}")
    private String lyricDirConfig;

    private static final Set<String> AUDIO_EXT = Set.of("mp3", "flac", "m4a", "aac", "wav", "ogg");
    private static final String URL_PREFIX = "/library/";
    private static final int MAX_SAMPLES = 200;

    /** 当前配置状态，给前端展示用 */
    public Map<String, Object> config() {
        Map<String, Object> m = new LinkedHashMap<>();
        String audioDir = trimToEmpty(audioDirConfig);
        String lyricDir = trimToEmpty(lyricDirConfig);
        m.put("audioDir", audioDir);
        m.put("lyricDir", lyricDir);
        m.put("enabled", !audioDir.isEmpty() && Files.isDirectory(Paths.get(audioDir)));
        m.put("audioDirExists", !audioDir.isEmpty() && Files.isDirectory(Paths.get(audioDir)));
        m.put("lyricDirExists", !lyricDir.isEmpty() && Files.isDirectory(Paths.get(lyricDir)));
        return m;
    }

    /**
     * 执行扫描
     *
     * @param dryRun true 时只统计不写入（用于先看会新增多少）
     */
    public Map<String, Object> scan(boolean dryRun) throws IOException {
        String audioDir = trimToEmpty(audioDirConfig);
        if (audioDir.isEmpty()) {
            throw new IllegalStateException("未配置音乐库目录：请在 .env 里设置 LIBRARY_AUDIO_DIR（例如 E:/音乐），然后重启后端");
        }
        Path audioRoot = Paths.get(audioDir);
        if (!Files.isDirectory(audioRoot)) {
            throw new IllegalStateException("音乐库目录不存在或不可访问：" + audioDir);
        }

        List<Path> audioFiles = listFiles(audioRoot, AUDIO_EXT);
        List<Path> lyricFiles = listFiles(Paths.get(trimToEmpty(lyricDirConfig).isEmpty() ? audioDir : lyricDirConfig), Set.of("lrc"));

        // 歌词索引：文件名归一化 → 文件；歌词内含歌名归一化 → 文件
        Map<String, Path> lyricByBaseName = new HashMap<>();
        Map<String, Path> lyricByTitle = new HashMap<>();
        for (Path lrc : lyricFiles) {
            lyricByBaseName.putIfAbsent(normalize(stripExt(lrc.getFileName().toString())), lrc);
            lyricByTitle.putIfAbsent(normalize(parseLyricTitle(lrc.getFileName().toString())), lrc);
        }

        int added = 0, updated = 0, skipped = 0, failed = 0;
        List<Map<String, Object>> samples = new ArrayList<>();

        for (Path file : audioFiles) {
            String rel = audioRoot.relativize(file).toString().replace('\\', '/');
            String url = URL_PREFIX + rel;
            try {
                long size = Files.size(file);
                Optional<Song> existing = songRepository.findByUrl(url);

                if (existing.isPresent()) {
                    Song song = existing.get();
                    // 文件被替换过（大小变化）→ 刷新文件大小
                    if (song.getFileSize() == null || song.getFileSize() != size) {
                        if (!dryRun) {
                            song.setFileSize(size);
                            songRepository.save(song);
                        }
                        updated++;
                        addSample(samples, "更新", rel, null);
                    } else {
                        skipped++;
                    }
                    continue;
                }

                if (dryRun) {
                    added++;
                    addSample(samples, "新增", rel, null);
                    continue;
                }

                Song song = buildSong(file, url, size, lyricByBaseName, lyricByTitle);
                songService.saveSong(song);
                added++;
                addSample(samples, "新增", rel, song.getSinger());
            } catch (Exception e) {
                failed++;
                addSample(samples, "失败", rel, e.getMessage());
                log.warn("扫描失败: {}", rel, e);
            }
        }

        // 记录还在、文件已消失的条目（只统计，不自动删除）
        int missing = 0;
        List<Map<String, Object>> missingSamples = new ArrayList<>();
        for (Song song : songRepository.findByUrlStartingWith(URL_PREFIX)) {
            String rel = song.getUrl().substring(URL_PREFIX.length());
            if (!Files.exists(audioRoot.resolve(rel.replace('/', java.io.File.separatorChar)))) {
                missing++;
                if (missingSamples.size() < 50) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", song.getId());
                    m.put("name", song.getName());
                    m.put("url", song.getUrl());
                    missingSamples.add(m);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dryRun", dryRun);
        result.put("audioDir", audioDir);
        result.put("totalAudioFiles", audioFiles.size());
        result.put("totalLyricFiles", lyricFiles.size());
        result.put("added", added);
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("failed", failed);
        result.put("missing", missing);
        result.put("samples", samples);
        result.put("missingSamples", missingSamples);
        log.info("音乐库扫描完成: 新增 {}, 更新 {}, 跳过 {}, 失败 {}, 失效 {}", added, updated, skipped, failed, missing);
        return result;
    }

    // ==================== 内部方法 ====================

    private Song buildSong(Path file, String url, long size,
                           Map<String, Path> lyricByBaseName,
                           Map<String, Path> lyricByTitle) {
        Map<String, String> tags = Id3TagReader.read(file);
        String baseName = stripExt(file.getFileName().toString());

        String title = firstNonBlank(tags.get("title"), parseTitleFromFileName(baseName), baseName);
        String artist = firstNonBlank(tags.get("artist"), parseArtistFromFileName(baseName), "");
        String album = firstNonBlank(tags.get("album"), "");
        String genre = firstNonBlank(tags.get("genre"), "");

        Song song = new Song();
        song.setName(title);
        song.setSinger(artist);
        song.setAlbum(album.isEmpty() ? null : album);
        song.setGenre(genre.isEmpty() ? null : genre);
        song.setUrl(url);
        song.setFileSize(size);
        song.setFormat(extOf(file.getFileName().toString()));
        song.setStatus(1);
        song.setPlayCount(0L);
        song.setLyric(readLyric(baseName, title, lyricByBaseName, lyricByTitle));
        return song;
    }

    /** 找同名歌词，找不到再按歌名匹配 */
    private String readLyric(String audioBaseName, String title,
                             Map<String, Path> byBaseName, Map<String, Path> byTitle) {
        Path lrc = byBaseName.get(normalize(audioBaseName));
        if (lrc == null) lrc = byTitle.get(normalize(title));
        if (lrc == null) return null;
        try {
            String text = readText(lrc);
            return text.contains("[") ? text : null;   // 至少要有 LRC 时间标签
        } catch (Exception e) {
            log.warn("读取歌词失败: {}", lrc, e);
            return null;
        }
    }

    /** 读取文本文件：先按 UTF-8，出现非法字节再按 GBK（中文 Windows 常见） */
    private String readText(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        try {
            String s = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            return stripBom(s);
        } catch (CharacterCodingException e) {
            return stripBom(new String(bytes, Charset.forName("GBK")));
        }
    }

    private static String stripBom(String s) {
        return s.startsWith("\uFEFF") ? s.substring(1) : s;
    }

    private static List<Path> listFiles(Path root, Set<String> exts) throws IOException {
        List<Path> list = new ArrayList<>();
        if (root == null || !Files.isDirectory(root)) return list;
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> exts.contains(extOf(p.getFileName().toString())))
                    .sorted()
                    .forEach(list::add);
        }
        return list;
    }

    private static String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase();
    }

    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(0, i);
    }

    /** 从 `歌手 - 歌名.mp3` / `歌名.mp3` 猜歌名（仅在没有 ID3 标签时使用） */
    private static String parseTitleFromFileName(String base) {
        String s = base.replaceFirst("^\\s*\\d{1,3}\\s*[.\\-_、)]\\s*", "");
        int idx = s.lastIndexOf(" - ");
        if (idx > 0) return s.substring(idx + 3).trim();
        int dash = s.lastIndexOf('-');
        if (dash > 0 && dash < s.length() - 1) return s.substring(dash + 1).trim();
        return s.trim();
    }

    private static String parseArtistFromFileName(String base) {
        String s = base.replaceFirst("^\\s*\\d{1,3}\\s*[.\\-_、)]\\s*", "");
        int idx = s.lastIndexOf(" - ");
        if (idx > 0) return s.substring(0, idx).trim();
        int dash = s.lastIndexOf('-');
        if (dash > 0 && dash < s.length() - 1) return s.substring(0, dash).trim();
        return "";
    }

    /** 歌词文件名 `歌名-歌手-歌词.lrc` → 歌名 */
    private static String parseLyricTitle(String fileName) {
        String s = stripExt(fileName).replaceAll("\\s*[-–—－]?\\s*歌词\\s*$", "").trim();
        int idx = s.indexOf('-');
        return idx > 0 ? s.substring(0, idx).trim() : s;
    }

    /** 归一化：去括号内容、去空格标点、小写（用于歌名比对） */
    private static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("（[^）]*）|\\([^)]*\\)|【[^】]*】|\\[[^]]*]", "")
                .replaceAll("[\\s\\-_·.,:;!?'\"“”‘’&+~`|/\\\\]", "")
                .trim();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return "";
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static void addSample(List<Map<String, Object>> samples, String action, String rel, String extra) {
        if (samples.size() >= MAX_SAMPLES) return;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("action", action);
        m.put("file", rel);
        if (extra != null && !extra.isBlank()) m.put("extra", extra);
        samples.add(m);
    }
}
