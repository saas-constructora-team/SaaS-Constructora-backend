# Graph Report - SaaS-Constructora-backend  (2026-11-02)

## Corpus Check
- 119 files · ~23,377 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 5 file(s) not represented in the graph (top: .properties 2, .example 1, (none) 1)

## Summary
- 1000 nodes · 2912 edges · 38 communities (23 shown, 15 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 384 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a4f5627e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- .update
- Membership
- Project
- .authenticate
- ProjectStage
- AuthenticatedUser
- org.junit.jupiter.api.Test
- PasswordResetToken
- uuid
- ExternalIdentity
- RequestPasswordResetUseCase.java
- V3__create_identity.sql
- UserJpaEntity
- ProjectStageIntegrationTest
- AuthController.java
- org.springframework.context.annotation.Bean
- .verify
- TenantJpaEntity
- .create
- ProjectServiceTest
- ProjectService
- CurrentUserArgumentResolver.java
- SecurityConfig.java
- GoogleTokenVerifierClient.java
- ProjectController
- JwtConfig.java
- Backend Guidelines (`saas-backend`)
- GoogleAuthUseCase
- ConstructoraApplication.java
- MembershipValidationFilter.java
- V2__create_event_publication.sql
- com.SaaS:Constructora
- org.springframework.transaction.annotation.Transactional

## God Nodes (most connected - your core abstractions)
1. `Role` - 64 edges
2. `Project` - 43 edges
3. `Membership` - 40 edges
4. `User` - 40 edges
5. `MembershipRepository` - 29 edges
6. `ProjectAssignmentIntegrationTest` - 28 edges
7. `UserRepository` - 27 edges
8. `ProjectService` - 27 edges
9. `ProjectStageIntegrationTest` - 27 edges
10. `GoogleAuthUseCase` - 26 edges

## Surprising Connections (you probably didn't know these)
- `1. Patrón Arquitectónico y Estructura` --references--> `ProjectRepository`  [INFERRED]
  AGENTS.md → src/main/java/com/SaaS/Constructora/projects/ProjectRepository.java
- `GoogleAuthUseCase` --references--> `EmailSender`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java → src/main/java/com/SaaS/Constructora/identity/application/ports/EmailSender.java
- `GoogleAuthUseCase` --references--> `ExternalIdentityRepository`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java → src/main/java/com/SaaS/Constructora/identity/application/ports/ExternalIdentityRepository.java
- `GoogleAuthUseCase` --references--> `MembershipRepository`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java → src/main/java/com/SaaS/Constructora/identity/application/ports/MembershipRepository.java
- `GoogleAuthUseCase` --references--> `TenantRepository`  [EXTRACTED]
  src/main/java/com/SaaS/Constructora/identity/application/GoogleAuthUseCase.java → src/main/java/com/SaaS/Constructora/identity/application/ports/TenantRepository.java

## Import Cycles
- None detected.

## Communities (38 total, 15 thin omitted)

### Community 0 - ".update"
Cohesion: 0.10
Nodes (18): BudgetRange, FROM_100M_TO_250M, FROM_250M_TO_500M, FROM_50M_TO_100M, OVER_500M, UNDER_50M, LotOwnership, IN_PURCHASE (+10 more)

### Community 1 - "Membership"
Cohesion: 0.15
Nodes (4): Membership, User, LoginRequest, LoginResponse

### Community 2 - "Project"
Cohesion: 0.11
Nodes (3): Project, ProjectStatus, ACTIVE

### Community 3 - ".authenticate"
Cohesion: 0.13
Nodes (6): Tenant, GoogleAuthRequest, GoogleAuthResponse, TenantResponse, RegisterTenantResponse, TenantResponse

### Community 4 - "ProjectStage"
Cohesion: 0.08
Nodes (7): CreateProjectStageRequest, ProjectStage, ProjectStageController, ProjectStageResponse, ProjectStageService, UpdateProjectStageRequest, ProjectStageServiceTest

### Community 5 - "AuthenticatedUser"
Cohesion: 0.14
Nodes (4): UserResponse, AuthenticatedUser, CurrentUser, TenantClaims

### Community 6 - "org.junit.jupiter.api.Test"
Cohesion: 0.05
Nodes (13): AuthControllerTest, FakeGoogleTokenVerifier, LoginIntegrationTest, RecordingEmailSender, TestEmailConfig, ProjectAssignmentDelegationIntegrationTest, Session, ProjectAssignmentIntegrationTest (+5 more)

### Community 7 - "PasswordResetToken"
Cohesion: 0.12
Nodes (4): PasswordResetToken, PasswordResetTokenJpaEntity, PasswordResetTokenJpaRepository, PasswordResetTokenRepositoryAdapter

### Community 8 - "uuid"
Cohesion: 0.05
Nodes (6): Role, ADMIN, MEMBER, MembershipJpaEntity, MembershipJpaRepository, MembershipRepositoryAdapter

### Community 9 - "ExternalIdentity"
Cohesion: 0.12
Nodes (5): ExternalIdentityRepository, ExternalIdentity, ExternalIdentityJpaEntity, ExternalIdentityJpaRepository, ExternalIdentityRepositoryAdapter

### Community 10 - "RequestPasswordResetUseCase.java"
Cohesion: 0.16
Nodes (5): EmailSender, PasswordResetTokenRepository, RequestPasswordResetUseCase, ResetPasswordUseCase, TokenHasher

### Community 11 - "V3__create_identity.sql"
Cohesion: 0.10
Nodes (24): idx_project_assignments_project_role, idx_projects_tenant, projects, idx_memberships_active, idx_memberships_tenant, idx_memberships_user, memberships, tenants (+16 more)

### Community 12 - "UserJpaEntity"
Cohesion: 0.14
Nodes (3): UserJpaEntity, UserJpaRepository, UserRepositoryAdapter

### Community 13 - "ProjectStageIntegrationTest"
Cohesion: 0.27
Nodes (3): ProjectStageIntegrationTest, Session, StageRef

### Community 14 - "AuthController.java"
Cohesion: 0.25
Nodes (5): AuthController, ForgotPasswordRequest, MessageResponse, RegisterTenantRequest, ResetPasswordRequest

### Community 17 - ".verify"
Cohesion: 0.06
Nodes (13): UserSummary, UserResponse, AssignProjectRequest, ProjectAssignment, ProjectAssignmentController, ProjectAssignmentResponse, ProjectAssignmentService, ProjectRole (+5 more)

### Community 19 - "TenantJpaEntity"
Cohesion: 0.16
Nodes (3): TenantJpaEntity, TenantJpaRepository, TenantRepositoryAdapter

### Community 23 - "ProjectService"
Cohesion: 0.16
Nodes (5): TenantQueryService, ProjectAssignmentRepository, ProjectRepository, ProjectService, ProjectStageRepository

### Community 26 - "GoogleTokenVerifierClient.java"
Cohesion: 0.06
Nodes (6): EmailTemplates, GoogleTokenVerifierClient, ParameterizedTypeReference<Map<String, Object>>, ResendEmailSender, SmtpEmailSender, GoogleTokenVerifierClientTest

### Community 30 - "Backend Guidelines (`saas-backend`)"
Cohesion: 0.50
Nodes (3): 1. Patrón Arquitectónico y Estructura, 2. Reglas Innegociables del Backend, Backend Guidelines (`saas-backend`)

### Community 31 - "GoogleAuthUseCase"
Cohesion: 0.25
Nodes (4): CompanyIdentity, GoogleAuthUseCase, GoogleAccount, GoogleTokenVerifier

### Community 35 - "V2__create_event_publication.sql"
Cohesion: 0.83
Nodes (3): event_publication, idx_event_publication_completion_date, idx_event_publication_publication_date

### Community 40 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.24
Nodes (9): LoginUseCase, PermissionManagementUseCase, MembershipRepository, TenantRepository, UserRepository, RegisterTenantUseCase, UserQueryService, UserController (+1 more)

## Knowledge Gaps
- **18 isolated node(s):** `com.SaaS:Constructora`, `ADMIN`, `MEMBER`, `UNDER_50M`, `FROM_50M_TO_100M` (+13 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 148 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **15 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Role` connect `uuid` to `.update`, `Membership`, `ProjectStage`, `AuthenticatedUser`, `org.junit.jupiter.api.Test`, `org.springframework.transaction.annotation.Transactional`, `.verify`, `.create`, `.list`, `ProjectServiceTest`, `ProjectService`?**
  _High betweenness centrality (0.113) - this node is a cross-community bridge._
- **What connects `com.SaaS:Constructora`, `ADMIN`, `MEMBER` to the rest of the system?**
  _18 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `.update` be split into smaller, more focused modules?**
  _Cohesion score 0.09852216748768473 - nodes in this community are weakly interconnected._
- **Why does `Project` connect `Project` to `.update`, `uuid`, `.list`, `ProjectServiceTest`, `ProjectService`?**
  _High betweenness centrality (0.053) - this node is a cross-community bridge._
- **Should `Membership` be split into smaller, more focused modules?**
  _Cohesion score 0.14962121212121213 - nodes in this community are weakly interconnected._
- **Why does `EmailSender` connect `RequestPasswordResetUseCase.java` to `org.springframework.transaction.annotation.Transactional`, `GoogleTokenVerifierClient.java`, `org.junit.jupiter.api.Test`, `GoogleAuthUseCase`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Should `Project` be split into smaller, more focused modules?**
  _Cohesion score 0.11088709677419355 - nodes in this community are weakly interconnected._