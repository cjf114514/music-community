package com.music.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 歌曲实体类 — 映射到数据库 song 表
 *
 * 字段说明:
 *   name/ singer/ album/ genre → 歌曲基本信息
 *   url/ coverUrl/ format → 音频文件和封面
 *   lyric (TEXT) → 歌词内容 (LRC 格式完整文本)
 *   duration/ fileSize → 音频属性
 *   playCount → 播放量统计
 *   status → 0=下架, 1=正常
 */
@Data
@Entity
@Table(name = "song")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 歌曲名 — 非空 */
    @Column(nullable = false)
    private String name;

    /** 歌手名 */
    private String singer;

    /** 专辑名 */
    private String album;

    /**
     * 流派: 如 "流行"、"摇滚"、"电子"
     * 当前是简单 String,不是枚举/多对多
     * 局限性: 一首歌只能写一个流派
     */
    private String genre;

    /** 音频文件 URL (本地路径或 S3 地址) */
    private String url;

    /** 封面图片 URL */
    @Column(name = "cover_url")
    private String coverUrl;

    /**
     * 歌词内容 — TEXT 类型
     * columnDefinition = "TEXT" → 指定数据库列类型为 TEXT (可存长文本)
     * 默认 VARCHAR(255) 只能存 255 字符,不够放完整歌词
     *
     * @JsonProperty(access = WRITE_ONLY) —— 写进得去，读不出来：
     *   · 反序列化（POST/PUT 请求体里的 lyric）照常可以写入
     *   · 序列化（返回给前端的 JSON）会被剔除
     *
     * 为什么？歌词动辄 3-4KB，而一首歌的其他字段加起来才几百字节。
     * 实测：17 首歌的 GET /api/songs 响应 21KB，其中歌词占 15KB（70%）。
     * 等歌曲上千首时，列表接口会变成几 MB，首页和搜索会明显变慢。
     *
     * 前端需要歌词时，走专用接口 GET /api/songs/{id}/lyric 单独取。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(columnDefinition = "TEXT")
    private String lyric;

    /** 时长 (秒) — 由前端或服务端计算后填入 */
    private Long duration;

    /** 文件大小 (字节) */
    @Column(name = "file_size")
    private Long fileSize;

    /** 文件格式: mp3, wav, flac 等 */
    private String format;

    /** 播放次数 — 用于排行榜计算 */
    @Column(name = "play_count")
    private Long playCount;

    /**
     * 歌曲状态
     * 0 = 下架 (前端不展示)
     * 1 = 正常 (默认)
     */
    private Integer status;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (playCount == null) playCount = 0L;
        if (status == null) status = 1;
        if (format == null) format = "mp3";   // 默认格式
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
