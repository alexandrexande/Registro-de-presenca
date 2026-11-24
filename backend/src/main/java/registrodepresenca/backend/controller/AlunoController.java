package registrodepresenca.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.repository.AlunoRepository;
import registrodepresenca.backend.repository.TurmaRepository;
import registrodepresenca.backend.seguranca.AcessoTurma;
import registrodepresenca.backend.seguranca.Sessao;

@RestController
@RequestMapping("/api/alunos")
public class AlunoController {

    private final AlunoRepository alunos;
    private final TurmaRepository turmas;
    private final AcessoTurma acesso;

    public AlunoController(AlunoRepository alunos, TurmaRepository turmas, AcessoTurma acesso) {
        this.alunos = alunos;
        this.turmas = turmas;
        this.acesso = acesso;
    }

    @GetMapping
    public List<Aluno> listar(HttpServletRequest request) {
        Sessao.Usuario usuario = Sessao.de(request);
        if (usuario.admin()) {
            return alunos.findAll();
        }
        Set<Integer> minhas = turmas.buscarPorUsuario(usuario.id()).stream()
                .map(t -> t.id_turma).collect(Collectors.toSet());
        return alunos.findAll().stream().filter(a -> minhas.contains(a.id_turma)).toList();
    }

    @GetMapping("/turma/{idTurma}")
    public List<Aluno> listarPorTurma(@PathVariable int idTurma, HttpServletRequest request) {
        acesso.conferirTurma(Sessao.de(request), idTurma);
        return alunos.buscarPorTurma(idTurma);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Aluno cadastrar(@RequestBody CadastroAluno dados, HttpServletRequest request) {
        Sessao.Usuario usuario = Sessao.exigirAdmin(request, "Só o administrador cadastra alunos");
        if (dados.matricula() <= 0 || dados.idTurma() <= 0
                || dados.nome() == null || dados.nome().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe matrícula, turma e nome");
        }
        acesso.conferirTurma(usuario, dados.idTurma());
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
