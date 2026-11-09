CREATE TABLE tickets (
    id                    BIGSERIAL PRIMARY KEY,
    title                 VARCHAR(150)  NOT NULL,
    description           VARCHAR(4000) NOT NULL,
    status                VARCHAR(20)   NOT NULL,
    category_id           BIGINT        NOT NULL,
    priority_id           BIGINT        NOT NULL,
    priority_code         VARCHAR(20)   NOT NULL,
    sla_response_hours    INTEGER       NOT NULL,
    sla_resolution_hours  INTEGER       NOT NULL,
    response_due_at       TIMESTAMPTZ   NOT NULL,
    resolution_due_at     TIMESTAMPTZ   NOT NULL,
    requester_id          BIGINT        NOT NULL,
    assignee_id           BIGINT,
    first_response_at     TIMESTAMPTZ,
    resolved_at           TIMESTAMPTZ,
    closed_at             TIMESTAMPTZ,
    version               BIGINT        NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_tickets_status CHECK (status IN ('ABIERTO', 'ASIGNADO', 'EN_PROGRESO', 'RESUELTO', 'CERRADO', 'CANCELADO')),
    CONSTRAINT ck_tickets_priority_code CHECK (priority_code IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_tickets_sla_response CHECK (sla_response_hours > 0),
    CONSTRAINT ck_tickets_sla_resolution CHECK (sla_resolution_hours >= sla_response_hours)
);

CREATE TABLE ticket_history (
    id                BIGSERIAL PRIMARY KEY,
    ticket_id         BIGINT       NOT NULL,
    event_type        VARCHAR(20)  NOT NULL,
    from_status       VARCHAR(20),
    to_status         VARCHAR(20),
    from_assignee_id  BIGINT,
    to_assignee_id    BIGINT,
    changed_by        BIGINT       NOT NULL,
    comment           VARCHAR(1000),
    changed_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_ticket_history_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ticket_history_event_type CHECK (event_type IN ('CREATED', 'ASSIGNED', 'STATUS_CHANGED')),
    CONSTRAINT ck_ticket_history_from_status CHECK (from_status IS NULL OR from_status IN ('ABIERTO', 'ASIGNADO', 'EN_PROGRESO', 'RESUELTO', 'CERRADO', 'CANCELADO')),
    CONSTRAINT ck_ticket_history_to_status CHECK (to_status IS NULL OR to_status IN ('ABIERTO', 'ASIGNADO', 'EN_PROGRESO', 'RESUELTO', 'CERRADO', 'CANCELADO'))
);

-- justifica: HU-02 listado "mis tickets" del solicitante ordenado por fecha de creacion descendente
CREATE INDEX idx_tickets_requester_created ON tickets (requester_id, created_at DESC);

-- justifica: HU-04 bandeja del agente para filtrar sus tickets asignados por estado
CREATE INDEX idx_tickets_assignee_status ON tickets (assignee_id, status);

-- justifica: HU-02 consulta de tickets vencidos por SLA (resolution_due_at por estado)
CREATE INDEX idx_tickets_status_resolution_due ON tickets (status, resolution_due_at);

-- justifica: HU-05 consulta del historial de un ticket en orden cronologico
CREATE INDEX idx_ticket_history_ticket_changed ON ticket_history (ticket_id, changed_at);
