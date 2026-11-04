package com.music.service;

import com.music.entity.Song;
import com.music.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;

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
        return songRepository.save(song);
    }

    public void deleteSong(Long id) {
        songRepository.deleteById(id);
    }

    public List<Song> getAlbumSongs(String album) {
        return songRepository.findByAlbumContainingAndStatus(album, 1);
    }
}
