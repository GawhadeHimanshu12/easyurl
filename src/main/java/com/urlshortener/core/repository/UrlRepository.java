package com.urlshortener.core.repository;

import com.urlshortener.core.entity.UrlEntity;
import com.urlshortener.core.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    Optional<UrlEntity> findByShortKey(String shortKey);

    List<UrlEntity> findByUser(UserEntity user);

    @Modifying
    @Query("UPDATE UrlEntity u SET u.clickCount = u.clickCount + 1 WHERE u.shortKey = :shortKey")
    void incrementClickCount(@Param("shortKey") String shortKey);

    @Modifying
    @Query("UPDATE UrlEntity u SET u.user = :user, u.anonymousSessionId = null WHERE u.anonymousSessionId = :anonId")
    void claimAnonymousUrls(@Param("user") UserEntity user, @Param("anonId") String anonId);
}