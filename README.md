# Link Tracker

Link Tracker — это backend-система для отслеживания обновлений по ссылкам и отправки уведомлений пользователям в Telegram.

Проект состоит из двух основных сервисов:

* **Bot** — Telegram-бот, который принимает команды пользователей и отправляет уведомления.
* **Scrapper** — сервис, который хранит подписки, периодически проверяет внешние ресурсы и публикует найденные обновления.

Коммуникация между `scrapper` и `bot` поддерживает два режима:

* **HTTP** — синхронная отправка уведомлений через REST API.
* **Kafka** — асинхронная отправка уведомлений через Apache Kafka. Этот режим используется по умолчанию.

---

## Возможности

* Регистрация Telegram-чата через команду `/start`.
* Добавление ссылок для отслеживания через `/track`.
* Удаление ссылок через `/untrack`.
* Просмотр отслеживаемых ссылок через `/list`.
* Поддержка тегов и фильтров для подписок.
* Проверка обновлений GitHub repositories.
* Проверка обновлений Stack Overflow questions.
* Асинхронная доставка уведомлений через Kafka.
* Retry и DLQ для ошибочных Kafka-сообщений.
* Поддержка двух реализаций доступа к БД:
  * SQL/JDBC
  * JPA/Hibernate
* Миграции базы данных через Liquibase.
* Интеграционные тесты с Testcontainers.

---

## Архитектура

Высокоуровневая схема работы:

```text
Telegram User
     |
     v
+---------+
|   Bot   |
+---------+
     |
     | REST API: регистрация чата, подписки, команды
     v
+------------+
|  Scrapper  |
+------------+
     |
     | polling GitHub / Stack Overflow
     v
External APIs
```

При обнаружении обновлений `scrapper` отправляет уведомление в `bot`.

В Kafka-режиме цепочка выглядит так:

```text
Scrapper
   |
   | LinkUpdate JSON
   v
Kafka topic: link-updates
   |
   v
Bot Kafka Consumer
   |
   v
Telegram message
```

Если сообщение невозможно обработать, оно попадает в DLQ:

```text
link-updates-dlq
```

---

## Модули проекта

```text
.
├── bot                 # Telegram Bot service
├── scrapper            # Link polling and subscription service
├── ai-agent            # Дополнительный модуль проекта
├── e2e-tests           # Сквозные интеграционные тесты
├── migrations          # Liquibase migrations
├── docker-compose.yml  # Локальная инфраструктура
└── pom.xml             # Root Maven project
```

---

## Технологический стек

* Java 25
* Spring Boot 4
* Spring Web MVC
* Spring Kafka
* Spring Data JDBC
* Spring Data JPA
* Hibernate
* PostgreSQL
* Liquibase
* Apache Kafka
* Zookeeper
* Testcontainers
* JUnit 6
* Mockito
* Maven

---

## База данных

Основные сущности:

* `chats` — Telegram-чаты.
* `links` — глобальный справочник ссылок.
* `link_chat` — подписки чатов на ссылки.
* `tags` — справочник тегов.
* `link_chat_tag` — связь тегов с конкретной подпиской.

Важная особенность модели: тег относится не к глобальной ссылке, а к конкретной подписке пользователя.

То есть одна и та же ссылка может быть добавлена разными пользователями с разными тегами.

---

## Kafka

Kafka используется как асинхронный транспорт уведомлений между `scrapper` и `bot`.

Основные topics:

|       Topic        |                           Назначение                           |
|--------------------|----------------------------------------------------------------|
| `link-updates`     | Основной topic для уведомлений                                 |
| `link-updates-dlq` | Dead Letter Queue для сообщений, которые не удалось обработать |

Сообщения передаются в JSON-формате.

Пример сообщения:

```json
{
  "id": 1,
  "url": "https://github.com/spring-projects/spring-kafka",
  "description": "Обнаружено новое обновление",
  "tgChatIds": [123456789]
}
```

Kafka topics создаются декларативно через Spring Kafka `KafkaAdmin` и `NewTopic` beans.
Auto-create topics в Kafka отключён, чтобы случайные topics не создавались из-за опечаток.

Для локального окружения используется Kafka-кластер из трёх брокеров.

