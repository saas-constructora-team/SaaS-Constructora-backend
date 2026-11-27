# Graph Report - SaaS-Constructora-backend  (2026-11-24)

## Corpus Check
- 278 files · ~60,069 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 9 file(s) not represented in the graph (top: (none) 5, .properties 2, .example 1)

## Summary
- 2270 nodes · 7584 edges · 121 communities (56 shown, 65 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 1085 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `6e2e8ee5`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Worker
- User
- TaskStatus
- .create
- ProjectStage
- UserSummary
- AuthControllerTest.java
- PasswordResetToken
- InvitationIntegrationTest
- uuid
- ProjectArea
- users
- org.junit.jupiter.api.Test
- EmailSendResult
- .update
- notblank
- ProjectAuthorizationIntegrationTest
- ProjectAssignmentDelegationTest
- Tenant
- ProjectAssignmentIntegrationTest
- ProjectStageIntegrationTest
- ProjectServiceTest
- UserJpaEntity
- ProjectTenantIntegrationTest
- .register
- GoogleTokenVerifierClient
- MembershipInvitation
- TenantJpaEntity
- MembershipValidationFilter.java
- AuditLog
- BudgetRange
- ProjectAssignmentDelegationIntegrationTest
- MembershipJpaEntity
- V2__create_event_publication.sql
- com.SaaS:Constructora
- Role
- Membership
- MembershipRepository
- .create
- Project
- package.json
- .accept
- Backend Guidelines (`saas-backend`)
- DocumentService
- ProjectService
- ExternalIdentity
- V13__create_workers.sql
- InviteMembershipUseCase.java
- .from
- AuthController.java
- V14__create_worker_positions.sql
- org.springframework.transaction.annotation.Transactional
- NeonJdbcConfig.java
- ProjectType
- ProjectAssignment
- Document
- TaskAreaDetails
- PersonnelServiceTest.java
- ProjectAssignmentRepositoryAdapter
- TaskIntegrationTest
- ProjectAssignmentService
- DocumentService.java
- ProjectRole
- DocumentController.java
- org.springframework.http.ResponseEntity
- FilePolicy
- DocumentRepositoryAdapter
- tasks
- .upload
- AuthenticatedUser
- .verify
- AreaDetailsRequest
- CategoriaInsumo
- ProjectController.java
- JwtConfig.java
- FilePolicyTest.java
- TaskAreaDetailsValidatorTest.java
- S3ObjectStorageAdapter
- org.springframework.context.annotation.Configuration
- DocumentIntegrationTest
- .reset
- WorkerPosition
- AuditIntegrationTest
- TaskAreaDetails.java
- MembershipInvitationTest
- project_assignments
- AuditController.java
- projects
- V27__create_documents.sql
- .findActiveByUserIdAndTenantId
- UnidadMedida
- TaskController
- membership_invitations
- StorageConfig
- TenantContext
- .create
- CapituloPresupuestal
- V23__create_audit_log.sql
- AddVersionCommand
- InvitationPreview
- memberships
- password_reset_tokens
- project_stages

## God Nodes (most connected - your core abstractions)
1. `Role` - 105 edges
2. `Membership` - 81 edges
3. `AuthenticatedUser` - 63 edges
4. `MembershipRepository` - 56 edges
5. `ProjectAssignment` - 54 edges
6. `User` - 53 edges
7. `ProjectService` - 50 edges
8. `UserRepository` - 48 edges
9. `Project` - 47 edges
10. `TaskStatus` - 40 edges

## Surprising Connections (you probably didn't know these)
- `Base de datos` --references--> `NeonJdbcConfig`  [INFERRED]
  README.md → src/main/java/com/SaaS/Constructora/shared/config/NeonJdbcConfig.java
- `AuditQueryService` --references--> `AuditLogRepository`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/audit/application/AuditQueryService.java → src/main/java/com/SaaS/Constructora/audit/application/ports/AuditLogRepository.java
- `DocumentService` --references--> `ObjectStoragePort`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/documents/application/DocumentService.java → src/main/java/com/SaaS/Constructora/documents/application/ports/ObjectStoragePort.java
- `DocumentService` --references--> `ProjectTenantPort`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/documents/application/DocumentService.java → src/main/java/com/SaaS/Constructora/projects/application/ports/ProjectTenantPort.java
- `DocumentController` --references--> `DocumentService`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/documents/interfaces/DocumentController.java → src/main/java/com/SaaS/Constructora/documents/application/DocumentService.java

## Import Cycles
- None detected.

## Communities (121 total, 65 thin omitted)

### Community 0 - "Worker"
Cohesion: 0.10
Nodes (10): InvitationIssued, DocumentType, FOREIGN_ID, IDENTITY_CARD, NATIONAL_ID, OTHER, PASSPORT, Worker (+2 more)

### Community 2 - "TaskStatus"
Cohesion: 0.07
Nodes (6): TaskStatus, CreateTaskStatusRequest, TaskStatusController, TaskStatusResponse, UpdateTaskStatusRequest, TaskStatusServiceTest

### Community 4 - "ProjectStage"
Cohesion: 0.08
Nodes (7): ProjectStageService, ProjectStage, CreateProjectStageRequest, ProjectStageController, ProjectStageResponse, UpdateProjectStageRequest, ProjectStageServiceTest

### Community 5 - "UserSummary"
Cohesion: 0.19
Nodes (3): PermissionManagementUseCase, UserSummary, UserResponse

### Community 6 - "AuthControllerTest.java"
Cohesion: 0.19
Nodes (4): FakeStorageConfig, TestEmailConfig, TestEmailConfig, MembershipValidationFilterIntegrationTest

### Community 7 - "PasswordResetToken"
Cohesion: 0.11
Nodes (4): PasswordResetToken, PasswordResetTokenJpaEntity, PasswordResetTokenJpaRepository, PasswordResetTokenRepositoryAdapter

### Community 9 - "uuid"
Cohesion: 0.07
Nodes (3): MembershipStatus, ACTIVE, INVITED

### Community 10 - "ProjectArea"
Cohesion: 0.07
Nodes (14): ProjectAreaService, AreaTipo, COMPRAS_ALMACEN, CONTABILIDAD_FINANZAS, DISENO_ARQUITECTURA, LEGAL_TRAMITES, OBRA_INGENIERIA, PERSONALIZADA (+6 more)

### Community 11 - "users"
Cohesion: 0.33
Nodes (8): idx_memberships_active, idx_memberships_tenant, idx_memberships_user, memberships, tenants, users, external_identities, idx_external_identity_user

### Community 12 - "org.junit.jupiter.api.Test"
Cohesion: 0.08
Nodes (4): AuthenticationResult, AuthControllerTest, FakeGoogleTokenVerifier, LoginIntegrationTest

### Community 13 - "EmailSendResult"
Cohesion: 0.09
Nodes (8): EmailSendResult, EmailTemplates, ResendEmailSender, SmtpEmailSender, ResendEmailSenderTest, SmtpEmailSenderTest, RecordingInvitationEmailSender, RecordingEmailSender

### Community 19 - "Tenant"
Cohesion: 0.15
Nodes (7): GoogleAuthCommand, RegisterTenantResult, Tenant, GoogleAuthResponse, TenantResponse, RegisterTenantResponse, TenantResponse

### Community 21 - "ProjectStageIntegrationTest"
Cohesion: 0.21
Nodes (4): ObjectStoragePort, ProjectStageIntegrationTest, Session, StageRef

### Community 23 - "UserJpaEntity"
Cohesion: 0.09
Nodes (4): ActiveUserProjection, UserJpaEntity, UserJpaRepository, UserRepositoryAdapter

### Community 26 - "GoogleTokenVerifierClient"
Cohesion: 0.16
Nodes (5): GoogleAccount, GoogleTokenVerifier, GoogleTokenVerifierClient, ParameterizedTypeReference<Map<String, Object>>, GoogleTokenVerifierClientTest

### Community 28 - "MembershipInvitation"
Cohesion: 0.14
Nodes (3): MembershipInvitationJpaEntity, MembershipInvitationJpaRepository, MembershipInvitationRepositoryAdapter

### Community 29 - "TenantJpaEntity"
Cohesion: 0.15
Nodes (3): TenantJpaEntity, TenantJpaRepository, TenantRepositoryAdapter

### Community 30 - "MembershipValidationFilter.java"
Cohesion: 0.05
Nodes (6): ApiError, GlobalExceptionHandler, CurrentUserArgumentResolver, MembershipValidationFilter, SecurityConfig, TenantClaims

### Community 31 - "AuditLog"
Cohesion: 0.06
Nodes (12): AuditLogRepository, AuditLog, AuditEventListener, AuditLogJpaRepository, AuditLogRepositoryAdapter, AuditEventResponse, DocumentEntity, DocumentVersionEntity (+4 more)

### Community 32 - "BudgetRange"
Cohesion: 0.10
Nodes (13): BudgetRange, FROM_100M_TO_250M, FROM_250M_TO_500M, FROM_50M_TO_100M, OVER_500M, UNDER_50M, LotOwnership, IN_PURCHASE (+5 more)

### Community 34 - "MembershipJpaEntity"
Cohesion: 0.10
Nodes (3): MembershipJpaEntity, MembershipJpaRepository, MembershipRepositoryAdapter

### Community 35 - "V2__create_event_publication.sql"
Cohesion: 0.83
Nodes (3): event_publication, idx_event_publication_completion_date, idx_event_publication_publication_date

### Community 38 - "Role"
Cohesion: 0.15
Nodes (4): Role, ADMIN, MEMBER, WORKER

### Community 40 - "MembershipRepository"
Cohesion: 0.21
Nodes (18): AcceptInvitationUseCase, GoogleAuthUseCase, InvitationGate, InvitationQueryService, InviteMembershipUseCase, LoginUseCase, MembershipInvitationRepository, MembershipRepository (+10 more)

### Community 43 - "package.json"
Cohesion: 0.29
Nodes (5): dependencies, @neon/config, @neon/env, @neon/config, @neon/env

### Community 45 - "Backend Guidelines (`saas-backend`)"
Cohesion: 0.50
Nodes (3): 1. Patrón Arquitectónico y Estructura, 2. Reglas Innegociables del Backend, Backend Guidelines (`saas-backend`)

### Community 46 - "DocumentService"
Cohesion: 0.15
Nodes (4): DocumentService, DownloadLink, DocumentRepository, DocumentVersion

### Community 47 - "ProjectService"
Cohesion: 0.18
Nodes (12): TaskCreationPermissionService, TenantQueryService, ProjectAssignmentRepository, ProjectService, TaskService, TaskStatusService, ProjectAreaRepository, ProjectRepository (+4 more)

### Community 48 - "ExternalIdentity"
Cohesion: 0.12
Nodes (5): ExternalIdentityRepository, ExternalIdentity, ExternalIdentityJpaEntity, ExternalIdentityJpaRepository, ExternalIdentityRepositoryAdapter

### Community 49 - "V13__create_workers.sql"
Cohesion: 0.83
Nodes (3): idx_workers_project, idx_workers_tenant, workers

### Community 54 - ".from"
Cohesion: 0.13
Nodes (5): Task, AreaResponse, AssignedUser, StatusResponse, TaskResponse

### Community 55 - "AuthController.java"
Cohesion: 0.12
Nodes (10): AcceptInvitationCommand, LoginCommand, RegisterTenantCommand, AcceptInvitationRequest, ForgotPasswordRequest, LoginRequest, LoginResponse, MessageResponse (+2 more)

### Community 60 - "NeonJdbcConfig.java"
Cohesion: 0.08
Nodes (19): Base de datos, Configuración local, Convenciones de desarrollo, Correo: verificación y solución de problemas, Despliegue, Destino de despliegue (pendiente), Ejecutar la aplicación, Guía de desarrollo (+11 more)

### Community 61 - "ProjectType"
Cohesion: 0.12
Nodes (9): ProjectStatus, ACTIVE, ProjectType, COMMERCIAL, INDUSTRIAL, MIXED, RESIDENTIAL, ProjectController (+1 more)

### Community 63 - "Document"
Cohesion: 0.18
Nodes (6): AssetKind, DOCUMENT, IMAGE, RENDER_3D, Document, UploadDocumentRequest

### Community 66 - "PersonnelServiceTest.java"
Cohesion: 0.28
Nodes (8): PersonnelPermissionService, InviteWorkerUseCase, PersonnelService, WorkerPositionService, WorkerPositionRepository, WorkerRepository, WorkerResponse, ProjectTenantPort

### Community 72 - "ProjectRole"
Cohesion: 0.18
Nodes (6): ProjectRole, LEAD, MEMBER, AssignProjectRequest, ProjectAssignmentResponse, UpdateProjectAssignmentRequest

### Community 73 - "DocumentController.java"
Cohesion: 0.18
Nodes (3): DocumentController, DocumentResponse, DocumentVersionResponse

### Community 76 - "DocumentRepositoryAdapter"
Cohesion: 0.21
Nodes (3): DocumentJpaRepository, DocumentRepositoryAdapter, DocumentVersionJpaRepository

### Community 77 - "tasks"
Cohesion: 0.21
Nodes (15): idx_tasks_project, idx_tasks_tenant, idx_tasks_tenant_project, tasks, idx_project_areas_project, idx_project_areas_tenant, idx_task_statuses_project, idx_task_statuses_tenant (+7 more)

### Community 78 - ".upload"
Cohesion: 0.33
Nodes (3): FileUpload, UploadDocumentCommand, DocumentServiceTest

### Community 79 - "AuthenticatedUser"
Cohesion: 0.46
Nodes (3): UserResponse, UserController, AuthenticatedUser

### Community 81 - "AreaDetailsRequest"
Cohesion: 0.29
Nodes (3): TaskAreaDetailsValidator, AreaDetailsRequest, TaskAreaDetailsValidatorTest

### Community 82 - "CategoriaInsumo"
Cohesion: 0.16
Nodes (11): CategoriaInsumo, ALQUILER_MAQUINARIA, DOTACION_EPP, HERRAMIENTAS, MATERIALES, PermisoSst, ESPACIO_CONFINADO, OTRO (+3 more)

### Community 86 - "TaskAreaDetailsValidatorTest.java"
Cohesion: 0.18
Nodes (6): Disciplina, ARQUITECTONICO, ELECTRICO, ESTRUCTURAL, HIDROSANITARIO, REDES_GAS

### Community 94 - "TaskAreaDetails.java"
Cohesion: 0.20
Nodes (5): TipoDocumentoContable, ACTA_CORTE_OBRA, ANTICIPO, CUENTA_COBRO, FACTURA

### Community 97 - "project_assignments"
Cohesion: 0.27
Nodes (7): idx_project_assignments_project_role, idx_project_assignments_tenant_user_project, idx_project_assignments_active_tenant_user, uk_project_assignments_active, idx_project_assignments_project, idx_project_assignments_tenant, project_assignments

### Community 99 - "projects"
Cohesion: 0.36
Nodes (7): idx_projects_tenant, projects, idx_projects_client_name_trgm, idx_projects_name_trgm, idx_projects_tenant_created, idx_projects_tenant_status_created, idx_projects_tenant_type_created

### Community 100 - "V27__create_documents.sql"
Cohesion: 0.42
Nodes (8): document_versions, documents, idx_document_versions_document, idx_document_versions_tenant, idx_document_versions_tenant_document, idx_documents_project, idx_documents_tenant, idx_documents_tenant_project

### Community 102 - "UnidadMedida"
Cohesion: 0.25
Nodes (7): UnidadMedida, GLOBAL, KG, M2, M3, ML, UNIDAD

### Community 104 - "membership_invitations"
Cohesion: 0.43
Nodes (6): idx_membership_invitations_email, idx_membership_invitations_expires, idx_membership_invitations_membership, idx_membership_invitations_tenant, idx_memberships_tenant_status, membership_invitations

### Community 108 - "CapituloPresupuestal"
Cohesion: 0.29
Nodes (7): CapituloPresupuestal, ACABADOS, CIMENTACION, ESTRUCTURA, INSTALACIONES, MAMPOSTERIA, OTRO

### Community 109 - "V23__create_audit_log.sql"
Cohesion: 0.60
Nodes (5): audit_log, idx_audit_log_tenant_actor_occurred, idx_audit_log_tenant_entity_occurred, idx_audit_log_tenant_occurred, idx_audit_log_tenant_type_occurred

### Community 112 - "memberships"
Cohesion: 0.70
Nodes (4): idx_memberships_active, idx_memberships_tenant, idx_memberships_user, memberships

### Community 113 - "password_reset_tokens"
Cohesion: 0.83
Nodes (3): idx_password_reset_hash, idx_password_reset_user, password_reset_tokens

### Community 114 - "project_stages"
Cohesion: 0.83
Nodes (3): idx_project_stages_project, idx_project_stages_tenant, project_stages

## Knowledge Gaps
- **65 isolated node(s):** `@neon/env`, `com.SaaS:Constructora`, `IMAGE`, `DOCUMENT`, `RENDER_3D` (+60 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 305 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **65 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Role` connect `Role` to `Worker`, `User`, `TaskStatus`, `ProjectStage`, `UserSummary`, `AuthControllerTest.java`, `uuid`, `ProjectArea`, `notblank`, `ProjectAssignmentDelegationTest`, `ProjectServiceTest`, `UserJpaEntity`, `MembershipValidationFilter.java`, `AuditLog`, `MembershipJpaEntity`, `Membership`, `MembershipRepository`, `.create`, `Project`, `DocumentService`, `ProjectService`, `TaskServiceTest.java`, `InviteMembershipUseCase.java`, `org.springframework.transaction.annotation.Transactional`, `ProjectType`, `PersonnelServiceTest.java`, `DocumentService.java`, `.upload`, `AuthenticatedUser`, `.verify`, `.findActiveByUserIdAndTenantId`, `TenantContext`?**
  _High betweenness centrality (0.086) - this node is a cross-community bridge._
- **What connects `@neon/env`, `com.SaaS:Constructora`, `IMAGE` to the rest of the system?**
  _65 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Worker` be split into smaller, more focused modules?**
  _Cohesion score 0.09982174688057041 - nodes in this community are weakly interconnected._
- **Why does `ProjectAssignment` connect `ProjectAssignment` to `ProjectAssignmentRepositoryAdapter`, `ProjectAssignmentService`, `ProjectRole`, `uuid`, `.create`, `ProjectService`, `.verify`, `ProjectAssignmentDelegationTest`, `TaskServiceTest.java`, `.from`, `org.springframework.transaction.annotation.Transactional`, `AuditLog`?**
  _High betweenness centrality (0.045) - this node is a cross-community bridge._
- **Should `TaskStatus` be split into smaller, more focused modules?**
  _Cohesion score 0.07088989441930618 - nodes in this community are weakly interconnected._
- **Why does `AuthenticatedUser` connect `AuthenticatedUser` to `AuditController.java`, `TaskStatus`, `ProjectStage`, `ProjectAssignmentService`, `TaskController`, `ProjectRole`, `DocumentController.java`, `ProjectArea`, `org.springframework.http.ResponseEntity`, `Role`, `ProjectController.java`, `org.springframework.transaction.annotation.Transactional`, `ProjectType`, `MembershipValidationFilter.java`?**
  _High betweenness centrality (0.032) - this node is a cross-community bridge._
- **Should `ProjectStage` be split into smaller, more focused modules?**
  _Cohesion score 0.07985480943738657 - nodes in this community are weakly interconnected._