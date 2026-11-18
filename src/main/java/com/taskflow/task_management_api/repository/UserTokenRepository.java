package com.taskflow.task_management_api.repository;

import com.taskflow.task_management_api.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true)
    @Query("delete from UserToken t where t.user.id = :userId and t.type = :type")
    void deleteByUserAndType(@Param("userId") Long userId, @Param("type") UserToken.Type type);

    @Modifying(flushAutomatically = true)
    @Query("delete from UserToken t where t.user.id = :userId")
    void deleteAllForUser(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from UserToken t where t.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}
