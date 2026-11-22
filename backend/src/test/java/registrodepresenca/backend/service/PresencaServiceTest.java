package registrodepresenca.backend.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.entity.Cartao;
import registrodepresenca.backend.entity.Presenca;
import registrodepresenca.backend.repository.AlunoRepository;
import registrodepresenca.backend.repository.CartaoRepository;
import registrodepresenca.backend.repository.PresencaRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PresencaServiceTest {

    private CartaoRepository cartoes;
    private AlunoRepository alunos;
    private PresencaRepository presencas;
    private PresencaService service;

    @BeforeEach
    void preparar() {
        cartoes = mock(CartaoRepository.class);
        alunos = mock(AlunoRepository.class);
        presencas = mock(PresencaRepository.class);
        service = new PresencaService(cartoes, alunos, presencas);
    }

    private Aluno alunoComCartao(String situacao) {
        Aluno aluno = new Aluno();
        aluno.matricula_aluno = 2026001;
        aluno.nome_aluno = "Aluno teste";
        Cartao cartao = new Cartao();
        cartao.id_cartao = 10;
        cartao.matricula_aluno = aluno.matricula_aluno;
        cartao.uid = "ABC123";
        cartao.situacao = situacao;
        when(cartoes.findByUid("ABC123")).thenReturn(Optional.of(cartao));
        when(alunos.buscarParaAtualizar(2026001)).thenReturn(Optional.of(aluno));
        return aluno;
    }

    @Test
    void registraPresenteSemAlterarCadastroDoAluno() {
        alunoComCartao("ATIVO");
        var resultado = service.registrarLeitura(" ABC123 ");
        assertTrue(resultado.aceita());
        assertEquals("PRESENTE", resultado.situacao());
        verify(presencas).save(argThat(p -> p.matricula_aluno == 2026001
                && p.id_cartao == 10 && "PRESENTE".equals(p.situacao) && p.data_hora != null));
        verify(alunos, never()).save(any());
    }

    @Test
    void cartaoDesconhecidoNaoGrava() {
        var resultado = service.registrarLeitura("INEXISTENTE");
        assertFalse(resultado.aceita());
        assertEquals("DESCONHECIDO", resultado.situacao());
        verifyNoInteractions(alunos, presencas);
    }

    @Test
    void cartaoBloqueadoNaoGrava() {
        alunoComCartao("BLOQUEADO");
        var resultado = service.registrarLeitura("ABC123");
        assertFalse(resultado.aceita());
        assertEquals("BLOQUEADO", resultado.situacao());
        verify(alunos, never()).buscarParaAtualizar(anyInt());
        verifyNoInteractions(presencas);
    }

    @Test
    void duplicadaNaoGrava() {
        alunoComCartao("ATIVO");
        when(presencas.ultimaLeituraGravada(10)).thenReturn(OffsetDateTime.now().minusMinutes(44));
        var resultado = service.registrarLeitura("ABC123");
        assertFalse(resultado.aceita());
        assertEquals("DUPLICADA", resultado.situacao());
        verify(presencas, never()).save(any(Presenca.class));
        verify(alunos, never()).save(any(Aluno.class));
    }

    @Test
    void segundaLeituraDoDiaContinuaDuplicadaApos45Minutos() {
        alunoComCartao("ATIVO");
        var entrada = OffsetDateTime.now().minusMinutes(45);
        when(presencas.ultimaLeituraGravada(10)).thenReturn(entrada);
        when(presencas.existeNoIntervalo(eq(2026001), any(), any())).thenReturn(true);
        var resultado = service.registrarLeitura("ABC123");
        assertFalse(resultado.aceita());
        assertEquals("DUPLICADA", resultado.situacao());
        verify(presencas, never()).save(any(Presenca.class));
        verify(alunos, never()).save(any());
    }

    @Test
    void uidVazioNaoConsultaBanco() {
        assertEquals("INVALIDA", service.registrarLeitura(null).situacao());
        assertEquals("INVALIDA", service.registrarLeitura(" ").situacao());
        verifyNoInteractions(cartoes, alunos, presencas);
    }

    @Test
    void alunoInexistenteNaoGrava() {
        alunoComCartao("ATIVO");
        when(alunos.buscarParaAtualizar(2026001)).thenReturn(Optional.empty());
        assertEquals("ALUNO_INEXISTENTE", service.registrarLeitura("ABC123").situacao());
        verifyNoInteractions(presencas);
    }
}