Настройки topics:

```yaml
app:
  kafka:
    topic:
      link-updates: link-updates
      link-updates-dlq: link-updates-dlq
      partitions: 3
      replication-factor: 3
      min-in-sync-replicas: 2
```

Для Testcontainers используется single-broker Kafka, поэтому в тестах задаются значения:

```yaml
app:
  kafka:
    topic:
      partitions: 1
      replication-factor: 1
      min-in-sync-replicas: 1
```

---

## Valkey Cache

Scrapper использует Valkey для кэширования списка отслеживаемых ссылок.

Кэшируется endpoint:

```http
GET /links
Tg-Chat-Id: <chat_id>
```

Формат кэша:

* ключ — значение заголовка `Tg-Chat-Id`;
* значение — JSON-ответ endpoint `GET /links`;
* TTL задаётся через переменную окружения `LINK_LIST_CACHE_TTL`.

Запросы с фильтром по тегу не кэшируются:

```http
GET /links?tag=<tag>
```

Это сделано намеренно, так как ключ кэша равен только `Tg-Chat-Id`. Если сохранить под этим ключом отфильтрованный список, следующий обычный `GET /links` может получить неполные данные.

Кэш инвалидируется после операций:

* `POST /links`
* `DELETE /links`
* `DELETE /tg-chat/{id}`

---

### Конфигурация

Основные переменные окружения:

```env
LINK_LIST_CACHE_ENABLED=true
LINK_LIST_CACHE_TTL=10m
VALKEY_CLUSTER_NODES=host.docker.internal:7000,host.docker.internal:7001,host.docker.internal:7002
VALKEY_CONNECT_TIMEOUT=2s
VALKEY_READ_TIMEOUT=2s
VALKEY_CLUSTER_MAX_REDIRECTS=3
VALKEY_CLUSTER_REFRESH_PERIOD=30s
```

Кэш можно отключить:

```env
LINK_LIST_CACHE_ENABLED=false
```

В этом случае Scrapper использует no-op реализацию кэша.

---

### Проверка Valkey Cluster

Поднимите инфраструктуру:

```powershell
docker compose up -d
```

Проверьте создание кластера:

```powershell
docker logs link-tracker-valkey-cluster-init
```

Ожидаемый результат:

```text
[OK] All 16384 slots covered.
```

Проверьте состояние кластера:

```powershell
docker exec -it link-tracker-valkey-1 valkey-cli -c -p 7000 cluster info
```

Ожидаемый результат:

```text
cluster_state:ok
```

Проверить доступность портов с хоста:

```powershell
Test-NetConnection host.docker.internal -Port 7000
Test-NetConnection host.docker.internal -Port 7001
Test-NetConnection host.docker.internal -Port 7002
```

---

### Ручная проверка кэша

Очистите Valkey:

```powershell
docker exec -it link-tracker-valkey-1 valkey-cli -c -p 7000 FLUSHALL
```

Зарегистрируйте чат:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/tg-chat/123"
```

Добавьте ссылку:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/links" `
  -Headers @{ "Tg-Chat-Id" = "123" } `
  -ContentType "application/json" `
  -Body '{"link":"https://github.com/test-owner/test-repo","tags":["java","backend"]}'
```

После `POST /links` кэш должен быть пустым, потому что список сохраняется в кэш только при `GET /links`:

```powershell
docker exec -it link-tracker-valkey-1 valkey-cli -c -p 7000 GET 123
```

Ожидаемый результат:

```text
(nil)
```

Выполните запрос списка ссылок:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8081/links" `
  -Headers @{ "Tg-Chat-Id" = "123" }
```

После этого в Valkey должна появиться JSON-запись:

```powershell
docker exec -it link-tracker-valkey-1 valkey-cli -c -p 7000 GET 123
```

Проверьте TTL:

```powershell
docker exec -it link-tracker-valkey-1 valkey-cli -c -p 7000 TTL 123
```

Если задано `LINK_LIST_CACHE_TTL=10m`, значение TTL должно быть меньше или равно `600`.

---

### Проверка чтения из кэша

Для наглядной проверки можно вручную подложить значение в Valkey:

```powershell
docker exec -it link-tracker-valkey-1 valkey-cli -c -p 7000 SET 123 "[{\"id\":999,\"url\":\"https://fake.example.com\",\"tags\":[\"fake\"],\"lastUpdate\":\"2026-06-10T18:00:00Z\"}]"
```

После этого обычный запрос:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8081/links" `
  -Headers @{ "Tg-Chat-Id" = "123" }
