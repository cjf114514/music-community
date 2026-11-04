package com.music.repository;

import com.music.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    //JpaRepository 提供了基本的CRUD操作方法
    //User为操作的实体类 Long为操作的主键类型
    Optional<User> findByUsername(String username);
    //单条组件，唯一查询用Optional
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.status = 1 AND (u.username LIKE %:keyword% OR u.nickname LIKE %:keyword%)")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
    //自定义查询方法，根据用户名或昵称查询用户
    //:keyword接收传入的查询关键词
    //pageable接收分页参数，用于分页查询结果
    //返回Page<User>类型，包含查询到的用户列表和分页信息
}
