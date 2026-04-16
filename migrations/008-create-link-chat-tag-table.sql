CREATE TABLE link_chat_tag (
    chat_id BIGINT NOT NULL,
    link_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,

    PRIMARY KEY (chat_id, link_id, tag_id),

    CONSTRAINT fk_link_chat_tag_subscription
        FOREIGN KEY (chat_id, link_id)
        REFERENCES link_chat (chat_id, link_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_link_chat_tag_tag
        FOREIGN KEY (tag_id)
        REFERENCES tags (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_link_chat_tag_chat_tag
    ON link_chat_tag (chat_id, tag_id);

CREATE INDEX idx_link_chat_tag_link
    ON link_chat_tag (link_id);
