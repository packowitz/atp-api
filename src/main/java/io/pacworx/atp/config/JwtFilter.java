package io.pacworx.atp.config;

import io.jsonwebtoken.Jwts;
import io.pacworx.atp.user.UserRepository;
import io.pacworx.atp.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER)) {
            try {
                String token = authHeader.substring(BEARER.length());
                String userId = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().getSubject();
                User user = userRepository.findOne(Long.parseLong(userId));
                if(user == null) {
                    throw new IllegalArgumentException();
                }
                request.setAttribute("user", user);
                chain.doFilter(request, response);
                return;
            }
            catch (Exception e) {}
        }

        if (request.getMethod().equals("OPTIONS")) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !request.getRequestURI().startsWith("/app/");
    }
}
