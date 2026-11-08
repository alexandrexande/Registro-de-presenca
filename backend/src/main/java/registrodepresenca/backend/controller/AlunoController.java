package registrodepresenca.backend.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.repository.AlunoRepository;
import registrodepresenca.backend.repository.TurmaRepository;

@RestController
@RequestMapping("/api/alunos")
public class AlunoController {

    private final AlunoRepository alunos;
    private final TurmaRepository turmas;

    public AlunoController(AlunoRepository alunos, TurmaRepository turmas) {
        this.alunos = alunos;
        this.turmas = turmas;
    }

    @GetMapping
    public List<Aluno> listar() {
        return alunos.findAll();
    }

    @GetMapping("/turma/{idTurma}")
    public List<Aluno> listarPorTurma(@PathVariable int idTurma) {
        conferirTurma(idTurma);
        return alunos.buscarPorTurma(idTurma);
    }

    private void conferirTurma(int idTurma) {
        if (!turmas.existsById(idTurma)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada");
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Aluno cadastrar(@RequestBody CadastroAluno dados) {
        if (dados.matricula() <= 0 || dados.idTurma() <= 0
                || dados.nome() == null || dados.nome().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe matrícula, turma e nome");
        }
        conferirTurma(dados.idTurma());
        if (alunos.existsById(dados.matricula())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matrícula já cadastrada");
        }
        Aluno aluno = new Aluno();
        aluno.matricula_aluno = dados.matricula();
        aluno.id_turma = dados.idTurma();
        aluno.nome_aluno = dados.nome().trim();
        return alunos.saveAndFlush(aluno);
    }

    public record CadastroAluno(int matricula, int idTurma, String nome) {}
}
