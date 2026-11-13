# Graph Report - SaaS-Constructora-backend  (2026-11-12)

## Corpus Check
- 167 files · ~34,355 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 6 file(s) not represented in the graph (top: (none) 2, .properties 2, .example 1)

## Summary
- 1384 nodes · 4290 edges · 70 communities (25 shown, 45 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 646 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `919c3c78`
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
- UserJpaEntity
- ExternalIdentity
- Project
- V3__create_identity.sql
- org.junit.jupiter.api.Test
- uuid
- .update
- notblank
- InvitationIntegrationTest
- ProjectRole
- JwtConfig.java
- Tenant
- ProjectAssignmentIntegrationTest
- ProjectStageIntegrationTest
- ProjectServiceTest
- .list
- ProjectTenantIntegrationTest
- GlobalExceptionHandler.java
- GoogleTokenVerifierClient.java
- MembershipInvitation
- ProjectAssignmentDelegationTest
- MembershipValidationFilter.java
- ConstructoraApplication.java
- localdatetime
- MembershipJpaEntity
- V2__create_event_publication.sql
- com.SaaS:Constructora
- Role
- Membership
- org.springframework.transaction.annotation.Transactional
- ProjectAuthorizationIntegrationTest
- .assign
- ProjectAssignment
- WorkerPosition
- ProjectAssignmentDelegationIntegrationTest
- README.md
- V13__create_workers.sql
- SecurityConfig.java
- CurrentUserArgumentResolver.java
- .accept
- V14__create_worker_positions.sql
- .register
- WebConfig.java
- UserRepositoryAdapter
- .create
- ProjectResponse
- PasswordResetTokenRepositoryAdapter
- BudgetRange
- ProjectType
- .create

## God Nodes (most connected - your core abstractions)
1. `Role` - 74 edges
2. `Membership` - 63 edges
3. `MembershipRepository` - 48 edges
4. `User` - 47 edges
5. `Project` - 43 edges
6. `UserRepository` - 42 edges
7. `ProjectService` - 35 edges
8. `AuthenticatedUser` - 34 edges
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

## Communities (70 total, 45 thin omitted)

### Community 0 - "Worker"
Cohesion: 0.11
Nodes (8): DocumentType, FOREIGN_ID, IDENTITY_CARD, NATIONAL_ID, OTHER, PASSPORT, InviteWorkerRequest, Worker

### Community 2 - "LotOwnership"
Cohesion: 0.19
Nodes (6): LotOwnership, IN_PURCHASE, OWNED, RENTED, ProjectDetails, UpdateProjectRequest

### Community 4 - "ProjectStage"
Cohesion: 0.08
Nodes (8): CreateProjectStageRequest, ProjectStage, ProjectStageController, ProjectStageRepository, ProjectStageResponse, ProjectStageService, UpdateProjectStageRequest, ProjectStageServiceTest

### Community 5 - "httpstatus"
Cohesion: 0.05
Nodes (15): PermissionManagementUseCase, UserQueryService, UserSummary, UserResponse, MessageResponse, ResetPasswordRequest, UserResponse, UserController (+7 more)

### Community 9 - "ExternalIdentity"
Cohesion: 0.12
Nodes (5): ExternalIdentityRepository, ExternalIdentity, ExternalIdentityJpaEntity, ExternalIdentityJpaRepository, ExternalIdentityRepositoryAdapter

### Community 11 - "V3__create_identity.sql"
Cohesion: 0.08
Nodes (30): idx_project_assignments_project_role, idx_membership_invitations_email, idx_membership_invitations_expires, idx_membership_invitations_membership, idx_membership_invitations_tenant, idx_memberships_tenant_status, membership_invitations, idx_projects_tenant (+22 more)

### Community 14 - ".update"
Cohesion: 0.17
Nodes (6): UpdateWorkerPositionRequest, WorkerPositionRepository, WorkerPositionResponse, WorkerPositionService, WorkerRepository, WorkerPositionServiceTest

### Community 16 - "InvitationIntegrationTest"
Cohesion: 0.11
Nodes (6): InvitationIntegrationTest, RecordingInvitationEmailSender, TestEmailConfig, FakeGoogleTokenVerifier, RecordingEmailSender, TestEmailConfig

### Community 17 - "ProjectRole"
Cohesion: 0.13
Nodes (8): AssignProjectRequest, ProjectAssignmentController, ProjectAssignmentResponse, ProjectAssignmentService, ProjectRole, LEAD, MEMBER, UpdateProjectAssignmentRequest

### Community 19 - "Tenant"
Cohesion: 0.11
Nodes (5): Tenant, TenantJpaEntity, TenantJpaRepository, TenantRepositoryAdapter, TenantResponse

### Community 21 - "ProjectStageIntegrationTest"
Cohesion: 0.27
Nodes (3): ProjectStageIntegrationTest, Session, StageRef

### Community 23 - ".list"
Cohesion: 0.22
Nodes (4): 1. Patrón Arquitectónico y Estructura, 2. Reglas Innegociables del Backend, Backend Guidelines (`saas-backend`), ProjectRepository

### Community 26 - "GoogleTokenVerifierClient.java"
Cohesion: 0.06
Nodes (8): GoogleAccount, GoogleTokenVerifier, EmailTemplates, GoogleTokenVerifierClient, ParameterizedTypeReference<Map<String, Object>>, ResendEmailSender, SmtpEmailSender, GoogleTokenVerifierClientTest

### Community 28 - "MembershipInvitation"
Cohesion: 0.07
Nodes (5): InvitationPreview, MembershipInvitation, MembershipInvitationJpaEntity, InvitationPreviewResponse, MembershipInvitationTest

### Community 34 - "MembershipJpaEntity"
Cohesion: 0.10
Nodes (6): MembershipStatus, ACTIVE, INVITED, MembershipJpaEntity, MembershipJpaRepository, MembershipRepositoryAdapter

### Community 35 - "V2__create_event_publication.sql"
Cohesion: 0.83
Nodes (3): event_publication, idx_event_publication_completion_date, idx_event_publication_publication_date

### Community 38 - "Role"
Cohesion: 0.21
Nodes (4): Role, ADMIN, MEMBER, WORKER

### Community 40 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.07
Nodes (25): AcceptInvitationUseCase, CompanyIdentity, GoogleAuthUseCase, InvitationGate, InvitationQueryService, InviteMembershipUseCase, LoginUseCase, PersonnelPermissionService (+17 more)

### Community 48 - "README.md"
Cohesion: 0.29
Nodes (6): Base del enlace enviado por correo (por defecto APP_BASE_URL)., Configuración Rápida Local, Correo por SMTP (Gmail). Completa MAIL_PASSWORD con la contraseña de aplicación., Interruptor del flujo de invitaciones (true por defecto en desarrollo)., Variables de Entorno (`.env`), Vigencia del enlace en horas (1 a 720).

### Community 49 - "V13__create_workers.sql"
Cohesion: 0.83
Nodes (3): idx_workers_project, idx_workers_tenant, workers

### Community 55 - ".accept"
Cohesion: 0.26
Nodes (3): AcceptInvitationRequest, LoginRequest, LoginResponse

### Community 57 - ".register"
Cohesion: 0.21
Nodes (3): TenantResponse, RegisterTenantRequest, RegisterTenantResponse

### Community 63 - "ProjectResponse"
Cohesion: 0.22
Nodes (3): ProjectResponse, ProjectStatus, ACTIVE

### Community 65 - "BudgetRange"
Cohesion: 0.25
Nodes (6): BudgetRange, FROM_100M_TO_250M, FROM_250M_TO_500M, FROM_50M_TO_100M, OVER_500M, UNDER_50M

### Community 66 - "ProjectType"
Cohesion: 0.25
Nodes (5): ProjectType, COMMERCIAL, INDUSTRIAL, MIXED, RESIDENTIAL

## Knowledge Gaps
- **26 isolated node(s):** `com.SaaS:Constructora`, `INVITED`, `ACTIVE`, `ADMIN`, `MEMBER` (+21 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 182 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **45 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Role` connect `Role` to `Worker`, `User`, `ProjectStage`, `httpstatus`, `AuthControllerTest.java`, `uuid`, `notblank`, `ProjectRole`, `ProjectServiceTest`, `.list`, `ProjectAssignmentDelegationTest`, `localdatetime`, `MembershipJpaEntity`, `Membership`, `org.springframework.transaction.annotation.Transactional`, `.assign`, `.invite`, `PersonnelServiceTest.java`, `.create`?**
  _High betweenness centrality (0.104) - this node is a cross-community bridge._
- **What connects `com.SaaS:Constructora`, `INVITED`, `ACTIVE` to the rest of the system?**
  _26 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Worker` be split into smaller, more focused modules?**
  _Cohesion score 0.10752688172043011 - nodes in this community are weakly interconnected._
- **Why does `Membership` connect `Membership` to `User`, `MembershipJpaEntity`, `httpstatus`, `Role`, `AuthControllerTest.java`, `org.springframework.transaction.annotation.Transactional`, `.invite`, `uuid`, `PersonnelServiceTest.java`, `.accept`, `.register`, `MembershipValidationFilter.java`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._
- **Should `ProjectStage` be split into smaller, more focused modules?**
  _Cohesion score 0.08 - nodes in this community are weakly interconnected._
- **Why does `EmailSender` connect `org.springframework.transaction.annotation.Transactional` to `User`, `AuthControllerTest.java`, `.invite`, `InvitationIntegrationTest`, `GoogleTokenVerifierClient.java`?**
  _High betweenness centrality (0.036) - this node is a cross-community bridge._
- **Should `httpstatus` be split into smaller, more focused modules?**
  _Cohesion score 0.05499316005471956 - nodes in this community are weakly interconnected._