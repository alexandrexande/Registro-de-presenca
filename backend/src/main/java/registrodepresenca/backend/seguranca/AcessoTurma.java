package registrodepresenca.backend.seguranca;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.entity.Turma;
import registrodepresenca.backend.repository.AlunoRepository;
import registrodepresenca.backend.repository.TurmaRepository;

/** Garante que o professor só mexa nas próprias turmas. Administrador acessa todas. */
@Component
public class AcessoTurma {

    private final TurmaRepository turmas;
    private final AlunoRepository alunos;

    public AcessoTurma(TurmaRepository turmas, AlunoRepository alunos) {
        this.turmas = turmas;
        this.alunos = alunos;
    }

    public Turma conferirTurma(Sessao.Usuario usuario, int idTurma) {
        Turma turma = turmas.findById(idTurma).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
        if (!usuario.admin() && turma.id_usuario != usuario.id()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta turma pertence a outro professor");
        }
        return turma;
    }

    public Aluno conferirAluno(Sessao.Usuario usuario, int matricula) {
        Aluno aluno = alunos.findById(matricula).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        conferirTurma(usuario, aluno.id_turma);
        return aluno;
    }
}
