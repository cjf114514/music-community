package com.music.controller;

import com.music.common.Result;
import com.music.entity.User;
import com.music.entity.Playlist;
import com.music.entity.Song;
import com.music.service.FollowService;
import com.music.service.SocialService;
import com.music.service.NotificationService;
import com.music.repository.UserRepository;
import com.music.repository.PlaylistRepository;
import com.music.repository.SongRepository;
import com.music.repository.UserBlockRepository;
import com.music.entity.UserBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {
    private final FollowService followService;
    private final SocialService socialService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;
    private final SongRepository songRepository;
    private final UserBlockRepository userBlockRepository;

    @PostMapping("/block/{userId}")
    public Result<Void> blockUser(@PathVariable Long userId, HttpServletRequest request) {
        Long myId = (Long) request.getAttribute("userId");
        if (myId.equals(userId)) return Result.success();
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(myId, userId)) {
            UserBlock block = new UserBlock();
            block.setBlockerId(myId);
            block.setBlockedId(userId);
            userBlockRepository.save(block);
        }
        followService.unfollow(myId, userId);
        return Result.success();
    }

    @DeleteMapping("/block/{userId}")
    public Result<Void> unblockUser(@PathVariable Long userId, HttpServletRequest request) {
        Long myId = (Long) request.getAttribute("userId");
        userBlockRepository.deleteByBlockerIdAndBlockedId(myId, userId);
        return Result.success();
    }

    @GetMapping("/block/status/{userId}")
    public Result<Boolean> isBlocked(@PathVariable Long userId, HttpServletRequest request) {
        Long myId = (Long) request.getAttribute("userId");
        return Result.success(userBlockRepository.existsByBlockerIdAndBlockedId(myId, userId));
    }

    @PostMapping("/follow/{userId}")
    public Result<Void> follow(@PathVariable Long userId, HttpServletRequest request) {
        Long followerId = (Long) request.getAttribute("userId");
        followService.follow(followerId, userId);
        User follower = userRepository.findById(followerId).orElse(null);
        String nickname = follower != null ? (follower.getNickname() != null ? follower.getNickname() : follower.getUsername()) : "用户";
        notificationService.sendNotification(userId, followerId, "FOLLOW", followerId, nickname + " 关注了你");
        return Result.success();
    }

    @DeleteMapping("/follow/{userId}")
    public Result<Void> unfollow(@PathVariable Long userId, HttpServletRequest request) {
        Long followerId = (Long) request.getAttribute("userId");
        followService.unfollow(followerId, userId);
        return Result.success();
    }

    @GetMapping("/follow/status/{userId}")
    public Result<Boolean> isFollowing(@PathVariable Long userId, HttpServletRequest request) {
        Long followerId = (Long) request.getAttribute("userId");
        return Result.success(followService.isFollowing(followerId, userId));
    }

    @GetMapping("/follow/stats/{userId}")
    public Result<Map<String, Object>> getFollowStats(@PathVariable Long userId) {
        return Result.success(followService.getFollowStats(userId));
    }

    @GetMapping("/followers/{userId}")
    public Result<List<Map<String, Object>>> getFollowers(@PathVariable Long userId) {
        return Result.success(followService.getFollowers(userId));
    }

    @GetMapping("/following/{userId}")
    public Result<List<Map<String, Object>>> getFollowing(@PathVariable Long userId) {
        return Result.success(followService.getFollowing(userId));
    }

    @PostMapping("/playlist/share/{playlistId}")
    public Result<Map<String, Object>> sharePlaylist(@PathVariable Long playlistId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var share = socialService.sharePlaylist(playlistId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("shareToken", share.getShareToken());
        result.put("shareCount", share.getShareCount());
        return Result.success(result);
    }

    @GetMapping("/playlist/shared/{shareToken}")
    public Result<Map<String, Object>> getSharedPlaylist(@PathVariable String shareToken) {
        var share = socialService.getSharedPlaylist(shareToken);
        Playlist playlist = playlistRepository.findById(share.getPlaylistId())
            .orElseThrow(() -> new RuntimeException("歌单不存在"));
        User user = userRepository.findById(share.getUserId())
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        Map<String, Object> result = new HashMap<>();
        result.put("id", playlist.getId());
        result.put("name", playlist.getName());
        result.put("description", playlist.getDescription());
        result.put("coverUrl", playlist.getCoverUrl());
        result.put("songCount", playlist.getSongCount());
        result.put("sharerNickname", user.getNickname());
        result.put("sharerAvatar", user.getAvatar());
        result.put("shareCount", share.getShareCount());
        return Result.success(result);
    }

    @PostMapping("/song/share/{songId}")
    public Result<Map<String, Object>> shareSong(@PathVariable Long songId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var share = socialService.shareSong(songId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("shareToken", share.getShareToken());
        result.put("shareCount", share.getShareCount());
        return Result.success(result);
    }

    @GetMapping("/song/shared/{shareToken}")
    public Result<Map<String, Object>> getSharedSong(@PathVariable String shareToken) {
        var share = socialService.getSharedSong(shareToken);
        Song song = songRepository.findById(share.getSongId())
            .orElseThrow(() -> new RuntimeException("歌曲不存在"));
        User user = userRepository.findById(share.getUserId())
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        Map<String, Object> result = new HashMap<>();
        result.put("id", song.getId());
        result.put("name", song.getName());
        result.put("singer", song.getSinger());
        result.put("album", song.getAlbum());
        result.put("coverUrl", song.getCoverUrl());
        result.put("url", song.getUrl());
        result.put("sharerNickname", user.getNickname());
        result.put("sharerAvatar", user.getAvatar());
        result.put("shareCount", share.getShareCount());
        return Result.success(result);
    }
}
