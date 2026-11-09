# Prompt 13 — SonarQube, informe de calidad y entrega (ms-catalog)

## ROL
Actúa como desarrollador Java senior que explica hallazgos de análisis estático, corrige con el mínimo cambio y prepara la documentación de entrega.

## CONTEXTO
Microservicio **ms-catalog**. El análisis de SonarQube for IDE se revisa en VS Code, en la pestaña Problems, sobre `PriorityService.java` (servicio principal).
Plantilla del informe: `docs/QUALITY-REPORT.md` con cinco secciones: 1. Antes y después, 2. Tres hallazgos (qué era, por qué era un problema, cómo lo corregí), 3. Una propuesta de la IA que rechacé, 4. Falsos positivos, 5. Evidencia (últimas 15 líneas de `./mvnw -B verify` e imagen).
Plantilla del PR: `.github/pull_request_template.md`, con las secciones Resumen, Qué hizo la IA y qué hice yo, Validación y Checklist.

## TAREA
Paso 6 (SonarQube):
1. Explica cada regla que marque Sonar y corrige con el mínimo cambio.
2. La captura se guarda en `docs/img/sonarqube-servicio.png`. No la generes.

Paso 7 (informe):
1. Completa los datos objetivos del informe: tablas, números y comandos ejecutados.
2. Las explicaciones de los hallazgos y el rechazo deben venir de mí. Pregúntame con preguntas concretas y ordénalas sin inventarlas.
3. Lo no ejecutado va como "pendiente" con el motivo.

Paso 8 (bitácora y entrega):
1. Verifica que `BITACORA-IA.md` tenga al menos 4 entradas nuevas de P2 con el formato existente.
2. Agrega el rechazo más importante y la prueba de activación de la skill.
3. Entrega el comando de commit `git add . && git commit -m "P2: QUALITY-REPORT y bitácora" && git push`.
4. Después, entrega el comando del PR con la plantilla de `.github/pull_request_template.md`: `gh pr create --base main --head practica-2-<nombre> --title "Entrega P2 – <Nombre Apellido> – ms-catalog" --reviewer eteck`.
5. Solo al confirmar que el PR existe: `git tag p2-entrega && git push origin p2-entrega`.

## RESTRICCIONES
- No ejecutes `git commit`, `git push`, `git tag` ni `gh pr create`. Entrega los comandos.
- No inventes capturas, explicaciones ni resultados.
- Solo reporta lo ejecutado.
- Las reglas de Sonar no se suprimen con comentarios ni con exclusiones.

## FORMATO DE SALIDA
1. Explicación de cada regla Sonar y corrección aplicada.
2. Secciones del informe completadas y preguntas pendientes para mí.
3. Entradas de bitácora en el formato existente.
4. Comandos de commit, PR y tag, en ese orden y con espera de confirmación entre cada uno.
