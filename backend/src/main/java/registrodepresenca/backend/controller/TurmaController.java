package registrodepresenca.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import registrodepresenca.backend.entity.Turma;
import registrodepresenca.backend.repository.TurmaRepository;
import registrodepresenca.backend.seguranca.Sessao;

@RestController
@RequestMapping("/api/turmas")
public class TurmaController {

    private final TurmaRepository turmas;

    public TurmaController(TurmaRepository turmas) {
        this.turmas = turmas;
    }

    @GetMapping
    public List<Turma> listar(HttpServletRequest request) {
        Sessao.Usuario usuario = Sessao.de(request);
        return usuario.admin() ? turmas.findAll() : turmas.buscarPorUsuario(usuario.id());
    }
}
