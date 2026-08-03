CREATE TABLE IF NOT EXISTS  image_files (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             file_path VARCHAR(2048) NULL,
                             file_category VARCHAR(50) NOT NULL,
                             uploader_id BIGINT NULL,
                             image_status VARCHAR(50) NOT NULL,

                             CONSTRAINT pk_image_files PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS  `users` (
                         `user_id` BIGINT NOT NULL AUTO_INCREMENT,
                         `email` VARCHAR(255) NOT NULL,
                         `password` VARCHAR(255) NOT NULL,
                         `nickname` VARCHAR(10) NOT NULL,
                         `image_file_id` BIGINT NULL,

                         `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         `deleted_at` DATETIME(6) NULL DEFAULT NULL,
                         `information_updated_at` DATETIME(6) NULL DEFAULT NULL,
                         `password_updated_at` DATETIME(6) NULL DEFAULT NULL,

                         CONSTRAINT `PK_USERS` PRIMARY KEY (`user_id`),
                         CONSTRAINT `UK_USERS_EMAIL` UNIQUE (`email`),
                         CONSTRAINT `UK_USERS_NICKNAME` UNIQUE (`nickname`),

                         CONSTRAINT fk_users_profile_image
                             FOREIGN KEY (`image_file_id`)
                                 REFERENCES image_files (`id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS  `articles` (
                            `article_id` BIGINT NOT NULL AUTO_INCREMENT,
                            `user_id` BIGINT NOT NULL,
                            `title` VARCHAR(26) NOT NULL,
                            `content` MEDIUMTEXT NOT NULL,
                            `content_images` JSON NULL,

                            `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            `updated_at` DATETIME(6) NULL DEFAULT NULL,
                            `deleted_at` DATETIME(6) NULL DEFAULT NULL,
                            `is_article_hidden` BOOLEAN NOT NULL DEFAULT FALSE,

                            CONSTRAINT `PK_ARTICLES` PRIMARY KEY (`article_id`),

                            CONSTRAINT `FK_USERS_TO_ARTICLES`
                                FOREIGN KEY (`user_id`)
                                    REFERENCES `users` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS  `article_update_history` (
                                          `article_history_id` BIGINT NOT NULL AUTO_INCREMENT,
                                          `article_id` BIGINT NOT NULL,
                                          `title` VARCHAR(26) NOT NULL,
                                          `content` MEDIUMTEXT NOT NULL,
                                          `content_images` JSON NULL,
                                          `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                          CONSTRAINT `PK_ARTICLE_UPDATE_HISTORY`
                                              PRIMARY KEY (`article_history_id`),

                                          CONSTRAINT `FK_ARTICLES_TO_ARTICLE_UPDATE_HISTORY`
                                              FOREIGN KEY (`article_id`)
                                                  REFERENCES `articles` (`article_id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS  `comments` (
                            `comment_id` BIGINT NOT NULL AUTO_INCREMENT,
                            `article_id` BIGINT NOT NULL,
                            `user_id` BIGINT NOT NULL,
                            `parent_comment_id` BIGINT NULL DEFAULT NULL,
                            `comment_text` TEXT NOT NULL,

                            `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            `updated_at` DATETIME(6) NULL DEFAULT NULL,
                            `deleted_at` DATETIME(6) NULL DEFAULT NULL,

                            CONSTRAINT `PK_COMMENTS` PRIMARY KEY (`comment_id`),

                            CONSTRAINT `FK_ARTICLES_TO_COMMENTS`
                                FOREIGN KEY (`article_id`)
                                    REFERENCES `articles` (`article_id`),

                            CONSTRAINT `FK_USERS_TO_COMMENTS`
                                FOREIGN KEY (`user_id`)
                                    REFERENCES `users` (`user_id`),

                            CONSTRAINT `FK_COMMENTS_TO_PARENT_COMMENTS`
                                FOREIGN KEY (`parent_comment_id`)
                                    REFERENCES `comments` (`comment_id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS  `temp_articles` (
                                 `user_id` BIGINT NOT NULL,
                                 `title` VARCHAR(26) NULL,
                                 `content` MEDIUMTEXT NOT NULL,
                                 `content_images` JSON NULL,
                                 `saved_at` DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                 CONSTRAINT `PK_TEMP_ARTICLES` PRIMARY KEY (`user_id`),

                                 CONSTRAINT `FK_USERS_TO_TEMP_ARTICLES`
                                     FOREIGN KEY (`user_id`)
                                         REFERENCES `users` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS  `article_reports` (
                                   `report_id` BIGINT NOT NULL AUTO_INCREMENT,
                                   `article_id` BIGINT NOT NULL,
                                   `user_id` BIGINT NOT NULL,
                                   `report_type` VARCHAR(100) NOT NULL DEFAULT 'spam',
                                   `reason` VARCHAR(500) NOT NULL,
                                   `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   `status` VARCHAR(20) NOT NULL DEFAULT 'waiting',

                                   CONSTRAINT `PK_ARTICLE_REPORTS` PRIMARY KEY (`report_id`),

                                   CONSTRAINT `FK_ARTICLES_TO_ARTICLE_REPORTS`
                                       FOREIGN KEY (`article_id`)
                                           REFERENCES `articles` (`article_id`),

                                   CONSTRAINT `FK_USERS_TO_ARTICLE_REPORTS`
                                       FOREIGN KEY (`user_id`)
                                           REFERENCES `users` (`user_id`),

                                   CONSTRAINT `UK_ARTICLE_REPORTS_ARTICLE_ID_USER_ID`
                                       UNIQUE (`article_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS  `article_likes` (
                                 `article_like_id` BIGINT NOT NULL AUTO_INCREMENT,
                                 `article_id` BIGINT NOT NULL,
                                 `user_id` BIGINT NOT NULL,
                                 `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                 CONSTRAINT `PK_ARTICLE_LIKES` PRIMARY KEY (`article_like_id`),

                                 CONSTRAINT `FK_ARTICLES_TO_ARTICLE_LIKES`
                                     FOREIGN KEY (`article_id`)
                                         REFERENCES `articles` (`article_id`),

                                 CONSTRAINT `FK_USERS_TO_ARTICLE_LIKES`
                                     FOREIGN KEY (`user_id`)
                                         REFERENCES `users` (`user_id`),

                                 CONSTRAINT `UK_ARTICLE_LIKES_ARTICLE_ID_USER_ID`
                                     UNIQUE (`article_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS  `article_views` (
                                 `article_view_id` BIGINT NOT NULL AUTO_INCREMENT,
                                 `article_id` BIGINT NOT NULL,
                                 `user_id` BIGINT NOT NULL,
                                 `updated_at` DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                 CONSTRAINT `PK_ARTICLE_VIEWS` PRIMARY KEY (`article_view_id`),

                                 CONSTRAINT `FK_ARTICLES_TO_ARTICLE_VIEWS`
                                     FOREIGN KEY (`article_id`)
                                         REFERENCES `articles` (`article_id`),

                                 CONSTRAINT `FK_USERS_TO_ARTICLE_VIEWS`
                                     FOREIGN KEY (`user_id`)
                                         REFERENCES `users` (`user_id`),

                                 CONSTRAINT `UK_ARTICLE_VIEWS_ARTICLE_ID_USER_ID`
                                     UNIQUE (`article_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS  `article_stats` (
                                 `article_id` BIGINT NOT NULL,
                                 `comment_count` BIGINT NOT NULL DEFAULT 0,
                                 `article_like_count` BIGINT NOT NULL DEFAULT 0,
                                 `article_view_count` BIGINT NOT NULL DEFAULT 0,
                                 `article_report_count` BIGINT NOT NULL DEFAULT 0,

                                 CONSTRAINT `PK_ARTICLE_STATS` PRIMARY KEY (`article_id`),

                                 CONSTRAINT `FK_ARTICLES_TO_ARTICLE_STATS`
                                     FOREIGN KEY (`article_id`)
                                         REFERENCES `articles` (`article_id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE  IF NOT EXISTS refresh_token (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               token VARCHAR(255) NOT NULL,
                               user_id BIGINT NOT NULL,
                               expires_at DATETIME(6) NOT NULL,

                               CONSTRAINT pk_refresh_token PRIMARY KEY (id),
                               CONSTRAINT uk_refresh_token_token UNIQUE (token)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS  chat_rooms (
                            chat_room_id BIGINT NOT NULL AUTO_INCREMENT,
                            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            room_type VARCHAR(20) NOT NULL,
                            direct_key VARCHAR(40) NOT NULL,

                            CONSTRAINT pk_chat_rooms PRIMARY KEY (chat_room_id),

                            CONSTRAINT uk_direct_key UNIQUE (direct_key)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE  IF NOT EXISTS chat_messages (
                               chat_message_id BIGINT NOT NULL AUTO_INCREMENT,
                               chat_room_id BIGINT NOT NULL,
                               sender_id BIGINT NOT NULL,

                               content TEXT NULL,
                               chat_type VARCHAR(30) NOT NULL,

                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at DATETIME(6) NULL DEFAULT NULL,
                               deleted_at DATETIME(6) NULL DEFAULT NULL,

                               CONSTRAINT pk_chat_messages
                                   PRIMARY KEY (chat_message_id),

                               CONSTRAINT fk_chat_messages_chat_room
                                   FOREIGN KEY (chat_room_id)
                                       REFERENCES chat_rooms (chat_room_id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_chat_messages_sender
                                   FOREIGN KEY (sender_id)
                                       REFERENCES `users` (user_id)
                                       ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS  chat_room_members (
                                   chat_room_member_id BIGINT NOT NULL AUTO_INCREMENT,
                                   chat_room_id BIGINT NOT NULL,
                                   user_id BIGINT NOT NULL,
                                   last_read_message_id BIGINT NULL,
                                   joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   left_at DATETIME(6),

                                   CONSTRAINT pk_chat_room_members
                                       PRIMARY KEY (chat_room_member_id),

                                   CONSTRAINT uk_chat_room_members_room_user
                                       UNIQUE (chat_room_id, user_id),

                                   CONSTRAINT fk_chat_room_members_chat_room
                                       FOREIGN KEY (chat_room_id)
                                           REFERENCES chat_rooms (chat_room_id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_chat_room_members_user
                                       FOREIGN KEY (user_id)
                                           REFERENCES `users` (user_id)
                                           ON DELETE RESTRICT,

                                   CONSTRAINT fk_chat_room_members_last_read_message
                                       FOREIGN KEY (last_read_message_id)
                                           REFERENCES chat_messages (chat_message_id)
                                           ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