```

должен вернуть ссылку:

```text
https://fake.example.com
```

Это означает, что Scrapper прочитал список ссылок из Valkey, а не из PostgreSQL.

---

### Проверка инвалидации

Добавьте новую ссылку:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/links" `
  -Headers @{ "Tg-Chat-Id" = "123" } `
  -ContentType "application/json" `
  -Body '{"link":"https://stackoverflow.com/questions/123","tags":["java"]}'
```

После добавления ссылки кэш должен быть удалён:

```powershell
docker exec -it link-tracker-valkey-1 valkey-cli -c -p 7000 GET 123
```

Ожидаемый результат:

```text
(nil)
```

Аналогично кэш очищается при удалении ссылки:

```powershell
Invoke-RestMethod `
  -Method Delete `
  -Uri "http://localhost:8081/links" `
  -Headers @{ "Tg-Chat-Id" = "123" } `
  -ContentType "application/json" `
  -Body '{"link":"https://stackoverflow.com/questions/123"}'
```

и при удалении чата:

```powershell
Invoke-RestMethod `
  -Method Delete `
  -Uri "http://localhost:8081/tg-chat/123"
```

---

## Нагрузочное тестирование

Для проверки эффекта от Valkey Cache используется нагрузочное тестирование с помощью `k6`.

Тестируемый endpoint:

```http
GET /links
Tg-Chat-Id: <chat_id>
```

Цель тестирования — сравнить поведение `Scrapper` в двух режимах:

* без кэша: `LINK_LIST_CACHE_ENABLED=false`;
* с Valkey Cache: `LINK_LIST_CACHE_ENABLED=true`.

В обоих режимах используется один и тот же сценарий нагрузки. Это позволяет сравнить задержку ответа и количество ошибок при одинаковой интенсивности запросов.

---

### Структура файлов

Файлы нагрузочного тестирования находятся в директории:

```text
load-tests
```

Структура:

```text
load-tests
├── k6
│   └── link-list-cache.js
└── results
    └── .gitkeep
```

Назначение файлов:

* `load-tests/k6/link-list-cache.js` — сценарий нагрузочного тестирования;
* `load-tests/results` — директория для результатов запусков;
* `.gitkeep` — пустой файл, который нужен, чтобы Git сохранил пустую директорию `results`.

Git не хранит пустые директории. Поэтому `.gitkeep` используется как техническая заглушка.

---

### Сценарий тестирования

Сценарий `link-list-cache.js` выполняет следующие действия:

1. Регистрирует тестовый Telegram-чат.
2. Добавляет заданное количество ссылок.
3. Выполняет прогревочный запрос `GET /links`.
4. Запускает нагрузку на `GET /links`.
5. Проверяет, что endpoint возвращает статус `200`.
6. Проверяет, что ответ содержит ожидаемый список ссылок.
7. Сохраняет summary-результат в директорию `load-tests/results`.

Прогревочный запрос нужен потому, что кэш списка ссылок наполняется именно при первом вызове:

```http
GET /links
```

После этого повторные запросы могут обслуживаться из Valkey.

---

### Docker Compose для k6

Для запуска нагрузочных тестов используется отдельный compose-файл:

```text
docker-compose.load-tests.yml
```

Он запускает контейнер `grafana/k6` и монтирует директории со сценариями и результатами.

Пример сервиса:

