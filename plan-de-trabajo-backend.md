# Plan de trabajo backend

## Plan de acción para el backend SaaS de gestión de constructoras

Este plan adapta las fases de `plan-desarrollo-backend.md` al estado actual del repositorio. Distingue lo que ya está implementado de lo que falta, y ordena el trabajo para completar las capacidades del producto sin rehacer las rebanadas que ya existen.

## Estado actual del proyecto

El backend es una aplicación Java con Spring Boot, PostgreSQL, JPA, Flyway y Spring Modulith. El código está organizado actualmente bajo `com.saas.constructora`, con los módulos implementados `identity` y `projects`, además de `shared/security`. Las migraciones están en `src/main/resources/shared/database/migration`.

| Área | Implementado | Pendiente relevante |
|---|---|---|
| Identidad | Registro de tenant y administrador, login con contraseña, login/registro con Google, recuperación y cambio de contraseña, persistencia y migraciones | Administración de usuarios, invitaciones, gestión efectiva de roles y permisos por endpoint |
| Proyectos | Crear y listar proyectos por `tenantId`; estado inicial `ACTIVE` | Detalle, actualización, cierre, etapas, asignaciones, tareas y tablero Kanban |
| Seguridad compartida | Emisión de JWT, BCrypt, hash de tokens de restablecimiento | Validar JWT en solicitudes protegidas, derivar el tenant del usuario autenticado y aplicar autorización |
| Persistencia | PostgreSQL, JPA y migraciones Flyway | Migraciones y modelos para las capacidades aún no desarrolladas |
| Correo | Adaptadores configurables para Resend y SMTP | Configuración operativa por entorno y manejo/observabilidad de fallos de envío |
| Bitácora, documentos y auditoría | No hay módulos de negocio implementados; existe infraestructura de publicación de eventos de Spring Modulith | Implementar módulos, almacenamiento de archivos y trazabilidad funcional |
| Pruebas | Pruebas de identidad y recuperación con PostgreSQL Testcontainers | Pruebas de proyectos, autorización/aislamiento y reglas de arquitectura entre módulos |

## Fases de trabajo

### Fase 0 — Consolidar la base técnica y el aislamiento multitenant

**Estado: parcialmente completada.** Ya existen la aplicación Spring Boot, PostgreSQL/JPA, Flyway, manejo de contraseñas y emisión de JWT. La organización de carpetas se alineó con `AGENTS.md` para los módulos existentes.

**Trabajo pendiente, prioritario:**

1. Configurar Spring Security para validar los JWT emitidos y proteger los endpoints que requieren sesión.
2. Construir el contexto autenticado de usuario, rol y tenant a partir de los claims verificados del token. Los endpoints de negocio no deben confiar en un `X-Tenant-Id` elegido por el cliente.
3. Aplicar autorización por rol y aislamiento de tenant en cada operación de negocio.
4. Definir un formato uniforme para errores HTTP y traducir de forma consistente errores de validación, autenticación, autorización, recursos inexistentes y conflictos.
5. Revisar la persistencia de la clave de firma JWT: actualmente se genera al iniciar el proceso, por lo que los tokens dejan de ser válidos tras un reinicio.
6. Confirmar versiones y entorno de compilación: el plan de origen menciona Java 21 y Spring Boot 3, mientras que `pom.xml` declara Java 25 y Spring Boot 4.1.1.
7. Añadir perfiles de configuración claros para desarrollo y despliegue, y comprobar que los secretos se suministran mediante variables de entorno.

**Criterio de salida:** una petición autenticada obtiene su identidad y tenant desde un JWT validado; una petición sin autorización no puede leer ni modificar datos de negocio de otro tenant.

### Fase 1 — Completar identidad y administración de usuarios

**Estado: rebanada inicial implementada.** `identity` ya permite registrar empresas y administradores, iniciar sesión con contraseña o Google y recuperar contraseñas. Existen entidades, puertos, adaptadores JPA, DTOs, migraciones y pruebas de integración para los flujos principales.

**Trabajo pendiente:**

