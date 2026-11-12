# Comparación de herramientas de IA (P1–P2)
| Integrante | Herramienta y modelo | Tarea comparada | Qué resolvió bien | Dónde falló o alucinó | Qué corregiste tú |
|---|---|---|---|---|---|
| Sebastián | Claude Haiku 5.5 | Crear quality-gates-java | Ejecutar quality-gates-java en ms-catalog | Hizo muchas preguntas pero la calidad de la skill era mediana en comparación con otro modelo usado | Use Claude Sonnet 5.5, hizo también muchas preguntas, pero el resultado final de la Skill fue muy superior |
| Isaac | | | | | |
| Fernando | Gemini 3.8 Flash en P1 y GitHub Copilot en P2 (Haiku 5.5) | P1: requerimientos, modelo de datos, arquitectura REST y pruebas de ms-users. P2: gates de calidad, cobertura y ajustes de seguridad/autorización | Gemini generó una base útil para las HU, la migración y la estructura REST. Copilot ayudó a corregir hallazgos de Checkstyle y PMD/CPD, fijar el umbral de JaCoCo y completar ajustes de JWT y autorización | En P1 hubo que corregir la referencia de stack a Java 17 y Spring Boot 4.1.2; los prompts iniciales tampoco fijaban del todo el formato de salida y las pruebas. En las iteraciones de autorización se retiraron rutas y enlaces inconsistentes. No quedó documentada una alucinación concreta | Alineé el stack con el pom.xml, precisé capas, validaciones y errores, y agregué casos de prueba para JWT y roles. En P2 reemplacé literales por constantes y ajusté líneas sin desactivar reglas; limité la supresión del CVE a la dependencia y aviso concretos, y rechacé forzar versiones transitivas incompatibles |

## Conclusión del equipo
<qué herramienta usarían para qué tipo de tarea y por qué, en 3-5 líneas>
Gemini sirvió para generar borradores de especificación y diseño, siempre contrastándolos con el stack y la arquitectura reales.
GitHub Copilot fue útil para iterar sobre el código del repositorio, corregir hallazgos y ejecutar ajustes verificables
En ambos casos, conviene aportar instrucciones concretas y revisar los cambios contra las reglas del proyecto.
Sin embago Claude Haiku 5.5 consideramos que es mejor para modo agente, y Claude Sonnet para el modo Ask y Plan.
