# Graph Report - SaaS-Constructora-backend  (2026-11-14)

## Corpus Check
- 167 files · ~35,826 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 8 file(s) not represented in the graph (top: (none) 4, .properties 2, .example 1)

## Summary
- 1386 nodes · 4305 edges · 67 communities (27 shown, 40 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 650 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e1fb4847`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Worker
- User
- LotOwnership
- .create
- ProjectStage
- httpstatus
- AuthControllerTest.java
- PasswordResetToken
- .assign
- ExternalIdentity
- Project
- V3__create_identity.sql
- org.junit.jupiter.api.Test
- .update
- RecordingEmailSender
- ProjectAssignment
- Tenant
- ProjectAssignmentIntegrationTest
- ProjectStageIntegrationTest
- ProjectServiceTest
- .list
- GlobalExceptionHandler.java
- GoogleTokenVerifierClient.java
- MembershipInvitation
- .ensureBelongsToTenant
- MembershipValidationFilter.java
- ConstructoraApplication.java
- MembershipJpaRepository
- MembershipJpaEntity
- V2__create_event_publication.sql
- com.SaaS:Constructora
- Role
- Membership
- org.springframework.transaction.annotation.Transactional
- ProjectResponse
- UserJpaEntity
- UserRepositoryAdapter
- JwtServiceTest.java
- WorkerPosition
- org.springframework.security.oauth2.jwt.Jwt
- README.md
- V13__create_workers.sql
- TenantRepositoryAdapter
- SecurityConfig.java
- CurrentUserArgumentResolver.java
- .login
- V14__create_worker_positions.sql
- .authenticate
- WebConfig.java
- BudgetRange
- ProjectType
- TenantContext
- ProjectStatus

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

## Communities (67 total, 40 thin omitted)

### Community 0 - "Worker"
Cohesion: 0.11
Nodes (8): DocumentType, FOREIGN_ID, IDENTITY_CARD, NATIONAL_ID, OTHER, PASSPORT, InviteWorkerRequest, Worker

### Community 2 - "LotOwnership"
Cohesion: 0.16
Nodes (7): CreateProjectRequest, LotOwnership, IN_PURCHASE, OWNED, RENTED, ProjectDetails, UpdateProjectRequest

### Community 4 - "ProjectStage"
Cohesion: 0.08
Nodes (8): CreateProjectStageRequest, ProjectStage, ProjectStageController, ProjectStageRepository, ProjectStageResponse, ProjectStageService, UpdateProjectStageRequest, ProjectStageServiceTest

### Community 5 - "httpstatus"
Cohesion: 0.08
Nodes (14): PermissionManagementUseCase, UserQueryService, UserSummary, ForgotPasswordRequest, UserResponse, MessageResponse, ResetPasswordRequest, UserResponse (+6 more)

### Community 6 - "AuthControllerTest.java"
Cohesion: 0.07
Nodes (10): InvitationIntegrationTest, RecordingInvitationEmailSender, TestEmailConfig, TestEmailConfig, ProjectAssignmentDelegationIntegrationTest, Session, ProjectAuthorizationIntegrationTest, Session (+2 more)

### Community 7 - "PasswordResetToken"
Cohesion: 0.11
Nodes (4): PasswordResetToken, PasswordResetTokenJpaEntity, PasswordResetTokenJpaRepository, PasswordResetTokenRepositoryAdapter

### Community 8 - ".assign"
Cohesion: 0.13
Nodes (4): ProjectAssignmentController, ProjectAssignmentRepository, ProjectAssignmentService, ProjectAssignmentServiceTest

### Community 9 - "ExternalIdentity"
Cohesion: 0.12
Nodes (5): ExternalIdentityRepository, ExternalIdentity, ExternalIdentityJpaEntity, ExternalIdentityJpaRepository, ExternalIdentityRepositoryAdapter

### Community 11 - "V3__create_identity.sql"
Cohesion: 0.08
Nodes (30): idx_project_assignments_project_role, idx_membership_invitations_email, idx_membership_invitations_expires, idx_membership_invitations_membership, idx_membership_invitations_tenant, idx_memberships_tenant_status, membership_invitations, idx_projects_tenant (+22 more)

### Community 12 - "org.junit.jupiter.api.Test"
Cohesion: 0.11
Nodes (3): AuthControllerTest, FakeGoogleTokenVerifier, LoginIntegrationTest

### Community 14 - ".update"
Cohesion: 0.12
Nodes (7): CreateWorkerPositionRequest, UpdateWorkerPositionRequest, WorkerPositionRepository, WorkerPositionResponse, WorkerPositionService, WorkerRepository, WorkerPositionServiceTest

### Community 17 - "ProjectAssignment"
Cohesion: 0.12
Nodes (7): AssignProjectRequest, ProjectAssignment, ProjectAssignmentResponse, ProjectRole, LEAD, MEMBER, UpdateProjectAssignmentRequest

### Community 19 - "Tenant"
Cohesion: 0.12
Nodes (3): TenantJpaEntity, TenantResponse, TenantResponse

### Community 21 - "ProjectStageIntegrationTest"
Cohesion: 0.27
Nodes (3): ProjectStageIntegrationTest, Session, StageRef

### Community 23 - ".list"
Cohesion: 0.24
Nodes (4): 1. Patrón Arquitectónico y Estructura, 2. Reglas Innegociables del Backend, Backend Guidelines (`saas-backend`), ProjectRepository

### Community 26 - "GoogleTokenVerifierClient.java"
Cohesion: 0.06
Nodes (8): GoogleAccount, GoogleTokenVerifier, EmailTemplates, GoogleTokenVerifierClient, ParameterizedTypeReference<Map<String, Object>>, ResendEmailSender, SmtpEmailSender, GoogleTokenVerifierClientTest

### Community 28 - "MembershipInvitation"
Cohesion: 0.06
Nodes (7): InvitationPreview, MembershipInvitation, MembershipInvitationJpaEntity, MembershipInvitationJpaRepository, MembershipInvitationRepositoryAdapter, InvitationPreviewResponse, MembershipInvitationTest

### Community 34 - "MembershipJpaEntity"
Cohesion: 0.15
Nodes (4): MembershipStatus, ACTIVE, INVITED, MembershipJpaEntity

### Community 35 - "V2__create_event_publication.sql"
Cohesion: 0.83
Nodes (3): event_publication, idx_event_publication_completion_date, idx_event_publication_publication_date

### Community 38 - "Role"
Cohesion: 0.20
Nodes (4): Role, ADMIN, MEMBER, WORKER

### Community 40 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.06
Nodes (28): AcceptInvitationUseCase, CompanyIdentity, GoogleAuthUseCase, InvitationGate, InvitationIssued, InvitationQueryService, InviteMembershipUseCase, LoginUseCase (+20 more)

### Community 48 - "README.md"
Cohesion: 0.22
Nodes (8): Base del enlace enviado por correo (por defecto APP_BASE_URL)., Configuración Rápida Local, Correo por SMTP (Gmail). Completa MAIL_PASSWORD con la contraseña de aplicación., Desde GHCR (tras un merge a main), Interruptor del flujo de invitaciones (true por defecto en desarrollo)., Local, Variables de Entorno (`.env`), Vigencia del enlace en horas (1 a 720).

### Community 49 - "V13__create_workers.sql"
Cohesion: 0.83
Nodes (3): idx_workers_project, idx_workers_tenant, workers

### Community 55 - ".login"
Cohesion: 0.22
Nodes (3): AcceptInvitationRequest, LoginRequest, LoginResponse

### Community 60 - "WebConfig.java"
Cohesion: 0.16
Nodes (3): JwtConfig, PasswordConfig, WebConfig

### Community 61 - "BudgetRange"
Cohesion: 0.25
Nodes (6): BudgetRange, FROM_100M_TO_250M, FROM_250M_TO_500M, FROM_50M_TO_100M, OVER_500M, UNDER_50M

### Community 62 - "ProjectType"
Cohesion: 0.29
Nodes (5): ProjectType, COMMERCIAL, INDUSTRIAL, MIXED, RESIDENTIAL

## Knowledge Gaps
- **26 isolated node(s):** `com.SaaS:Constructora`, `INVITED`, `ACTIVE`, `ADMIN`, `MEMBER` (+21 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 183 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **40 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Role` connect `Role` to `Worker`, `User`, `ProjectStage`, `httpstatus`, `AuthControllerTest.java`, `.assign`, `PersonnelServiceTest.java`, `notblank`, `ProjectAssignment`, `ProjectServiceTest`, `.list`, `list`, `.ensureBelongsToTenant`, `MembershipJpaRepository`, `MembershipJpaEntity`, `Membership`, `org.springframework.transaction.annotation.Transactional`, `.invite`, `org.springframework.security.oauth2.jwt.Jwt`?**
  _High betweenness centrality (0.102) - this node is a cross-community bridge._
- **What connects `com.SaaS:Constructora`, `INVITED`, `ACTIVE` to the rest of the system?**
  _26 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Worker` be split into smaller, more focused modules?**
  _Cohesion score 0.10752688172043011 - nodes in this community are weakly interconnected._
- **Why does `Membership` connect `Membership` to `User`, `MembershipJpaEntity`, `MembershipJpaRepository`, `httpstatus`, `Role`, `AuthControllerTest.java`, `org.springframework.transaction.annotation.Transactional`, `.invite`, `PersonnelServiceTest.java`, `JwtService.java`, `.login`, `list`, `.authenticate`, `MembershipValidationFilter.java`?**
  _High betweenness centrality (0.060) - this node is a cross-community bridge._
- **Should `ProjectStage` be split into smaller, more focused modules?**
  _Cohesion score 0.07767722473604827 - nodes in this community are weakly interconnected._
- **Why does `Project` connect `Project` to `ProjectStatus`, `.onCreate`, `LotOwnership`, `ProjectServiceTest`, `.list`, `BudgetRange`, `ProjectType`, `Worker.java`?**
  _High betweenness centrality (0.036) - this node is a cross-community bridge._
- **Should `httpstatus` be split into smaller, more focused modules?**
  _Cohesion score 0.08035714285714286 - nodes in this community are weakly interconnected._