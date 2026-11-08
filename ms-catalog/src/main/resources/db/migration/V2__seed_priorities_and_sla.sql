-- Datos semilla: 4 prioridades con su política SLA y 3 categorías iniciales.
-- Valores de SLA ajustables por el equipo (ARCHITECTURE §2.2).

INSERT INTO priorities (code, name, level, active) VALUES
    ('CRITICA', 'Crítica', 1, TRUE),
    ('ALTA',    'Alta',    2, TRUE),
    ('MEDIA',   'Media',   3, TRUE),
    ('BAJA',    'Baja',    4, TRUE);

INSERT INTO sla_policies (priority_id, response_hours, resolution_hours, updated_at)
SELECT p.id, s.response_hours, s.resolution_hours, now()
FROM (VALUES
        ('CRITICA', 1,  4),
        ('ALTA',    4,  24),
        ('MEDIA',   8,  48),
        ('BAJA',    24, 120)
     ) AS s (code, response_hours, resolution_hours)
JOIN priorities p ON p.code = s.code;

INSERT INTO categories (name, description, active, created_at, updated_at) VALUES
    ('Hardware', 'Fallas de equipos físicos: computadoras, impresoras y periféricos', TRUE, now(), now()),
    ('Software', 'Errores, instalación y licencias de aplicaciones',                  TRUE, now(), now()),
    ('Accesos',  'Restablecimiento de contraseñas y permisos de acceso',             TRUE, now(), now());
