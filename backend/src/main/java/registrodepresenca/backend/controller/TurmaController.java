package registrodepresenca.backend.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import registrodepresenca.backend.entity.Turma;
import registrodepresenca.backend.repository.TurmaRepository;

@RestController
@RequestMapping("/api/turmas")
public class TurmaController {

    private final TurmaRepository turmas;

    public TurmaController(TurmaRepository turmas) {
        this.turmas = turmas;
    }

    @GetMapping
    public List<Turma> listar() {
        return turmas.findAll();
    }
}
