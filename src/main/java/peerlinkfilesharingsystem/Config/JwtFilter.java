package peerlinkfilesharingsystem.Config;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import peerlinkfilesharingsystem.Service.Jwt.JwtService;
import peerlinkfilesharingsystem.Service.UserService.UserPrincipleService;

import java.io.IOException;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserPrincipleService userPrincipleService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        log.debug("=== JWT FILTER ===");
        log.debug("Request URI: {}", path);
        log.debug("Method: {}", request.getMethod());

        if (path.startsWith("/api/register") ||
                path.startsWith("/api/login") ||
                path.startsWith("/api/mail/verifymail") ||
                path.startsWith("/api/mail/valid") ||
                path.startsWith("/files/info/public") ||
                path.endsWith("/public")) {

            log.debug("Public endpoint - skipping JWT check");
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        log.debug("Auth Header: {}", authHeader != null ? authHeader.substring(0, Math.min(50, authHeader.length())) + "..." : "None");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found - treating as anonymous");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        log.debug("Extracted token: {}...", token.substring(0, Math.min(30, token.length())));

        String username = null;
        try {
            username = jwtService.extractUserName(token);
            log.debug("Extracted username: {}", username);

        } catch (ExpiredJwtException e) {
            log.error("Token expired for request: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token expired");
            return;

        } catch (Exception e) {
            log.error("Invalid token for request: {}", path, e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userPrincipleService.loadUserByUsername(username);
            log.debug("Loaded UserDetails for: {}", username);

            if (jwtService.validateToken(token, userDetails)) {
                log.debug("Token is VALID for user: {}", username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authentication set in SecurityContext for user: {}", username);

            } else {
                log.warn("Token validation FAILED for user: {}", username);
            }
        } else if (username != null) {
            log.debug("User {} is already authenticated", username);
        }

        filterChain.doFilter(request, response);
    }
}
