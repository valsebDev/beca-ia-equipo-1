# Especificación de Requerimientos: ms-users

**Microservicio:** `ms-users`  
**Sistema:** Mesa de Ayuda (Sistema de Tickets)  
**Stack:** Java 17, Spring Boot 4.1.2, PostgreSQL 16, Spring Security, JWT
**Fase:** Práctica 1 (Análisis y Especificación de Requerimientos)

---

## 1. Actores

* **Usuario Anónimo / Visitante**: Persona no autenticada que interactúa con el sistema para crear una cuenta o iniciar sesión.
* **Solicitante**: Usuario final registrado y autenticado con rol `SOLICITANTE` que genera tickets de soporte y gestiona su perfil básico.
* **Agente**: Miembro del equipo de soporte autenticado con rol `AGENTE` encargado de atender tickets y consultar su perfil de trabajo.
* **Administrador (Admin)**: Usuario con privilegios de gestión técnica y de accesos con rol `ADMIN`, facultado para consultar perfiles ajenos y desactivar cuentas.

---

## 2. Entidades Principales

### Entidad: `Usuario` (`users`)
Representa la cuenta de identidad y acceso al ecosistema de la Mesa de Ayuda.

* `id` (`UUID`): Clave primaria única del usuario.
* `email` (`VARCHAR(150)`): Identificador de acceso, único y en formato RFC 5322.
* `password` (`VARCHAR(255)`): Contraseña cifrada en algoritmo hash seguro (BCrypt). Nunca se persiste ni expone en texto plano.
* `nombre` (`VARCHAR(100)`): Nombre(s) de la persona.
* `apellido` (`VARCHAR(100)`): Apellido(s) de la persona.
* `rol` (`VARCHAR(30)`): Rol de control de acceso RBAC (`SOLICITANTE`, `AGENTE`, `ADMIN`).
* `activo` (`BOOLEAN`): Estado lógico de la cuenta (`true` = activa, `false` = deshabilitada por administración).
* `intentosFallidos` (`INTEGER`): Contador de intentos fallidos consecutivos de autenticación.
* `bloqueadoHasta` (`TIMESTAMP WITH TIME ZONE`): Marca de tiempo hasta la cual la cuenta permanece inhabilitada temporalmente por fuerza bruta (o `NULL` si no está bloqueada).
* `createdAt` (`TIMESTAMP WITH TIME ZONE`): Fecha/hora de creación del registro.
* `updatedAt` (`TIMESTAMP WITH TIME ZONE`): Fecha/hora de la última actualización.

---

## 3. Reglas de Negocio

* **RN-01 (Unicidad de cuenta)**: No pueden coexistir dos usuarios con el mismo correo electrónico en la base de datos (clave única e insensible a mayúsculas/minúsculas).
* **RN-02 (Cifrado criptográfico)**: Toda contraseña debe procesarse mediante BCrypt con un factor de trabajo (*work factor*) mínimo de 10 antes de persistirse.
* **RN-03 (Emisión de token JWT)**: Una autenticación válida emite un token JWT firmado (HMAC-SHA256 o RSA) conteniendo claims esenciales: `sub` (ID de usuario), `email`, `role`, `iat` (emisión) y `exp` (expiración).
* **RN-04 (Umbral y ventana de bloqueo)**: Tras acumular 5 intentos consecutivos erróneos de inicio de sesión (*SUPUESTO*), la cuenta se bloquea automáticamente por un lapso de 15 minutos (*SUPUESTO*).
* **RN-05 (Reinicio de intentos fallidos)**: Un inicio de sesión exitoso reinicia inmediatamente a 0 el contador `intentosFallidos` y anula `bloqueadoHasta`.
* **RN-06 (Condición de acceso)**: Un usuario con cuenta inactiva (`activo = false`) o bloqueada temporalmente (`bloqueadoHasta > now()`) no puede iniciar sesión ni generar tokens JWT.
* **RN-07 (Autogestión acotada de perfil)**: El usuario solo puede actualizar sus datos personales (nombre y apellido); no tiene permitido modificar su propio rol, correo, intentos fallidos ni estado activo.
* **RN-08 (Desactivación privativa)**: La acción de desactivar lógicamente una cuenta es exclusiva de usuarios autenticados con rol `ADMIN`.

