package com.music.service;

import com.music.entity.Notification;
import com.music.entity.User;
import com.music.repository.NotificationRepository;
import com.music.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void sendNotification(Long userId, Long actorId, String type, Long targetId, String content) {
        if (userId.equals(actorId)) return; // 不能给自己发送通知
        Notification n = new Notification(); // 创建通知对象
        n.setUserId(userId); // 设置接收用户ID
        n.setActorId(actorId); // 设置发送用户ID
        n.setType(type); // 设置通知类型
        n.setTargetId(targetId); // 设置目标用户ID
        n.setContent(content); // 设置通知内容
        notificationRepository.save(n); // 保存通知
    }

    //获取某个用户的通知列表
    public List<Notification> getNotifications(Long userId) {
        //根据 userId 查询所有通知，按创建时间 最新 → 最早 排序
        List<Notification> list = notificationRepository.findByUserIdOrderByCreateTimeDesc(userId);
        for (Notification n : list) { // 遍历每一条通知
            User actor = userRepository.findById(n.getActorId()).orElse(null); // 查询发送用户
            if (actor != null) { // 如果发送用户存在
                n.setActorNickname(actor.getNickname() != null ? actor.getNickname() : actor.getUsername()); // 设置发送用户昵称（没有昵称就用用户名）
                n.setActorAvatar(actor.getAvatar()); // 设置发送用户头像
            }
        }
        return list; //返回组装好的通知列表给前端
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional //保证一段代码里的多个数据库操作，要么全部成功，要么全部失败。方法里有 【多个数据库操作】 或者 【修改 / 删除数据】时添加
    public void markAllAsRead(Long userId) {
        // 根据 userId 标记所有未读通知为已读
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        // 根据 notificationId 标记通知为已读
        Notification n = notificationRepository.findById(notificationId).orElse(null);
        if (n != null && n.getUserId().equals(userId)) {
            n.setIsRead(true); // 标记为已读
            notificationRepository.save(n); // 保存通知
        }
    }
}
