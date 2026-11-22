package registrodepresenca.backend.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokens;

    public AuthInterceptor(TokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String cabecalho = request.getHeader("Authorization");
        boolean enviouToken = cabecalho != null && cabecalho.regionMatches(true, 0, "Bearer ", 0, 7);
        if (enviouToken) {
            Optional<Sessao.Usuario> usuario = tokens.validar(cabecalho.substring(7).trim());
            if (usuario.isPresent()) {
                request.setAttribute(Sessao.ATRIBUTO, usuario.get());
                return true;
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(enviouToken
                ? "{\"mensagem\":\"Sessão expirada. Entre novamente.\"}"
                : "{\"mensagem\":\"Faça login e envie o token no cabeçalho Authorization.\"}");
        return false;
    }
}
