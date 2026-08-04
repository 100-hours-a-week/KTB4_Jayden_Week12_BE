CREATE TABLE IF NOT EXISTS image_files (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             file_path VARCHAR(2048),
                             file_category VARCHAR(50) NOT NULL,
                             uploader_id BIGINT,
                             image_status VARCHAR(50) NOT NULL,

                             CONSTRAINT pk_image_files PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS `users` (
                         `user_id`	bigint	NOT NULL AUTO_INCREMENT,
                         `email`	varchar(255)	NOT NULL,
                         `password`	varchar(255)	NOT NULL,
                         `nickname`	varchar(10)	NOT NULL,

                         `image_file_id` BIGINT	NULL,

                         `created_at`	timestamp	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `deleted_at`	timestamp	NULL DEFAULT NULL,
                         `information_updated_at`	timestamp	NULL DEFAULT NULL,
                         `password_updated_at`	timestamp	NULL DEFAULT NULL,

                         CONSTRAINT `PK_USERS` PRIMARY KEY (`user_id`),
                         CONSTRAINT `UK_USERS_EMAIL` UNIQUE (`email`),
                         CONSTRAINT `UK_USERS_NICKNAME` UNIQUE (`nickname`),

                         CONSTRAINT fk_users_profile_image FOREIGN KEY (image_file_id) REFERENCES image_files (id)
);

CREATE TABLE IF NOT EXISTS `articles` (
                            `article_id`	bigint	NOT NULL AUTO_INCREMENT,
                            `user_id`	bigint	NOT NULL,
                            `title`	varchar(26)	NOT NULL,
                            `content`	mediumtext	NOT NULL,
                            `content_images`	json	NULL,
                            `created_at`	timestamp	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `updated_at`	timestamp	NULL DEFAULT NULL,
                            `deleted_at`	timestamp	NULL DEFAULT NULL,
                            `is_article_hidden`	boolean	NOT NULL	DEFAULT 0,

                            CONSTRAINT `PK_ARTICLES` PRIMARY KEY (`article_id`),

                            CONSTRAINT `FK_USERS_TO_ARTICLES`
                                FOREIGN KEY (`user_id`)
                                    REFERENCES `users` (`user_id`)
);

CREATE TABLE IF NOT EXISTS `article_update_history` (
                                          `article_history_id`	bigint	NOT NULL AUTO_INCREMENT,
                                          `article_id`	bigint	NOT NULL,
                                          `title`	varchar(26)	NOT NULL,
                                          `content`	mediumtext	NOT NULL,
                                          `content_images`	json	NULL,
                                          `created_at`	timestamp	NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                          CONSTRAINT `PK_ARTICLE_UPDATE_HISTORY` PRIMARY KEY (`article_history_id`),

                                          CONSTRAINT `FK_ARTICLES_TO_ARTICLE_UPDATE_HISTORY`
                                              FOREIGN KEY (`article_id`)
                                                  REFERENCES `articles` (`article_id`)
);

CREATE TABLE IF NOT EXISTS `comments` (
                            `comment_id`	bigint	NOT NULL AUTO_INCREMENT,
                            `article_id`	bigint	NOT NULL,
                            `user_id`	bigint	NOT NULL,
                            `parent_comment_id`	bigint	NULL DEFAULT NULL,
                            `comment_text`	text	NOT NULL,
                            `created_at`	timestamp	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `updated_at`	timestamp	NULL DEFAULT NULL,
                            `deleted_at`	timestamp	NULL DEFAULT NULL,

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
);

CREATE TABLE IF NOT EXISTS `temp_articles` (
                                 `user_id`	bigint	NOT NULL,
                                 `title`	varchar(26)	NULL,
                                 `content`	mediumtext	NOT NULL,
                                 `content_images`	json	NULL,
                                 `saved_at`	timestamp	NOT NULL	DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                 CONSTRAINT `PK_TEMP_ARTICLES` PRIMARY KEY (`user_id`),

                                 CONSTRAINT `FK_USERS_TO_TEMP_ARTICLES`
                                     FOREIGN KEY (`user_id`)
                                         REFERENCES `users` (`user_id`)
);

CREATE TABLE IF NOT EXISTS  `article_reports` (
                                   `report_id`	bigint	NOT NULL AUTO_INCREMENT,
                                   `article_id`	bigint	NOT NULL,
                                   `user_id`	bigint	NOT NULL,
                                   `report_type`	varchar(100)	NOT NULL	DEFAULT 'spam',
                                   `reason`	varchar(500)	NOT NULL,
                                   `created_at`	timestamp	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
                                   `status`	varchar(20)	NOT NULL	DEFAULT 'waiting',

                                   CONSTRAINT `PK_ARTICLE_REPORTS` PRIMARY KEY (`report_id`),

                                   CONSTRAINT `FK_ARTICLES_TO_ARTICLE_REPORTS`
                                       FOREIGN KEY (`article_id`)
                                           REFERENCES `articles` (`article_id`),

                                   CONSTRAINT `FK_USERS_TO_ARTICLE_REPORTS`
                                       FOREIGN KEY (`user_id`)
                                           REFERENCES `users` (`user_id`),

                                   CONSTRAINT `UK_ARTICLE_REPORTS_ARTICLE_ID_USER_ID`
                                       UNIQUE (`article_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS  `article_likes` (
                                 `article_like_id`	bigint	NOT NULL AUTO_INCREMENT,
                                 `article_id`	bigint	NOT NULL,
                                 `user_id`	bigint	NOT NULL,
                                 `created_at`	timestamp	NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT `PK_ARTICLE_LIKES` PRIMARY KEY (`article_like_id`),

                                 CONSTRAINT `FK_ARTICLES_TO_ARTICLE_LIKES`
                                     FOREIGN KEY (`article_id`)
                                         REFERENCES `articles` (`article_id`),

                                 CONSTRAINT `FK_USERS_TO_ARTICLE_LIKES`
                                     FOREIGN KEY (`user_id`)
                                         REFERENCES `users` (`user_id`),

                                 CONSTRAINT `UK_ARTICLE_LIKES_ARTICLE_ID_USER_ID`
                                     UNIQUE (`article_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS  `article_views` (
                                 `article_view_id`	bigint	NOT NULL AUTO_INCREMENT,
                                 `article_id`	bigint	NOT NULL,
                                 `user_id`	bigint	NOT NULL,
                                 `updated_at`	timestamp	NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                 CONSTRAINT `PK_ARTICLE_VIEWS` PRIMARY KEY (`article_view_id`),

                                 CONSTRAINT `FK_ARTICLES_TO_ARTICLE_VIEWS`
                                     FOREIGN KEY (`article_id`)
                                         REFERENCES `articles` (`article_id`),

                                 CONSTRAINT `FK_USERS_TO_ARTICLE_VIEWS`
                                     FOREIGN KEY (`user_id`)
                                         REFERENCES `users` (`user_id`),

                                 CONSTRAINT `UK_ARTICLE_VIEWS_ARTICLE_ID_USER_ID`
                                     UNIQUE (`article_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS  `article_stats` (
                                 `article_id`	bigint	NOT NULL,
                                 `comment_count`	bigint	NOT NULL	DEFAULT 0,
                                 `article_like_count`	bigint	NOT NULL	DEFAULT 0,
                                 `article_view_count`	bigint	NOT NULL	DEFAULT 0,
                                 `article_report_count`	bigint	NOT NULL	DEFAULT 0,

                                 CONSTRAINT `PK_ARTICLE_STATS` PRIMARY KEY (`article_id`),

                                 CONSTRAINT `FK_ARTICLES_TO_ARTICLE_STATS`
                                     FOREIGN KEY (`article_id`)
                                         REFERENCES `articles` (`article_id`)
);

CREATE TABLE IF NOT EXISTS  refresh_token (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               token VARCHAR(255) NOT NULL,
                               user_id BIGINT NOT NULL,
                               expires_at TIMESTAMP NOT NULL,

                               CONSTRAINT pk_refresh_token PRIMARY KEY (id),
                               CONSTRAINT uk_refresh_token_token UNIQUE (token)
);


CREATE TABLE IF NOT EXISTS  chat_rooms (
                            chat_room_id BIGINT NOT NULL AUTO_INCREMENT,
                            created_at TIMESTAMP NOT NULL,
                            room_type VARCHAR(20) NOT NULL,
                            direct_key VARCHAR(40) NOT NULL,

                            CONSTRAINT pk_chat_rooms
                                PRIMARY KEY (chat_room_id),

                            CONSTRAINT uk_direct_key UNIQUE (direct_key)
);

CREATE TABLE IF NOT EXISTS  chat_messages (
                               chat_message_id BIGINT NOT NULL AUTO_INCREMENT,
                               client_message_id VARCHAR(36) NOT NULL,
                               chat_room_id BIGINT NOT NULL,
                               sender_id BIGINT NOT NULL,

                               content text,
                               chat_type VARCHAR(30) NOT NULL,

                               created_at TIMESTAMP NOT NULL,
                               updated_at TIMESTAMP,
                               deleted_at TIMESTAMP,

                               CONSTRAINT pk_chat_messages
                                   PRIMARY KEY (chat_message_id),

                               CONSTRAINT fk_chat_messages_chat_room
                                   FOREIGN KEY (chat_room_id)
                                       REFERENCES chat_rooms (chat_room_id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_chat_messages_sender
                                   FOREIGN KEY (sender_id)
                                       REFERENCES users (user_id)
                                       ON DELETE RESTRICT,

                               CONSTRAINT `UK_CLIENT_MESSAGE_ID_SENDER_ID`
                                   UNIQUE (`client_message_id`, `sender_id`)
);

CREATE TABLE IF NOT EXISTS  chat_room_members (
                                   chat_room_member_id BIGINT NOT NULL AUTO_INCREMENT,
                                   chat_room_id BIGINT NOT NULL,
                                   user_id BIGINT NOT NULL,
                                   last_read_message_id BIGINT,
                                   joined_at TIMESTAMP NOT NULL,
                                   left_at TIMESTAMP,

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
                                           REFERENCES users (user_id)
                                           ON DELETE RESTRICT,

                                   CONSTRAINT fk_chat_room_members_last_read_message
                                       FOREIGN KEY (last_read_message_id)
                                           REFERENCES chat_messages (chat_message_id)
                                           ON DELETE SET NULL
);