---

## 4. Historias de Usuario y Criterios Gherkin

### Tabla Resumen de HU
| ID | Título | Prioridad (MoSCoW) | Estimación (Story Points) |
| :--- | :--- | :--- | :--- |
| **HU-01** | Registro de nuevo solicitante | Must Have | 3 |
| **HU-02** | Autenticación con correo y contraseña (JWT) | Must Have | 5 |
| **HU-03** | Bloqueo temporal por intentos fallidos de login | Must Have | 5 |
| **HU-04** | Consulta y actualización de perfil de usuario | Should Have | 3 |
| **HU-05** | Desactivación lógica de cuentas por el Administrador | Should Have | 3 |

---

### HU-01: Registro de nuevo solicitante
**Como** usuario anónimo  
**Quiero** registrarme ingresando mis nombres, apellidos, correo y contraseña  
**Para** acceder al sistema de Mesa de Ayuda con una cuenta de rol solicitante.

* **Escenario 1 (Positivo): Registro exitoso de usuario**
  * **Dado** que un usuario no registrado ingresa a la plataforma
  * **Cuando** envía una solicitud a `POST /api/v1/auth/register` con nombre, apellido, correo válido y contraseña que cumple la política
  * **Entonces** el sistema persiste al usuario con rol `SOLICITANTE`, estado `activo = true`, contraseña en hash BCrypt y retorna código HTTP `201 Created` con los datos del perfil (sin incluir la contraseña).

* **Escenario 2 (Negativo): Registro fallido por correo duplicado**
  * **Dado** que ya existe una cuenta con el correo `juan.perez@empresa.com`
  * **Cuando** otro usuario intenta registrarse enviando `POST /api/v1/auth/register` con dicho correo
  * **Entonces** el sistema rechaza la operación, no persiste información y responde con código HTTP `409 Conflict` bajo formato ProblemDetail.

* **Escenario 3 (Negativo): Datos de entrada inválidos en el registro**
  * **Dado** que un usuario envía el formulario con el formato de correo incorrecto o campos vacíos
  * **Cuando** el microservicio procesa la solicitud `POST /api/v1/auth/register`
  * **Entonces** la validación de Jakarta Validation falla y responde con código HTTP `400 Bad Request` detallando los campos erróneos.

---

### HU-02: Autenticación con correo y contraseña (JWT)
**Como** usuario registrado  
**Quiero** iniciar sesión con mi correo electrónico y contraseña  
**Para** obtener un token JWT que acredite mi identidad en los microservicios del sistema.

* **Escenario 1 (Positivo): Autenticación exitosa**
  * **Dado** que un usuario con cuenta activa y sin bloqueos envía sus credenciales correctas vía `POST /api/v1/auth/login`
  * **Cuando** el servicio valida la correspondencia de la contraseña con el hash almacenado
  * **Entonces** el sistema responde con código HTTP `200 OK`, resetea el contador de fallos a 0 y entrega un token JWT con tiempo de expiración y roles asignados.

* **Escenario 2 (Negativo): Credenciales incorrectas**
  * **Dado** que existe una cuenta con el correo `agente@empresa.com`
  * **Cuando** el usuario envía una solicitud a `POST /api/v1/auth/login` con una contraseña incorrecta
  * **Entonces** el sistema incrementa el campo `intentosFallidos` en 1 y responde con código HTTP `401 Unauthorized` sin revelar si falló el correo o la clave.

* **Escenario 3 (Negativo): Intento de login con cuenta desactivada**
  * **Dado** que un usuario tiene el atributo `activo = false`
  * **Cuando** intenta autenticarse mediante `POST /api/v1/auth/login` con la contraseña correcta
  * **Entonces** el sistema deniega el acceso y responde con código HTTP `403 Forbidden` informando que la cuenta está deshabilitada.

