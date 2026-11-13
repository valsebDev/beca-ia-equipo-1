package com.ids.ms_users.security;

import java.util.function.Supplier;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import com.ids.ms_users.exception.UnmappedRouteException;

@Component
public class UnmappedRouteAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
                                         RequestAuthorizationContext context) {
        Authentication current = authentication.get();
        if (current == null || current instanceof AnonymousAuthenticationToken
                || !current.isAuthenticated()) {
            throw new UnmappedRouteException();
        }
        return new AuthorizationDecision(true);
    }
}
