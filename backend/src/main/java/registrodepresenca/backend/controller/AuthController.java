package registrodepresenca.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import registrodepresenca.backend.entity.Usuario;
import registrodepresenca.backend.repository.UsuarioRepository;
import registrodepresenca.backend.seguranca.TokenService;

@RestController
@RequestMapping("/api/login")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final TokenService tokens;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UsuarioRepository usuarios, TokenService tokens) {
        this.usuarios = usuarios;
        this.tokens = tokens;
    }

    @PostMapping
    public Resposta entrar(@RequestBody(required = false) Credenciais dados) {
        if (dados == null || dados.nome() == null || dados.nome().isBlank()
                || dados.senha() == null || dados.senha().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe usuário e senha");
        }
        Usuario usuario = usuarios.findByNome(dados.nome().trim()).orElse(null);
        boolean confere = usuario != null && usuario.senha_hash != null
                && encoder.matches(dados.senha(), usuario.senha_hash);
        if (!confere) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha incorretos");
        }
        boolean admin = "ADMIN".equalsIgnoreCase(usuario.perfil) || "ADMINISTRADOR".equalsIgnoreCase(usuario.perfil);
        return new Resposta(tokens.gerar(usuario), usuario.id_usuario, usuario.nome, usuario.perfil, admin);
    }

    public record Credenciais(String nome, String senha) {}

    public record Resposta(String token, int idUsuario, String nome, String perfil, boolean admin) {}
}
