package registrodepresenca.demo.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import registrodepresenca.demo.entity.Cartao;

@SpringBootTest
@Transactional
class CartaoRepositoryTest {

    @Autowired
    private CartaoRepository cartaoRepository;

    @Test
    void deveBuscarCartaoPorUid() {
        Optional<Cartao> resultado = cartaoRepository.findByUid("A1B2C3D4");

        assertTrue(resultado.isPresent(), "Cartão não encontrado");
        assertNotNull(resultado.get().getAluno().getNomeAluno());
        System.out.println("Aluno: " + resultado.get().getAluno().getNomeAluno());
    }
}