# Especificación de Requerimientos: ms-users

**Microservicio:** `ms-users`  
**Sistema:** Mesa de Ayuda (Sistema de Tickets)  
**Stack:** Java 17, Spring Boot 4.1.2, PostgreSQL 16, Spring Security, JWT
**Fase:** Práctica 1 (Análisis y Especificación de Requerimientos). Actualizado en Práctica 3 con JWT HS256 (Nimbus), access token y refresh token.

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

* `id` (`Long`): Clave primaria del usuario.
* `email` (`VARCHAR(254)`): Identificador de acceso, único, en minúsculas y en formato RFC 5322.
* `password_hash` (`VARCHAR(255)`): Contraseña cifrada con BCrypt. Nunca se persiste ni expone en texto plano.
* `full_name` (`VARCHAR(150)`): Nombre completo de la persona (entre 2 y 150 caracteres).
* `role` (`VARCHAR(30)`): Rol de control de acceso RBAC (`SOLICITANTE`, `AGENTE`, `ADMIN`).
* `active` (`BOOLEAN`): Estado lógico de la cuenta (`true` = activa, `false` = deshabilitada por administración).
* `createdAt` (`TIMESTAMP WITH TIME ZONE`): Fecha/hora de creación del registro.
* `updatedAt` (`TIMESTAMP WITH TIME ZONE`): Fecha/hora de la última actualización.

### Entidad: `RefreshToken` (`refresh_tokens`)
Token de renovación de sesión. Solo se persiste su hash SHA-256; el valor en claro se entrega una sola vez al cliente.

* `id` (`BIGINT`): Clave primaria.
* `user` (`Long`, FK a `users.id`): Usuario dueño del token. Se elimina en cascada con el usuario.
* `token_hash` (`CHAR(64)`): Hash SHA-256 del token, único.
* `expires_at` (`TIMESTAMP WITH TIME ZONE`): Fin de vigencia (7 días desde la emisión).
* `revoked_at` (`TIMESTAMP WITH TIME ZONE`): Momento de revocación por logout, rotación o desactivación de la cuenta; vacío si está vigente.
* `created_at` (`TIMESTAMP WITH TIME ZONE`): Fecha/hora de emisión.

---

## 3. Reglas de Negocio

* **RN-01 (Unicidad de cuenta)**: No pueden coexistir dos usuarios con el mismo correo electrónico en la base de datos (clave única e insensible a mayúsculas/minúsculas).
* **RN-02 (Cifrado criptográfico)**: Toda contraseña debe procesarse mediante BCrypt con un factor de trabajo (*work factor*) mínimo de 10 antes de persistirse.
* **RN-03 (Emisión de token JWT)**: Una autenticación válida emite un access token JWT firmado con HMAC-SHA256 (HS256) y vigencia de 15 minutos. Claims: `iss` (`ms-users`), `aud` (`mesa-ayuda`), `sub` (ID del usuario como texto), `email`, `name`, `roles` (arreglo), `iat` (emisión), `exp` (expiración) y `jti`.
* **RN-04 (Sin bloqueo temporal por intentos fallidos)**: El sistema no bloquea cuentas por intentos fallidos de login; el bloqueo temporal (HU-USR-03) fue retirado porque ARCHITECTURE no lo contempla.
* **RN-05 (Respuesta uniforme de login)**: Login fallido, con correo inexistente o con cuenta inactiva (`activo = false`) responde siempre `401 Unauthorized` con el mismo mensaje genérico, sin revelar la causa ni si el correo existe.
* **RN-06 (Condición de acceso)**: Un usuario con cuenta inactiva (`activo = false`) no puede iniciar sesión ni generar tokens JWT.
* **RN-07 (Consulta de perfil propio)**: El usuario autenticado solo puede consultar su propio perfil (`GET /api/v1/users/me`). No existe endpoint de autogestión de datos; no puede modificar su rol, correo ni estado activo.
* **RN-08 (Desactivación privativa)**: La acción de desactivar lógicamente una cuenta es exclusiva de usuarios autenticados con rol `ADMIN`.
* **RN-09 (Refresh token con rotación)**: Tras un login válido se emite un refresh token opaco de 7 días. Cada uso lo rota: se emite uno nuevo y el anterior queda revocado. Si se reutiliza un refresh token ya rotado o revocado, se revocan todos los refresh tokens del usuario.
* **RN-10 (Cierre de sesión)**: `POST /api/v1/auth/logout` revoca el refresh token enviado y requiere un access token válido.
* **RN-11 (Desactivación revoca sesiones)**: Al desactivar una cuenta se revocan sus refresh tokens vigentes. El access token ya emitido sigue válido hasta su expiración (máximo 15 minutos).
* **RN-12 (Administrador inicial)**: Si no existe ningún usuario `ADMIN`, el sistema crea uno al arrancar a partir de `ADMIN_EMAIL` y `ADMIN_PASSWORD`. Estos valores no tienen valor por defecto.
* **RN-13 (Política de contraseña)**: La contraseña tiene entre 8 y 72 caracteres e incluye al menos una letra y un dígito. Aplica al registro y a la creación de usuarios por el `ADMIN`.

