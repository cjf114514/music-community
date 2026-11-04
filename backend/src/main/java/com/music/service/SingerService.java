package com.music.service;

import com.music.entity.Singer;
import com.music.entity.Song;
import com.music.repository.SingerRepository;
import com.music.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SingerService {
    private final SingerRepository singerRepository;
    private final SongRepository songRepository;

    public List<Singer> getAllSingers() {
        List<Singer> singers = singerRepository.findAllOrderBySongCountDesc();
        singers.forEach(singer -> {
            long count = songRepository.findBySingerContainingAndStatus(singer.getName(), 1).size();
            singer.setSongCount((int) count);
        });
        return singers;
    }

    public Singer getSingerByName(String name) {
        Optional<Singer> singer = singerRepository.findByName(name);
        if (singer.isPresent()) {
            Singer s = singer.get();
            long count = songRepository.findBySingerContainingAndStatus(s.getName(), 1).size();
            s.setSongCount((int) count);
            return s;
        }
        Singer newSinger = new Singer();
        newSinger.setName(name);
        newSinger.setSongCount(0);
        return newSinger;
    }

    public List<Song> getSingerSongs(String singerName) {
        return songRepository.findBySingerContainingAndStatus(singerName, 1);
    }

    public List<Singer> searchSingers(String keyword) {
        return singerRepository.findByNameContaining(keyword, org.springframework.data.domain.PageRequest.of(0, 20)).getContent();
    }

    @Transactional
    public Singer saveSinger(Singer singer) {
        return singerRepository.save(singer);
    }
}
