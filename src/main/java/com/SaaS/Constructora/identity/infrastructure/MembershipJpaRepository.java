package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.domain.MembershipStatus;
import com.SaaS.Constructora.identity.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface MembershipJpaRepository extends JpaRepository<MembershipJpaEntity, UUID> {

    @Query("SELECT m FROM MembershipJpaEntity m WHERE m.userId = :userId AND m.tenantId = :tenantId "
            + "AND m.status = :status AND m.active = true")
    Optional<MembershipJpaEntity> findActiveByUserIdAndTenantId(
            @Param("userId") UUID userId, @Param("tenantId") UUID tenantId,
            @Param("status") MembershipStatus status);

    @Query("SELECT m FROM MembershipJpaEntity m WHERE m.userId = :userId "
            + "AND m.status = :status AND m.active = true")
    List<MembershipJpaEntity> findActiveByUserId(
            @Param("userId") UUID userId, @Param("status") MembershipStatus status);

    @Query("SELECT m FROM MembershipJpaEntity m WHERE m.tenantId = :tenantId "
            + "AND m.status = :status AND m.active = true")
    List<MembershipJpaEntity> findByTenantIdAndActiveTrue(
            @Param("tenantId") UUID tenantId, @Param("status") MembershipStatus status);

    Optional<MembershipJpaEntity> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    Optional<MembershipJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByUserIdAndTenantIdAndRoleAndActiveTrue(UUID userId, UUID tenantId, Role role);

    void deleteByUserIdAndTenantIdAndRoleAndActiveFalse(UUID userId, UUID tenantId, Role role);
}
