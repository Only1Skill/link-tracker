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

