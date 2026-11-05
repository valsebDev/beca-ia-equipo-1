# Prompt 4: Pruebas Automatizadas Unitarias y WebMvc

## 1. ROL
Actúa como Ingeniero de Pruebas (QA Automation) en Java/Spring Boot.

## 2. CONTEXTO
Microservicio ms-users. Pruebas de servicio y controlador según los escenarios Gherkin de requerimientos.

## 3. TAREA
1. Pruebas unitarias para UserService con JUnit 5 y Mockito.
2. Pruebas de integración web para UserController y AuthController con @WebMvcTest y MockMvc.
3. Cobertura de escenarios positivos (200, 201) y negativos (400, 401, 404, 409, 423).

## 4. RESTRICCIONES
- Mínimo 8 pruebas independientes.
- Nomenclatura descriptiva (ej. login_credencialesInvalidas_devuelve401).
- Sintaxis moderna BDD (given / when / then).

## 5. FORMATO DE SALIDA
Archivos UserServiceTest.java y UserControllerTest.java con rutas completas.

## 6. VERIFICACIÓN
Ejecutar ./mvnw test y confirmar todas en verde.

## 7. SUPUESTOS / EJEMPLO
- Aislar el contexto de Spring en pruebas web usando @AutoConfigureMockMvc(addFilters = false).