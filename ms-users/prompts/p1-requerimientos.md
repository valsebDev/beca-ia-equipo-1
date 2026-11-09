# Prompt 1: Requerimientos de Software

## 1. ROL
Actúa como analista de requerimientos senior con experiencia en arquitectura de microservicios y Spring Boot.

## 2. CONTEXTO
Microservicio ms-users del sistema de Mesa de Ayuda en Java 17 y Spring Boot 4.1.2.
Texto de negocio: "El microservicio ms-users maneja registro, autenticación (login con correo y contraseña), emisión de tokens JWT, roles (SOLICITANTE, AGENTE, ADMIN), consulta y edición de perfil, desactivación de cuentas y bloqueo temporal por intentos fallidos de login."

## 3. TAREA
1. Identifica actores, entidades principales y reglas de negocio.
2. Genera 5 historias de usuario (HU) con formato: "Como <rol> quiero <acción> para <beneficio>".
3. Redacta al menos 2 escenarios Gherkin por HU, con al menos 1 escenario negativo con código HTTP explícito.
4. Define 3 requerimientos no funcionales (seguridad, auditoría, RFC 9457).
5. Lista preguntas abiertas para el cliente.

## 4. RESTRICCIONES
- No inventes reglas ajenas al módulo de usuarios; marca supuestos como SUPUESTO.
- Escenarios negativos deben tener códigos HTTP claros (400, 401, 403, 404, 409, 423).

## 5. FORMATO DE SALIDA
Documento Markdown estructurado con secciones: Actores, Entidades, Reglas de Negocio, Historias de Usuario (con Gherkin), RNF y Preguntas Abiertas.

## 6. VERIFICACIÓN
Verifica que cada HU tenga al menos un escenario negativo en Gherkin.

## EJEMPLO
Escenario Gherkin negativo esperado:
Scenario: Intento de registro con correo ya existente
  Given que el correo "usuario@empresa.com" ya se encuentra registrado
  When el cliente intenta registrarse con dicho correo
  Then el sistema rechaza la petición respondiendo con código HTTP 409 Conflict.

## SUPUESTOS
Marca como SUPUESTO cualquier regla que no esté confirmada por el cliente.