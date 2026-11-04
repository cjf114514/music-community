package com.music.controller;

import com.music.annotation.RequireAdmin;
import com.music.common.Result;
import com.music.entity.OperationLog;
import com.music.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/operation-logs")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    @RequireAdmin
    public Result<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword) {

        Page<OperationLog> logPage = operationLogService.getLogs(
                module, action, keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime")));

        Map<String, Object> result = new HashMap<>();
        result.put("total", logPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("list", logPage.getContent());
        return Result.success(result);
    }
}
