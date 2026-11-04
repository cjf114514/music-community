package com.music.service;

import com.music.entity.SearchHistory;
import com.music.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {
    private final SearchHistoryRepository searchHistoryRepository;

    public void recordSearch(Long userId, String keyword) {
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        searchHistoryRepository.save(history);
    }

    public List<String> getHistory(Long userId, int limit) {
        List<SearchHistory> histories = searchHistoryRepository.findByUserIdOrderBySearchTimeDesc(userId, limit);
        return histories.stream()
                .map(SearchHistory::getKeyword)
                .distinct()
                .collect(Collectors.toList());
    }

    public void deleteHistory(Long userId, String keyword) {
        searchHistoryRepository.deleteByUserIdAndKeyword(userId, keyword);
    }

    public void clearHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }

    public List<String> getHotSearch(int limit) {
        List<Object[]> results = searchHistoryRepository.findHotKeywords(limit);
        return results.stream()
                .map(r -> (String) r[0])
                .collect(Collectors.toList());
    }
}
