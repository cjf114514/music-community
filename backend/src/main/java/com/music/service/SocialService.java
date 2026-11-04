package com.music.service;

import com.music.entity.PlaylistShare;
import com.music.entity.SongShare;
import com.music.repository.PlaylistRepository;
import com.music.repository.PlaylistShareRepository;
import com.music.repository.SongShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialService {
    private final PlaylistShareRepository playlistShareRepository;
    private final PlaylistRepository playlistRepository;
    private final SongShareRepository songShareRepository;

    @Transactional
    public PlaylistShare sharePlaylist(Long playlistId, Long userId) {
        var existing = playlistShareRepository.findByPlaylistId(playlistId);
        if (existing.isPresent()) {
            PlaylistShare share = existing.get();
            share.setShareCount(share.getShareCount() + 1);
            return playlistShareRepository.save(share);
        }

        PlaylistShare share = new PlaylistShare();
        share.setPlaylistId(playlistId);
        share.setUserId(userId);
        share.setShareToken(UUID.randomUUID().toString().substring(0, 8));
        share.setShareCount(1);
        return playlistShareRepository.save(share);
    }

    public PlaylistShare getSharedPlaylist(String shareToken) {
        return playlistShareRepository.findByShareToken(shareToken)
            .orElseThrow(() -> new RuntimeException("分享链接无效"));
    }

    @Transactional
    public SongShare shareSong(Long songId, Long userId) {
        var existing = songShareRepository.findBySongId(songId);
        if (existing.isPresent()) {
            SongShare share = existing.get();
            share.setShareCount(share.getShareCount() + 1);
            return songShareRepository.save(share);
        }

        SongShare share = new SongShare();
        share.setSongId(songId);
        share.setUserId(userId);
        share.setShareToken(UUID.randomUUID().toString().substring(0, 8));
        share.setShareCount(1);
        return songShareRepository.save(share);
    }

    public SongShare getSharedSong(String shareToken) {
        return songShareRepository.findByShareToken(shareToken)
            .orElseThrow(() -> new RuntimeException("分享链接无效"));
    }
}
