# Requerimientos — ms-catalog (Catálogo de servicio)

Responsable: Sebastián · Práctica 1 · Base de datos: `catalog_db`

## 1. Texto de negocio

Como responsable de la mesa de ayuda, necesitamos un catálogo que organice los tipos de incidencia que atendemos (hardware, software, accesos). Hoy cada agente clasifica los reportes a su manera y no hay un criterio común de urgencia ni de tiempos de atención. Queremos administrar las categorías desde un solo lugar y, si una ya no se usa, desactivarla sin borrar el historial de tickets que la utilizaron. También necesitamos definir las prioridades (crítica, alta, media y baja) con su política de SLA (tiempo de respuesta y de resolución), para que el sistema calcule automáticamente las fechas límite de cada ticket. Los solicitantes deben poder consultar qué categorías y prioridades existen para elegir correctamente al reportar. Solo los administradores pueden modificar el catálogo; el resto del personal únicamente lo consulta.

## 2. Actores, entidades y reglas de negocio

### 2.1 Actores

| Actor | Descripción | Acceso en esta práctica |
|---|---|---|
| ADMIN | Administra categorías, prioridades y SLA | Lectura y escritura |
| SOLICITANTE | Consulta categorías activas y prioridades al reportar un ticket | Solo lectura |
| AGENTE | Consulta el catálogo para atender tickets | Solo lectura |
| ms-tickets | Sistema que consumirá el catálogo (copia el SLA al crear un ticket) | Lectura (integración en Práctica 3) |

> SUPUESTO: en la Práctica 1 no hay autenticación; los roles se documentan para la matriz de autorización (ARCHITECTURE §6.3) y se aplicarán en la Práctica 3 con JWT.

### 2.2 Entidades

- **Categoría** (`categories`): nombre, descripción, indicador activo.
- **Prioridad** (`priorities`): código (`CRITICA`, `ALTA`, `MEDIA`, `BAJA`), nombre, nivel de urgencia y estado activo.
- **Política SLA** (`sla_policies`): horas de respuesta y horas de resolución de una prioridad.

### 2.3 Reglas de negocio

| ID | Regla | Origen |
|---|---|---|
| RN-01 | El nombre de una categoría es único y tiene entre 2 y 80 caracteres. | ARCHITECTURE §2.2 |
| RN-02 | Las categorías nunca se borran físicamente; se desactivan (`active=false`) para conservar el historial de tickets. | ARCHITECTURE §2.2 |
| RN-03 | El listado `GET /categories?active=true` solo devuelve categorías activas, ordenadas por nombre. | ARCHITECTURE §3.2 |
| RN-04 | El código de prioridad solo puede ser `CRITICA`, `ALTA`, `MEDIA` o `BAJA`, y es único. | ARCHITECTURE §2.2 (CHECK en BD) |
| RN-05 | El nivel de prioridad va de 1 a 10, es único y 1 es la más urgente. | ARCHITECTURE §2.2 y §3.2 |
| RN-06 | Cada prioridad tiene exactamente una política SLA (relación 1:1). | ARCHITECTURE §2.2 |
| RN-07 | `responseHours` está entre 1 y 720; `resolutionHours` está entre `responseHours` y 2160. | ARCHITECTURE §3.2 |
| RN-08 | Las prioridades no se eliminan ni se desactivan en esta práctica. | SUPUESTO (no hay endpoint de baja en el contrato) |
| RN-09 | Un cambio de SLA aplica solo a tickets creados después del cambio; los tickets existentes conservan su copia (snapshot). | SUPUESTO (derivado de D10: ms-tickets copia el SLA al crear) |
| RN-10 | Solo ADMIN crea, modifica o desactiva; SOLICITANTE y AGENTE solo consultan. | ARCHITECTURE §6.3 (aplicado en Práctica 3) |
| RN-11 | Los mensajes de error al usuario y los valores de dominio se expresan en español. | ARCHITECTURE §5.3 |

## 3. Historias de usuario