---

### HU-03: Bloqueo temporal por intentos fallidos de login
**Como** oficial de seguridad del sistema  
**Quiero** que la cuenta se bloquee automáticamente tras 5 intentos fallidos consecutivos de login  
**Para** mitigar vulnerabilidades y ataques de fuerza bruta contra las credenciales.

* **Escenario 1 (Negativo / Seguridad): Bloqueo tras alcanzar el límite de fallos**
  * **Dado** que una cuenta tiene acumulados 4 intentos fallidos previos *(SUPUESTO)*
  * **Cuando** el usuario ingresa por 5.ª ocasión consecutiva una contraseña errónea en `POST /api/v1/auth/login`
  * **Entonces** el sistema incrementa el contador a 5, asigna en `bloqueadoHasta` la hora actual más 15 minutos *(SUPUESTO)* y responde con código HTTP `423 Locked`.

* **Escenario 2 (Negativo): Rechazo de acceso durante el período de bloqueo**
  * **Dado** que una cuenta tiene un bloqueo temporal activo (`bloqueadoHasta > now()`)
  * **Cuando** el usuario intenta iniciar sesión vía `POST /api/v1/auth/login` aun enviando la contraseña correcta
  * **Entonces** el sistema deniega la petición y responde con código HTTP `423 Locked` indicando el tiempo restante de bloqueo.

* **Escenario 3 (Positivo): Desbloqueo automático tras expirar el lapso de tiempo**
  * **Dado** que transcurrieron más de 15 minutos desde el bloqueo (`bloqueadoHasta <= now()`)
  * **Cuando** el usuario ingresa sus credenciales válidas en `POST /api/v1/auth/login`
  * **Entonces** el sistema permite el acceso, retorna HTTP `200 OK`, emite el token JWT y limpia los valores de bloqueo.

---

### HU-04: Consulta y actualización de perfil de usuario
**Como** usuario autenticado  
**Quiero** consultar y actualizar mis datos básicos de perfil (nombre y apellido)  
**Para** mantener actualizada mi información personal en la mesa de ayuda.

* **Escenario 1 (Positivo): Actualización exitosa del propio perfil**
  * **Dado** que un usuario está autenticado con un token JWT válido
  * **Cuando** realiza una petición `PUT /api/v1/users/me` enviando nuevos valores para nombre y apellido
  * **Entonces** el sistema persiste las modificaciones, actualiza `updatedAt` y responde con código HTTP `200 OK` junto con el perfil actualizado.

* **Escenario 2 (Negativo): Petición de perfil no autenticada o token inválido**
  * **Dado** que una solicitud a `GET /api/v1/users/me` no incluye token o este se encuentra expirado
  * **Cuando** el filtro de seguridad intercepta la petición
  * **Entonces** el sistema bloquea el procesamiento y responde con código HTTP `401 Unauthorized`.

* **Escenario 3 (Negativo): Actualización con campos vacíos o inválidos**
  * **Dado** que el usuario autenticado envía `PUT /api/v1/users/me` con nombre en blanco
  * **Cuando** el validador procesa el cuerpo de la petición
  * **Entonces** el sistema rechaza los cambios y responde con código HTTP `400 Bad Request`.

---

### HU-05: Desactivación lógica de cuentas por el Administrador
**Como** Administrador  
**Quiero** desactivar la cuenta de un usuario específico  
**Para** suspender su acceso de manera indefinida sin eliminar su historial de tickets.

* **Escenario 1 (Positivo): Desactivación exitosa realizada por un ADMIN**
  * **Dado** que un usuario con rol `ADMIN` se encuentra debidamente autenticado
  * **Cuando** envía una petición `PATCH /api/v1/users/{id}/deactivate` con el ID de un usuario existente
  * **Entonces** el sistema actualiza el registro con `activo = false` y responde con código HTTP `204 No Content`.

