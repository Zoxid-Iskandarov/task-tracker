package com.walking.backend.integration.annotation;

import com.walking.backend.security.principal.CustomUserDetails;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockUserSecurityContextFactory implements WithSecurityContextFactory<WithMockUser> {
    @Override
    public @NonNull SecurityContext createSecurityContext(WithMockUser mockUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        CustomUserDetails userDetails =
                new CustomUserDetails(mockUser.id(), mockUser.username(), mockUser.email(), mockUser.password());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        context.setAuthentication(authentication);

        return context;
    }
}
