# 💳 Bank Cards Management System

Система управления банковскими картами с JWT аутентификацией и ролевым доступом (ADMIN/USER).

## 🚀 Технологии

- Java 17
- Spring Boot 3.1.5
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Liquibase
- Docker
- Swagger/OpenAPI
- JUnit 5 + Mockito

## 📋 Функциональность

### 👤 Пользователь
- Просмотр своих карт (с пагинацией и фильтрацией)
- Просмотр баланса карты
- Запрос блокировки карты
- Переводы между своими картами

### 👑 Администратор
- Управление пользователями (создание, блокировка, смена роли)
- Управление картами (создание, блокировка, активация, удаление)
- Просмотр всех карт и пользователей

### 🔐 Безопасность
- JWT токены для аутентификации
- Шифрование номеров карт
- Маскирование номеров (**** **** **** 1234)
- Ролевой доступ (ADMIN/USER)

## 🏁 Запуск проекта

### 📦 Локальный запуск

1. **Установить PostgreSQL** и создать базу данных:
```sql
CREATE DATABASE bank_cards;
CREATE USER bank_user WITH PASSWORD 'bank_pass';
GRANT ALL PRIVILEGES ON DATABASE bank_cards TO bank_user;