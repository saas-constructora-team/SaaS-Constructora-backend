package com.SaaS.Constructora.shared.security;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resuelve parámetros anotados con {@link CurrentUser} a partir del JWT que el
 * resource server ya validó y dejó en el {@code SecurityContext}.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Se requiere autenticación");
        }
        AuthenticatedUser fromToken = AuthenticatedUser.from(jwt);
        // El rol del token puede quedar obsoleto tras un cambio de permisos.
        // El filtro de membresía ya consultó la BD en esta petición: esa es la
        // autoridad vigente. Si no hay filtro (endpoints públicos), se usa el claim.
        return TenantContext.getCurrentRole()
                .map(role -> new AuthenticatedUser(
                        fromToken.userId(), fromToken.tenantId(), role))
                .orElse(fromToken);
    }
}