```yaml
services:
  k6:
    image: grafana/k6:latest
    container_name: link-tracker-k6
    extra_hosts:
      - "host.docker.internal:host-gateway"
    volumes:
      - ./load-tests/k6:/scripts:ro
      - ./load-tests/results:/results
    environment:
      BASE_URL: ${LOAD_TEST_BASE_URL:-http://host.docker.internal:8081}
      CHAT_ID: ${LOAD_TEST_CHAT_ID:-900001}
      LINKS_COUNT: ${LOAD_TEST_LINKS_COUNT:-20}
      LOAD_RATE: ${LOAD_TEST_RATE:-50}
      LOAD_DURATION: ${LOAD_TEST_DURATION:-1m}
      PRE_ALLOCATED_VUS: ${LOAD_TEST_PRE_ALLOCATED_VUS:-20}
      MAX_VUS: ${LOAD_TEST_MAX_VUS:-100}
      CLEANUP_AFTER_TEST: ${LOAD_TEST_CLEANUP_AFTER_TEST:-false}
    command:
      - run
      - --summary-export=/results/${LOAD_TEST_RESULT_FILE:-link-list-cache-result.json}
      - /scripts/link-list-cache.js
```

`host.docker.internal` используется, потому что `k6` запускается внутри Docker-контейнера, а `Scrapper` обычно запускается локально из IDE или через Maven.

---

### Запуск теста без кэша

Сначала нужно запустить `Scrapper` с отключённым кэшем:

```env
LINK_LIST_CACHE_ENABLED=false
```

После запуска приложения выполните нагрузочный тест:

```powershell
$env:LOAD_TEST_BASE_URL="http://host.docker.internal:8081"
$env:LOAD_TEST_CHAT_ID="910001"
$env:LOAD_TEST_LINKS_COUNT="20"
$env:LOAD_TEST_RATE="50"
$env:LOAD_TEST_DURATION="1m"
$env:LOAD_TEST_PRE_ALLOCATED_VUS="20"
$env:LOAD_TEST_MAX_VUS="100"
$env:LOAD_TEST_RESULT_FILE="link-list-cache-disabled.json"

docker compose -f docker-compose.load-tests.yml run --rm k6
```

Результат будет сохранён в файл:

```text
load-tests/results/link-list-cache-disabled.json
```

---

### Запуск теста с Valkey Cache

Перед запуском нужно убедиться, что Valkey Cluster поднят:

```powershell
docker compose up -d
```

Проверить состояние кластера:

```powershell
docker exec -it link-tracker-valkey-1 valkey-cli -c -p 7000 cluster info
```

Ожидаемый результат:

```text
cluster_state:ok
```

Затем нужно запустить `Scrapper` с включённым кэшем:

```env
SERVER_ADDRESS=0.0.0.0
SERVER_PORT=8081
LINK_LIST_CACHE_ENABLED=true
LINK_LIST_CACHE_TTL=10m
VALKEY_CLUSTER_NODES=host.docker.internal:7000,host.docker.internal:7001,host.docker.internal:7002
```

Перед тестом можно очистить Valkey:

```powershell
docker exec -it link-tracker-valkey-1 valkey-cli -c -p 7000 FLUSHALL
```

После запуска приложения выполните нагрузочный тест:

```powershell
$env:LOAD_TEST_BASE_URL="http://host.docker.internal:8081"
$env:LOAD_TEST_CHAT_ID="920001"
$env:LOAD_TEST_LINKS_COUNT="20"
$env:LOAD_TEST_RATE="50"
$env:LOAD_TEST_DURATION="1m"
$env:LOAD_TEST_PRE_ALLOCATED_VUS="20"
$env:LOAD_TEST_MAX_VUS="100"
$env:LOAD_TEST_RESULT_FILE="link-list-cache-enabled.json"

docker compose -f docker-compose.load-tests.yml run --rm k6
```

Результат будет сохранён в файл:

```text
load-tests/results/link-list-cache-enabled.json
```

---

### Проверка, что кэш использовался

После запуска теста с включённым кэшем можно проверить значение в Valkey:

```powershell
docker exec -it link-tracker-valkey-1 valkey-cli -c -p 7000 GET 920001
```

Ожидается JSON со списком ссылок.

Также можно проверить TTL:

```powershell
docker exec -it link-tracker-valkey-1 valkey-cli -c -p 7000 TTL 920001
```

Если задано:

```env
LINK_LIST_CACHE_TTL=10m
```

то TTL должен быть меньше или равен `600`.

---

### Основные параметры нагрузки

