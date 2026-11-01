# Graph Report - SaaS-Constructora-backend  (2026-11-01)

## Corpus Check
- 112 files · ~20,803 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 5 file(s) not represented in the graph (top: .properties 2, .example 1, (none) 1)

## Summary
- 939 nodes · 2639 edges · 45 communities (24 shown, 21 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 341 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `81021223`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- UserRepository
- User
- Project
- GoogleTokenVerifierClient.java
- ProjectStage
- Role
- org.junit.jupiter.api.Test
- PasswordResetToken
- AuthenticatedUser
- ExternalIdentity
- ResendEmailSender
- V3__create_identity.sql
- UserJpaEntity
- LotOwnership
- ProjectAssignmentIntegrationTest
- .create
- UserSummary
- BudgetRange
- uuid
- CurrentUserArgumentResolver.java
- ProjectService
- ProjectServiceTest
- .update
- ProjectAssignment
- GoogleAuthUseCase.java
- org.springframework.context.annotation.Bean
- SecurityConfig.java
- ProjectType
- .fakeGoogleTokenVerifier
- ConstructoraApplication.java
- AuthController.java
- MembershipValidationFilter.java
- V2__create_event_publication.sql
- com.SaaS:Constructora
- ProjectStage
- WebConfig.java
- org.springframework.stereotype.Service
- SmtpEmailSender
- Backend Guidelines (`saas-backend`)

## God Nodes (most connected - your core abstractions)
1. `Role` - 51 edges
2. `Project` - 43 edges
3. `User` - 40 edges
4. `Membership` - 38 edges
5. `ProjectAssignmentIntegrationTest` - 28 edges
6. `UserRepository` - 27 edges
7. `ProjectStageIntegrationTest` - 27 edges
8. `GoogleAuthUseCase` - 26 edges
9. `MembershipRepository` - 26 edges
10. `ProjectService` - 26 edges

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

## Communities (45 total, 21 thin omitted)

### Community 0 - "UserRepository"
Cohesion: 0.43
Nodes (6): LoginUseCase, MembershipRepository, TenantRepository, UserRepository, RegisterTenantUseCase, JwtService

### Community 1 - "User"
Cohesion: 0.06
Nodes (11): CompanyIdentity, Membership, Tenant, User, GoogleAuthRequest, GoogleAuthResponse, TenantResponse, LoginRequest (+3 more)

### Community 2 - "Project"
Cohesion: 0.14
Nodes (3): Project, ProjectStatus, ACTIVE

### Community 3 - "GoogleTokenVerifierClient.java"
Cohesion: 0.13
Nodes (5): GoogleAccount, GoogleTokenVerifier, GoogleTokenVerifierClient, ParameterizedTypeReference<Map<String, Object>>, GoogleTokenVerifierClientTest

### Community 4 - "ProjectStage"
Cohesion: 0.11
Nodes (7): CreateProjectStageRequest, ProjectStageController, ProjectStageRepository, ProjectStageResponse, ProjectStageService, UpdateProjectStageRequest, ProjectStageServiceTest

### Community 5 - "Role"
Cohesion: 0.07
Nodes (8): Role, ADMIN, MEMBER, MembershipJpaEntity, MembershipJpaRepository, MembershipRepositoryAdapter, UserResponse, TenantClaims

### Community 6 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (8): AuthControllerTest, FakeGoogleTokenVerifier, LoginIntegrationTest, ProjectStageIntegrationTest, Session, StageRef, ProjectTenantIntegrationTest, MembershipValidationFilterIntegrationTest

### Community 7 - "PasswordResetToken"
Cohesion: 0.11
Nodes (4): PasswordResetToken, PasswordResetTokenJpaEntity, PasswordResetTokenJpaRepository, PasswordResetTokenRepositoryAdapter

### Community 8 - "AuthenticatedUser"
Cohesion: 0.10
Nodes (5): AssignProjectRequest, ProjectAssignmentController, ProjectAssignmentResponse, AuthenticatedUser, CurrentUser

### Community 9 - "ExternalIdentity"
Cohesion: 0.12
Nodes (5): ExternalIdentityRepository, ExternalIdentity, ExternalIdentityJpaEntity, ExternalIdentityJpaRepository, ExternalIdentityRepositoryAdapter

### Community 11 - "V3__create_identity.sql"
Cohesion: 0.12
Nodes (19): idx_projects_tenant, projects, idx_memberships_active, idx_memberships_tenant, idx_memberships_user, memberships, tenants, users (+11 more)

### Community 12 - "UserJpaEntity"
Cohesion: 0.15
Nodes (3): UserJpaEntity, UserJpaRepository, UserRepositoryAdapter

### Community 13 - "LotOwnership"
Cohesion: 0.24
Nodes (5): LotOwnership, IN_PURCHASE, OWNED, RENTED, ProjectDetails

### Community 14 - "ProjectAssignmentIntegrationTest"
Cohesion: 0.18
Nodes (4): ProjectAssignmentIntegrationTest, Session, ProjectAuthorizationIntegrationTest, Session

### Community 17 - "UserSummary"
Cohesion: 0.16
Nodes (5): UserQueryService, UserSummary, UserResponse, UserController, ProjectAssignmentServiceTest

### Community 18 - "BudgetRange"
Cohesion: 0.25
Nodes (6): BudgetRange, FROM_100M_TO_250M, FROM_250M_TO_500M, FROM_50M_TO_100M, OVER_500M, UNDER_50M

### Community 19 - "uuid"
Cohesion: 0.06
Nodes (3): TenantJpaEntity, TenantJpaRepository, TenantRepositoryAdapter

### Community 21 - "ProjectService"
Cohesion: 0.26
Nodes (5): TenantQueryService, ProjectAssignmentRepository, ProjectAssignmentService, ProjectRepository, ProjectService

### Community 23 - ".update"
Cohesion: 0.21
Nodes (3): ProjectController, ProjectResponse, UpdateProjectRequest

### Community 30 - "ProjectType"
Cohesion: 0.25
Nodes (5): ProjectType, COMMERCIAL, INDUSTRIAL, MIXED, RESIDENTIAL

### Community 33 - "AuthController.java"
Cohesion: 0.25
Nodes (5): AuthController, ForgotPasswordRequest, MessageResponse, RegisterTenantRequest, ResetPasswordRequest

### Community 35 - "V2__create_event_publication.sql"
Cohesion: 0.83
Nodes (3): event_publication, idx_event_publication_completion_date, idx_event_publication_publication_date

### Community 40 - "org.springframework.stereotype.Service"
Cohesion: 0.29
Nodes (4): PasswordResetTokenRepository, RequestPasswordResetUseCase, ResetPasswordUseCase, TokenHasher

### Community 43 - "Backend Guidelines (`saas-backend`)"
Cohesion: 0.50
Nodes (3): 1. Patrón Arquitectónico y Estructura, 2. Reglas Innegociables del Backend, Backend Guidelines (`saas-backend`)

## Knowledge Gaps
- **16 isolated node(s):** `com.SaaS:Constructora`, `ADMIN`, `MEMBER`, `UNDER_50M`, `FROM_50M_TO_100M` (+11 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 144 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **21 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Role` connect `Role` to `UserRepository`, `User`, `ProjectStage`, `org.junit.jupiter.api.Test`, `AuthenticatedUser`, `.create`, `UserSummary`, `uuid`, `ProjectService`, `ProjectServiceTest`, `.update`, `GoogleAuthUseCase.java`?**
  _High betweenness centrality (0.099) - this node is a cross-community bridge._
- **What connects `com.SaaS:Constructora`, `ADMIN`, `MEMBER` to the rest of the system?**
  _16 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `User` be split into smaller, more focused modules?**
  _Cohesion score 0.06004543979227524 - nodes in this community are weakly interconnected._
- **Why does `Project` connect `Project` to `Role`, `.onCreate`, `LotOwnership`, `.create`, `BudgetRange`, `uuid`, `ProjectService`, `ProjectServiceTest`, `ProjectType`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._
- **Should `Project` be split into smaller, more focused modules?**
  _Cohesion score 0.14333333333333334 - nodes in this community are weakly interconnected._
- **Why does `User` connect `User` to `UserRepository`, `org.junit.jupiter.api.Test`, `org.springframework.stereotype.Service`, `UserJpaEntity`, `uuid`, `JwtService.java`, `GoogleAuthUseCase.java`?**
  _High betweenness centrality (0.040) - this node is a cross-community bridge._
- **Should `GoogleTokenVerifierClient.java` be split into smaller, more focused modules?**
  _Cohesion score 0.13043478260869565 - nodes in this community are weakly interconnected._