package Electricity.Management.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        final String authorizationHeader = request.getHeader("Authorization");
        String requestPath = request.getRequestURI();


        log.debug("Processing request: {} {}", request.getMethod(), requestPath);


        String username = null;
        String jwt = null;


        // Extract JWT from Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {

            jwt = authorizationHeader.substring(7);
            log.debug("JWT token found in Authorization header");
            
            try {

                username = jwtUtil.extractUsername(jwt);
                log.debug("Extracted username: {}", username);


            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                log.warn("JWT token has expired for request: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token expired\",\"message\":\"Your session has expired. Please login again.\"}");
                return;

            } catch (io.jsonwebtoken.MalformedJwtException e) {
                log.error("Malformed JWT token: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid token\",\"message\":\"Invalid authentication token.\"}");
                return;

            } catch (Exception e) {
                log.error("Error extracting username from JWT: {}", e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Authentication error\",\"message\":\"Error processing authentication token.\"}");
                return;
            }
        }
        else { log.debug("No JWT token found in request to: {}", requestPath); }



        // Validate token and set authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (jwtUtil.validateToken(jwt, username)) {

                    String selectedRole = jwtUtil.extractSelectedRole(jwt);
                    Integer userId = jwtUtil.extractUserId(jwt);

                    log.debug("Token validated successfully for user: {} with selected role: {}", username, selectedRole);

                    // Create authentication token with selected role
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + selectedRole))
                            );

                    // Set additional details (userId)
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Store userId and selected role in request attribute for easy access
                    request.setAttribute("userId", userId);
                    request.setAttribute("selectedRole", selectedRole);

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
                else { log.warn("Token validation failed for user: {}", username); }

            }
            catch (Exception e) {
                log.error("Error during token validation: {}", e.getMessage(), e);
            }
        }


        filterChain.doFilter(request, response);
    }
}
