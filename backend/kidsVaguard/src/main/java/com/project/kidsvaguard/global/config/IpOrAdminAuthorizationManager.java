package com.project.kidsvaguard.global.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class IpOrAdminAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        String remoteAddr = request.getRemoteAddr();

        boolean isLocalhost = remoteAddr.equals("127.0.0.1")
                || remoteAddr.equals("0:0:0:0:0:0:0:1")
                || remoteAddr.equals("::1");

        System.out.println("접속 IP: " + remoteAddr);

        if (isLocalhost) {
            System.out.println("[LocalhostBypassAuthorizationManager] 로컬 접속 - 인증 무시 허용, IP: " + remoteAddr);
            return new AuthorizationDecision(true);
        }

        // 로컬이 아니면 ROLE_ADMIN 체크
        Authentication auth = authentication.get();
        boolean hasAdminRole = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        System.out.println("[LocalhostBypassAuthorizationManager] 외부 접속 - 권한 체크, IP: " + remoteAddr + ", ADMIN 권한 여부: " + hasAdminRole);

        return new AuthorizationDecision(hasAdminRole);
    }
}
