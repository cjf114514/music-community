package com.music.service;

import com.music.entity.UserFollow;
import com.music.repository.UserFollowRepository;
import com.music.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }
        if (userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new RuntimeException("已关注该用户");
        }
        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        userFollowRepository.save(follow);
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        userFollowRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public Map<String, Object> getFollowStats(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("followerCount", userFollowRepository.countFollowersByUserId(userId));
        result.put("followingCount", userFollowRepository.countFollowingByUserId(userId));
        return result;
    }

    public List<Map<String, Object>> getFollowers(Long userId) {
        List<UserFollow> follows = userFollowRepository.findByFollowingId(userId);
        List<Long> userIds = follows.stream().map(UserFollow::getFollowerId).collect(Collectors.toList());
        return userRepository.findAllById(userIds).stream()
            .map(u -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("nickname", u.getNickname());
                m.put("avatar", u.getAvatar());
                m.put("username", u.getUsername());
                return m;
            })
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getFollowing(Long userId) {
        List<UserFollow> follows = userFollowRepository.findByFollowerId(userId);
        List<Long> userIds = follows.stream().map(UserFollow::getFollowingId).collect(Collectors.toList());
        return userRepository.findAllById(userIds).stream()
            .map(u -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("nickname", u.getNickname());
                m.put("avatar", u.getAvatar());
                m.put("username", u.getUsername());
                return m;
            })
            .collect(Collectors.toList());
    }
}