| ID | Título | Prioridad MoSCoW | Estimación (pts) |
|---|---|---|---|
| HU-01 | Crear categorías de servicio | Must | 3 |
| HU-02 | Consultar categorías activas | Must | 2 |
| HU-03 | Modificar una categoría | Should | 2 |
| HU-04 | Desactivar una categoría sin perder historial | Must | 2 |
| HU-05 | Consultar y registrar prioridades | Must | 3 |
| HU-06 | Definir el SLA de una prioridad | Must | 3 |

**HU-01 — Crear categorías de servicio**
Como ADMIN quiero crear categorías de servicio para clasificar las incidencias, para que los solicitantes reporten cada problema en la categoría correcta.

**HU-02 — Consultar categorías activas**
Como SOLICITANTE quiero consultar las categorías activas para elegir la adecuada al reportar un ticket, para que el reporte llegue al equipo correcto.

**HU-03 — Modificar una categoría**
Como ADMIN quiero modificar el nombre y la descripción de una categoría, para corregir su clasificación cuando cambie el servicio.

**HU-04 — Desactivar una categoría sin perder historial**
Como ADMIN quiero desactivar una categoría, para que deje de ofrecerse sin perder el historial de tickets que la usaron.

**HU-05 — Consultar y registrar prioridades**
Como ADMIN quiero registrar y consultar las prioridades con su nivel de urgencia, para ordenar la atención de los tickets.

**HU-06 — Definir el SLA de una prioridad**
Como ADMIN quiero definir las horas de respuesta y de resolución de cada prioridad, para que ms-tickets calcule las fechas límite de los tickets.

### 3.1 Criterios de aceptación (Gherkin)

**HU-01**
```gherkin
Escenario: Crear una categoría válida
  Dado que soy ADMIN y no existe una categoría llamada "Redes"
  Cuando envío POST /api/v1/categories con name "Redes" y description "Problemas de red"
  Entonces recibo 201 con la cabecera Location y la categoría con active = true

Escenario negativo: Rechazar un nombre duplicado
  Dado que ya existe una categoría llamada "Software"
  Cuando envío POST /api/v1/categories con name "Software"
  Entonces recibo 409 con code CATEGORY_ALREADY_EXISTS y no se crea ningún registro

Escenario negativo: Rechazar un nombre de un solo carácter
  Dado que soy ADMIN
  Cuando envío POST /api/v1/categories con name "A"
  Entonces recibo 400 con code VALIDATION_ERROR y errors que incluye el campo "name"
```

**HU-02**
```gherkin
Escenario: Listar solo categorías activas ordenadas por nombre
  Dado que existen "Software" (activa) y "Hardware" (inactiva)
  Cuando envío GET /api/v1/categories?active=true
  Entonces recibo 200 con una lista que contiene solo "Software"

Escenario negativo: Consultar una categoría inexistente
  Dado que no existe una categoría con id 999
  Cuando envío GET /api/v1/categories/999
  Entonces recibo 404 con code CATEGORY_NOT_FOUND
```

**HU-03**
```gherkin
Escenario: Modificar nombre y descripción
  Dado que existe la categoría "Accesos" con id 3
  Cuando envío PUT /api/v1/categories/3 con name "Accesos y cuentas" y description "Contraseñas y permisos"
  Entonces recibo 200 con el nuevo nombre y descripción

Escenario negativo: Usar el nombre de otra categoría
  Dado que existen "Accesos" (id 3) y "Software" (id 2)
  Cuando envío PUT /api/v1/categories/3 con name "Software"
  Entonces recibo 409 con code CATEGORY_ALREADY_EXISTS y la categoría no cambia

Escenario negativo: Modificar una categoría inexistente
  Dado que no existe una categoría con id 999
  Cuando envío PUT /api/v1/categories/999 con name "Redes"
  Entonces recibo 404 con code CATEGORY_NOT_FOUND
```

**HU-04**
```gherkin
Escenario: Desactivar una categoría
  Dado que existe la categoría "Hardware" con id 1 y active = true
  Cuando envío DELETE /api/v1/categories/1
  Entonces recibo 204 y GET /api/v1/categories/1 devuelve active = false

Escenario negativo: Desactivar una categoría inexistente
  Dado que no existe una categoría con id 999
  Cuando envío DELETE /api/v1/categories/999
  Entonces recibo 404 con code CATEGORY_NOT_FOUND y no se elimina ningún registro
```

