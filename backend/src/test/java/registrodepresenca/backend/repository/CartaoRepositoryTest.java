package registrodepresenca.backend.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import registrodepresenca.backend.entity.*;

@SpringBootTest
@Transactional
class CartaoRepositoryTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TurmaRepository turmaRepository;
    @Autowired private AlunoRepository alunoRepository;
    @Autowired private CartaoRepository cartaoRepository;

    @Test
    void deveBuscarCartaoPorUid() {
        Usuario u = new Usuario();
        u.nome = "Professor Teste";
        u.senha_hash = "hash-teste";
        u.perfil = "PROFESSOR";
        usuarioRepository.save(u);

        Turma t = new Turma();
        t.nome = "Turma Teste";
        t.turma = "A";
        t.ano = 2026;
        t.id_usuario = u.id_usuario;
        turmaRepository.save(t);

        // 1) Aluno SEM uid (o cartão ainda não existe)
        Aluno a = new Aluno();
        a.matricula_aluno = 999999;
        a.id_turma = t.id_turma;
        a.nome_aluno = "Aluno Teste";
        Aluno aluno = alunoRepository.saveAndFlush(a);

        // 2) Cartão com uid, apontando para o aluno
        Cartao c = new Cartao();
        c.matricula_aluno = aluno.matricula_aluno;
        c.uid = "TESTE-UID-123";
        c.situacao = "ATIVO";
        cartaoRepository.saveAndFlush(c);

        // 3) Agora sim, amarra o uid no aluno
        aluno.uid = "TESTE-UID-123";
        alunoRepository.saveAndFlush(aluno);

        Optional<Cartao> resultado = cartaoRepository.findByUid("TESTE-UID-123");
        assertTrue(resultado.isPresent(), "Cartão não encontrado");
        assertEquals(999999, resultado.get().matricula_aluno);
    }
}