|          Переменная           |                      Описание                      |               Пример               |
|-------------------------------|----------------------------------------------------|------------------------------------|
| `LOAD_TEST_BASE_URL`          | URL запущенного Scrapper                           | `http://host.docker.internal:8081` |
| `LOAD_TEST_CHAT_ID`           | ID тестового чата                                  | `920001`                           |
| `LOAD_TEST_LINKS_COUNT`       | Количество ссылок, добавляемых перед тестом        | `20`                               |
| `LOAD_TEST_RATE`              | Количество запросов в секунду                      | `50`                               |
| `LOAD_TEST_DURATION`          | Длительность теста                                 | `1m`                               |
| `LOAD_TEST_PRE_ALLOCATED_VUS` | Предварительно выделенные виртуальные пользователи | `20`                               |
| `LOAD_TEST_MAX_VUS`           | Максимальное количество виртуальных пользователей  | `100`                              |
| `LOAD_TEST_RESULT_FILE`       | Имя файла с результатом                            | `link-list-cache-enabled.json`     |

---

### Метрики k6

В выводе `k6` используются следующие основные метрики:

|        Метрика        |                   Значение                   |
|-----------------------|----------------------------------------------|
| `http_req_duration`   | Время выполнения HTTP-запросов               |
| `http_req_failed`     | Доля неуспешных HTTP-запросов                |
| `http_reqs`           | Общее количество HTTP-запросов               |
| `checks`              | Доля успешно пройденных проверок             |
| `get_links_duration`  | Время выполнения именно `GET /links`         |
| `get_links_status_ok` | Доля ответов `GET /links` со статусом `200`  |
| `get_links_body_ok`   | Доля ответов `GET /links` с корректным телом |

Для сравнения важнее всего смотреть:

* `avg` — среднее время ответа;
* `p(95)` — 95% запросов были быстрее этого значения;
* `max` — самый медленный запрос;
* `http_req_failed` — процент ошибок;
* `checks` — процент успешных проверок.

---

### Результаты нагрузочного тестирования

Сценарий:

* endpoint: `GET /links`;
* количество ссылок у тестового чата: `20`;
* нагрузка: `50 req/s`;
* длительность: `1m`;
* инструмент: `k6`;
* режим нагрузки: `constant-arrival-rate`.

|     Режим      | avg latency | p95 latency | max latency | failed requests | http reqs |
|----------------|------------:|------------:|------------:|----------------:|----------:|
| Без кэша       |         6.8 |        15.7 |       130.3 |           0.007 |      3023 |
| С Valkey Cache |         4.4 |         9.5 |       131.9 |               0 |      3022 |

Вывод:

При включённом Valkey Cache повторные запросы `GET /links` обслуживаются из кэша. Это снижает количество обращений к PostgreSQL и уменьшает задержку ответа для сценария частого чтения списка ссылок.

---

## Retry и DLQ

Bot consumer обрабатывает сообщения из `link-updates`.

Если во время обработки возникает бизнес-ошибка, например Telegram API временно недоступен, consumer повторяет обработку заданное количество раз.

После исчерпания retry сообщение отправляется в:

```text
link-updates-dlq
```

Ошибки десериализации и валидации отправляются в DLQ сразу, без retry.

Конфигурация:

```yaml
app:
  kafka:
    consumer:
      retry-attempts: 3
      retry-interval: 1s
```

---

## Локальный запуск

### Требования

Перед запуском должны быть установлены:

* JDK 25
* Docker Desktop
* Maven или Maven Wrapper

---

## Переменные окружения

Создайте `.env` файл в корне проекта.

Пример:

```env
POSTGRES_DB=scrapper
DB_USER=postgres
DB_PASSWORD=postgres

TELEGRAM_TOKEN=your_telegram_bot_token

GITHUB_TOKEN=your_github_token

STACKOVERFLOW_KEY=test
STACKOVERFLOW_ACCESS_KEY=test

KAFKA_BOOTSTRAP_SERVERS=localhost:9092,localhost:9093,localhost:9094
NOTIFICATION_TRANSPORT=KAFKA
ACCESS_TYPE=SQL
```

---

## Запуск инфраструктуры

```powershell
docker compose up -d
```

После запуска будут доступны:

