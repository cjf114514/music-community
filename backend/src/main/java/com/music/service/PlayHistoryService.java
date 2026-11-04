package com.music.service;

import com.music.entity.PlayHistory;
import com.music.entity.Song;
import com.music.repository.PlayHistoryRepository;
import com.music.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayHistoryService {
    private final PlayHistoryRepository playHistoryRepository;
    private final SongRepository songRepository;

    @Transactional
    public void recordPlay(Long userId, Long songId) {
        PlayHistory history = new PlayHistory();
        history.setUserId(userId);
        history.setSongId(songId);
        playHistoryRepository.save(history);
    }

    public List<Song> getRecentPlays(Long userId, int limit) {
        List<PlayHistory> histories = playHistoryRepository.findRecentByUserId(userId, limit);
        if (histories.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> songIds = histories.stream().map(PlayHistory::getSongId).distinct().toList();
        List<Song> songs = songRepository.findAllById(songIds).stream()
                .filter(song -> song.getStatus() != null && song.getStatus() == 1)
                .toList();
        Map<Long, Song> songMap = songs.stream().collect(Collectors.toMap(Song::getId, s -> s));
        return histories.stream()
                .map(PlayHistory::getSongId)
                .distinct()
                .map(songMap::get)
                .filter(s -> s != null)
                .collect(Collectors.toList());
    }

    public void clearHistory(Long userId) {
        playHistoryRepository.deleteByUserId(userId);
    }

    public List<Song> getRanking(String type, int limit) {
        LocalDateTime startTime;
        switch (type) {
            case "daily":
                startTime = LocalDateTime.now().minusDays(1);
                break;
            case "weekly":
                startTime = LocalDateTime.now().minusWeeks(1);
                break;
            case "total":
            default:
                startTime = LocalDateTime.of(2000, 1, 1, 0, 0);
                break;
        }
        List<Object[]> results = playHistoryRepository.findTopSongs(startTime, limit);
        if (results.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> songIds = results.stream().map(r -> (Long) r[0]).toList();
        List<Song> songs = songRepository.findAllById(songIds).stream()
                .filter(song -> song.getStatus() != null && song.getStatus() == 1)
                .toList();
        Map<Long, Song> songMap = songs.stream().collect(Collectors.toMap(Song::getId, s -> s));
        return results.stream()
                .map(r -> songMap.get((Long) r[0]))
                .filter(s -> s != null)
                .collect(Collectors.toList());
    }
}
