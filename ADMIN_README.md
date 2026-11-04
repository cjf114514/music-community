# 音乐网站管理员系统

## 📋 系统概述

已为你的音乐项目创建了一个完整的管理员后台系统，包含以下功能：

### 核心功能
- ✅ **数据概览** - 用户统计、歌曲统计、播放趋势
- ✅ **用户管理** - 角色分配、账号禁用/启用、删除用户
- ✅ **歌曲管理** - 添加/编辑歌曲、上下架、删除
- ✅ **歌单管理** - 审核歌单、上下架、删除
- ✅ **评论管理** - 审核评论、删除违规评论

---

## 🚀 快速开始

### 1. 数据库与管理账号

表结构由 JPA 自动维护（`application.yml` 中 `ddl-auto: update`），**首次启动会自动建表，不需要手动执行建表或加字段的 SQL**。

导入演示数据（`music_db_export.sql`）后，管理员账号已预置，可以直接用下面的 SQL 确认：

```sql
USE music_db;

-- 查看所有管理员
SELECT id, username, role FROM `user` WHERE role = 'ADMIN';

-- 如果需要把其他账号提升为管理员（把用户名换成你自己的）
UPDATE `user` SET `role` = 'ADMIN' WHERE `username` = '你的用户名';
```

### 2. 登录管理员账号

演示数据中预置了两个角色共 4 个账号：

| 用户名 | 密码 | 角色 |
|---|---|---|
| `cjf` | `Demo@123456` | `ADMIN`（管理员） |
| `cjf1` / `cjf2` / `cjf5` | `Demo@123456` | `USER`（普通用户） |

> 如果你导入的是自己维护的数据库，请用你自己的管理员账号登录，上面的密码只对演示数据有效。

### 3. 访问管理后台

登录后，点击头像下拉菜单，选择 **"管理后台"** 即可进入。

或直接访问：http://localhost:5173/admin

---

## 📁 文件结构

### 后端文件

```
backend/src/main/java/com/music/
├── annotation/
│   └── RequireAdmin.java              # 管理员权限注解
├── interceptor/
│   └── AdminInterceptor.java          # 管理员权限拦截器
├── config/
│   └── WebConfig.java                 # 已注册拦截器
├── entity/
│   └── User.java                      # 已添加 role 字段
├── dto/
│   └── LoginResponse.java             # 已添加 role 字段
├── service/
│   └── AuthService.java               # 登录返回 role
├── controller/
│   ├── AdminDashboardController.java  # 数据概览 + 用户管理
│   ├── AdminSongController.java       # 歌曲管理
│   ├── AdminPlaylistController.java   # 歌单管理
│   └── AdminCommentController.java    # 评论管理
└── repository/
    ├── SongRepository.java            # 已添加分页查询
    └── PlayHistoryRepository.java     # 已添加查询方法
```

### 前端文件

```
frontend/src/
├── views/admin/
│   ├── AdminLayout.vue        # 管理员布局
│   ├── Dashboard.vue          # 数据概览
│   ├── UserManage.vue         # 用户管理
│   ├── SongManage.vue         # 歌曲管理
│   ├── PlaylistManage.vue     # 歌单管理
│   └── CommentManage.vue      # 评论管理
├── router/
│   └── index.js               # 已添加 admin 路由
├── components/
│   └── Header.vue             # 已添加管理后台入口
└── stores/
    └── user.js                # 已优化
```

---

## 🔐 权限说明

### 权限控制机制

1. **@RequireAdmin 注解** - 标记需要管理员权限的接口
2. **AdminInterceptor** - 拦截请求，验证用户角色
3. **路由守卫** - 前端路由权限验证

### 受保护的接口

- `GET /api/admin/dashboard` - 数据概览
- `GET /api/admin/users` - 用户列表
- `PUT /api/admin/users/{id}/role` - 修改用户角色
- `PUT /api/admin/users/{id}/status` - 修改用户状态
- `DELETE /api/admin/users/{id}` - 删除用户
- `GET /api/admin/songs` - 歌曲列表
- `POST /api/admin/songs` - 新增歌曲
- `PUT /api/admin/songs/{id}` - 编辑歌曲
- `DELETE /api/admin/songs/{id}` - 删除歌曲
- `GET /api/admin/playlists` - 歌单列表
- `DELETE /api/admin/playlists/{id}` - 删除歌单
- `GET /api/admin/comments` - 评论列表
- `PUT /api/admin/comments/{id}/status` - 修改评论状态

---

## 🎨 界面预览

### 1. 数据概览
- 4 个统计卡片：用户、歌曲、歌单、今日播放
- 近 7 日播放趋势图表

### 2. 用户管理
- 用户列表展示
- 搜索/筛选功能
- 设置管理员
- 禁用/启用账号
- 删除用户

### 3. 歌曲管理
- 歌曲列表展示
- 新增/编辑歌曲
- 上下架歌曲
- 删除歌曲

### 4. 歌单管理
- 歌单列表展示
- 上下架歌单
- 删除歌单

### 5. 评论管理
- 评论列表展示
- 删除/恢复评论
- 状态筛选

---

## ⚙️ 技术实现

### 后端技术
- Spring Boot 3.2.4
- Spring Security Crypto（密码加密）
- JPA（数据持久化）
- JWT（身份认证）
- 自定义拦截器（权限控制）

### 前端技术
- Vue 3 + Vite
- Element Plus（UI 组件）
- Vue Router（路由管理）
- Pinia（状态管理）
- ECharts（数据可视化）

---

## 🔧 扩展建议

### 短期优化
1. 添加操作日志记录
2. 添加批量操作功能
3. 优化大数据分页性能
4. 添加导出 Excel 功能

### 长期规划
1. 多角色权限系统（超级管理员、内容管理员、用户管理员）
2. 数据统计分析（DAU、MAU、留存率）
3. 敏感词过滤系统
4. 举报处理机制
5. 系统监控告警

---

## 📝 注意事项

1. **表结构由 JPA 自动创建**（`ddl-auto: update`），首次启动无需手动建表
2. **演示账号密码为 `Demo@123456`，上线前必须修改**
3. **管理员入口仅对 ADMIN 角色可见**
4. **所有管理操作都会受到权限拦截器保护**

---

##  常见问题

### Q: 登录后看不到"管理后台"入口？
A: 检查数据库中该用户的 `role` 字段是否为 `'ADMIN'`（演示数据里的管理员是 `cjf`）

### Q: 访问管理页面被重定向到首页？
A: 路由守卫会拦截非管理员访问，请使用管理员账号登录

### Q: 接口返回 403 权限不足？
A: 确认用户 role 字段为 'ADMIN'，并检查 JWT token 是否正确

---

**祝你使用愉快！** 🎉