**HU-05**
```gherkin
Escenario: Listar prioridades ordenadas por nivel
  Dado que existen las prioridades CRITICA, ALTA, MEDIA y BAJA (seed V2)
  Cuando envío GET /api/v1/priorities
  Entonces recibo 200 con las prioridades ordenadas de nivel 1 a 4

Escenario negativo: Registrar un código fuera del dominio
  Dado que soy ADMIN
  Cuando envío POST /api/v1/priorities con code "URGENTE"
  Entonces recibo 400 con code MALFORMED_REQUEST

Escenario negativo: Registrar un código ya existente
  Dado que ya existe la prioridad ALTA
  Cuando envío POST /api/v1/priorities con code "ALTA"
  Entonces recibo 409 con code PRIORITY_ALREADY_EXISTS
```

**HU-06**
```gherkin
Escenario: Actualizar el SLA de una prioridad
  Dado que existe la prioridad ALTA con id 2
  Cuando envío PUT /api/v1/priorities/2/sla con responseHours 2 y resolutionHours 8
  Entonces recibo 200 con sla.responseHours = 2 y sla.resolutionHours = 8

Escenario negativo: Resolución menor que respuesta
  Dado que existe la prioridad ALTA con id 2
  Cuando envío PUT /api/v1/priorities/2/sla con responseHours 10 y resolutionHours 5
  Entonces recibo 400 con code VALIDATION_ERROR y errors que incluye el campo "resolutionHours"

Escenario negativo: Prioridad inexistente
  Dado que no existe una prioridad con id 999
  Cuando envío PUT /api/v1/priorities/999/sla con responseHours 2 y resolutionHours 8
  Entonces recibo 404 con code PRIORITY_NOT_FOUND
```

## 4. Requerimientos no funcionales

| ID | Atributo | Requerimiento medible | Verificación |
|---|---|---|---|
| RNF-01 | Rendimiento | Los `GET` de catálogo responden con p95 ≤ 300 ms con hasta 500 registros y 50 usuarios concurrentes. | Prueba de carga (JMeter o k6) en la demo |
| RNF-02 | Disponibilidad | `/actuator/health` responde 200 en ≤ 1 s y el servicio está disponible ≥ 99 % durante el horario de operación (08:00–20:00). | Healthcheck de Docker cada 30 s durante una semana de pruebas |
| RNF-03 | Integridad de datos | Las migraciones Flyway V1 y V2 se aplican sin error en PostgreSQL 16 y `ddl-auto=validate` arranca sin discrepancias; el 100 % de las categorías desactivadas conserva su registro. | Arranque contra `catalog_db` y prueba de desactivación (HU-04) |
| RNF-04 | Seguridad de errores | El 100 % de las respuestas 500 devuelven `detail` genérico, sin stack trace ni mensaje interno. | Prueba del `GlobalExceptionHandler` y configuración `include-stacktrace=never` |
| RNF-05 | Mantenibilidad | Cobertura de líneas ≥ 70 %, métodos ≤ 30 líneas y complejidad ciclomática ≤ 10. | JaCoCo y revisión de calidad (QUALITY-REPORT) |

## 5. Preguntas abiertas para el cliente

1. ¿Los nombres de categoría deben ser únicos sin distinguir mayúsculas y acentos (por ejemplo, "Software" y "software")? Hoy la regla es exacta.
2. ¿Se necesitan prioridades adicionales a CRITICA, ALTA, MEDIA y BAJA? Con el CHECK de la base, `POST /priorities` solo acepta esos cuatro códigos, por lo que siempre responderá 409 para los existentes.
3. ¿Una categoría desactivada debe poder reactivarse? El contrato actual no lo contempla.
4. ¿Los límites de SLA (720 h de respuesta y 2160 h de resolución) son los definitivos o cambian por prioridad?
5. ¿Un cambio de SLA debe aplicarse también a los tickets abiertos o solo a los nuevos (RN-09)?
6. ¿Qué tan frecuente es la consulta del catálogo por parte de los solicitantes, para ajustar el RNF-01?
