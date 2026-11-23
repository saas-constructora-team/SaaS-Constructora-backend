> **Sección sugerida para agregar al final de `README.md` del backend.**
> Cubre cómo ejecutar el proyecto en local y cómo desplegarlo. Puedes copiar
> todo el contenido desde `## Guía de desarrollo` en adelante.

---

## Guía de desarrollo

### Requisitos previos

| Herramienta | Versión / nota |
| --- | --- |
| JDK | 25 (Temurin o Corretto). El repo incluye `./mvnw`, así que no hace falta instalar Maven. |
| PostgreSQL | 16, local o una rama de Neon. |
| Docker | Solo para ejecutar la suite de integración (Testcontainers) y para construir la imagen de despliegue. |
| Neon CLI | Opcional. Permite traer las cadenas de conexión con `neon env pull`. |

### Configuración local

1. Copia la plantilla y completa los valores reales:

   ```bash
   cp .env.example .env
   ```

2. Revisa las variables mínimas para arrancar:

   ```env
   DB_URL=jdbc:postgresql://localhost:5432/saas_constructora
   DB_USERNAME=saas
   DB_PASSWORD=saas
   APP_BASE_URL=http://localhost:4200
   ```

   `application.properties` importa automáticamente `.env` y `.env.local`
   (`spring.config.import=optional:file:.env[.properties],optional:file:.env.local[.properties]`).
   Si no existen, la aplicación arranca con los valores por defecto, siempre que
   haya un PostgreSQL local con la base `saas_constructora` y las credenciales
   `saas` / `saas`.

3. Servicios externos (según lo que vayas a probar):

   - **Correo:** por defecto se usa Resend (`RESEND_API_KEY`, `RESEND_FROM`).
     Para usar SMTP configura `EMAIL_PROVIDER=smtp` y las variables `MAIL_*`.
   - **Google OAuth:** define `GOOGLE_CLIENT_ID`. Si queda vacío, la validación
     falla cerrada y se rechazan todos los inicios de sesión con Google.
   - **JWT:** en local se genera una clave temporal si `JWT_PRIVATE_KEY` está
     vacía. En despliegue define una clave PKCS#8 en Base64 para conservar la
     validez de los tokens entre reinicios (comando en `.env.example`).
   - **Invitaciones:** `INVITATION_ENABLED`, `APP_INVITATION_BASE_URL` y
     `INVITATION_TTL_HOURS` controlan el flujo `invite-and-claim`.

#### Correo: verificación y solución de problemas

Al arrancar, el backend registra el proveedor efectivo:

```
[EMAIL] Proveedor activo=resend, remitente=..., apiKeyConfigurada=true, apiKeyLongitud=...
[EMAIL] Proveedor activo=smtp, remitente=..., host=smtp.gmail.com
```

Si `apiKeyConfigurada=false` o el remitente está vacío, verá un `ERROR`
indicando que los correos **no** se enviarán. Cada intento fallido se registra
con el motivo y el caso de uso devuelve `emailSent: false` junto a `emailError`
en la respuesta de `POST /api/personnel/invite` y en el reenvío. La invitación
queda registrada y se puede reintentar con `POST /api/personnel/invitations/{membershipId}/resend`.

Comprobaciones habituales:

1. **El proveedor activo no es el esperado.** Las variables de entorno del
   sistema y la configuración de ejecución del IDE tienen prioridad sobre
   `.env`. Revise el log `[EMAIL] Proveedor activo=...` antes de seguir.
2. **Resend sin API key.** Defina `RESEND_API_KEY` y reinicie. Un mensaje
   `RESEND_API_KEY no configurada` confirma esta causa.
3. **Resend con remitente no verificado.** `RESEND_FROM` debe ser un correo de
   un dominio verificado en Resend (en pruebas, `onboarding@resend.dev`). Un
   valor sin `@` se rechaza con `RESEND_FROM inválido`.
4. **Gmail por SMTP.** Use una *contraseña de aplicación* de 16 caracteres, no
   la contraseña normal. Un fallo de autenticación aparece como
   `Fallo enviando por SMTP`.
5. **Confirmar la entrega real.** Con Resend, el log de éxito incluye el id del
   mensaje `(id=re_...)`; búsquelo en el panel de Resend → *Emails*. Con SMTP,
   el log confirma que el servidor aceptó el mensaje; revise además la carpeta
   de spam del destinatario.

#### Base de datos

- **Opción A — PostgreSQL local:** crea la base y deja que Flyway aplique las
  migraciones al arrancar:

  ```bash
  createdb -U saas saas_constructora
  ```

- **Opción B — Neon:** el proyecto ya está enlazado (`.neon`). Ejecuta:

  ```bash
  neon env pull          # genera .env.local con DATABASE_URL / DATABASE_URL_UNPOOLED
  ```

  Con esas variables, `shared.config.NeonJdbcConfig` usa la conexión agrupada
  (`-pooler`) para la aplicación y la conexión directa para Flyway. Sin ellas,
  se usan las propiedades `spring.datasource.*` de `application.properties`.

> El esquema lo gestiona exclusivamente Flyway (`spring.flyway.enabled=true`,
> migraciones en `classpath:db/migration`). Hibernate corre con
> `ddl-auto=validate`: no modifiques las tablas a mano.

### Ejecutar la aplicación

```bash
./mvnw spring-boot:run
```

- La API queda disponible en `http://localhost:8080`.
- Health check: `GET http://localhost:8080/actuator/health`.
- Al arrancar, Flyway aplica las migraciones pendientes y Hibernate valida el
  esquema. Si falla la validación, revisa que la base esté vacía o alineada con
  la última migración de `src/main/resources/db/migration/`.

### Pruebas

