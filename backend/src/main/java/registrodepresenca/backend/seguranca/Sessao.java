package registrodepresenca.backend.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Sessao {

    public static final String ATRIBUTO = "usuarioAutenticado";

    private Sessao() {}

    public record Usuario(int id, String perfil) {
        public boolean admin() {
            return perfil != null
                    && (perfil.equalsIgnoreCase("ADMIN") || perfil.equalsIgnoreCase("ADMINISTRADOR"));
        }
    }

    /** Só o administrador passa; professor recebe 403. */
    public static Usuario exigirAdmin(HttpServletRequest request, String mensagem) {
        Usuario usuario = de(request);
        if (!usuario.admin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, mensagem);
        }
        return usuario;
    }

    public static Usuario de(HttpServletRequest request) {
        if (request.getAttribute(ATRIBUTO) instanceof Usuario usuario) {
            return usuario;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para continuar");
    }
}