|     Сервис     |       URL / Port        |
|----------------|-------------------------|
| PostgreSQL     | `localhost:5432`        |
| Kafka broker 1 | `localhost:9092`        |
| Kafka broker 2 | `localhost:9093`        |
| Kafka broker 3 | `localhost:9094`        |
| Kafka UI       | `http://localhost:8090` |

Проверить Kafka topics:

```powershell
docker exec -it link-tracker-kafka-1 kafka-topics `
  --bootstrap-server kafka-1:29092 `
  --list
```

Ожидаемый результат после старта приложения:

```text
link-updates
link-updates-dlq
```

---

## Запуск Bot

PowerShell:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092,localhost:9093,localhost:9094"
$env:TELEGRAM_TOKEN="your_telegram_bot_token"

.\mvnw.cmd -pl bot spring-boot:run
```

Ожидаемые признаки успешного запуска:

```text
Started BotApplication
Subscribed to topic(s): link-updates
partitions assigned: [link-updates-0, link-updates-1, link-updates-2]
```

---

## Запуск Scrapper

PowerShell:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092,localhost:9093,localhost:9094"
$env:NOTIFICATION_TRANSPORT="KAFKA"
$env:DB_URL="jdbc:postgresql://localhost:5432/scrapper"
$env:DB_USER="postgres"
$env:DB_PASSWORD="postgres"
$env:GITHUB_TOKEN="your_github_token"
$env:STACKOVERFLOW_KEY="test"
$env:STACKOVERFLOW_ACCESS_KEY="test"
$env:ACCESS_TYPE="SQL"

.\mvnw.cmd -pl scrapper spring-boot:run
```

---

## Проверка Kafka вручную

### Проверка успешного сообщения

Откройте producer:

```powershell
docker exec -it link-tracker-kafka-1 kafka-console-producer `
  --bootstrap-server kafka-1:29092 `
  --topic link-updates
```

Отправьте сообщение, заменив `123456789` на реальный Telegram chat id:

```json
{"id":1,"url":"https://github.com/test-owner/test-repo","description":"Kafka работает: сообщение пришло через topic link-updates","tgChatIds":[123456789]}
```

Если `bot` запущен, пользователь получит сообщение в Telegram.

---

### Проверка DLQ

Отправьте невалидное сообщение с пустым `description`:

```json
{"id":2,"url":"https://github.com/test-owner/test-repo","description":"","tgChatIds":[123456789]}
```

Проверьте DLQ:

```powershell
docker exec -it link-tracker-kafka-1 kafka-console-consumer `
  --bootstrap-server kafka-1:29092 `
  --topic link-updates-dlq `
  --from-beginning
```

Сообщение должно появиться в `link-updates-dlq`.

---

## Проверка consumer group

```powershell
docker exec -it link-tracker-kafka-1 kafka-consumer-groups `
  --bootstrap-server kafka-1:29092 `
  --describe `
  --group bot-link-updates
```

Если `LAG = 0`, значит `bot` прочитал все сообщения из Kafka.

---

## Работа с Telegram Bot

Основные команды:

|  Команда   |               Описание               |
|------------|--------------------------------------|
| `/start`   | Зарегистрировать чат                 |
| `/help`    | Показать список команд               |
| `/track`   | Начать отслеживание ссылки           |
| `/untrack` | Прекратить отслеживание ссылки       |
| `/list`    | Показать список отслеживаемых ссылок |

Пример сценария:

```text
/start
/track https://github.com/spring-projects/spring-kafka
/list
/untrack https://github.com/spring-projects/spring-kafka
```

---

## Режимы отправки уведомлений

По умолчанию используется Kafka:

```env
NOTIFICATION_TRANSPORT=KAFKA
```

Для переключения на HTTP:

```env
NOTIFICATION_TRANSPORT=HTTP
```

HTTP-режим полезен для локальной отладки и совместимости со старыми тестами.

---

## Режимы доступа к базе данных

Scrapper поддерживает несколько реализаций storage layer.

Пример выбора SQL/JDBC:

```env
ACCESS_TYPE=SQL
```

Пример выбора ORM/JPA:

```env
ACCESS_TYPE=ORM
```

---

## Тесты

Запуск всех тестов:

```powershell
.\mvnw.cmd test
```

Запуск тестов Bot:

```powershell
.\mvnw.cmd -pl bot test
```

