package com.example.spring_rest_api.chat.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatDestinationUtils {
    private static final Pattern CHAT_ROOM_PATTERN = Pattern.compile("/chatrooms/(\\d+)(?:/|$)");

    public static Long extractRoomId(String destination) {
        if (destination == null) {
            return null;
        }

        Matcher matcher = CHAT_ROOM_PATTERN.matcher(destination);

        if (!matcher.find()) {
            return null;
        }
        return Long.valueOf(matcher.group(1));
    }
}
