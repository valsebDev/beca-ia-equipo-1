-- =============================================================================
-- Migración Flyway: V5__alinear_columnas_usuarios.sql
-- Microservicio: ms-users (Mesa de Ayuda)
-- Descripción: Completa la alineación de 'users' con ARCHITECTURE.md §2.1.
--   - Renombra 'is_active' a 'active' y 'password' a 'password_hash'.
--   - Ajusta 'role' a VARCHAR(20) y 'password_hash' a VARCHAR(100).
--   - Convierte 'created_at' a TIMESTAMPTZ (los valores previos se interpretan en UTC) y agrega 'updated_at'.
--   - Normaliza los emails a minúsculas e indexa lower(email) como único.
-- Las columnas heredadas (user_identifier, name, paternal_name, maternal_name, jwt_*,
-- refresh_token_*, failed_attempts, locked_until) se conservan sin mapear en JPA.
-- =============================================================================

ALTER TABLE users RENAME COLUMN is_active TO active;
ALTER TABLE users RENAME COLUMN password TO password_hash;
ALTER TABLE users ALTER COLUMN password_hash TYPE VARCHAR(100);
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20);
ALTER TABLE users ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE users ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE users SET email = LOWER(email);
CREATE UNIQUE INDEX uk_users_email_lower ON users (LOWER(email));
