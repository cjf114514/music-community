package com.music.util;

import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 极简 ID3v2 标签读取器（只读，不依赖第三方库）
 *
 * 为什么自己写？
 *   扫描本地音乐库时要读出歌名/歌手/专辑，而这些信息在文件头部的 ID3 标签里。
 *   常用库（jaudiotagger）需要额外引入依赖；这里只需要 4 个文本帧，
 *   自己解析一百来行就够，还能避免依赖可用性问题。
 *
 * 支持：ID3v2.3 / ID3v2.4（覆盖绝大多数 mp3）
 * 不处理：ID3v2.2（90 年代格式）、ID3v1（文件末尾，只有 ASCII/GBK 纯文本）、封面图
 *
 * 帧：TIT2 标题 / TPE1 歌手 / TALB 专辑 / TCON 流派
 */
public final class Id3TagReader {

    private Id3TagReader() {
    }

    private static final int MAX_TAG_BYTES = 2 * 1024 * 1024;   // 标签一般几十 KB，2MB 足够

    /**
     * 读取 ID3v2 标签
     *
     * @return 键为 title / artist / album / genre 的 Map；读不到时返回空 Map（不会返回 null）
     */
    public static Map<String, String> read(Path file) {
        Map<String, String> result = new HashMap<>();
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long length = raf.length();
            if (length < 10) return result;

            byte[] header = new byte[10];
            raf.readFully(header);
            if (header[0] != 'I' || header[1] != 'D' || header[2] != '3') return result;

            int major = header[3] & 0xff;
            if (major != 3 && major != 4) return result;   // 只处理 v2.3 / v2.4

            int tagSize = syncsafeToInt(header, 6);
            int toRead = (int) Math.min(Math.min(tagSize, length - 10), MAX_TAG_BYTES);
            if (toRead <= 0) return result;

            byte[] buf = new byte[toRead];
            raf.readFully(buf);

            parseFrames(buf, major, result);
        } catch (Exception e) {
            // 单个文件解析失败不应该中断整个扫描
        }
        return result;
    }

    private static void parseFrames(byte[] buf, int major, Map<String, String> out) {
        int pos = 0;
        while (pos + 10 <= buf.length) {
            String id = new String(buf, pos, 4, StandardCharsets.ISO_8859_1);
            if (!id.chars().allMatch(c -> (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))) break;

            int size = (major == 4) ? syncsafeToInt(buf, pos + 4) : uint32BE(buf, pos + 4);
            if (size <= 0) break;

            int dataStart = pos + 10;
            int dataEnd = Math.min(dataStart + size, buf.length);
            if (dataEnd <= dataStart) break;

            if (id.equals("TIT2") || id.equals("TPE1") || id.equals("TALB") || id.equals("TCON")) {
                int encoding = buf[dataStart] & 0xff;
                String text = decodeText(buf, dataStart + 1, dataEnd, encoding);
                if (!text.isEmpty()) {
                    switch (id) {
                        case "TIT2" -> out.put("title", text);
                        case "TPE1" -> out.put("artist", text);
                        case "TALB" -> out.put("album", text);
                        case "TCON" -> out.put("genre", stripGenreNumber(text));
                        default -> { }
                    }
                }
            }
            pos = dataStart + size;
        }
    }

    /** TCON 有时是 "(17)Rock" 这种旧写法，去掉前缀数字 */
    private static String stripGenreNumber(String s) {
        return s.replaceAll("^\\(\\d+\\)", "").trim();
    }

    private static String decodeText(byte[] buf, int start, int end, int encoding) {
        if (end <= start) return "";
        Charset charset = switch (encoding) {
            case 1 -> StandardCharsets.UTF_16;      // 带 BOM
            case 2 -> StandardCharsets.UTF_16BE;    // 无 BOM
            case 3 -> StandardCharsets.UTF_8;
            default -> StandardCharsets.ISO_8859_1;
        };
        String text = new String(buf, start, end - start, charset);
        return text.replace("\u0000", "").trim();
    }

    /** ID3 的长度字段是 syncsafe：每字节只用低 7 位 */
    private static int syncsafeToInt(byte[] b, int offset) {
        return (b[offset] & 0x7f) << 21
                | (b[offset + 1] & 0x7f) << 14
                | (b[offset + 2] & 0x7f) << 7
                | (b[offset + 3] & 0x7f);
    }

    private static int uint32BE(byte[] b, int offset) {
        return ((b[offset] & 0xff) << 24)
                | ((b[offset + 1] & 0xff) << 16)
                | ((b[offset + 2] & 0xff) << 8)
                | (b[offset + 3] & 0xff);
    }
}
