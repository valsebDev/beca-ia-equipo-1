-- Esquema de ms-catalog (catalog_db): categorías, prioridades y políticas SLA.

CREATE TABLE categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_categories_name_length CHECK (char_length(name) >= 2),
    -- justifica: HU-01 y HU-03 rechazan nombres duplicados al crear y al renombrar una categoría (consulta de unicidad por name)
    CONSTRAINT uk_categories_name UNIQUE (name)
);

-- justifica: HU-02 listado de categorías activas ordenado por nombre para el selector al crear un ticket (WHERE active = true ORDER BY name)
CREATE INDEX idx_categories_active_name ON categories (active, name);

CREATE TABLE priorities (
    id     BIGSERIAL   PRIMARY KEY,
    code   VARCHAR(20) NOT NULL,
    name   VARCHAR(50) NOT NULL,
    level  SMALLINT    NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_priorities_code CHECK (code IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_priorities_level CHECK (level BETWEEN 1 AND 10),
    -- justifica: HU-05 impide registrar dos veces el mismo código de prioridad (búsqueda por code antes del INSERT)
    CONSTRAINT uk_priorities_code UNIQUE (code),
    -- justifica: HU-05 garantiza un único nivel de urgencia por prioridad, base del orden de atención (ORDER BY level)
    CONSTRAINT uk_priorities_level UNIQUE (level)
);

CREATE TABLE sla_policies (
    id               BIGSERIAL   PRIMARY KEY,
    priority_id      BIGINT      NOT NULL,
    response_hours   INTEGER     NOT NULL,
    resolution_hours INTEGER     NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_sla_policies_response_positive CHECK (response_hours > 0),
    CONSTRAINT ck_sla_policies_resolution_order CHECK (resolution_hours >= response_hours),
    CONSTRAINT fk_sla_policies_priority FOREIGN KEY (priority_id) REFERENCES priorities (id) ON DELETE RESTRICT,
    -- justifica: HU-06 y HU-05 garantizan una sola política SLA por prioridad (relación 1:1) y permiten obtener el SLA de una prioridad por su id
    CONSTRAINT uk_sla_policies_priority UNIQUE (priority_id)
);
