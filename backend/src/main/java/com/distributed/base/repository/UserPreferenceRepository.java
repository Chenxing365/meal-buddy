package com.distributed.base.repository;

import com.distributed.base.entity.UserPreference;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户饮食偏好持久层接口
 */
@Repository
public interface UserPreferenceRepository extends MongoRepository<UserPreference, String> {

    Optional<UserPreference> findByUserId(String userId);

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);
}