```bash
# Solo pruebas unitarias (excluye *IntegrationTest)
./mvnw -B -ntp test -Dtest='!*IntegrationTest'

# Suite completa: unitarias + integración con PostgreSQL real (Testcontainers)
./mvnw -B -ntp verify
```

- `verify` además genera el reporte de cobertura JaCoCo en
  `target/site/jacoco/` y exige un mínimo del **80 % de líneas**
  (`jacoco.minimum.line.coverage` en `pom.xml`).
- Los reportes de Surefire quedan en `target/surefire-reports/`.
- Las pruebas de integración necesitan Docker (Testcontainers levanta un
  PostgreSQL efímero).

### Convenciones de desarrollo

El backend es un **monolito modular**: una sola unidad desplegable, con el
código separado por módulos de dominio (`identity`, `projects`, `site-log`,
`documents`, `audit`). Cada módulo sigue, cuando su tamaño lo justifica, la
separación `interfaces/` → `application/` → `domain/` → `infrastructure/`.
`identity`, `projects` y `personnel` ya están organizados así: controladores y
DTOs en `interfaces/`, servicios en `application/`, enums y reglas en `domain/`,
y entidades JPA y repositorios en `infrastructure/`. El contrato público de
`projects` está en `application/ports/ProjectTenantPort` y sus eventos de
integración en `shared/events/`. Queda como deuda que los servicios de
`application/` importen directamente entidades y repositorios JPA.

Reglas innegociables:

1. **Propiedad de datos:** un módulo nunca hace `JOIN` ni consulta directa sobre
   tablas de otro módulo; debe usar el contrato expuesto.
2. **Validación en tres niveles:** estructural (DTO), contextual (servicio de
   aplicación, incluido el `tenantId`) y de negocio (dominio).
3. **Códigos HTTP semánticos:** `200`/`201` éxito, `400` validación, `401`/`403`
   autenticación y aislamiento, `404` no encontrado, `409` regla de negocio.
4. **Multi-tenancy:** toda consulta o filtro debe incluir explícitamente el
   `tenantId` del usuario autenticado. El tenant viaja dentro del JWT; no se
   envía `X-Tenant-Id`.

Tras modificar código, actualiza el grafo de conocimiento desde la raíz del
proyecto (`Software como Servicio`):

```bash
graphify update .
```

---

## Despliegue

### Imagen Docker

El `Dockerfile` es multi-stage:

1. **Build:** `maven:3.9-eclipse-temurin-25` compila con `./mvnw package -DskipTests`
   (con caché de dependencias).
2. **Runtime:** `eclipse-temurin:25-jre-alpine`, con parches de seguridad
   (`apk upgrade`), usuario no-root, puerto `8080` expuesto y un `HEALTHCHECK`
   sobre `/actuator/health`.

Construcción y ejecución local de la imagen:

```bash
docker build -t saas-constructora-backend:local .
docker run --rm -p 8080:8080 --env-file .env saas-constructora-backend:local
```

> En despliegue, las variables sensibles se inyectan como variables de entorno
> (o secretos de la plataforma), nunca se versionan en el repositorio.

### Integración continua

Los workflows viven en `.github/workflows/`:

| Workflow | Disparador | Qué hace |
| --- | --- | --- |
| `ci.yml` | Push a `main`/`chore/*`/`feat/*`/`refactor/*` y PRs a `main` | Unitarias en cada push; en PR/main corre la suite completa con Testcontainers, cobertura y umbral del 80 %; empaqueta el JAR. |
| `build-image.yml` | Push a `main`, tags `v*.*.*` y PRs a `main` | Construye la imagen, escanea con Trivy (falla en HIGH/CRITICAL), la publica en GHCR (`:sha-<commit>`, `:main` y tags semver) y firma con cosign en releases. |
| `codeql.yml` | Push/PR a `main` y semanal | SAST con CodeQL (`security-and-quality`). |
| `cleanup-packages.yml` | Semanal (lunes 04:00 UTC) | Conserva las 10 imágenes más recientes en GHCR y elimina las sin tag. |

Imagen publicada:

```bash
docker pull ghcr.io/saas-constructora-team/saas-constructora-backend:main
# o una versión concreta:
docker pull ghcr.io/saas-constructora-team/saas-constructora-backend:v0.1.0
```

### Secrets requeridos en GitHub

Configurar en **Settings → Secrets and variables → Actions**:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `RESEND_API_KEY`, `RESEND_FROM`, `EMAIL_PROVIDER`, `MAIL_HOST`, `MAIL_PORT`,
  `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- `GOOGLE_CLIENT_ID`
- `JWT_PRIVATE_KEY`, `JWT_TTL_MINUTES`
- `APP_BASE_URL`

`GITHUB_TOKEN` lo provee Actions para publicar en GHCR.

### Destino de despliegue (pendiente)

El CD definitivo aún no tiene destino elegido. Cuando se defina (VM con Docker,
PaaS, etc.), completar:

1. Crear los **GitHub Environments** `staging` y `production` (producción con
   *required reviewers*).
2. Despliegue automático a staging y **con aprobación** a producción.
3. Estrategia blue/green o canary y **rollback** por tag `sha-<commit>` anterior.
4. Smoke/E2E test contra staging antes de promover.
5. Autenticación **OIDC** con el proveedor cloud (sin secrets estáticos).

### Verificación post-despliegue

```bash
# La imagen debe responder "UP"
curl https://<host>/actuator/health

# Confirmar que Flyway aplicó las migraciones (logs de arranque):
# "Successfully applied N migrations"
```

Comprobar además que el emisor de correo funciona (recuperación de contraseña
e invitaciones), que el `GOOGLE_CLIENT_ID` corresponde al entorno y que la
misma clave `JWT_PRIVATE_KEY` se usa en todas las réplicas para no invalidar
sesiones entre reinicios.
