package registrodepresenca.demo.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import registrodepresenca.demo.classes.Aluno;
import registrodepresenca.demo.classes.Cartao;

@SpringBootTest
@Transactional
class CartaoRepositoryTest {

    @Autowired
    private CartaoRepository cartaoRepository;

    @Autowired
    private AlunoRepository alunoRepository;

    @Test
    void deveBuscarCartaoPorUid() {
        Optional<Cartao> cartao = cartaoRepository.findByUid("A1B2C3D4");
        assertTrue(cartao.isPresent(), "Cartão não encontrado");

        Optional<Aluno> aluno = alunoRepository.findById(cartao.get().matricula_aluno);
        assertTrue(aluno.isPresent(), "Aluno do cartão não encontrado");
        System.out.println("Aluno: " + aluno.get().nome_aluno);
    }
}