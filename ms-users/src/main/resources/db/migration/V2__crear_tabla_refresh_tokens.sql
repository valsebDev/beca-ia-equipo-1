-- =============================================================================
-- Migración Flyway: V2__crear_tabla_refresh_tokens.sql
-- Microservicio: ms-users (Mesa de Ayuda)
-- Motor: PostgreSQL 16
-- Descripción: Tabla 'refresh_tokens' para rotación y revocación de sesiones (ARCHITECTURE §2.1).
-- =============================================================================

CREATE TABLE refresh_tokens (
    -- Clave primaria autoincremental
    id              BIGSERIAL PRIMARY KEY,

    -- Usuario propietario del token de refresco
    -- ON DELETE CASCADE: los usuarios nunca se borran físicamente (se desactivan), pero la FK garantiza limpieza
    user_id         BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    -- Hash SHA-256 (64 hex) del token opaco; el token en claro nunca se persiste
    -- UNIQUE: lookup directo en el endpoint de refresh (HU-02)
    token_hash      CHAR(64) NOT NULL UNIQUE,

    -- Vencimiento del token (7 días por defecto)
    expires_at      TIMESTAMPTZ NOT NULL,

    -- Momento de revocación; NULL indica token vigente
    -- Se usa para logout, rotación y detección de reutilización
    revoked_at      TIMESTAMPTZ,

    -- Marca temporal de emisión
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Índice para revocar en lote todos los tokens de un usuario (logout global y desactivación de cuenta)
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
