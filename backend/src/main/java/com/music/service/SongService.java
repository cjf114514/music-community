package com.music.service;

import com.music.entity.Singer;
import com.music.entity.Song;
import com.music.repository.SingerRepository;
import com.music.repository.SongRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;
    private final SingerRepository singerRepository;

    public List<Song> getAllSongs() {
        return songRepository.findByStatus(1);
    }

    public List<Song> searchSongs(String keyword) {
        return songRepository.findByNameContaining(keyword);
    }

    public List<Song> searchAll(String keyword) {
        return songRepository.searchAll(keyword);
    }

    public List<Song> searchBySinger(String singer) {
        return songRepository.findBySingerContainingAndStatus(singer, 1);
    }

    public List<Song> getRandomSongs(int limit) {
        return songRepository.findRandomSongs(limit);
    }

    public Song getSongById(Long id) {
        return songRepository.findById(id).orElse(null);
    }

    public Song saveSong(Song song) {
        Song saved = songRepository.save(song);
        syncSinger(saved.getSinger());
        return saved;
    }

    public void deleteSong(Long id) {
        Song song = songRepository.findById(id).orElse(null);
        songRepository.deleteById(id);
        if (song != null) syncSinger(song.getSinger());
    }

    /**
     * 同步歌手表 —— 创建/编辑/删除歌曲后调用
     *
     * 为什么需要它？
     *   singer 表原本全靠手动维护，而歌曲创建路径完全没写过它，
     *   结果就是「歌声有 100 首，歌手页永远是空的」。
     *   这里在每次歌曲变更后按名字 upsert 一首歌手并刷新作品数，
     *   让歌手列表 / 歌手详情 / 歌手搜索自动有数据。
     *
     * 用 try-catch 包住：歌手同步失败不应该让保存歌曲整体失败。
     */
    private void syncSinger(String singerName) {
        if (singerName == null || singerName.isBlank()) return;
        try {
            String name = singerName.trim();
            Singer singer = singerRepository.findByName(name).orElseGet(() -> {
                Singer s = new Singer();
                s.setName(name);
                return s;
            });
            singer.setSongCount((int) songRepository.countBySinger(name));
            singerRepository.save(singer);
        } catch (Exception e) {
            log.warn("同步歌手失败: {}", singerName, e);
        }
    }

    public List<Song> getAlbumSongs(String album) {
        return songRepository.findByAlbumContainingAndStatus(album, 1);
    }

    /**
     * 智能歌单查询 —— 按条件动态筛选歌曲
     *
     * 为什么用 Specification 而不是写死几个 @Query？
     *   条件是用户在前端组合出来的（流派 + 播放次数区间 + 添加时间 + 排序），
     *   组合数量很大，写死查询会爆炸。Specification 可以按需拼 Predicate。
     *
     * @param genre           流派精确匹配（可空）
     * @param singer          歌手模糊匹配（可空）
     * @param album           专辑模糊匹配（可空）
     * @param minPlay         最小播放次数（可空）
     * @param maxPlay         最大播放次数（传 0 即"从未播放"）
     * @param addedWithinDays 最近 N 天添加（可空）
     * @param order           playCountDesc / playCountAsc / newest / oldest
     * @param limit           最多返回多少首（上限 500，防止一次拉爆前端）
     */
    public List<Song> smartQuery(String genre, String singer, String album,
                                 Long minPlay, Long maxPlay, Integer addedWithinDays,
                                 String order, int limit) {
        Specification<Song> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("status"), 1));   // 只查上架的

            if (genre != null && !genre.isBlank()) ps.add(cb.equal(root.get("genre"), genre));
            if (singer != null && !singer.isBlank()) ps.add(cb.like(root.get("singer"), "%" + singer + "%"));
            if (album != null && !album.isBlank()) ps.add(cb.like(root.get("album"), "%" + album + "%"));
            if (minPlay != null) ps.add(cb.greaterThanOrEqualTo(root.get("playCount"), minPlay));
            if (maxPlay != null) ps.add(cb.lessThanOrEqualTo(root.get("playCount"), maxPlay));
            if (addedWithinDays != null && addedWithinDays > 0) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createTime"),
                        LocalDateTime.now().minusDays(addedWithinDays)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Sort sort = switch (order == null ? "" : order) {
            case "playCountDesc" -> Sort.by(Sort.Direction.DESC, "playCount");
            case "playCountAsc" -> Sort.by(Sort.Direction.ASC, "playCount");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createTime");
            default -> Sort.by(Sort.Direction.DESC, "createTime");
        };

        int pageSize = Math.max(1, Math.min(limit, 500));
        return songRepository.findAll(spec, PageRequest.of(0, pageSize, sort)).getContent();
    }

    /**
     * 批量修改歌曲标签（流派 / 专辑 / 歌手 / 状态）
     *
     * @param ids    要修改的歌曲 id
     * @param fields 只处理非 null 的字段；空字符串表示"清空该字段"
     * @return 实际修改的条数
     */
    public int batchUpdate(List<Long> ids, Map<String, Object> fields) {
        if (ids == null || ids.isEmpty()) return 0;
        List<Song> songs = songRepository.findAllById(ids);
        int changed = 0;
        for (Song song : songs) {
            if (fields.containsKey("genre")) song.setGenre(emptyToNull(fields.get("genre")));
            if (fields.containsKey("album")) song.setAlbum(emptyToNull(fields.get("album")));
            if (fields.containsKey("singer")) song.setSinger(emptyToNull(fields.get("singer")));
            if (fields.containsKey("status") && fields.get("status") != null) {
                song.setStatus(Integer.valueOf(fields.get("status").toString()));
            }
            songRepository.save(song);
            changed++;
            // 歌手改名后统计要跟着更新
            if (fields.containsKey("singer")) syncSinger(song.getSinger());
        }
        return changed;
    }

    private static String emptyToNull(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
