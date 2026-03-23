CREATE TABLE link_chat (
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    chat_id BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    PRIMARY KEY (link_id, chat_id)
);
