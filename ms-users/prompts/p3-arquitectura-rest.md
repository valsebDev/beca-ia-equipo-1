# Prompt 3: Arquitectura REST por Capas

## 1. ROL
Actúa como Desarrollador Backend Senior en Java 17 y Spring Boot 4.1.2.

## 2. CONTEXTO
Microservicio ms-users. Stack: Java 17, Spring Boot 4.1.2, Spring Data JPA, Maven. Paquete base: com.ids.ms_users.

## 3. TAREA
Generar la arquitectura REST por capas completa:
1. Entidad JPA User y Enum Role.
2. DTOs para Request y Response.
3. UserRepository con Spring Data JPA.
4. TokenService y UserService con lógica de negocio y bloqueo.
5. AuthController y UserController.
6. GlobalExceptionHandler con ProblemDetail (RFC 9457).

## 4. RESTRICCIONES
- Capas estrictas: Controller -> Service -> Repository.
- DTOs obligatorios como record de Java.
- Validaciones Jakarta (@NotBlank, @Email, etc.).
- Sin valores en duro (valores en application.yaml).
- Líneas de código con longitud máxima de 120 caracteres para cumplir Checkstyle.

## 5. FORMATO DE SALIDA
Bloques de código con rutas completas relativas.

## 6. VERIFICACIÓN
Comprobar compilación limpia con ./mvnw clean compile.

## EJEMPLO
Una respuesta de error de validación debe usar ProblemDetail con status 400 y una propiedad `invalidParams`.

## SUPUESTOS
- El formato RFC 9457 puede incluir campos type, title, status, detail, instance e invalidParams.