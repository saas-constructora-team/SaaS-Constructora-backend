# Graph Report - SaaS-Constructora-backend  (2026-11-09)

## Corpus Check
- 137 files · ~26,480 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 5 file(s) not represented in the graph (top: .properties 2, .example 1, (none) 1)

## Summary
- 1148 nodes · 3317 edges · 54 communities (26 shown, 28 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 451 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `529a0cf3`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Worker
- User
- Project
- .register
- ProjectStage
- httpstatus
- org.junit.jupiter.api.Test
- PasswordResetToken
- MembershipJpaEntity
- ExternalIdentity
- RequestPasswordResetUseCase.java
- V3__create_identity.sql
- org.springframework.transaction.annotation.Transactional
- AuthController.java
- org.springframework.context.annotation.Bean
- ProjectAssignment
- Tenant
- Membership
- ProjectServiceTest
- ProjectService
- CurrentUserArgumentResolver.java
- SecurityConfig.java
- GoogleTokenVerifierClient.java
- ProjectController
- .verify
- GlobalExceptionHandler.java
- GoogleAuthUseCase.java
- ConstructoraApplication.java
- MembershipValidationFilter.java
- V2__create_event_publication.sql
- com.SaaS:Constructora
- Role
- ExternalIdentity
- org.springframework.transaction.annotation.Transactional
- ProjectAuthorizationIntegrationTest
- ProjectAssignmentService
- ProjectStageService
- ProjectStageController
- .create
- JwtServiceTest.java
- README.md
- V13__create_workers.sql

## God Nodes (most connected - your core abstractions)
1. `Role` - 67 edges
2. `Membership` - 47 edges
3. `Project` - 43 edges
4. `User` - 40 edges
5. `MembershipRepository` - 32 edges
6. `ProjectService` - 32 edges
7. `AuthenticatedUser` - 28 edges
8. `ProjectAssignmentIntegrationTest` - 28 edges
9. `UserRepository` - 27 edges
10. `Tenant` - 27 edges

## Surprising Connections (you probably didn't know these)
- `1. Patrón Arquitectónico y Estructura` --references--> `ProjectRepository`  [INFERRED]
  AGENTS.md → src/main/java/com/SaaS/Constructora/projects/ProjectRepository.java
- `GoogleAuthUseCase` --references--> `ExternalIdentityRepository`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java → src/main/java/com/SaaS/Constructora/identity/application/ports/ExternalIdentityRepository.java
- `GoogleAuthUseCase` --references--> `GoogleTokenVerifier`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java → src/main/java/com/SaaS/Constructora/identity/application/ports/GoogleTokenVerifier.java
- `GoogleAuthUseCase` --references--> `MembershipRepository`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java → src/main/java/com/SaaS/Constructora/identity/application/ports/MembershipRepository.java
- `GoogleAuthUseCase` --references--> `TenantRepository`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java → src/main/java/com/SaaS/Constructora/identity/application/ports/TenantRepository.java

## Import Cycles
- None detected.

## Communities (54 total, 28 thin omitted)

### Community 0 - "Worker"
Cohesion: 0.06
Nodes (18): PersonnelPermissionService, CreateWorkerRequest, DocumentType, FOREIGN_ID, IDENTITY_CARD, NATIONAL_ID, OTHER, PASSPORT (+10 more)

### Community 1 - "User"
Cohesion: 0.18
Nodes (3): User, GoogleAuthRequest, GoogleAuthResponse

### Community 2 - "Project"
Cohesion: 0.05
Nodes (22): BudgetRange, FROM_100M_TO_250M, FROM_250M_TO_500M, FROM_50M_TO_100M, OVER_500M, UNDER_50M, CreateProjectRequest, LotOwnership (+14 more)

### Community 3 - ".register"
Cohesion: 0.17
Nodes (3): TenantResponse, RegisterTenantRequest, RegisterTenantResponse

### Community 5 - "httpstatus"
Cohesion: 0.09
Nodes (6): UserResponse, UserController, PersonnelController, AuthenticatedUser, CurrentUser, TenantClaims

### Community 6 - "org.junit.jupiter.api.Test"
Cohesion: 0.05
Nodes (12): AuthControllerTest, LoginIntegrationTest, TestEmailConfig, ProjectAssignmentDelegationIntegrationTest, Session, ProjectAssignmentIntegrationTest, Session, ProjectStageIntegrationTest (+4 more)

### Community 7 - "PasswordResetToken"
Cohesion: 0.11
Nodes (4): PasswordResetToken, PasswordResetTokenJpaEntity, PasswordResetTokenJpaRepository, PasswordResetTokenRepositoryAdapter

### Community 8 - "MembershipJpaEntity"
Cohesion: 0.07
Nodes (6): MembershipJpaEntity, MembershipJpaRepository, MembershipRepositoryAdapter, UserJpaEntity, UserJpaRepository, UserRepositoryAdapter

### Community 9 - "ExternalIdentity"
Cohesion: 0.20
Nodes (3): ExternalIdentityJpaEntity, ExternalIdentityJpaRepository, ExternalIdentityRepositoryAdapter

### Community 10 - "RequestPasswordResetUseCase.java"
Cohesion: 0.26
Nodes (3): PasswordResetTokenRepository, RequestPasswordResetUseCase, TokenHasher

### Community 11 - "V3__create_identity.sql"
Cohesion: 0.10
Nodes (24): idx_project_assignments_project_role, idx_projects_tenant, projects, idx_memberships_active, idx_memberships_tenant, idx_memberships_user, memberships, tenants (+16 more)

### Community 12 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.18
Nodes (5): PermissionManagementUseCase, MembershipRepository, UserQueryService, UserSummary, UserResponse

### Community 14 - "AuthController.java"
Cohesion: 0.22
Nodes (5): ForgotPasswordRequest, LoginRequest, LoginResponse, MessageResponse, ResetPasswordRequest

### Community 16 - "org.springframework.context.annotation.Bean"
Cohesion: 0.13
Nodes (4): JwtConfig, PasswordConfig, WebConfig, RecordingEmailSender

### Community 17 - "ProjectAssignment"
Cohesion: 0.12
Nodes (7): AssignProjectRequest, ProjectAssignment, ProjectAssignmentResponse, ProjectRole, LEAD, MEMBER, UpdateProjectAssignmentRequest

### Community 19 - "Tenant"
Cohesion: 0.10
Nodes (5): Tenant, TenantJpaEntity, TenantJpaRepository, TenantRepositoryAdapter, TenantResponse

### Community 23 - "ProjectService"
Cohesion: 0.25
Nodes (7): 1. Patrón Arquitectónico y Estructura, 2. Reglas Innegociables del Backend, Backend Guidelines (`saas-backend`), TenantQueryService, ProjectRepository, ProjectService, ProjectStageRepository

### Community 26 - "GoogleTokenVerifierClient.java"
Cohesion: 0.05
Nodes (9): GoogleAccount, GoogleTokenVerifier, EmailTemplates, GoogleTokenVerifierClient, ParameterizedTypeReference<Map<String, Object>>, ResendEmailSender, SmtpEmailSender, GoogleTokenVerifierClientTest (+1 more)

### Community 31 - "GoogleAuthUseCase.java"
Cohesion: 0.22
Nodes (3): CompanyIdentity, GoogleAuthUseCase, EmailSender

### Community 35 - "V2__create_event_publication.sql"
Cohesion: 0.83
Nodes (3): event_publication, idx_event_publication_completion_date, idx_event_publication_publication_date

### Community 38 - "Role"
Cohesion: 0.18
Nodes (3): Role, ADMIN, MEMBER

### Community 40 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.26
Nodes (7): LoginUseCase, TenantRepository, UserRepository, RegisterTenantUseCase, ResetPasswordUseCase, AuthController, JwtService

### Community 42 - "ProjectAssignmentService"
Cohesion: 0.20
Nodes (3): ProjectAssignmentController, ProjectAssignmentRepository, ProjectAssignmentService

### Community 43 - "ProjectStageService"
Cohesion: 0.29
Nodes (3): ProjectStageService, UpdateProjectStageRequest, ProjectStageServiceTest

### Community 48 - "README.md"
Cohesion: 0.50
Nodes (3): Configuración Rápida Local, Correo por SMTP (Gmail). Completa MAIL_PASSWORD con la contraseña de aplicación., Variables de Entorno (`.env`)

### Community 49 - "V13__create_workers.sql"
Cohesion: 0.83
Nodes (3): idx_workers_project, idx_workers_tenant, workers

## Knowledge Gaps
- **27 isolated node(s):** `com.SaaS:Constructora`, `ADMIN`, `MEMBER`, `NATIONAL_ID`, `FOREIGN_ID` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 175 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **28 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Role` connect `Role` to `User`, `Project`, `httpstatus`, `org.junit.jupiter.api.Test`, `MembershipJpaEntity`, `org.springframework.transaction.annotation.Transactional`, `uuid`, `ProjectAssignment`, `Membership`, `.list`, `ProjectServiceTest`, `.verify`, `GoogleAuthUseCase.java`, `localdatetime`, `org.springframework.transaction.annotation.Transactional`, `ProjectAssignmentService`, `ProjectStageService`, `ProjectStageController`, `.requireCanManage`, `.create`?**
  _High betweenness centrality (0.111) - this node is a cross-community bridge._
- **What connects `com.SaaS:Constructora`, `ADMIN`, `MEMBER` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Worker` be split into smaller, more focused modules?**
  _Cohesion score 0.05501165501165501 - nodes in this community are weakly interconnected._
- **Why does `Project` connect `Project` to `localdatetime`, `.list`, `ProjectServiceTest`, `ProjectService`?**
  _High betweenness centrality (0.056) - this node is a cross-community bridge._
- **Should `Project` be split into smaller, more focused modules?**
  _Cohesion score 0.05010351966873706 - nodes in this community are weakly interconnected._
- **Why does `Tenant` connect `Tenant` to `localdatetime`, `User`, `.register`, `org.springframework.transaction.annotation.Transactional`, `uuid`, `GoogleAuthUseCase.java`?**
  _High betweenness centrality (0.041) - this node is a cross-community bridge._
- **Should `httpstatus` be split into smaller, more focused modules?**
  _Cohesion score 0.08997668997668998 - nodes in this community are weakly interconnected._