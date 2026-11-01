package com.SaaS.Constructora.shared.security;

import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

/**
 * Valida que el usuario autenticado tenga una membresía activa en el tenant
 * indicado por su JWT y publica ese tenant en {@link TenantContext}.
 * <p>
 * Se ejecuta dentro de la cadena de seguridad, después de que el resource
 * server haya validado el token y poblado el {@code SecurityContext}. De este
 * modo una petición sin membresía se rechaza con 403 antes de llegar a
 * cualquier controlador.
 */
public class MembershipValidationFilter extends OncePerRequestFilter {

    /** Nombre del filtro Hibernate que aplica el aislamiento por tenant. */
    static final String TENANT_FILTER = "tenantFilter";

    /** Parámetro con el que se alimenta el filtro Hibernate. */
    static final String TENANT_FILTER_PARAM = "tenantId";

    private static final Logger log = LoggerFactory.getLogger(MembershipValidationFilter.class);

    private final MembershipRepository membershipRepository;
    private final EntityManager entityManager;

    public MembershipValidationFilter(MembershipRepository membershipRepository,
                                      EntityManager entityManager) {
        this.membershipRepository = membershipRepository;
        this.entityManager = entityManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        AuthenticatedUser user;
        try {
            user = currentUser();
        } catch (ResponseStatusException ex) {
            // La excepción se lanza dentro de un filtro, fuera del DispatcherServlet:
            // hay que trasladarla a la respuesta HTTP para no terminar en un 500.
            response.sendError(ex.getStatusCode().value(), ex.getReason());
            return;
        }
        if (user == null) {
            // Endpoint público o sin token: la autorización HTTP decide si pasa.
            filterChain.doFilter(request, response);
            return;
        }

        Membership membership;
        try {
            membership = membershipRepository
                    .findActiveByUserIdAndTenantId(user.userId(), user.tenantId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Usuario no tiene membresía activa en esta empresa"));
        } catch (ResponseStatusException ex) {
            response.sendError(ex.getStatusCode().value(), ex.getReason());
            return;
        }

        try {
            UUID tenantId = membership.getTenantId();
            TenantContext.setCurrentTenantId(tenantId);
            enableTenantFilter(tenantId);

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /** Extrae el contexto del JWT que el resource server dejó en el SecurityContext. */
    private static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        return AuthenticatedUser.from(jwt);
    }

    /**
     * Habilita el filtro Hibernate {@code tenantFilter} en la sesión actual para
     * que las consultas queden acotadas al tenant incluso si una capa olvida
     * filtrar explícitamente.
     * <p>
     * El filtro solo se activa si está declarado en el modelo y si Hibernate ya
     * tiene una sesión ligada al hilo. {@link WebConfig} registra
     * {@code OpenEntityManagerInViewFilter} antes de la cadena de seguridad para
     * que esa sesión sea la misma que usan los controladores y repositorios.
     */
    private void enableTenantFilter(UUID tenantId) {
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        if (!sessionFactory.getDefinedFilterNames().contains(TENANT_FILTER)) {
            return;
        }
        try {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter(TENANT_FILTER)
                    .setParameter(TENANT_FILTER_PARAM, tenantId);
        } catch (IllegalStateException ex) {
            log.debug("No hay una sesión Hibernate activa para habilitar {} en esta petición",
                    TENANT_FILTER);
        }
    }
}