1. Añadir operaciones de administración de usuarios dentro de un tenant: alta/invitación, consulta, actualización, desactivación y asignación de roles.
2. Definir permisos y reglas de transición para roles; hoy el modelo contempla roles, pero no hay gestión de usuarios ni autorización de operaciones.
3. Revisar unicidad de correo según la política del producto (actualmente la base de datos lo hace único globalmente).
4. Asegurar que los cambios de contraseña, cuentas desactivadas y flujos Google respeten las mismas reglas de sesión y estado de usuario.
5. Exponer contratos internos estables si otros módulos necesitan validar usuarios o consultar una representación pública de identidad, sin acceder a repositorios o entidades JPA de `identity`.

**Criterio de salida:** el administrador puede gestionar usuarios de su empresa y las operaciones quedan restringidas por tenant y rol.

### Fase 2 — Completar proyectos, etapas, asignaciones y tareas

**Estado: MVP parcial implementado.** `projects` permite crear un proyecto y listar los proyectos de un tenant. La entidad actual almacena nombre, cliente, ubicación, estado inicial `ACTIVE` y fecha de creación.

**Trabajo pendiente:**

1. Completar el aislamiento de las rutas actuales con el contexto autenticado definido en la Fase 0.
2. Añadir consulta de detalle, actualización y transiciones de estado de proyectos con sus validaciones de dominio.
3. Implementar etapas de proyecto con orden, estado y reglas para evitar cambios incompatibles con proyectos cerrados.
4. Implementar asignaciones de usuarios a proyectos mediante contratos del módulo `identity`.
5. Implementar tareas y sus estados para habilitar el tablero Kanban asociado a HU-03 y HU-09.
6. Publicar eventos de dominio relevantes, por ejemplo creación de proyecto y cierre de etapa, si los consumidores de auditoría los requieren.
7. Mantener DTOs/controladores en interfaces, reglas en aplicación/dominio y persistencia en infraestructura. Definir puertos de aplicación antes de que otros módulos dependan de proyectos.

**Criterio de salida:** los flujos de proyectos, etapas y tareas cuentan con reglas de tenant y estado, contratos HTTP documentados y pruebas de integración.

### Fase 3 — Implementar bitácora de campo (`site-log`)

**Estado: pendiente.** No existe código de bitácora en este momento.

**Trabajo propuesto:**

1. Acordar el contrato HTTP, actores, DTOs, errores y reglas antes de implementar la rebanada.
2. Crear `modules/site-log` con capas de interfaces, aplicación, dominio e infraestructura.
3. Registrar novedades asociadas a proyecto y etapa, autor, tipo de evento, descripción y fecha; agregar filtros de consulta.
4. Validar en aplicación que el proyecto y la etapa pertenecen al tenant actual y que la etapa permite registrar novedades, usando el contrato público de `projects`.
5. Añadir evidencia fotográfica como metadatos y referencia de almacenamiento; no guardar archivos binarios directamente en la base de datos.
6. Publicar un evento de bitácora creada para permitir auditoría desacoplada.
7. Añadir migración y pruebas unitarias e integración de la rebanada.

**Criterio de salida:** un usuario autorizado registra y consulta novedades de una obra propia, con reglas de etapa y tenant comprobadas.

### Fase 4 — Implementar documentos, versiones y almacenamiento

**Estado: pendiente.** No existe módulo `documents` ni adaptador de almacenamiento; en `pom.xml` la dependencia S3 aparece como una alternativa comentada.

**Trabajo propuesto:**

1. Definir el contrato de carga y consulta, tipos/tamaños permitidos, límites y permisos por rol.
2. Decidir proveedor de almacenamiento compatible con S3 para desarrollo y despliegue (por ejemplo, S3 o MinIO) y definir configuración segura.
3. Crear `modules/documents` con metadatos del documento y versiones inmutables; guardar en PostgreSQL los metadatos y la clave del objeto, no el binario.
4. Asociar documentos a proyectos y validar pertenencia del proyecto al tenant mediante contratos del módulo `projects`.
5. Implementar carga de nuevas versiones sin sobrescribir las anteriores y consulta del historial.
6. Publicar eventos de nueva versión para auditoría y, si aplica, procesamiento posterior de renders/visores.
7. Añadir migraciones y pruebas para permisos, versiones y fallos del almacenamiento.

