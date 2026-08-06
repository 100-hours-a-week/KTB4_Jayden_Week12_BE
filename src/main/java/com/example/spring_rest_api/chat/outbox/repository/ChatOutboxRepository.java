package com.example.spring_rest_api.chat.outbox.repository;

import com.example.spring_rest_api.chat.outbox.entity.ChatOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatOutboxRepository extends JpaRepository<ChatOutbox, Long> {

}
