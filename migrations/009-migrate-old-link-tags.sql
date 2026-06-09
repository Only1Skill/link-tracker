INSERT INTO link_chat_tag (chat_id, link_id, tag_id)
SELECT lc.chat_id, lc.link_id, lt.tag_id
FROM link_chat lc
JOIN link_tags lt ON lt.link_id = lc.link_id
ON CONFLICT DO NOTHING;
