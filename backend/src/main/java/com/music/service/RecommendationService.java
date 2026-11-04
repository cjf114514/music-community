package com.music.service;

import com.music.entity.Favorite;
import com.music.entity.PlayHistory;
import com.music.entity.Song;
import com.music.repository.FavoriteRepository;
import com.music.repository.PlayHistoryRepository;
import com.music.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final PlayHistoryRepository playHistoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final SongRepository songRepository;

    private static final double W_GENRE = 6.0;
    private static final double W_SINGER = 3.0;
    private static final double W_ALBUM = 2.5;
    private static final double W_ITEM_CF = 7.0;
    private static final double W_RECENCY = 1.5;
    private static final double W_POPULARITY = 0.8;
    private static final double EPSILON = 0.15;

    public List<Song> getDailyRecommendations(Long userId, int limit) {
        if (userId == null) {
            return songRepository.findRandomSongs(limit);
        }

        List<PlayHistory> history = playHistoryRepository.findRecentByUserId(userId, 200);
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        Set<Long> listenedSongIds = history.stream()
                .map(PlayHistory::getSongId)
                .collect(Collectors.toSet());

        Set<Long> favoriteSongIds = favorites.stream()
                .map(Favorite::getSongId)
                .collect(Collectors.toSet());

        Map<Long, Double> songScores = new HashMap<>();

        scoreByGenrePreference(history, favoriteSongIds, listenedSongIds, songScores);
        scoreBySingerPreference(history, favoriteSongIds, listenedSongIds, songScores);
        scoreByAlbumPreference(history, favoriteSongIds, listenedSongIds, songScores);
        scoreByItemCF(history, listenedSongIds, songScores);
        scoreByRecency(history, songScores);
        scoreByPopularity(songScores);

        List<Song> recommended = songScores.entrySet().stream()
                .filter(e -> !listenedSongIds.contains(e.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> songRepository.findById(entry.getKey()).orElse(null))
                .filter(Objects::nonNull)
                .filter(song -> song.getStatus() != null && song.getStatus() == 1)
                .collect(Collectors.toList());

        if (Math.random() < EPSILON) {
            int exploreCount = Math.max(1, (int) (limit * EPSILON));
            Set<Long> recommendedIds = recommended.stream().map(Song::getId).collect(Collectors.toSet());
            List<Song> randomPool = songRepository.findRandomSongs(exploreCount * 5);
            int added = 0;
            for (Song s : randomPool) {
                if (!listenedSongIds.contains(s.getId()) && !recommendedIds.contains(s.getId())
                        && s.getStatus() != null && s.getStatus() == 1) {
                    int insertPos = Math.min(added, recommended.size());
                    recommended.add(insertPos, s);
                    recommendedIds.add(s.getId());
                    added++;
                    if (added >= exploreCount) break;
                }
            }
        }

        if (recommended.size() < limit) {
            int remaining = limit - recommended.size();
            Set<Long> recommendedIds = recommended.stream().map(Song::getId).collect(Collectors.toSet());
            recommendedIds.addAll(listenedSongIds);

            List<Song> randomSongs = songRepository.findRandomSongs(remaining * 3);
            for (Song song : randomSongs) {
                if (!recommendedIds.contains(song.getId()) && song.getStatus() != null && song.getStatus() == 1) {
                    recommended.add(song);
                    recommendedIds.add(song.getId());
                    if (recommended.size() >= limit) break;
                }
            }
        }

        return recommended;
    }

    private void scoreByGenrePreference(List<PlayHistory> history, Set<Long> favoriteSongIds,
                                         Set<Long> listenedSongIds, Map<Long, Double> songScores) {
        Map<String, Double> genreWeights = new HashMap<>();

        for (PlayHistory ph : history) {
            Song song = songRepository.findById(ph.getSongId()).orElse(null);
            if (song == null || song.getGenre() == null) continue;
            double w = favoriteSongIds.contains(ph.getSongId()) ? 2.5 : 1.0;
            genreWeights.merge(song.getGenre(), w, Double::sum);
        }

        List<Map.Entry<String, Double>> topGenres = genreWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(8)
                .collect(Collectors.toList());

        if (topGenres.isEmpty()) return;

        double maxWeight = topGenres.get(0).getValue();

        for (Map.Entry<String, Double> genreEntry : topGenres) {
            List<Song> songs = songRepository.findByGenreContainingAndStatus(genreEntry.getKey(), 1);
            double normalizedWeight = (genreEntry.getValue() / maxWeight) * W_GENRE;
            for (Song s : songs) {
                if (!listenedSongIds.contains(s.getId())) {
                    songScores.merge(s.getId(), normalizedWeight, Double::sum);
                }
            }
        }
    }

    private void scoreBySingerPreference(List<PlayHistory> history, Set<Long> favoriteSongIds,
                                          Set<Long> listenedSongIds, Map<Long, Double> songScores) {
        Map<String, Double> singerWeights = new HashMap<>();

        for (PlayHistory ph : history) {
            Song song = songRepository.findById(ph.getSongId()).orElse(null);
            if (song == null || song.getSinger() == null) continue;
            double w = favoriteSongIds.contains(ph.getSongId()) ? 2.0 : 1.0;
            singerWeights.merge(song.getSinger(), w, Double::sum);
        }

        List<Map.Entry<String, Double>> topSingers = singerWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        if (topSingers.isEmpty()) return;

        double maxWeight = topSingers.get(0).getValue();

        for (Map.Entry<String, Double> singerEntry : topSingers) {
            List<Song> songs = songRepository.findBySingerContainingAndStatus(singerEntry.getKey(), 1);
            double normalizedWeight = (singerEntry.getValue() / maxWeight) * W_SINGER;
            for (Song s : songs) {
                if (!listenedSongIds.contains(s.getId())) {
                    songScores.merge(s.getId(), normalizedWeight, Double::sum);
                }
            }
        }
    }

    private void scoreByAlbumPreference(List<PlayHistory> history, Set<Long> favoriteSongIds,
                                         Set<Long> listenedSongIds, Map<Long, Double> songScores) {
        Map<String, Double> albumWeights = new HashMap<>();

        for (PlayHistory ph : history) {
            Song song = songRepository.findById(ph.getSongId()).orElse(null);
            if (song == null || song.getAlbum() == null) continue;
            double w = favoriteSongIds.contains(ph.getSongId()) ? 2.0 : 1.0;
            albumWeights.merge(song.getAlbum(), w, Double::sum);
        }

        List<Map.Entry<String, Double>> topAlbums = albumWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        if (topAlbums.isEmpty()) return;

        double maxWeight = topAlbums.get(0).getValue();

        for (Map.Entry<String, Double> albumEntry : topAlbums) {
            List<Song> songs = songRepository.findByAlbumContainingAndStatus(albumEntry.getKey(), 1);
            double normalizedWeight = (albumEntry.getValue() / maxWeight) * W_ALBUM;
            for (Song s : songs) {
                if (!listenedSongIds.contains(s.getId())) {
                    songScores.merge(s.getId(), normalizedWeight, Double::sum);
                }
            }
        }
    }

    private void scoreByItemCF(List<PlayHistory> userHistory, Set<Long> listenedSongIds,
                                Map<Long, Double> songScores) {
        Set<Long> userSongSet = userHistory.stream()
                .map(PlayHistory::getSongId)
                .collect(Collectors.toSet());

        if (userSongSet.size() < 2) return;

        Map<Long, Map<Long, Integer>> songCoOccurrence = new HashMap<>();
        Map<Long, Integer> songTotalCount = new HashMap<>();

        List<PlayHistory> allHistory = playHistoryRepository.findAllByOrderByPlayTimeDesc();
        Map<Long, List<Long>> userSongLists = allHistory.stream()
                .collect(Collectors.groupingBy(
                        PlayHistory::getUserId,
                        Collectors.mapping(PlayHistory::getSongId, Collectors.toList())
                ));

        for (List<Long> songs : userSongLists.values()) {
            Set<Long> uniqueSongs = new HashSet<>(songs);
            for (Long s1 : uniqueSongs) {
                songTotalCount.merge(s1, 1, Integer::sum);
                for (Long s2 : uniqueSongs) {
                    if (!s1.equals(s2)) {
                        songCoOccurrence.computeIfAbsent(s1, k -> new HashMap<>())
                                .merge(s2, 1, Integer::sum);
                    }
                }
            }
        }

        for (Long listenedId : userSongSet) {
            Map<Long, Integer> coSongs = songCoOccurrence.get(listenedId);
            if (coSongs == null) continue;

            int totalCount = songTotalCount.getOrDefault(listenedId, 1);

            for (Map.Entry<Long, Integer> coEntry : coSongs.entrySet()) {
                Long candidateId = coEntry.getKey();
                if (listenedSongIds.contains(candidateId)) continue;

                double similarity = (double) coEntry.getValue() / totalCount;
                if (similarity > 0.01) {
                    songScores.merge(candidateId, similarity * W_ITEM_CF, Double::sum);
                }
            }
        }
    }

    private void scoreByRecency(List<PlayHistory> history, Map<Long, Double> songScores) {
        LocalDateTime now = LocalDateTime.now();

        Map<String, Double> recentGenreBonus = new HashMap<>();
        Map<String, Double> recentSingerBonus = new HashMap<>();

        for (PlayHistory ph : history) {
            if (ph.getPlayTime() == null) continue;
            long daysAgo = ChronoUnit.DAYS.between(ph.getPlayTime(), now);
            double recencyFactor = Math.max(0, 1.0 - daysAgo / 30.0);

            Song song = songRepository.findById(ph.getSongId()).orElse(null);
            if (song == null) continue;

            if (song.getGenre() != null) {
                recentGenreBonus.merge(song.getGenre(), recencyFactor, Double::sum);
            }
            if (song.getSinger() != null) {
                recentSingerBonus.merge(song.getSinger(), recencyFactor * 0.5, Double::sum);
            }
        }

        for (Map.Entry<String, Double> entry : recentGenreBonus.entrySet()) {
            List<Song> songs = songRepository.findByGenreContainingAndStatus(entry.getKey(), 1);
            double bonus = Math.min(entry.getValue(), W_RECENCY);
            for (Song s : songs) {
                songScores.merge(s.getId(), bonus, Double::sum);
            }
        }

        for (Map.Entry<String, Double> entry : recentSingerBonus.entrySet()) {
            List<Song> songs = songRepository.findBySingerContainingAndStatus(entry.getKey(), 1);
            double bonus = Math.min(entry.getValue(), W_RECENCY * 0.5);
            for (Song s : songs) {
                songScores.merge(s.getId(), bonus, Double::sum);
            }
        }
    }

    private void scoreByPopularity(Map<Long, Double> songScores) {
        for (Map.Entry<Long, Double> entry : songScores.entrySet()) {
            Song song = songRepository.findById(entry.getKey()).orElse(null);
            if (song == null) continue;
            long playCount = song.getPlayCount() != null ? song.getPlayCount() : 0;
            double popularityBonus = Math.min(W_POPULARITY, Math.log10(playCount + 1) * 0.2);
            entry.setValue(entry.getValue() + popularityBonus);
        }
    }

    public List<Song> getSimilarSongs(Long songId, Long userId, int limit) {
        Song target = songRepository.findById(songId).orElse(null);
        if (target == null) return List.of();

        Map<Long, Double> scores = new HashMap<>();

        if (target.getGenre() != null) {
            List<Song> sameGenre = songRepository.findByGenreContainingAndStatus(target.getGenre(), 1);
            for (Song s : sameGenre) {
                if (!s.getId().equals(songId)) {
                    scores.merge(s.getId(), 6.0, Double::sum);
                }
            }
        }

        if (target.getSinger() != null) {
            List<Song> sameSinger = songRepository.findBySingerContainingAndStatus(target.getSinger(), 1);
            for (Song s : sameSinger) {
                if (!s.getId().equals(songId)) {
                    scores.merge(s.getId(), 4.0, Double::sum);
                }
            }
        }

        if (target.getAlbum() != null) {
            List<Song> sameAlbum = songRepository.findByAlbumContainingAndStatus(target.getAlbum(), 1);
            for (Song s : sameAlbum) {
                if (!s.getId().equals(songId)) {
                    scores.merge(s.getId(), 3.0, Double::sum);
                }
            }
        }

        List<PlayHistory> allHistory = playHistoryRepository.findAllByOrderByPlayTimeDesc();
        Set<Long> coListeners = new HashSet<>();
        for (PlayHistory ph : allHistory) {
            if (ph.getSongId().equals(songId)) {
                coListeners.add(ph.getUserId());
            }
        }

        Map<Long, Double> coListenScores = new HashMap<>();
        for (PlayHistory ph : allHistory) {
            if (coListeners.contains(ph.getUserId()) && !ph.getSongId().equals(songId)) {
                coListenScores.merge(ph.getSongId(), 1.0, Double::sum);
            }
        }

        double maxCoScore = coListenScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        for (Map.Entry<Long, Double> entry : coListenScores.entrySet()) {
            Song s = songRepository.findById(entry.getKey()).orElse(null);
            if (s != null && s.getStatus() != null && s.getStatus() == 1) {
                scores.merge(entry.getKey(), (entry.getValue() / maxCoScore) * 5.0, Double::sum);
            }
        }

        if (userId != null) {
            Set<Long> listened = playHistoryRepository.findRecentByUserId(userId, 200).stream()
                    .map(PlayHistory::getSongId).collect(Collectors.toSet());
            listened.forEach(scores::remove);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> songRepository.findById(entry.getKey()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getTasteAnalysis(Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (userId == null) {
            result.put("totalPlays", 0);
            result.put("totalFavorites", 0);
            result.put("topSingers", List.of());
            result.put("topAlbums", List.of());
            result.put("topGenres", List.of());
            result.put("tasteTags", List.of());
            result.put("playDistribution", List.of());
            return result;
        }

        List<PlayHistory> history = playHistoryRepository.findRecentByUserId(userId, 500);
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        result.put("totalPlays", history.size());
        result.put("totalFavorites", favorites.size());

        Map<String, Long> singerPlayCount = new HashMap<>();
        Map<String, Long> albumPlayCount = new HashMap<>();
        Map<String, Long> genrePlayCount = new HashMap<>();
        Map<Integer, Long> hourDistribution = new HashMap<>();

        for (PlayHistory ph : history) {
            Song song = songRepository.findById(ph.getSongId()).orElse(null);
            if (song == null) continue;
            if (song.getSinger() != null) {
                singerPlayCount.merge(song.getSinger(), 1L, Long::sum);
            }
            if (song.getAlbum() != null) {
                albumPlayCount.merge(song.getAlbum(), 1L, Long::sum);
            }
            if (song.getGenre() != null) {
                genrePlayCount.merge(song.getGenre(), 1L, Long::sum);
            }
            if (ph.getPlayTime() != null) {
                int hour = ph.getPlayTime().getHour();
                hourDistribution.merge(hour, 1L, Long::sum);
            }
        }

        for (Favorite fav : favorites) {
            Song song = songRepository.findById(fav.getSongId()).orElse(null);
            if (song == null) continue;
            if (song.getSinger() != null) {
                singerPlayCount.merge(song.getSinger(), 3L, Long::sum);
            }
            if (song.getAlbum() != null) {
                albumPlayCount.merge(song.getAlbum(), 2L, Long::sum);
            }
            if (song.getGenre() != null) {
                genrePlayCount.merge(song.getGenre(), 2L, Long::sum);
            }
        }

        List<Map<String, Object>> topSingers = singerPlayCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("topSingers", topSingers);

        List<Map<String, Object>> topAlbums = albumPlayCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("topAlbums", topAlbums);

        List<Map<String, Object>> topGenres = genrePlayCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("topGenres", topGenres);

        List<String> tasteTags = generateTasteTags(singerPlayCount, genrePlayCount, hourDistribution, history.size(), favorites.size());
        result.put("tasteTags", tasteTags);

        List<Map<String, Object>> playDistribution = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> m = new HashMap<>();
            m.put("hour", h);
            m.put("count", hourDistribution.getOrDefault(h, 0L));
            playDistribution.add(m);
        }
        result.put("playDistribution", playDistribution);

        return result;
    }

    private List<String> generateTasteTags(Map<String, Long> singerPlayCount,
                                            Map<String, Long> genrePlayCount,
                                            Map<Integer, Long> hourDistribution,
                                            int totalPlays, int totalFavorites) {
        List<String> tags = new ArrayList<>();

        if (totalPlays > 100) tags.add("音乐达人");
        else if (totalPlays > 50) tags.add("音乐爱好者");
        else if (totalPlays > 10) tags.add("音乐探索者");

        if (totalFavorites > 20) tags.add("收藏控");
        if (singerPlayCount.size() > 10) tags.add("杂食听众");
        else if (singerPlayCount.size() <= 3 && singerPlayCount.size() > 0) tags.add("专一粉丝");

        if (genrePlayCount.size() >= 1) {
            String topGenre = genrePlayCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse("");
            if (!topGenre.isEmpty()) {
                tags.add(topGenre + "爱好者");
            }
        }

        long nightPlays = hourDistribution.entrySet().stream()
                .filter(e -> e.getKey() >= 22 || e.getKey() < 6)
                .mapToLong(Map.Entry::getValue).sum();
        long morningPlays = hourDistribution.entrySet().stream()
                .filter(e -> e.getKey() >= 6 && e.getKey() < 12)
                .mapToLong(Map.Entry::getValue).sum();

        if (nightPlays > morningPlays * 1.5) tags.add("夜猫子");
        else if (morningPlays > nightPlays * 1.5) tags.add("早起鸟");

        return tags.stream().limit(6).collect(Collectors.toList());
    }
}
