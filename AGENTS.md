# Backend Guidelines (`saas-backend`)

## 1. Patrón Arquitectónico y Estructura
* **Patrón:** Monolito Modular. El backend se despliega como una única unidad ejecutable, pero el código está estrictamente dividido en **módulos de dominio independientes** (`identity`, `projects`, `site-log`, `documents`, `audit`).
* **Clean Architecture interna:** Cada módulo vive en `src/main/java/com/SaaS/Constructora/{module-name}/` y, cuando su tamaño lo justifica, mantiene esta separación:
  * `interfaces/`: Controladores HTTP y DTOs (Validación Estructural).
  * `application/`: Casos de Uso y Servicios de Aplicación (Validación Contextual) + `ports/`.
  * `domain/`: Entidades y Reglas de Negocio (Validación de Dominio).
  * `infrastructure/`: Implementación de Repositorios y Persistencia (adaptadores JPA).
* **Estado actual de los módulos:** `identity` ya usa esa separación (`application/`, `domain/`, `infrastructure/`, `interfaces/`). `projects` es un módulo plano: `Project`, `ProjectRepository`, `ProjectService`, `ProjectController` y sus DTOs comparten el paquete base, y debe mantenerse así mientras no gane complejidad.

## 2. Reglas Innegociables del Backend
1. **Propiedad de Datos (Data Ownership):** Queda **estrictamente prohibido** el acoplamiento cruzado directo entre bases de datos de distintos módulos. Un módulo **NUNCA** ejecuta `SQL JOIN` ni consultas directas sobre las tablas de otro módulo. Debe invocar la interfaz/contrato expuesto.
2. **Validación en 3 Niveles:**
   * **Estructural (Capa Interface/DTO):** Formato de datos, campos requeridos, UUIDs, ISO dates.
   * **Contextual (Capa Application):** Verificación de existencia de entidades relacionadas y aislamiento del `tenantId`.
   * **De Negocio (Capa Domain):** Reglas puras del dominio (ej. prohibir registros en etapas cerradas).
3. **Manejo Semántico de Códigos HTTP:**
   * `200 OK` / `201 Created` para respuestas exitosas.
   * `400 Bad Request` para fallos de validación estructural.
   * `401 Unauthorized` / `403 Forbidden` para autenticación y aislamiento multitenant.
   * `404 Not Found` para recursos inexistentes.
   * `409 Conflict` para violaciones de reglas de negocio.
4. **Protección Multi-tenancy:** Toda consulta o filtro en base de datos **DEBE** incluir explícitamente el `tenantId` correspondiente al contexto del usuario autenticado.