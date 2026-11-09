# beca-ia-equipo-1 — Mesa de Ayuda

Sistema de tickets de soporte formado por tres microservicios Spring Boot independientes.
Los colaboradores reportan problemas, un administrador los asigna a un agente y el equipo
de soporte los atiende hasta resolverlos.

## Proyectos

| Microservicio | Dueño | Qué hace |
|---|---|---|
| [ms-users](./ms-users/README.md) | Fernando | Registro, login y emisión de tokens JWT. Administra usuarios y sus roles: SOLICITANTE, AGENTE y ADMIN. |
| [ms-catalog](./ms-catalog/README.md) | Sebastián | Administra las categorías de los tickets y las prioridades, cada una con su SLA (horas para responder y resolver). |
| [ms-tickets](./ms-tickets/README.md) | Isaac | Crea, asigna y actualiza tickets, y guarda su historial de cambios. Consulta a ms-catalog y ms-users por REST. |

## Cómo se relacionan

- ms-users emite el token JWT que usan los otros dos servicios para autenticarse.
- ms-tickets consulta a ms-catalog para validar la categoría y obtener el SLA de la prioridad.
- ms-tickets consulta a ms-users para validar que el agente asignado existe y está activo.

Cada servicio tiene su propia base de datos PostgreSQL.

## Documentación compartida

- [Backlog y glosario](./docs/backlog.md)
- [Contratos de API](./docs/contratos/)
- [Acuerdos de seguridad](./docs/seguridad.md)
- [Plan de contenedores](./docs/plan-contenedores.md)
- [Herramientas](./docs/herramientas.md)

## Flujo de Trabajo y Ramas

El desarrollo individual se realiza en las siguientes ramas de trabajo:
- `practica-1-fernando`
- `practica-1-isaac`
- `practica-1-sebastian`

