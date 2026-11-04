package com.music.service;

import com.music.entity.OperationLog;
import com.music.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final OperationLogRepository operationLogRepository;

    @Async
    public void log(Long userId, String username, String action, String module, String description,
                    String targetType, Long targetId, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setModule(module);
        log.setDescription(description);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setIp(ip);
        operationLogRepository.save(log);
    }

    public Page<OperationLog> getLogs(String module, String action, String keyword, Pageable pageable) {
        if ((module == null || module.isEmpty()) && (action == null || action.isEmpty()) && (keyword == null || keyword.isEmpty())) {
            return operationLogRepository.findAllByOrderByCreateTimeDesc(pageable);
        }
        return operationLogRepository.findByFilters(
                module != null && !module.isEmpty() ? module : null,
                action != null && !action.isEmpty() ? action : null,
                keyword != null && !keyword.isEmpty() ? keyword : null,
                pageable);
    }
}
