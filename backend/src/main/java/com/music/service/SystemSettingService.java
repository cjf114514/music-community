package com.music.service;

import com.music.entity.SystemSetting;
import com.music.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemSettingService {
    private final SystemSettingRepository systemSettingRepository;

    public String get(String key) {
        return systemSettingRepository.findByKey(key).map(SystemSetting::getValue).orElse(null);
    }

    public String get(String key, String defaultValue) {
        String val = get(key);
        return val != null ? val : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    @Transactional
    public void set(String key, String value, String description) {
        SystemSetting setting = systemSettingRepository.findByKey(key)
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setKey(key);
                    return s;
                });
        setting.setValue(value);
        if (description != null) setting.setDescription(description);
        systemSettingRepository.save(setting);
    }

    @Transactional
    public void set(String key, String value) {
        set(key, value, null);
    }

    public Map<String, String> getAllSettings() {
        List<SystemSetting> list = systemSettingRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (SystemSetting s : list) {
            map.put(s.getKey(), s.getValue());
        }
        return map;
    }

    @Transactional
    public void batchUpdate(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }
}
