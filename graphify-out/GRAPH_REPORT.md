# Graph Report - SaaS-Constructora-backend  (2026-11-18)

## Corpus Check
- 170 files · ~36,774 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 8 file(s) not represented in the graph (top: (none) 4, .properties 2, .example 1)

## Summary
- 1420 nodes · 4377 edges · 58 communities (26 shown, 32 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 650 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `51b8d2af`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Worker
- User
- .update
- .create
- ProjectStage
- httpstatus
- AuthControllerTest.java
- PasswordResetToken
- org.springframework.context.annotation.Bean
- uuid
- Project
- V3__create_identity.sql
- org.junit.jupiter.api.Test
- ResendEmailSender
- .update
- ProjectAuthorizationIntegrationTest
- .verify
- JwtService.java
- Tenant
- ProjectAssignmentIntegrationTest
- ProjectStageIntegrationTest
- ProjectServiceTest
- ProjectTenantIntegrationTest
- .register
- GoogleTokenVerifierClient
- MembershipInvitation
- TenantJpaEntity
- MembershipValidationFilter.java
- ConstructoraApplication.java
- ProjectAssignmentDelegationIntegrationTest
- MembershipJpaEntity
- V2__create_event_publication.sql
- com.SaaS:Constructora
- Role
- Membership
- org.springframework.transaction.annotation.Transactional
- ProjectResponse
- .create
- package.json
- .invite
- Backend Guidelines (`saas-backend`)
- WorkerPosition
- README.md
- V13__create_workers.sql
- .login
- V14__create_worker_positions.sql
- .authenticate
- NeonJdbcConfig.java

## God Nodes (most connected - your core abstractions)
1. `Role` - 74 edges
2. `Membership` - 63 edges
3. `MembershipRepository` - 48 edges
4. `User` - 47 edges
5. `UserRepository` - 44 edges
6. `Project` - 43 edges
7. `ProjectService` - 35 edges
8. `AuthenticatedUser` - 35 edges
9. `AuthControllerTest` - 32 edges
10. `Tenant` - 30 edges

## Surprising Connections (you probably didn't know these)
- `1. Patrón Arquitectónico y Estructura` --references--> `ProjectRepository`  [INFERRED]
  AGENTS.md → src/main/java/com/SaaS/Constructora/projects/ProjectRepository.java
- `AuthControllerTest` --references--> `AcceptInvitationUseCase`  [EXTRACTED]
  src/test/java/com/SaaS/Constructora/identity/interfaces/AuthControllerTest.java → src/main/java/com/SaaS/Constructora/identity/application/AcceptInvitationUseCase.java
- `GoogleAuthUseCase` --references--> `ExternalIdentityRepository`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java → src/main/java/com/SaaS/Constructora/identity/application/ports/ExternalIdentityRepository.java
- `GoogleAuthUseCase` --references--> `GoogleTokenVerifier`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java → src/main/java/com/SaaS/Constructora/identity/application/ports/GoogleTokenVerifier.java
- `AuthControllerTest` --references--> `GoogleAuthUseCase`  [EXTRACTED]
  src/test/java/com/SaaS/Constructora/identity/interfaces/AuthControllerTest.java → src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java

## Import Cycles
- None detected.

## Communities (58 total, 32 thin omitted)

### Community 0 - "Worker"
Cohesion: 0.10
Nodes (10): InvitationIssued, DocumentType, FOREIGN_ID, IDENTITY_CARD, NATIONAL_ID, OTHER, PASSPORT, InviteWorkerRequest (+2 more)

### Community 2 - ".update"
Cohesion: 0.10
Nodes (18): BudgetRange, FROM_100M_TO_250M, FROM_250M_TO_500M, FROM_50M_TO_100M, OVER_500M, UNDER_50M, LotOwnership, IN_PURCHASE (+10 more)

### Community 3 - ".create"
Cohesion: 0.19
Nodes (3): CreateWorkerRequest, WorkerResponse, PersonnelServiceTest

### Community 4 - "ProjectStage"
Cohesion: 0.08
Nodes (8): CreateProjectStageRequest, ProjectStage, ProjectStageController, ProjectStageRepository, ProjectStageResponse, ProjectStageService, UpdateProjectStageRequest, ProjectStageServiceTest

### Community 5 - "httpstatus"
Cohesion: 0.08
Nodes (11): InvitationPreview, PermissionManagementUseCase, UserQueryService, UserSummary, InvitationPreviewResponse, UserResponse, UserResponse, UserController (+3 more)

### Community 7 - "PasswordResetToken"
Cohesion: 0.11
Nodes (4): PasswordResetToken, PasswordResetTokenJpaEntity, PasswordResetTokenJpaRepository, PasswordResetTokenRepositoryAdapter

### Community 8 - "org.springframework.context.annotation.Bean"
Cohesion: 0.13
Nodes (5): InvitationIntegrationTest, RecordingInvitationEmailSender, TestEmailConfig, FakeGoogleTokenVerifier, TestEmailConfig

### Community 9 - "uuid"
Cohesion: 0.05
Nodes (8): ExternalIdentityRepository, ExternalIdentity, ExternalIdentityJpaEntity, ExternalIdentityJpaRepository, ExternalIdentityRepositoryAdapter, TenantJpaRepository, TenantRepositoryAdapter, WorkerRepository

### Community 10 - "Project"
Cohesion: 0.11
Nodes (3): Project, ProjectStatus, ACTIVE

### Community 11 - "V3__create_identity.sql"
Cohesion: 0.08
Nodes (30): idx_project_assignments_project_role, idx_membership_invitations_email, idx_membership_invitations_expires, idx_membership_invitations_membership, idx_membership_invitations_tenant, idx_memberships_tenant_status, membership_invitations, idx_projects_tenant (+22 more)

### Community 14 - ".update"
Cohesion: 0.14
Nodes (6): CreateWorkerPositionRequest, UpdateWorkerPositionRequest, WorkerPositionRepository, WorkerPositionResponse, WorkerPositionService, WorkerPositionServiceTest

### Community 17 - ".verify"
Cohesion: 0.06
Nodes (13): AssignProjectRequest, ProjectAssignment, ProjectAssignmentController, ProjectAssignmentRepository, ProjectAssignmentResponse, ProjectAssignmentService, ProjectRole, LEAD (+5 more)

### Community 21 - "ProjectStageIntegrationTest"
Cohesion: 0.27
Nodes (3): ProjectStageIntegrationTest, Session, StageRef

### Community 26 - "GoogleTokenVerifierClient"
Cohesion: 0.16
Nodes (5): GoogleAccount, GoogleTokenVerifier, GoogleTokenVerifierClient, ParameterizedTypeReference<Map<String, Object>>, GoogleTokenVerifierClientTest

### Community 28 - "MembershipInvitation"
Cohesion: 0.08
Nodes (5): MembershipInvitation, MembershipInvitationJpaEntity, MembershipInvitationJpaRepository, MembershipInvitationRepositoryAdapter, MembershipInvitationTest

### Community 30 - "MembershipValidationFilter.java"
Cohesion: 0.05
Nodes (7): ApiError, GlobalExceptionHandler, CurrentUserArgumentResolver, MembershipValidationFilter, SecurityConfig, TenantClaims, TenantContext

### Community 34 - "MembershipJpaEntity"
Cohesion: 0.06
Nodes (9): MembershipStatus, ACTIVE, INVITED, MembershipJpaEntity, MembershipJpaRepository, MembershipRepositoryAdapter, UserJpaEntity, UserJpaRepository (+1 more)

### Community 35 - "V2__create_event_publication.sql"
Cohesion: 0.83
Nodes (3): event_publication, idx_event_publication_completion_date, idx_event_publication_publication_date

### Community 38 - "Role"
Cohesion: 0.18
Nodes (5): Role, ADMIN, MEMBER, WORKER, ProjectService

### Community 40 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.07
Nodes (28): AcceptInvitationUseCase, GoogleAuthUseCase, InvitationGate, InvitationQueryService, InviteMembershipUseCase, LoginUseCase, PersonnelPermissionService, EmailSender (+20 more)

### Community 43 - "package.json"
Cohesion: 0.29
Nodes (5): dependencies, @neon/config, @neon/env, @neon/config, @neon/env

### Community 45 - "Backend Guidelines (`saas-backend`)"
Cohesion: 0.50
Nodes (3): 1. Patrón Arquitectónico y Estructura, 2. Reglas Innegociables del Backend, Backend Guidelines (`saas-backend`)

### Community 48 - "README.md"
Cohesion: 0.22
Nodes (8): Base del enlace enviado por correo (por defecto APP_BASE_URL)., Configuración Rápida Local, Correo por SMTP (Gmail). Completa MAIL_PASSWORD con la contraseña de aplicación., Desde GHCR (tras un merge a main), Interruptor del flujo de invitaciones (true por defecto en desarrollo)., Local, Variables de Entorno (`.env`), Vigencia del enlace en horas (1 a 720).

### Community 49 - "V13__create_workers.sql"
Cohesion: 0.83
Nodes (3): idx_workers_project, idx_workers_tenant, workers

### Community 57 - ".authenticate"
Cohesion: 0.16
Nodes (3): GoogleAuthRequest, GoogleAuthResponse, TenantResponse

### Community 60 - "NeonJdbcConfig.java"
Cohesion: 0.08
Nodes (7): NeonConnectionDetails, NeonFlywayConnectionDetails, NeonJdbcConfig, PostgresUrl, JwtConfig, PasswordConfig, WebConfig

## Knowledge Gaps
- **27 isolated node(s):** `@neon/env`, `com.SaaS:Constructora`, `INVITED`, `ACTIVE`, `ADMIN` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 190 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **32 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Role` connect `Role` to `Worker`, `MembershipJpaEntity`, `.update`, `ProjectStage`, `httpstatus`, `AuthControllerTest.java`, `Membership`, `org.springframework.transaction.annotation.Transactional`, `uuid`, `.create`, `.invite`, `notblank`, `.verify`, `ProjectServiceTest`, `.list`, `MembershipValidationFilter.java`?**
  _High betweenness centrality (0.085) - this node is a cross-community bridge._
- **What connects `@neon/env`, `com.SaaS:Constructora`, `INVITED` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Worker` be split into smaller, more focused modules?**
  _Cohesion score 0.09523809523809523 - nodes in this community are weakly interconnected._
- **Why does `Membership` connect `Membership` to `User`, `MembershipJpaEntity`, `httpstatus`, `Role`, `AuthControllerTest.java`, `org.springframework.transaction.annotation.Transactional`, `uuid`, `.invite`, `JwtService.java`, `MembershipValidationFilter.java`, `.login`, `.authenticate`, `.register`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Should `.update` be split into smaller, more focused modules?**
  _Cohesion score 0.09852216748768473 - nodes in this community are weakly interconnected._
- **Why does `Project` connect `Project` to `.update`, `Role`, `ProjectServiceTest`, `.list`, `Worker.java`?**
  _High betweenness centrality (0.034) - this node is a cross-community bridge._
- **Should `ProjectStage` be split into smaller, more focused modules?**
  _Cohesion score 0.07547169811320754 - nodes in this community are weakly interconnected._