package com.music.controller;

import com.music.annotation.OperationLogAnnotation;
import com.music.annotation.RequireAdmin;
import com.music.common.Result;
import com.music.entity.*;
import com.music.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;
    private final PlayHistoryRepository playHistoryRepository;
    private final SingerRepository singerRepository;
    private final CommentRepository commentRepository;
    private final CommunityPostRepository communityPostRepository;

    @GetMapping("/dashboard")
    @RequireAdmin
    public Result<Map<String, Object>> getDashboard() {
        Map<String, Object> data = new HashMap<>();

        long userCount = userRepository.count();
        long songCount = songRepository.count();
        long playlistCount = playlistRepository.count();
        long singerCount = singerRepository.count();
        long commentCount = commentRepository.count();
        long postCount = communityPostRepository.count();

        data.put("userCount", userCount);
        data.put("songCount", songCount);
        data.put("playlistCount", playlistCount);
        data.put("singerCount", singerCount);
        data.put("commentCount", commentCount);
        data.put("postCount", postCount);

        LocalDate today = LocalDate.now();
        List<PlayHistory> allHistories = playHistoryRepository.findAll();
        long todayPlayCount = allHistories.stream()
                .filter(h -> h.getPlayTime() != null && h.getPlayTime().toLocalDate().equals(today))
                .count();
        data.put("todayPlayCount", todayPlayCount);

        long totalPlayCount = songRepository.findAll().stream()
                .mapToLong(s -> s.getPlayCount() != null ? s.getPlayCount() : 0L)
                .sum();
        data.put("totalPlayCount", totalPlayCount);

        long newUsersToday = userRepository.findAll().stream()
                .filter(u -> u.getCreateTime() != null && u.getCreateTime().toLocalDate().equals(today))
                .count();
        data.put("newUsersToday", newUsersToday);

        List<Map<String, Object>> dailyData = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = allHistories.stream()
                    .filter(h -> h.getPlayTime() != null && h.getPlayTime().toLocalDate().equals(date))
                    .count();
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("count", count);
            dailyData.add(dayData);
        }
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("dailyPlays", dailyData);
        data.put("chartData", chartData);

        List<Song> topSongsList = songRepository.findAllByOrderByPlayCountDesc(PageRequest.of(0, 10)).getContent();
        List<Map<String, Object>> topSongs = topSongsList.stream().map(song -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", song.getId());
            map.put("name", song.getName());
            map.put("singer", song.getSinger());
            map.put("coverUrl", song.getCoverUrl());
            map.put("playCount", song.getPlayCount() != null ? song.getPlayCount() : 0L);
            return map;
        }).toList();
        data.put("topSongs", topSongs);

        List<Map<String, Object>> activities = new ArrayList<>();
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        List<User> recentUsers = userRepository.findAll().stream()
                .filter(u -> u.getCreateTime() != null && u.getCreateTime().isAfter(weekAgo))
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .limit(3)
                .toList();
        for (User u : recentUsers) {
            Map<String, Object> act = new HashMap<>();
            act.put("text", "新用户 " + (u.getNickname() != null ? u.getNickname() : u.getUsername()) + " 注册了账号");
            act.put("time", formatRelativeTime(u.getCreateTime()));
            act.put("color", "#667eea");
            activities.add(act);
        }

        List<Song> recentSongs = songRepository.findAll().stream()
                .filter(s -> s.getCreateTime() != null && s.getCreateTime().isAfter(weekAgo))
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .limit(3)
                .toList();
        for (Song s : recentSongs) {
            Map<String, Object> act = new HashMap<>();
            act.put("text", "新歌曲《" + s.getName() + "》上架");
            act.put("time", formatRelativeTime(s.getCreateTime()));
            act.put("color", "#f5576c");
            activities.add(act);
        }

        List<CommunityPost> recentPosts = communityPostRepository.findAllByOrderByCreateTimeDesc(PageRequest.of(0, 3));
        for (CommunityPost p : recentPosts) {
            User author = userRepository.findById(p.getUserId()).orElse(null);
            String name = author != null ? (author.getNickname() != null ? author.getNickname() : author.getUsername()) : "未知用户";
            Map<String, Object> act = new HashMap<>();
            act.put("text", name + " 发布了帖子「" + p.getTitle() + "」");
            act.put("time", formatRelativeTime(p.getCreateTime()));
            act.put("color", "#4facfe");
            activities.add(act);
        }

        activities.sort((a, b) -> {
            String tA = (String) a.get("time");
            String tB = (String) b.get("time");
            return tB.compareTo(tA);
        });
        data.put("recentActivities", activities.stream().limit(8).toList());

        return Result.success(data);
    }

    @GetMapping("/users")
    @RequireAdmin
    public Result<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {

        List<User> allUsers;
        if (keyword != null && !keyword.isEmpty()) {
            allUsers = userRepository.findAll().stream()
                    .filter(u -> u.getUsername().contains(keyword) ||
                            (u.getNickname() != null && u.getNickname().contains(keyword)))
                    .toList();
        } else {
            allUsers = userRepository.findAll();
        }

        if (role != null && !role.isEmpty()) {
            allUsers = allUsers.stream()
                    .filter(u -> role.equals(u.getRole()))
                    .toList();
        }

        int total = allUsers.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<User> users = fromIndex < total ? allUsers.subList(fromIndex, toIndex) : new ArrayList<>();

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("list", users);

        return Result.success(result);
    }

    @PutMapping("/users/{userId}/role")
    @RequireAdmin
    @OperationLogAnnotation(module = "用户管理", action = "UPDATE", description = "修改用户角色", targetType = "user")
    public Result<Void> updateUserRole(@PathVariable Long userId, @RequestBody Map<String, String> params) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String role = params.get("role");
        if (!"ADMIN".equals(role) && !"USER".equals(role)) {
            throw new RuntimeException("无效的角色");
        }

        user.setRole(role);
        userRepository.save(user);
        return Result.success();
    }

    @PutMapping("/users/{userId}/status")
    @RequireAdmin
    @OperationLogAnnotation(module = "用户管理", action = "UPDATE", description = "修改用户状态", targetType = "user")
    public Result<Void> updateUserStatus(@PathVariable Long userId, @RequestBody Map<String, Integer> params) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Integer status = params.get("status");
        if (status != 0 && status != 1) {
            throw new RuntimeException("无效的状态");
        }

        user.setStatus(status);
        userRepository.save(user);
        return Result.success();
    }

    @DeleteMapping("/users/{userId}")
    @RequireAdmin
    @OperationLogAnnotation(module = "用户管理", action = "DELETE", description = "删除用户", targetType = "user")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在");
        }
        userRepository.deleteById(userId);
        return Result.success();
    }

    private String formatRelativeTime(LocalDateTime time) {
        if (time == null) return "未知";
        long minutes = java.time.Duration.between(time, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "刚刚";
        if (minutes < 60) return minutes + "分钟前";
        long hours = minutes / 60;
        if (hours < 24) return hours + "小时前";
        long days = hours / 24;
        if (days < 7) return days + "天前";
        return time.toString().replace('T', ' ').substring(0, 16);
    }
}
