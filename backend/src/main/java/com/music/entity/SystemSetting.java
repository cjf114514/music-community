package com.music.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "system_setting")
public class SystemSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "`key`", nullable = false, unique = true, length = 100)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(length = 200)
    private String description;
}
