package com.music.service;

import com.music.entity.Favorite;
import com.music.entity.Song;
import com.music.repository.FavoriteRepository;
import com.music.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final SongRepository songRepository;

    @Transactional
    public void toggleFavorite(Long userId, Long songId) {
        favoriteRepository.findByUserIdAndSongId(userId, songId).ifPresentOrElse(
                favoriteRepository::delete,
                () -> {
                    Favorite favorite = new Favorite();
                    favorite.setUserId(userId);
                    favorite.setSongId(songId);
                    favoriteRepository.save(favorite);
                }
        );
    }

    public boolean isFavorite(Long userId, Long songId) {
        return favoriteRepository.existsByUserIdAndSongId(userId, songId);
    }

    public List<Song> getMyFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreateTimeDesc(userId);
        if (favorites.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> songIds = favorites.stream().map(Favorite::getSongId).toList();
        List<Song> songs = songRepository.findAllById(songIds).stream()
                .filter(song -> song.getStatus() != null && song.getStatus() == 1)
                .toList();
        Map<Long, Song> songMap = songs.stream().collect(Collectors.toMap(Song::getId, s -> s));
        return favorites.stream()
                .map(Favorite::getSongId)
                .map(songMap::get)
                .filter(s -> s != null)
                .collect(Collectors.toList());
    }
}
