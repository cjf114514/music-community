package com.music.service;

import com.music.entity.Singer;
import com.music.entity.Song;
import com.music.repository.SingerRepository;
import com.music.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

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
}
