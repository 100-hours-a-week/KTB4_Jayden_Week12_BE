package com.example.spring_rest_api.user.repository;

import com.example.spring_rest_api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByNickname(String nickname);

    @Modifying
    @Query(value = "delete from refresh_token where user_id = :userId", nativeQuery = true)
    void deleteRefreshTokensByUserId(@Param("userId") Long userId);
}