* **Escenario 2 (Negativo): Intento de desactivación por un rol no autorizado**
  * **Dado** que un usuario autenticado con rol `SOLICITANTE` o `AGENTE` invoca `PATCH /api/v1/users/{id}/deactivate`
  * **Cuando** el evaluador de autorización RBAC verifica los claims del token
  * **Entonces** la solicitud es rechazada y el servicio responde con código HTTP `403 Forbidden`.

* **Escenario 3 (Negativo): Desactivación de un ID de usuario inexistente**
  * **Dado** que un `ADMIN` invoca `PATCH /api/v1/users/{id}/deactivate` enviando un UUID inexistente
  * **Cuando** el servicio busca al usuario en la base de datos
  * **Entonces** el sistema no encuentra el recurso y responde con código HTTP `404 Not Found`.

---

## 5. Requerimientos No Funcionales (RNF)

* **RNF-01 (Seguridad Criptográfica y Gestión de Secretos)**:
  * Toda contraseña debe cifrarse con BCrypt utilizando `BCryptPasswordEncoder` con factor de fuerza configurable (mínimo 10).
  * La clave de firma del JWT (`JWT_SECRET`) y las credenciales de base de datos deben inyectarse exclusivamente a través de variables de entorno o perfiles externos; queda prohibido almacenar secretos en duro en el código o en `application.yaml`.
  * La configuración de Actuator debe limitar la exposición únicamente al endpoint `/actuator/health` en entornos productivos.

* **RNF-02 (Auditoría de Eventos de Seguridad y Prevención de Fugas - OWASP A09)**:
  * Debe registrarse bitácora estructurada (SLF4J/Logback) de eventos críticos: creación de usuario, login exitoso, fallo de login, bloqueo temporal de cuenta y desactivación.
  * Por estricto control de seguridad (OWASP A09), queda terminantemente prohibido escribir en los logs contraseñas en texto plano, tokens JWT completos o información crediticia/sensible.

* **RNF-03 (Estandarización de Errores - RFC 9457 ProblemDetail)**:
  * Todo error devuelto por la API (series 4xx y 5xx) debe adoptar el formato estándar **Problem Details for HTTP APIs (RFC 9457)** mediante un controlador `@RestControllerAdvice`.
  * Las respuestas de error contendrán los atributos `type`, `title`, `status`, `detail`, `instance` y un mapa de errores de campo para peticiones inválidas (`400 Bad Request`), omitiendo stack traces o detalles internos de la base de datos (mitigación OWASP A05).

---

## 6. Preguntas Abiertas para el Cliente

1. **Parámetros del bloqueo temporal**: ¿Se confirman los 5 intentos fallidos y los 15 minutos de bloqueo propuestos como estándar, o se prefiere una política distinta (p. ej., bloqueo indefinido con intervención de soporte)?
2. **Creación de Agentes y Administradores**: El registro autoservicio asigna por defecto el rol `SOLICITANTE`. ¿Cómo se deberán dar de alta los usuarios `AGENTE` y `ADMIN`? ¿Existirá un endpoint privado exclusivo para que el `ADMIN` cree usuarios con rol asignable?
3. **Ciclo de vida y revocación de tokens JWT**: ¿Cuál es el tiempo de expiración (TTL) requerido para el token de acceso (ej. 15 minutos, 60 minutos, 8 horas)? ¿Se requerirá soporte de Refresh Token o mecanismo de invalidación en lista negra cuando se desactive una cuenta?
4. **Política de robustez de contraseña**: ¿Cuáles son las reglas de complejidad a validar (mínimo de 8 caracteres, alfanumérico, caracteres especiales, mayúsculas)?
5. **Reactivación de usuarios**: ¿Se requerirá habilitar un endpoint recíproco `PATCH /api/v1/users/{id}/activate` para que el Administrador pueda rehabilitar cuentas previamente desactivadas?