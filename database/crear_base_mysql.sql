-- Ejecutar una sola vez en MySQL Workbench como root u otro administrador.
-- No borra bases ni tablas existentes.
CREATE DATABASE IF NOT EXISTS el_tumi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tumi'@'localhost' IDENTIFIED BY 'tumi_local_2026';
GRANT ALL PRIVILEGES ON el_tumi.* TO 'tumi'@'localhost';
-- Spring Boot / Flyway crea las 54 tablas de negocio y su tabla de migraciones.
-- Si el usuario tumi ya existia, conserva su clave anterior: ajusta config/application.properties.
