# Prompt 2: Modelo Relacional y Migración Flyway

## 1. ROL
Actúa como Administrador de Bases de Datos especializado en PostgreSQL 16.

## 2. CONTEXTO
Microservicio ms-users basado en ms-users/docs/requerimientos.md. Motor: PostgreSQL 16. Migración: Flyway.

## 3. TAREA
1. Diseñar el modelo relacional para la tabla users.
2. Generar el script de migración SQL Flyway V1__crear_tablas_usuarios.sql.
3. Justificar en comentarios SQL cada índice propuesto según las HU.

## 4. RESTRICCIONES
- Tipos de datos nativos de PostgreSQL 16 (BIGSERIAL, VARCHAR, TIMESTAMP, BOOLEAN).
- Restricción UNIQUE en email y user_identifier.
- Incluir columnas failed_attempts y locked_until para HU-03.
- Sin sintaxis de H2 ni MySQL.

## 5. FORMATO DE SALIDA
Bloque de código SQL único listo para V1__crear_tablas_usuarios.sql.

## 6. VERIFICACIÓN
Verificar que todas las columnas NOT NULL, UNIQUE y llaves foráneas/check estén comentadas.

## EJEMPLO
```sql
-- Justifica: HU-02 consulta usuarios por correo durante el inicio de sesión.
CREATE UNIQUE INDEX uk_users_email ON users (email);
```

## SUPUESTOS
- Contraseñas almacenadas como hash BCrypt de longitud variable (VARCHAR 255).