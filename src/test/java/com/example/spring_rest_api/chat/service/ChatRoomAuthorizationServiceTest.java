package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.entity.ChatRoomMember;
import com.example.spring_rest_api.chat.repository.ChatRoomMemberRepository;
import com.example.spring_rest_api.common.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.example.spring_rest_api.chat.ChatTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatRoomAuthorizationServiceTest {

    @InjectMocks
    ChatRoomAuthorizationService authorizationService;

    @Mock
    ChatRoomMemberRepository memberRepository;

    @Test
    @DisplayName("활성 채팅방 멤버는 참여자 검증을 통과한다")
    void validateActiveParticipantSucceeds() {
        ChatRoomMember member = member(1L, room(10L), user(1L));
        given(memberRepository.findByChatRoom_ChatRoomIdAndUser_userId(10L, 1L))
                .willReturn(Optional.of(member));

        assertThatCode(() -> authorizationService.validateParticipant(10L, 1L))
                .doesNotThrowAnyException();
        verify(memberRepository).findByChatRoom_ChatRoomIdAndUser_userId(10L, 1L);
    }

    @Test
    @DisplayName("채팅방 멤버가 아니면 참여자 검증에 실패한다")
    void validateMissingParticipantThrowsException() {
        given(memberRepository.findByChatRoom_ChatRoomIdAndUser_userId(10L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.validateParticipant(10L, 1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("퇴장한 채팅방 멤버는 참여자 검증에 실패한다")
    void validateLeftParticipantThrowsException() {
        ChatRoomMember member = member(1L, room(10L), user(1L));
        member.leave();
        given(memberRepository.findByChatRoom_ChatRoomIdAndUser_userId(10L, 1L))
                .willReturn(Optional.of(member));

        assertThatThrownBy(() -> authorizationService.validateParticipant(10L, 1L))
                .isInstanceOf(ForbiddenException.class);
    }
}
