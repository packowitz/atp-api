package io.pacworx.atp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.pacworx.atp.exception.ExceptionInfo;
import io.pacworx.atp.user.User;
import io.pacworx.atp.user.UserRepository;
import io.pacworx.atp.user.UserRights;
import io.pacworx.atp.user.UserRightsRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtWebFilter extends OncePerRequestFilter {
    private static final Logger log = LogManager.getLogger();
    private static final String BEARER = "Bearer ";

    @Value("${jwt.web.secret}")
    private String secret;

    private final UserRepository userRepository;
    private final UserRightsRepository userRightsRepository;

    @Autowired
    public JwtWebFilter(UserRepository userRepository, UserRightsRepository userRightsRepository) {
        this.userRepository = userRepository;
        this.userRightsRepository = userRightsRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER)) {
            try {
                String token = authHeader.substring(BEARER.length());
                String userId = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().getSubject();
                User user = userRepository.findOne(Long.parseLong(userId));
                if(user != null) {
                    UserRights rights = userRightsRepository.findOne(user.getId());
                    if(rights == null) {
                        rights = new UserRights(user.getId());
                    }
                    request.setAttribute("webuser", user);
                    request.setAttribute("userRights", rights);
                    chain.doFilter(request, response);
                    return;
                }
            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        if(request.getMethod().equals("OPTIONS")) {
            chain.doFilter(request, response);
        } else {
            ExceptionInfo info = new ExceptionInfo(HttpStatus.FORBIDDEN.value());
            info.setCustomMessage("Your authentication information is incorrect.");
            info.enableShowResetAccountBtn();
            info.enableShowCloseBtn();

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            ObjectMapper mapper = new ObjectMapper();
            response.getWriter().write(mapper.writeValueAsString(info));
            response.getWriter().flush();
            response.getWriter().close();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !request.getRequestURI().startsWith("/web/app/");
    }
}
