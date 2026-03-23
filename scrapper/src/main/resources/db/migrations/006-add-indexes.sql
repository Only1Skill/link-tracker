CREATE INDEX idx_links_url ON links(url);
CREATE INDEX idx_link_chat_chat_id ON link_chat(chat_id);
CREATE INDEX idx_link_tags_tag_id ON link_tags(tag_id);
CREATE INDEX idx_links_last_update_time ON links(last_update_time);
