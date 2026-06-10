package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.properties.LinkListCacheProperties;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class ValkeyLinkListCache implements LinkListCache {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final LinkListCacheProperties properties;

    @Override
    public Optional<List<LinkResponse>> get(Long chatId) {
        String key = key(chatId);

        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("В Valkey нет кэша списка ссылок, key={}", key);
                return Optional.empty();
            }

            LinkResponse[] links = objectMapper.readValue(json, LinkResponse[].class);
            List<LinkResponse> result = List.copyOf(Arrays.asList(links));

            log.debug("Список ссылок прочитан из Valkey, key={}, size={}", key, result.size());
            return Optional.of(result);
        } catch (JacksonException e) {
            log.warn("В Valkey найден некорректный JSON списка ссылок, chatId={}. Запись будет удалена", chatId, e);
            evict(chatId);
            return Optional.empty();
        } catch (DataAccessException e) {
            log.warn("Не удалось прочитать список ссылок из Valkey, chatId={}", chatId, e);
            return Optional.empty();
        }
    }

    @Override
    public void put(Long chatId, List<LinkResponse> links) {
        String key = key(chatId);
        List<LinkResponse> safeLinks = List.copyOf(Objects.requireNonNull(links, "links"));

        try {
            String json = objectMapper.writeValueAsString(safeLinks);
            stringRedisTemplate.opsForValue().set(key, json, properties.getTtl());

            log.debug(
                    "Список ссылок сохранён в Valkey, key={}, size={}, ttl={}",
                    key,
                    safeLinks.size(),
                    properties.getTtl());
        } catch (JacksonException e) {
            log.warn("Не удалось сериализовать список ссылок для сохранения в Valkey, chatId={}", chatId, e);
        } catch (DataAccessException e) {
            log.warn("Не удалось сохранить список ссылок в Valkey, chatId={}", chatId, e);
        }
    }

    @Override
    public void evict(Long chatId) {
        String key = key(chatId);

        try {
            Boolean deleted = stringRedisTemplate.delete(key);
            log.debug("Запись списка ссылок удалена из Valkey, key={}, deleted={}", key, deleted);
        } catch (DataAccessException e) {
            log.warn("Не удалось удалить список ссылок из Valkey, chatId={}", chatId, e);
        }
    }

    private String key(Long chatId) {
        return String.valueOf(Objects.requireNonNull(chatId, "chatId"));
    }
}
