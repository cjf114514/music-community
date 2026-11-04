package com.music.service;

import com.music.entity.Playlist;
import com.music.entity.PlaylistSong;
import com.music.entity.Song;
import com.music.repository.PlaylistRepository;
import com.music.repository.PlaylistSongRepository;
import com.music.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 歌单服务 — 处理歌单的增删改查和歌曲排序
 *
 * ========== @Transactional 是什么？==========
 * 标注在方法上,表示这个方法内的数据库操作在一个"事务"中执行
 *
 * 事务 = 一组操作要么全部成功,要么全部回滚
 * 例如 addSongToPlaylist:
 *   1. 往 playlist_song 表插入一条记录
 *   2. 更新 playlist 表的 song_count
 *   如果第 2 步失败了,第 1 步会自动回滚 → 不会出现数据不一致
 *
 * 为什么只在"写"方法上加 @Transactional？
 *   "读"方法 (如 getPublicPlaylists) 不需要事务,加了反而浪费资源
 */
@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;

    // ==================== 查询 ====================

    /** 获取所有公开歌单 */
    public List<Playlist> getPublicPlaylists() {
        return playlistRepository.findByIsPublicAndStatus(1, 1);
    }

    /**
     * 按分类筛选公开歌单
     * 如果 category 为空 → 返回所有公开歌单
     * 否则 → 只返回指定分类的歌单
     */
    public List<Playlist> getPublicPlaylistsByCategory(String category) {
        if (category == null || category.isEmpty()) {
            return playlistRepository.findByIsPublicAndStatus(1, 1);
        }
        return playlistRepository.findByCategoryAndStatus(category, 1);
    }

    /** 获取所有已有的歌单分类列表 (去重) */
    public List<String> getCategories() {
        return playlistRepository.findDistinctCategories();
    }

    /** 获取某个用户的所有歌单 */
    public List<Playlist> getUserPlaylists(Long userId) {
        return playlistRepository.findByUserIdAndStatus(userId, 1);
    }

    /** 根据 ID 获取单个歌单 */
    public Playlist getPlaylistById(Long id) {
        return playlistRepository.findById(id).orElse(null);
    }

    /** 关键词搜索歌单 */
    public List<Playlist> searchPlaylists(String keyword) {
        return playlistRepository.searchPlaylists(keyword);
    }

    /**
     * 获取歌单中的所有歌曲 (按 sortOrder 排序)
     *
     * 实现思路:
     *   1. 从 playlist_song 表查歌曲 ID 列表 (已按 sortOrder 排序)
     *   2. 批量查出所有歌曲
     *   3. 过滤掉已下架的歌曲 (status != 1)
     *   4. 按 sortOrder 排序 (保证顺序不变)
     *
     * 为什么分批查而不是 JOIN？
     *   JPA 的关联查询比较麻烦,分批查更直观
     *   缺点: 如果歌单有 1000+ 首歌,IN 查询可能慢 (不过个人使用够用了)
     */
    public List<Song> getPlaylistSongs(Long playlistId) {
        // 第 1 步: 查 playlist_song 表,已按 sortOrder ASC 排序
        List<PlaylistSong> playlistSongs =
                playlistSongRepository.findByPlaylistIdOrderBySortOrder(playlistId);

        if (playlistSongs.isEmpty()) {
            return new ArrayList<>();
        }

        // 第 2 步: 提取所有 songId
        List<Long> songIds = playlistSongs.stream()
                .map(PlaylistSong::getSongId)
                .distinct()
                .toList();

        // 第 3 步: 批量查出歌曲,同时过滤掉已下架的
        List<Song> songs = new ArrayList<>(songRepository.findAllById(songIds).stream()
                .filter(song -> song.getStatus() != null && song.getStatus() == 1)
                .toList());

        // 第 4 步: 按 sortOrder 排序
        // 用 Collectors.toMap 构建 songId → sortOrder 的映射
        // 第 3 个参数 (a, b) -> a: 如果 songId 重复,保留第一个的 sortOrder
        Map<Long, Integer> sortOrderMap = playlistSongs.stream()
                .collect(Collectors.toMap(
                        PlaylistSong::getSongId,
                        PlaylistSong::getSortOrder,
                        (a, b) -> a));

        songs.sort((a, b) -> sortOrderMap.get(a.getId()) - sortOrderMap.get(b.getId()));
        return songs;
    }

    // ==================== 增删改 ====================

    /** 新建或更新歌单 (JPA 根据 ID 是否为 null 自动判断 INSERT 还是 UPDATE) */
    public Playlist savePlaylist(Playlist playlist) {
        return playlistRepository.save(playlist);
    }

    /** 删除歌单 */
    public void deletePlaylist(Long id) {
        playlistRepository.deleteById(id);
    }

    /**
     * 向歌单添加歌曲
     *
     * @Transactional: 保证 插入 playlist_song 和 更新 song_count 要么都成功要么都失败
     *
     * 注意: 如果歌曲已存在 → 直接返回 (不抛异常,幂等操作)
     */
    @Transactional
    public void addSongToPlaylist(Long playlistId, Long songId) {
        // 防止重复添加
        if (playlistSongRepository.findByPlaylistIdAndSongId(playlistId, songId).isPresent()) {
            return;
        }

        // 新增歌曲的 sortOrder = 当前曲目数 (自动放到最后)
        long count = playlistSongRepository.countByPlaylistId(playlistId);

        PlaylistSong playlistSong = new PlaylistSong();
        playlistSong.setPlaylistId(playlistId);
        playlistSong.setSongId(songId);
        playlistSong.setSortOrder((int) count);
        playlistSongRepository.save(playlistSong);

        // 更新歌单的歌曲计数
        Playlist playlist = playlistRepository.findById(playlistId).orElse(null);
        if (playlist != null) {
            playlist.setSongCount((int) playlistSongRepository.countByPlaylistId(playlistId));
            playlistRepository.save(playlist);
        }
    }

    /**
     * 从歌单移除歌曲
     *
     * @Transactional: 保证 删除记录 和 更新 song_count 要么都成功要么都失败
     */
    @Transactional
    public void removeSongFromPlaylist(Long playlistId, Long songId) {
        playlistSongRepository.deleteByPlaylistIdAndSongId(playlistId, songId);

        // 更新歌曲计数
        Playlist playlist = playlistRepository.findById(playlistId).orElse(null);
        if (playlist != null) {
            playlist.setSongCount((int) playlistSongRepository.countByPlaylistId(playlistId));
            playlistRepository.save(playlist);
        }
    }

    /**
     * 歌单歌曲拖拽重新排序
     *
     * @Transactional: 批量更新需要事务保护
     *
     * 实现原理:
     *   前端拖拽后传回新的 songId 顺序列表 [3, 1, 5, 2, ...]
     *   遍历这个列表,依次把 sortOrder 设为对应的 index (0, 1, 2, 3, ...)
     *
     * 性能考虑:
     *   当前实现: 每次拖拽 N 首歌 = N 次 SELECT + N 次 UPDATE
     *   歌单歌曲数量不大时 (< 100 首) 没问题
     *   如果歌单很大,可以改用一次批量 UPDATE 的 SQL
     */
    @Transactional
    public void reorderSongs(Long playlistId, List<Long> songIds) {
        for (int i = 0; i < songIds.size(); i++) {
            Long songId = songIds.get(i);
            final int sortOrder = i;  // 必须用 final 变量,否则 Lambda 表达式中不能用
            playlistSongRepository.findByPlaylistIdAndSongId(playlistId, songId)
                .ifPresent(ps -> {
                    ps.setSortOrder(sortOrder);
                    playlistSongRepository.save(ps);
                });
        }
    }
}