**Criterio de salida:** la carga y consulta de documentos conserva el historial, respeta tenant/roles y maneja errores del almacenamiento sin dejar metadatos inconsistentes.

### Fase 5 — Implementar auditoría y trazabilidad (`audit`)

**Estado: pendiente.** No hay un módulo de auditoría funcional. La tabla `event_publication` instalada por Spring Modulith es infraestructura para publicaciones de eventos; no representa por sí misma un historial de acciones de negocio.

**Trabajo propuesto:**

1. Definir qué acciones se auditan, qué datos se conservan, quién puede consultarlos y por cuánto tiempo.
2. Crear `modules/audit` con registros append-only filtrados por tenant y controles de acceso.
3. Escuchar eventos de negocio publicados por `projects`, `site-log` y `documents`; los módulos productores no deben invocar directamente repositorios o servicios de auditoría.
4. Acordar el comportamiento ante reintentos y fallos de publicación/persistencia para evitar pérdida o duplicación de registros.
5. Exponer consultas paginadas y filtros autorizados; no registrar secretos, tokens ni contenido sensible innecesario.
6. Añadir migración y pruebas de publicación, consumo y aislamiento.

**Criterio de salida:** las acciones definidas se registran y pueden consultarse por usuarios autorizados sin acoplar módulos productores a la persistencia de auditoría.

### Fase 6 — Pruebas de límites modulares y recorridos completos

**Estado: parcial.** Hay pruebas unitarias/de controlador y pruebas de integración de identidad con PostgreSQL Testcontainers. No encontré pruebas de arquitectura de módulos ni un recorrido completo de proyectos, bitácora, documentos y auditoría.

**Trabajo pendiente:**

1. Añadir pruebas de arquitectura con Spring Modulith para detectar dependencias prohibidas y ciclos entre módulos.
2. Completar pruebas de integración de `projects`, incluyendo creación, listados, autorización y acceso entre tenants.
3. Añadir pruebas de integración por vertical slice para cada módulo nuevo, con PostgreSQL y adaptadores externos simulados cuando corresponda.
4. Incorporar el recorrido principal: autenticación, proyecto/etapa, bitácora con evidencia, documento/versionado y registro de auditoría a medida que esas capacidades existan.
5. Ejecutar compilación y suite de pruebas en un entorno con las versiones requeridas y servicios disponibles antes de integrar cada fase.

**Criterio de salida:** los límites entre módulos y las reglas críticas quedan cubiertos por pruebas repetibles, además de las pruebas específicas de cada endpoint.

## Orden recomendado

1. Fase 0: seguridad JWT y aislamiento multitenant.
2. Fase 1: completar autorización y administración de usuarios.
3. Fase 2: completar proyectos, etapas, asignaciones y tareas.
4. Fase 3: bitácora de campo.
5. Fase 4: documentos, versiones y almacenamiento.
6. Fase 5: auditoría por eventos.
7. Fase 6: reforzar pruebas modulares y ejecutar recorridos completos conforme avancen las fases.

Las fases deben trabajarse como rebanadas verticales: contrato, persistencia/migración, dominio/aplicación, interfaz, pruebas y documentación del endpoint. Este plan no implica que las capacidades marcadas como pendientes ya estén implementadas.

## Dependencias entre módulos

| Módulo productor | Contratos síncronos permitidos | Eventos que puede publicar/consumir |
|---|---|---|
| `identity` | Ninguno requerido inicialmente | Publica eventos de identidad solo si una necesidad de negocio lo justifica |
| `projects` | Contrato público de `identity` para validar usuarios/asignaciones | Publica eventos de proyecto y etapas |
| `site-log` | Contratos públicos de `projects` e `identity` | Publica evento de bitácora creada |
| `documents` | Contratos públicos de `projects` e `identity` | Publica evento de versión cargada |
| `audit` | No consulta sincrónicamente módulos de negocio | Consume eventos de los módulos productores |

Ningún módulo debe consultar directamente tablas, repositorios o entidades de otro módulo. Cada módulo es dueño de sus datos y ofrece contratos internos explícitos para las consultas que otros necesiten.
