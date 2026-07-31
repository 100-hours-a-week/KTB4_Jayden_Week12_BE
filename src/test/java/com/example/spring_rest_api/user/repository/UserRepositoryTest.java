package com.example.spring_rest_api.user.repository;

import com.example.spring_rest_api.user.entity.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("중복 이메일 저장 거부")
    void findByEmail() {
        //given
        String email = "search@abc.com";
        User user = User.create(
                email,
                "Abc1234!",
                "nickname",
                null
        );
        User another = User.create(
                email,
                "Abc1234!",
                "nickname2",
                null
        );
        userRepository.saveAndFlush(user);
        em.clear();
        Assertions.assertThatThrownBy(() -> userRepository.saveAndFlush(another))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("중복 닉네임 저장 거부")
    void findByNickname() {
        //given
        String nickname = "nickname";
        User user = User.create(
                "email@abc.com",
                "Abc1234!",
                nickname,
                null
        );
        User another = User.create(
                "email2@abc.com",
                "Abc1234!",
                nickname,
                null
        );
        userRepository.saveAndFlush(user);
        em.clear();
        Assertions.assertThatThrownBy(() -> userRepository.saveAndFlush(another))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}