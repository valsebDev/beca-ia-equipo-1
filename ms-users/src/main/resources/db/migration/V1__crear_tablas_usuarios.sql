-- =============================================================================
-- Migración Flyway: V1__crear_tablas_usuarios.sql
-- Microservicio: ms-users (Mesa de Ayuda)
-- Motor: PostgreSQL 16
-- Descripción: Creación de la tabla 'users', restricciones e índices justificados.
-- =============================================================================

CREATE TABLE users (
    -- Identificador numérico interno autoincremental para clave primaria y relaciones físicas eficientes
    id                          BIGSERIAL PRIMARY KEY,

    -- Identificador público/externo expuesto en APIs REST y tokens (UUID v4)
    -- NOT NULL: Obligatorio para garantizar identidad pública inmutable en la arquitectura distribuida
    -- UNIQUE: Previene colisiones al resolver usuarios por su identificador público (HU-04, HU-05)
    user_identifier             VARCHAR(36) NOT NULL UNIQUE,

    -- Nombre de pila del usuario
    -- NOT NULL: Campo requerido según la especificación de registro (HU-01) y perfil (HU-04)
    name                        VARCHAR(100) NOT NULL,

    -- Primer apellido / apellido paterno
    -- NOT NULL: Obligatorio para identificación completa del titular de la cuenta (HU-01, HU-04)
    paternal_name               VARCHAR(100) NOT NULL,

    -- Segundo apellido / apellido materno
    -- Opcional (admite NULL) para dar soporte a variaciones internacionales o personas sin segundo apellido
    maternal_name               VARCHAR(100),

    -- Correo electrónico institucional o personal utilizado como identificador de login
    -- NOT NULL: Requerimiento mandatorio de autenticación (HU-01, HU-02)
    -- UNIQUE: Cumplimiento de RN-01 para asegurar que no existan cuentas duplicadas con el mismo correo
    email                       VARCHAR(150) NOT NULL UNIQUE,

    -- Hash de la contraseña del usuario (formato BCrypt de 60 caracteres)
    -- NOT NULL: RN-02 y RNF-01 exigen que toda cuenta local posea una credencial protegida criptográficamente
    password                    VARCHAR(255) NOT NULL,

    -- Rol operativo del usuario dentro del sistema (SOLICITANTE, AGENTE, ADMIN)
    -- NOT NULL: RN-03 exige que todo usuario posea un rol asignado para control de acceso RBAC
    role                        VARCHAR(30) NOT NULL,

    -- Bandera de habilitación lógica de la cuenta
    -- NOT NULL con valor por defecto TRUE: Controla el ciclo de vida de la cuenta y desactivación (HU-05)
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,

    -- Marca temporal del registro en el sistema
    -- NOT NULL con valor por defecto now(): Requerimiento de auditoría RNF-02
    created_at                  TIMESTAMP NOT NULL DEFAULT now(),

    -- Identificador único del JWT activo emitido ('jti')
    -- Admite NULL cuando el usuario no tiene una sesión activa o se ha cerrado
    jwt_id                      VARCHAR(100),

    -- Marca temporal de vencimiento del JWT activo
    -- Admite NULL si no hay sesión abierta; permite validar revocaciones inmediatas
    jwt_expires_at              TIMESTAMP,

    -- Hash criptográfico (SHA-256) del token de refresco almacenado para renovar sesión
    -- Admite NULL si el usuario no cuenta con sesión de refresco vigente
    refresh_token_hash          VARCHAR(255),

    -- Marca temporal de vencimiento del token de refresco
    -- Admite NULL si no hay token de refresco activo
    refresh_token_expires_at    TIMESTAMP,
    -- Columnas para control de intentos y bloqueo temporal (HU-03)
    failed_attempts             INT NOT NULL DEFAULT 0,
    locked_until                TIMESTAMP,
    -- Restricción a nivel de dominio para asegurar la integridad de los roles admitidos
    CONSTRAINT chk_users_role CHECK (role IN ('SOLICITANTE', 'AGENTE', 'ADMIN'))
);

-- =============================================================================
-- JUSTIFICACIÓN DE ÍNDICES PROPUESTOS
-- =============================================================================

-- 1. Justificación para idx_users_email (implícito por restricción UNIQUE):
-- PostgreSQL crea automáticamente un índice B-Tree único al definir 'email VARCHAR(150) NOT NULL UNIQUE'.
-- Justificación HU: Optimiza el proceso crítico de búsqueda de usuario por correo durante la autenticación
-- (HU-02: POST /api/v1/auth/login) y la verificación inmediata de no duplicidad durante el registro (HU-01).

-- 2. Justificación para idx_users_user_identifier (implícito por restricción UNIQUE):
-- PostgreSQL crea automáticamente un índice B-Tree único al definir 'user_identifier VARCHAR(36) NOT NULL UNIQUE'.
-- Justificación HU: Permite búsquedas directas O(log N) cuando los microservicios externos o el propio frontend
-- consultan o actualizan el perfil por UUID (HU-04: /api/v1/users/me o /api/v1/users/{identifier})
-- y cuando el administrador ejecuta la desactivación lógica (HU-05: PATCH /api/v1/users/{identifier}/deactivate).

-- 3. Índice para acelerar la validación y rotación de Refresh Tokens
-- Justificación HU: Durante el flujo de rotación de tokens de refresco, el backend busca al usuario
-- comparando el hash recibido contra 'refresh_token_hash' filtrando adicionalmente por su vigencia.
-- Al ser un campo con alta cardinalidad y sujeto a consultas directas en autenticación extendida,
-- evita escaneos secuenciales (Sequential Scans) de la tabla 'users'.
CREATE INDEX idx_users_refresh_token_hash 
    ON users (refresh_token_hash) 
    WHERE refresh_token_hash IS NOT NULL;

-- 4. Índice compuesto para administración, auditoría y listados por rol y estado
-- Justificación HU: Para HU-05 y operaciones administrativas de gestión de agentes/solicitantes,
-- las consultas de catálogo de usuarios frecuentemente filtran por estado operativo ('is_active')
-- y clasifican por rol ('role'). Este índice optimiza el filtrado sin penalizar escrituras.
CREATE INDEX idx_users_role_is_active 
    ON users (role, is_active);