Запуск тестов Scrapper:

```powershell
.\mvnw.cmd -pl scrapper test
```

Запуск Kafka producer integration test:

```powershell
.\mvnw.cmd -pl scrapper -Dtest=KafkaNotificationSenderIntegrationTest test
```

Запуск Kafka consumer tests:

```powershell
.\mvnw.cmd -pl bot -Dtest=LinkUpdateKafkaConsumerIntegrationTest,LinkUpdateKafkaConsumerDlqIntegrationTest test
```

Запуск сквозного E2E-теста:

```powershell
.\mvnw.cmd -pl e2e-tests -am -Dtest=ScrapperToBotKafkaIntegrationTest test
```

E2E-тест проверяет цепочку:

```text
Scrapper KafkaNotificationSender
        |
        v
Kafka topic link-updates
        |
        v
Bot Kafka consumer
        |
        v
TelegramClient.sendMessage(...)
```

---

## Форматирование кода

Перед коммитом рекомендуется выполнить:

```powershell
.\mvnw.cmd spotless:apply
```

Проверка:

```powershell
.\mvnw.cmd spotless:check
```

---

## Liquibase

Миграции находятся в директории:

```text
migrations
```

Основной changelog:

```text
migrations/db.changelog-master.yaml
```

В приложении используется classpath-путь:

```yaml
spring:
  liquibase:
    change-log: classpath:migrations/db.changelog-master.yaml
```

---

## Docker Compose

`docker-compose.yml` поднимает:

* PostgreSQL
* Zookeeper
* Kafka broker 1
* Kafka broker 2
* Kafka broker 3
* Kafka UI

Kafka настроена с отключённым auto-create topics:

```yaml
KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
```

Topics создаются приложением через `KafkaAdmin`.

---

## Полезные команды

Список topics:

```powershell
docker exec -it link-tracker-kafka-1 kafka-topics `
  --bootstrap-server kafka-1:29092 `
  --list
```

Описание topic:

```powershell
docker exec -it link-tracker-kafka-1 kafka-topics `
  --bootstrap-server kafka-1:29092 `
  --describe `
  --topic link-updates
```

Чтение сообщений из topic:

```powershell
docker exec -it link-tracker-kafka-1 kafka-console-consumer `
  --bootstrap-server kafka-1:29092 `
  --topic link-updates `
  --from-beginning
```

Чтение сообщений из DLQ:

```powershell
docker exec -it link-tracker-kafka-1 kafka-console-consumer `
  --bootstrap-server kafka-1:29092 `
  --topic link-updates-dlq `
  --from-beginning
```

Просмотр consumer group:

```powershell
docker exec -it link-tracker-kafka-1 kafka-consumer-groups `
  --bootstrap-server kafka-1:29092 `
  --describe `
  --group bot-link-updates
```

---

## Основной сценарий работы

1. Пользователь отправляет `/start` Telegram-боту.
2. Bot регистрирует чат в Scrapper.
3. Пользователь добавляет ссылку через `/track`.
4. Scrapper сохраняет подписку в PostgreSQL.
5. Scheduler в Scrapper периодически проверяет ссылку через GitHub или Stack Overflow API.
6. При обнаружении обновления Scrapper публикует `LinkUpdate` в Kafka topic `link-updates`.
7. Bot читает сообщение из Kafka.
8. Bot отправляет уведомление пользователю в Telegram.
9. Если обработка не удалась, сообщение попадает в `link-updates-dlq`.

---

## Надёжность

В проекте реализованы следующие механизмы надёжности:

* Kafka как асинхронная очередь между Scrapper и Bot.
* Retry обработки Kafka-сообщений.
* Dead Letter Queue для ошибочных сообщений.
* Валидация входящих Kafka-сообщений.
* Обработка ошибок десериализации.
* Liquibase-миграции для воспроизводимой схемы БД.
* Testcontainers для интеграционных тестов.
* Возможность переключения между HTTP и Kafka transport.

---

## Примечания по безопасности

Не коммитьте в Git:

```text
.env
*.env
```

В репозиторий можно добавить только шаблон:

```text
.env.example
```

Все реальные токены должны храниться локально или в secret storage CI/CD.

---

