package com.music.service;

import com.music.entity.Singer;
import com.music.entity.Song;
import com.music.repository.SingerRepository;
import com.music.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SongService 单元测试
 *
 * 重点测「保存歌曲时自动维护歌手表」这段逻辑：
 *   这个功能是后来补的 —— 在此之前 singer 表与歌曲创建完全脱钩，
 *   导致「歌有 100 首、歌手页永远是空的」。
 *   用 Mockito 把两个 Repository 换成假的，不碰数据库，秒级跑完。
 */
@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private SingerRepository singerRepository;

    private SongService songService;

    @BeforeEach
    void setUp() {
        songService = new SongService(songRepository, singerRepository);
    }

    private static Song songBy(String singer) {
        Song song = new Song();
        song.setName("晴天");
        song.setSinger(singer);
        return song;
    }

    @Test
    @DisplayName("保存歌曲时，歌手不存在则自动新建并写入作品数")
    void saveSong_createsSingerWhenAbsent() {
        when(songRepository.save(any(Song.class))).thenAnswer(inv -> inv.getArgument(0));
        when(singerRepository.findByName("周杰伦")).thenReturn(Optional.empty());
        when(songRepository.countBySinger("周杰伦")).thenReturn(3L);

        songService.saveSong(songBy("周杰伦"));

        ArgumentCaptor<Singer> captor = ArgumentCaptor.forClass(Singer.class);
        verify(singerRepository).save(captor.capture());

        Singer saved = captor.getValue();
        assertEquals("周杰伦", saved.getName(), "应当新建这个歌手");
        assertEquals(3, saved.getSongCount(), "作品数应取自歌曲表中该歌手的数量");
    }

    @Test
    @DisplayName("歌手已存在时复用原记录，不重复创建")
    void saveSong_reusesExistingSinger() {
        Singer existing = new Singer();
        existing.setName("周杰伦");
        existing.setSongCount(1);

        when(songRepository.save(any(Song.class))).thenAnswer(inv -> inv.getArgument(0));
        when(singerRepository.findByName("周杰伦")).thenReturn(Optional.of(existing));
        when(songRepository.countBySinger("周杰伦")).thenReturn(5L);

        songService.saveSong(songBy("周杰伦"));

        verify(singerRepository).save(existing);   // 保存的是同一个对象，不是新建的
        assertEquals(5, existing.getSongCount(), "作品数应刷新为最新统计");
    }

    @Test
    @DisplayName("歌手名为空时跳过歌手表操作")
    void saveSong_skipsSingerWhenBlank() {
        when(songRepository.save(any(Song.class))).thenAnswer(inv -> inv.getArgument(0));

        songService.saveSong(songBy("   "));

        verify(singerRepository, never()).save(any());
        verify(singerRepository, never()).findByName(any());
    }

    @Test
    @DisplayName("歌手同步失败不应影响歌曲保存本身")
    void saveSong_singerSyncFailureDoesNotBreakSave() {
        when(songRepository.save(any(Song.class))).thenAnswer(inv -> inv.getArgument(0));
        when(singerRepository.findByName("周杰伦")).thenThrow(new RuntimeException("数据库连接失败"));

        Song saved = assertDoesNotThrow(() -> songService.saveSong(songBy("周杰伦")),
                "歌手同步是附属操作，失败不能拖垮主流程");
        assertNotNull(saved);
        assertEquals("晴天", saved.getName());
    }

    @Test
    @DisplayName("删除歌曲后会把该歌手的作品数重新统计")
    void deleteSong_refreshesSingerCount() {
        Song existing = songBy("周杰伦");
        existing.setId(10L);

        when(songRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(singerRepository.findByName("周杰伦")).thenReturn(Optional.of(new Singer()));
        when(songRepository.countBySinger("周杰伦")).thenReturn(2L);

        songService.deleteSong(10L);

        verify(songRepository).deleteById(10L);
        verify(singerRepository).save(any(Singer.class));
    }
}
