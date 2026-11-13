# Graph Report - SaaS-Constructora-backend  (2026-11-11)

## Corpus Check
- 149 files · ~29,864 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 5 file(s) not represented in the graph (top: .properties 2, .example 1, (none) 1)

## Summary
- 1250 nodes · 3675 edges · 60 communities (26 shown, 34 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 520 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `211f9af7`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Worker
- Membership
- .create
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
- .update
- org.springframework.context.annotation.Bean
- ProjectRole
- JwtService.java
- Tenant
- ProjectAssignmentIntegrationTest
- ProjectStageIntegrationTest
- ProjectServiceTest
- Backend Guidelines (`saas-backend`)
- ProjectTenantIntegrationTest
- PersonnelService
- GoogleTokenVerifierClient.java
- ProjectController
- ProjectAssignmentDelegationTest
- MembershipValidationFilter.java
- ConstructoraApplication.java
- MembershipJpaEntity
- V2__create_event_publication.sql
- com.SaaS:Constructora
- Role
- MembershipJpaRepository
- org.springframework.transaction.annotation.Transactional
- ProjectAuthorizationIntegrationTest
- ProjectAssignment
- ProjectStageController
- ProjectAssignmentService
- WorkerPosition
- ProjectAssignmentDelegationIntegrationTest
- README.md
- V13__create_workers.sql
- .ensureBelongsToTenant
- package.json
- TenantRepositoryAdapter
- V14__create_worker_positions.sql

## God Nodes (most connected - your core abstractions)
1. `Role` - 69 edges
2. `Membership` - 49 edges
3. `User` - 43 edges
4. `Project` - 43 edges
5. `MembershipRepository` - 37 edges
6. `UserRepository` - 32 edges
7. `ProjectService` - 32 edges
8. `AuthenticatedUser` - 32 edges
9. `ProjectAssignmentIntegrationTest` - 28 edges
10. `Tenant` - 27 edges

## Surprising Connections (you probably didn't know these)
- `1. Patrón Arquitectónico y Estructura` --references--> `ProjectRepository`  [INFERRED]
  AGENTS.md → src/main/java/com/SaaS/Constructora/projects/ProjectRepository.java
- `GoogleAuthUseCase` --references--> `ExternalIdentityRepository`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java → src/main/java/com/SaaS/Constructora/identity/application/ports/ExternalIdentityRepository.java
- `AuthController` --references--> `GoogleAuthUseCase`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/interfaces/AuthController.java → src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java
- `AuthControllerTest` --references--> `GoogleAuthUseCase`  [EXTRACTED]
  src/test/java/com/SaaS/Constructora/identity/interfaces/AuthControllerTest.java → src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java
- `AuthController` --references--> `LoginUseCase`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/interfaces/AuthController.java → src/main/java/com/SaaS/Constructora/identity/application/LoginUseCase.java

## Import Cycles
- None detected.

## Communities (60 total, 34 thin omitted)

### Community 0 - "Worker"
Cohesion: 0.13
Nodes (7): DocumentType, FOREIGN_ID, IDENTITY_CARD, NATIONAL_ID, OTHER, PASSPORT, Worker

### Community 1 - "Membership"
Cohesion: 0.06
Nodes (11): PermissionManagementUseCase, UserSummary, Membership, User, GoogleAuthResponse, TenantResponse, LoginRequest, LoginResponse (+3 more)

### Community 2 - ".create"
Cohesion: 0.08
Nodes (19): BudgetRange, FROM_100M_TO_250M, FROM_250M_TO_500M, FROM_50M_TO_100M, OVER_500M, UNDER_50M, CreateProjectRequest, LotOwnership (+11 more)

### Community 5 - "httpstatus"
Cohesion: 0.08
Nodes (14): AuthController, ForgotPasswordRequest, GoogleAuthRequest, UserResponse, MessageResponse, RegisterTenantRequest, ResetPasswordRequest, UserController (+6 more)

### Community 7 - "PasswordResetToken"
Cohesion: 0.11
Nodes (4): PasswordResetToken, PasswordResetTokenJpaEntity, PasswordResetTokenJpaRepository, PasswordResetTokenRepositoryAdapter

### Community 8 - "UserJpaEntity"
Cohesion: 0.15
Nodes (3): UserJpaEntity, UserJpaRepository, UserRepositoryAdapter

### Community 9 - "ExternalIdentity"
Cohesion: 0.12
Nodes (5): ExternalIdentityRepository, ExternalIdentity, ExternalIdentityJpaEntity, ExternalIdentityJpaRepository, ExternalIdentityRepositoryAdapter

### Community 10 - "Project"
Cohesion: 0.11
Nodes (3): Project, ProjectStatus, ACTIVE

### Community 11 - "V3__create_identity.sql"
Cohesion: 0.10
Nodes (24): idx_project_assignments_project_role, idx_projects_tenant, projects, idx_memberships_active, idx_memberships_tenant, idx_memberships_user, memberships, tenants (+16 more)

### Community 14 - ".update"
Cohesion: 0.16
Nodes (4): CreateWorkerPositionRequest, UpdateWorkerPositionRequest, WorkerPositionService, WorkerPositionServiceTest

### Community 16 - "org.springframework.context.annotation.Bean"
Cohesion: 0.07
Nodes (10): NeonConnectionDetails, NeonFlywayConnectionDetails, NeonJdbcConfig, PostgresUrl, JwtConfig, PasswordConfig, WebConfig, FakeGoogleTokenVerifier (+2 more)

### Community 17 - "ProjectRole"
Cohesion: 0.15
Nodes (6): AssignProjectRequest, ProjectAssignmentResponse, ProjectRole, LEAD, MEMBER, UpdateProjectAssignmentRequest

### Community 19 - "Tenant"
Cohesion: 0.12
Nodes (3): Tenant, TenantJpaEntity, TenantResponse

### Community 21 - "ProjectStageIntegrationTest"
Cohesion: 0.27
Nodes (3): ProjectStageIntegrationTest, Session, StageRef

### Community 22 - "ProjectServiceTest"
Cohesion: 0.15
Nodes (4): ProjectAssignmentRepository, ProjectRepository, ProjectService, ProjectServiceTest

### Community 23 - "Backend Guidelines (`saas-backend`)"
Cohesion: 0.50
Nodes (3): 1. Patrón Arquitectónico y Estructura, 2. Reglas Innegociables del Backend, Backend Guidelines (`saas-backend`)

### Community 25 - "PersonnelService"
Cohesion: 0.20
Nodes (4): PersonnelPermissionService, PersonnelService, WorkerPositionRepository, WorkerRepository

### Community 26 - "GoogleTokenVerifierClient.java"
Cohesion: 0.06
Nodes (6): EmailTemplates, GoogleTokenVerifierClient, ParameterizedTypeReference<Map<String, Object>>, ResendEmailSender, SmtpEmailSender, GoogleTokenVerifierClientTest

### Community 30 - "MembershipValidationFilter.java"
Cohesion: 0.05
Nodes (6): ApiError, GlobalExceptionHandler, CurrentUserArgumentResolver, MembershipValidationFilter, SecurityConfig, TenantContext

### Community 35 - "V2__create_event_publication.sql"
Cohesion: 0.83
Nodes (3): event_publication, idx_event_publication_completion_date, idx_event_publication_publication_date

### Community 38 - "Role"
Cohesion: 0.20
Nodes (4): Role, ADMIN, MEMBER, WORKER

### Community 40 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.07
Nodes (17): CompanyIdentity, GoogleAuthUseCase, LoginUseCase, EmailSender, GoogleAccount, GoogleTokenVerifier, MembershipRepository, PasswordResetTokenRepository (+9 more)

### Community 44 - "ProjectStageController"
Cohesion: 0.14
Nodes (6): CreateProjectStageRequest, ProjectStageController, ProjectStageRepository, ProjectStageResponse, ProjectStageService, ProjectStageServiceTest

### Community 48 - "README.md"
Cohesion: 0.50
Nodes (3): Configuración Rápida Local, Correo por SMTP (Gmail). Completa MAIL_PASSWORD con la contraseña de aplicación., Variables de Entorno (`.env`)

### Community 49 - "V13__create_workers.sql"
Cohesion: 0.83
Nodes (3): idx_workers_project, idx_workers_tenant, workers

### Community 54 - "package.json"
Cohesion: 0.29
Nodes (5): dependencies, @neon/config, @neon/env, @neon/config, @neon/env

## Knowledge Gaps
- **25 isolated node(s):** `@neon/env`, `com.SaaS:Constructora`, `ADMIN`, `MEMBER`, `WORKER` (+20 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 177 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **34 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Role` connect `Role` to `Membership`, `MembershipJpaEntity`, `localdatetime`, `.create`, `ProjectStage`, `httpstatus`, `MembershipJpaRepository`, `org.springframework.transaction.annotation.Transactional`, `AuthControllerTest.java`, `.list`, `ProjectAssignment`, `ProjectStageController`, `uuid`, `ProjectAssignmentService`, `.ensureBelongsToTenant`, `ProjectServiceTest`, `ProjectAssignmentDelegationTest`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **What connects `@neon/env`, `com.SaaS:Constructora`, `ADMIN` to the rest of the system?**
  _25 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Worker` be split into smaller, more focused modules?**
  _Cohesion score 0.12666666666666668 - nodes in this community are weakly interconnected._
- **Why does `User` connect `Membership` to `.create`, `AuthControllerTest.java`, `org.springframework.transaction.annotation.Transactional`, `UserJpaEntity`, `.list`, `uuid`, `JwtService.java`?**
  _High betweenness centrality (0.048) - this node is a cross-community bridge._
- **Should `Membership` be split into smaller, more focused modules?**
  _Cohesion score 0.05871725383920506 - nodes in this community are weakly interconnected._
- **Why does `Membership` connect `Membership` to `MembershipJpaEntity`, `Role`, `MembershipJpaRepository`, `org.springframework.transaction.annotation.Transactional`, `AuthControllerTest.java`, `uuid`, `JwtService.java`, `MembershipValidationFilter.java`?**
  _High betweenness centrality (0.034) - this node is a cross-community bridge._
- **Should `.create` be split into smaller, more focused modules?**
  _Cohesion score 0.0761904761904762 - nodes in this community are weakly interconnected._