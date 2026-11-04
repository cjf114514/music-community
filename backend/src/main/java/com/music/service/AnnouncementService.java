package com.music.service;

import com.music.entity.Announcement;
import com.music.entity.Notification;
import com.music.entity.User;
import com.music.repository.AnnouncementRepository;
import com.music.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public Page<Announcement> getAnnouncements(Pageable pageable) {
        Page<Announcement> page = announcementRepository.findAllByOrderByIsTopDescCreateTimeDesc(pageable);
        for (Announcement a : page.getContent()) {
            fillCreator(a);
        }
        return page;
    }

    public Announcement getAnnouncement(Long id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公告不存在"));
        fillCreator(a);
        return a;
    }

    @Transactional
    public Announcement createAnnouncement(String title, String content, String type, Boolean isTop, Long creatorId) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setContent(content);
        a.setType(type != null ? type : "system");
        a.setIsTop(isTop != null ? isTop : false);
        a.setStatus("published");
        a.setCreatorId(creatorId);
        Announcement saved = announcementRepository.save(a);

        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (!user.getId().equals(creatorId)) {
                notificationService.sendNotification(user.getId(), creatorId, "announcement", saved.getId(),
                        "系统公告：" + title);
            }
        }
        return saved;
    }

    @Transactional
    public Announcement updateAnnouncement(Long id, String title, String content, String type, Boolean isTop) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公告不存在"));
        if (title != null) a.setTitle(title);
        if (content != null) a.setContent(content);
        if (type != null) a.setType(type);
        if (isTop != null) a.setIsTop(isTop);
        return announcementRepository.save(a);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new RuntimeException("公告不存在");
        }
        announcementRepository.deleteById(id);
    }

    @Transactional
    public Announcement toggleStatus(Long id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公告不存在"));
        a.setStatus("published".equals(a.getStatus()) ? "draft" : "published");
        return announcementRepository.save(a);
    }

    private void fillCreator(Announcement a) {
        User creator = userRepository.findById(a.getCreatorId()).orElse(null);
        if (creator != null) {
            a.setCreatorName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }
    }
}
