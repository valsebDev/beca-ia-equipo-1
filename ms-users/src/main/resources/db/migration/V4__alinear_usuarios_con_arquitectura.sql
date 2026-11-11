-- =============================================================================
-- Migración Flyway: V4__alinear_usuarios_con_arquitectura.sql
-- Microservicio: ms-users (Mesa de Ayuda)
-- Descripción: Alinea la tabla 'users' con ARCHITECTURE.md §2.1 sin borrar datos.
--   - Agrega 'full_name' y lo rellena con nombre + apellidos existentes.
--   - Amplía 'email' a 254 caracteres (§2.1 y §3.1).
--   - Relaja NOT NULL en columnas heredadas que dejan de mapearse.
-- Las columnas heredadas se conservan para no perder datos; V1 no se modifica.
-- =============================================================================

ALTER TABLE users ADD COLUMN full_name VARCHAR(150);

-- LEFT(..., 150) evita fallar la migración si un nombre histórico supera el nuevo límite.
UPDATE users
   SET full_name = LEFT(TRIM(CONCAT_WS(' ', name, paternal_name, maternal_name)), 150);

ALTER TABLE users ALTER COLUMN full_name SET NOT NULL;

ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(254);

ALTER TABLE users ALTER COLUMN name DROP NOT NULL;
ALTER TABLE users ALTER COLUMN paternal_name DROP NOT NULL;
ALTER TABLE users ALTER COLUMN user_identifier DROP NOT NULL;
