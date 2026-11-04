package com.music.common;

import lombok.Data;

/**
 * 统一响应结果类
 *
 * 整个后端所有 API 接口都通过这个类返回给前端,保证响应格式一致:
 * {
 *   "code": 200,       // 状态码: 200=成功, 500=失败, 401=未登录
 *   "message": "ok",   // 提示信息
 *   "data": { ... }    // 实际数据 (泛型 T, 可以是对象、列表、null)
 * }
 *
 * 为什么用泛型 <T>?
 *   这样 data 可以是任意类型 — 歌单列表是 List<Playlist>, 单首歌曲是 Song
 *   如果不用泛型,就需要为每种返回值写一个类
 *
 * 使用示例:
 *   Result.success(songList);           → code=200, data=歌单列表
 *   Result.error("歌单不存在");          → code=500, message="歌单不存在"
 *   Result.error(401, "请先登录");       → code=401
 */
@Data
public class Result<T> {
    private int code;       // 状态码
    private String message; // 提示信息
    private T data;         // 数据泛型,可以是任何类型的返回数据

    // 私有构造方法 — 外部只能通过静态工厂方法创建实例
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功 — 带数据 (最常用) */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /** 成功 — 不带数据 (如:删除操作) */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /** 失败 — 默认状态码 500 */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /** 失败 — 自定义状态码 (如:401 未登录, 403 无权限) */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
