package com.distributed.base.repository;

import com.distributed.base.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户持久层接口
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    Page<User> findByRole(Integer role, Pageable pageable);

    Page<User> findByStatus(Integer status, Pageable pageable);

    Page<User> findByRoleAndStatus(Integer role, Integer status, Pageable pageable);
}