---

## 4. Historias de Usuario y Criterios Gherkin

### Tabla Resumen de HU
| ID | Título | Prioridad (MoSCoW) | Estimación (Story Points) |
| :--- | :--- | :--- | :--- |
| **HU-USR-01** | Registro de nuevo solicitante | Must Have | 3 |
| **HU-USR-02** | Autenticación con correo y contraseña (JWT) | Must Have | 5 |
| **HU-USR-03** | ~~Bloqueo temporal por intentos fallidos de login~~ (Retirada) | ~~Must Have~~ | ~~5~~ |
| **HU-USR-04** | Consulta de perfil propio | Should Have | 3 |
| **HU-USR-05** | Desactivación lógica de cuentas por el Administrador | Should Have | 3 |
| **HU-USR-06** | Renovación de sesión con refresh token | Must Have | 5 |
| **HU-USR-07** | Cierre de sesión (logout) | Must Have | 2 |
| **HU-USR-08** | Listado de usuarios por el Administrador | Should Have | 3 |
| **HU-USR-09** | Creación de usuarios con rol por el Administrador | Should Have | 3 |

---

### HU-USR-01: Registro de nuevo solicitante
[Volver al backlog](../../docs/backlog.md#hu-usr-01)
**Como** usuario anónimo  
**Quiero** registrarme ingresando mi nombre completo, correo y contraseña  
**Para** acceder al sistema de Mesa de Ayuda con una cuenta de rol solicitante.

* **Escenario 1 (Positivo): Registro exitoso de usuario**
  * **Dado** que un usuario no registrado ingresa a la plataforma
  * **Cuando** envía una solicitud a `POST /api/v1/auth/register` con `fullName`, correo válido y contraseña que cumple RN-13
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

### HU-USR-02: Autenticación con correo y contraseña (JWT)
[Volver al backlog](../../docs/backlog.md#hu-usr-02)
**Como** usuario registrado  
**Quiero** iniciar sesión con mi correo electrónico y contraseña  
**Para** obtener un token JWT que acredite mi identidad en los microservicios del sistema.

* **Escenario 1 (Positivo): Autenticación exitosa**
  * **Dado** que un usuario con cuenta activa envía sus credenciales correctas vía `POST /api/v1/auth/login`
  * **Cuando** el servicio valida la correspondencia de la contraseña con el hash almacenado
  * **Entonces** el sistema responde con código HTTP `200 OK` y entrega un access token JWT (vigencia de 15 minutos, RN-03) y un refresh token (vigencia de 7 días, RN-09).

* **Escenario 2 (Negativo): Credenciales incorrectas**
  * **Dado** que existe una cuenta con el correo `agente@empresa.com`
  * **Cuando** el usuario envía una solicitud a `POST /api/v1/auth/login` con una contraseña incorrecta
  * **Entonces** el sistema responde con código HTTP `401 Unauthorized` sin revelar si falló el correo o la clave.

* **Escenario 3 (Negativo): Intento de login con cuenta desactivada**
  * **Dado** que un usuario tiene el atributo `activo = false`
  * **Cuando** intenta autenticarse mediante `POST /api/v1/auth/login` con la contraseña correcta
  * **Entonces** el sistema deniega el acceso y responde con código HTTP `401 Unauthorized` con el mismo mensaje genérico que en el escenario 2, sin revelar que la cuenta está deshabilitada.


---

### HU-USR-03: Bloqueo temporal por intentos fallidos de login (RETIRADA)
[Volver al backlog](../../docs/backlog.md#hu-usr-03)
**Estado:** Retirada. ARCHITECTURE no contempla el bloqueo temporal de cuentas (código `423 Locked`). Los fallos de login se responden con `401 Unauthorized` (ver HU-USR-02) y no hay contador de intentos ni campo de bloqueo en la entidad.

---

### HU-USR-04: Consulta de perfil propio
[Volver al backlog](../../docs/backlog.md#hu-usr-04)
**Como** usuario autenticado  
**Quiero** consultar mis datos de perfil vigentes  
**Para** verificar mi información personal registrada en la mesa de ayuda.

* **Escenario 1 (Positivo): Consulta exitosa del propio perfil**
  * **Dado** que un usuario está autenticado con un token JWT válido
  * **Cuando** realiza una petición `GET /api/v1/users/me`
  * **Entonces** el sistema responde con código HTTP `200 OK` y los datos de su propio perfil, sin contraseña ni campos internos.

* **Escenario 2 (Negativo): Petición de perfil no autenticada o token inválido**
  * **Dado** que una solicitud a `GET /api/v1/users/me` no incluye token o este se encuentra expirado
  * **Cuando** el filtro de seguridad intercepta la petición
  * **Entonces** el sistema bloquea el procesamiento y responde con código HTTP `401 Unauthorized`.

---

### HU-USR-05: Desactivación lógica de cuentas por el Administrador
[Volver al backlog](../../docs/backlog.md#hu-usr-05)
**Como** Administrador  
**Quiero** desactivar la cuenta de un usuario específico  
**Para** suspender su acceso de manera indefinida sin eliminar su historial de tickets.

* **Escenario 1 (Positivo): Desactivación exitosa realizada por un ADMIN**
  * **Dado** que un usuario con rol `ADMIN` se encuentra debidamente autenticado
  * **Cuando** envía `PATCH /api/v1/users/{id}/status` con `{"active": false}`
  * **Entonces** el sistema actualiza el estado, revoca sus refresh tokens vigentes (RN-11) y responde con código HTTP `200 OK` y el perfil actualizado. Para reactivar la cuenta se envía `{"active": true}` al mismo endpoint.

* **Escenario 2 (Negativo): Intento de desactivación por un rol no autorizado**
  * **Dado** que un usuario autenticado con rol `SOLICITANTE` o `AGENTE`   invoca `PATCH /api/v1/users/{id}/status`
  * **Cuando** el evaluador de autorización RBAC verifica los claims del token
  * **Entonces** la solicitud es rechazada y el servicio responde con código HTTP `403 Forbidden`.

* **Escenario 3 (Negativo): Desactivación de un ID de usuario inexistente**
  * **Dado** que un `ADMIN` invoca `PATCH /api/v1/users/{id}/status` enviando un identificador inexistente
  * **Cuando** el servicio busca al usuario en la base de datos
  * **Entonces** el sistema no encuentra el recurso y responde con código HTTP `404 Not Found`.

  * **Escenario 4 (Negativo): Petición sin token válido**
    * **Dado** que la solicitud a `PATCH /api/v1/users/{id}/status` no incluye token o este se encuentra expirado
    * **Cuando** el filtro de seguridad intercepta la petición
    * **Entonces** el sistema responde con código HTTP `401 Unauthorized`.

---

### HU-USR-06: Renovación de sesión con refresh token
[Volver al backlog](../../docs/backlog.md#hu-usr-06)
**Como** usuario autenticado  
**Quiero** renovar mi access token con mi refresh token  
**Para** mantener la sesión sin volver a ingresar mis credenciales.

* **Escenario 1 (Positivo): Rotación exitosa**
  * **Dado** que tengo un refresh token vigente y no revocado
  * **Cuando** envío `POST /api/v1/auth/refresh` con `refreshToken`
  * **Entonces** el sistema responde con código HTTP `200 OK` con un nuevo access token y un nuevo refresh token, y revoca el refresh token anterior (RN-09).

* **Escenario 2 (Negativo): Refresh token expirado, revocado o inexistente**
  * **Dado** que envío un refresh token expirado, revocado o que no existe
  * **Cuando** envío `POST /api/v1/auth/refresh`
  * **Entonces** el sistema responde con código HTTP `401 Unauthorized` sin indicar la causa.

* **Escenario 3 (Negativo): Reutilización de un refresh token ya rotado**
  * **Dado** que un refresh token ya fue rotado y se envía de nuevo
  * **Cuando** envío `POST /api/v1/auth/refresh` con ese token
  * **Entonces** el sistema responde con código HTTP `401 Unauthorized` y revoca todos los refresh tokens del usuario (RN-09).

_No aplica 403: el endpoint es público y no exige un rol concreto._

---

### HU-USR-07: Cierre de sesión (logout)
[Volver al backlog](../../docs/backlog.md#hu-usr-07)
**Como** usuario autenticado  
**Quiero** cerrar mi sesión  
**Para** invalidar mi refresh token y que no pueda usarse para renovar el acceso.

* **Escenario 1 (Positivo): Cierre de sesión exitoso**
  * **Dado** que un usuario con rol `SOLICITANTE`, `AGENTE` o `ADMIN` tiene un access token válido y un refresh token vigente
  * **Cuando** envía `POST /api/v1/auth/logout` con su `refreshToken`
  * **Entonces** el sistema responde con código HTTP `204 No Content` y revoca el refresh token (RN-10).

* **Escenario 2 (Negativo): Petición sin token válido**
  * **Dado** que la solicitud a `POST /api/v1/auth/logout` no incluye access token o este se encuentra expirado
  * **Cuando** el filtro de seguridad intercepta la petición
  * **Entonces** el sistema responde con código HTTP `401 Unauthorized`.

_No aplica 403: cualquier rol autenticado puede cerrar su propia sesión._

---

### HU-USR-08: Listado de usuarios por el Administrador
[Volver al backlog](../../docs/backlog.md#hu-usr-08)
**Como** Administrador  
**Quiero** listar los usuarios, opcionalmente filtrados por rol  
**Para** revisar las cuentas registradas y sus roles.

* **Escenario 1 (Positivo): Listado exitoso por un ADMIN**
  * **Dado** que un usuario con rol `ADMIN` está autenticado
  * **Cuando** envía `GET /api/v1/users`, con o sin el parámetro `role`
  * **Entonces** el sistema responde con código HTTP `200 OK` con la lista de usuarios, filtrada por rol si se indicó, sin contraseñas.

* **Escenario 2 (Negativo): Petición de un rol distinto de ADMIN**
  * **Dado** que un usuario con rol `SOLICITANTE` o `AGENTE` está autenticado
  * **Cuando** envía `GET /api/v1/users`
  * **Entonces** el sistema responde con código HTTP `403 Forbidden`.

* **Escenario 3 (Negativo): Petición sin token válido**
  * **Dado** que la solicitud a `GET /api/v1/users` no incluye token o este se encuentra expirado
  * **Cuando** el filtro de seguridad intercepta la petición
  * **Entonces** el sistema responde con código HTTP `401 Unauthorized`.

---

### HU-USR-09: Creación de usuarios con rol por el Administrador
[Volver al backlog](../../docs/backlog.md#hu-usr-09)
**Como** Administrador  
**Quiero** crear usuarios asignándoles un rol  
**Para** dar de alta agentes y otros administradores, que no se registran por autoservicio.

* **Escenario 1 (Positivo): Creación exitosa por un ADMIN**
  * **Dado** que un usuario con rol `ADMIN` está autenticado
  * **Cuando** envía `POST /api/v1/users` con `email`, `password`, `fullName` y `role`
  * **Entonces** el sistema persiste al usuario con `active = true`, contraseña en hash BCrypt y responde con código HTTP `201 Created` con su perfil, sin contraseña.

* **Escenario 2 (Negativo): Correo duplicado**
  * **Dado** que ya existe una cuenta con el correo indicado
  * **Cuando** el `ADMIN` envía `POST /api/v1/users` con ese correo
  * **Entonces** el sistema responde con código HTTP `409 Conflict` y no persiste información (RN-01).

* **Escenario 3 (Negativo): Petición de un rol distinto de ADMIN**
  * **Dado** que un usuario con rol `SOLICITANTE` o `AGENTE` está autenticado
  * **Cuando** envía `POST /api/v1/users`
  * **Entonces** el sistema responde con código HTTP `403 Forbidden` y no persiste información.

* **Escenario 4 (Negativo): Petición sin token válido**
  * **Dado** que la solicitud a `POST /api/v1/users` no incluye token o este se encuentra expirado
  * **Cuando** el filtro de seguridad intercepta la petición
  * **Entonces** el sistema responde con código HTTP `401 Unauthorized`.

---

## 5. Requerimientos No Funcionales (RNF)

* **RNF-01 (Seguridad Criptográfica y Gestión de Secretos)**:
  * Toda contraseña debe cifrarse con BCrypt utilizando `BCryptPasswordEncoder` con factor de fuerza configurable (mínimo 10).
  * La clave de firma del JWT (`JWT_SECRET`) y las credenciales de base de datos deben inyectarse exclusivamente a través de variables de entorno o perfiles externos; queda prohibido almacenar secretos en duro en el código o en `application.yaml`.
  * La configuración de Actuator debe limitar la exposición únicamente al endpoint `/actuator/health` en entornos productivos.

* **RNF-02 (Auditoría de Eventos de Seguridad y Prevención de Fugas - OWASP A09)**:
  * Debe registrarse bitácora estructurada (SLF4J/Logback) de eventos críticos: creación de usuario, login exitoso, fallo de login y desactivación.
  * Por estricto control de seguridad (OWASP A09), queda terminantemente prohibido escribir en los logs contraseñas en texto plano, tokens JWT completos o información crediticia/sensible.

* **RNF-03 (Estandarización de Errores - RFC 9457 ProblemDetail)**:
  * Todo error devuelto por la API (series 4xx y 5xx) debe adoptar el formato estándar **Problem Details for HTTP APIs (RFC 9457)** mediante un controlador `@RestControllerAdvice`.
  * Las respuestas de error contendrán los atributos `type`, `title`, `status`, `detail`, `instance` y un mapa de errores de campo para peticiones inválidas (`400 Bad Request`), omitiendo stack traces o detalles internos de la base de datos (mitigación OWASP A05).

---

## 6. Preguntas Abiertas para el Cliente

1. **Creación de Agentes y Administradores**: El registro autoservicio asigna por defecto el rol `SOLICITANTE`. ¿Cómo se deberán dar de alta los usuarios `AGENTE` y `ADMIN`? ¿Existirá un endpoint privado exclusivo para que el `ADMIN` cree usuarios con rol asignable?
   **Resuelta:** alta de `AGENTE` y `ADMIN` con `POST /api/v1/users`, solo para el `ADMIN` (HU-USR-09).
2. **Ciclo de vida y revocación de tokens JWT**: ¿Cuál es el tiempo de expiración (TTL) requerido para el token de acceso (ej. 15 minutos, 60 minutos, 8 horas)? ¿Se requerirá soporte de Refresh Token o mecanismo de invalidación en lista negra cuando se desactive una cuenta?
   **Resuelta:** access token de 15 minutos y refresh token de 7 días con rotación (RN-03, RN-09). Al desactivar una cuenta se revocan sus refresh tokens (RN-11). No hay lista negra de access tokens.
3. **Política de robustez de contraseña**: ¿Cuáles son las reglas de complejidad a validar (mínimo de 8 caracteres, alfanumérico, caracteres especiales, mayúsculas)?
   **Resuelta:** entre 8 y 72 caracteres con al menos una letra y un dígito (RN-13).
4. **Reactivación de usuarios**: ¿Se requerirá habilitar un endpoint recíproco `PATCH /api/v1/users/{id}/activate` para que el Administrador pueda rehabilitar cuentas previamente desactivadas?
   **Resuelta:** se reactiva con `PATCH /api/v1/users/{id}/status` y `{"active": true}` (HU-USR-05). No se crea un endpoint `/activate`.