package com.example.spring_rest_api.chat.controller;

import com.example.spring_rest_api.chat.principal.StompPrincipal;
import com.example.spring_rest_api.chat.service.response.ReAuthResponse;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class StompAuthController {

    @MessageMapping("/auth/reauth")
    @SendToUser(destinations = "/queue/auth", broadcast = false)
    public ReAuthResponse reauthenticate(Principal principal) {
        StompPrincipal stompPrincipal = (StompPrincipal) principal;

        return new ReAuthResponse(
                "REAUTHENTICATED",
                stompPrincipal.getExpiresAt()
        );
    }
}
