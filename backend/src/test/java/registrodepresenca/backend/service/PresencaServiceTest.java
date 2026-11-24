package registrodepresenca.backend.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.entity.Aula;
import registrodepresenca.backend.entity.Cartao;
import registrodepresenca.backend.entity.Presenca;
import registrodepresenca.backend.repository.AlunoRepository;
import registrodepresenca.backend.repository.AulaRepository;
import registrodepresenca.backend.repository.CartaoRepository;
import registrodepresenca.backend.repository.PresencaRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PresencaServiceTest {

    private CartaoRepository cartoes;
    private AlunoRepository alunos;
    private PresencaRepository presencas;
    private AulaRepository aulas;

    @BeforeEach
    void preparar() {
        cartoes = mock(CartaoRepository.class);
        alunos = mock(AlunoRepository.class);
        presencas = mock(PresencaRepository.class);
        aulas = mock(AulaRepository.class);
        when(aulas.listarEmOrdem()).thenReturn(List.of(
                aula(1, "07:00", "07:50"), aula(2, "07:50", "08:40"), aula(3, "08:40", "09:30"),
                aula(4, "09:50", "10:40"), aula(5, "10:40", "11:30")));
    }

    private static Aula aula(int numero, String inicio, String fim) {
        Aula aula = new Aula();
        aula.numero = numero;
        aula.nome = numero + "ª aula";
        aula.inicio = LocalTime.parse(inicio);
        aula.fim = LocalTime.parse(fim);
        return aula;
    }

    /** Serviço com o relógio parado em uma segunda-feira, na hora informada (fuso de Maceió). */
    private PresencaService servicoAs(String hora) {
        Clock relogio = Clock.fixed(OffsetDateTime.parse("2026-11-23T" + hora + "-03:00").toInstant(),
                ZoneId.of("America/Maceio"));
        return new PresencaService(cartoes, alunos, presencas, aulas, relogio, 10);
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
    void registraPresencaNaJanelaDoFimDaAula() {
        alunoComCartao("ATIVO");
        var resultado = servicoAs("07:45:00").registrarLeitura(" ABC123 ");
        assertTrue(resultado.aceita());
        assertEquals("PRESENTE", resultado.situacao());
        verify(presencas).save(argThat(p -> p.matricula_aluno == 2026001 && p.id_cartao == 10
                && "PRESENTE".equals(p.situacao) && p.data_hora != null && p.numero_aula == 1));
        verify(alunos, never()).save(any());
    }

    @Test
    void janelaIncluiOComecoEOFimDaJanela() {
        alunoComCartao("ATIVO");
        assertTrue(servicoAs("07:40:00").registrarLeitura("ABC123").aceita());
        assertTrue(servicoAs("07:50:00").registrarLeitura("ABC123").aceita());
    }

    @Test
    void leituraNoComecoOuMeioDaAulaEhRecusada() {
        alunoComCartao("ATIVO");
        for (String hora : new String[] {"07:00:00", "07:20:00", "07:39:59"}) {
            var resultado = servicoAs(hora).registrarLeitura("ABC123");
            assertFalse(resultado.aceita(), hora);
            assertEquals("FORA_DO_HORARIO", resultado.situacao(), hora);
        }
        verify(presencas, never()).save(any(Presenca.class));
    }

    @Test
    void intervaloEForaDoExpedienteSaoRecusados() {
        alunoComCartao("ATIVO");
        for (String hora : new String[] {"09:40:00", "09:55:00", "11:31:00", "14:00:00", "06:30:00"}) {
            assertEquals("FORA_DO_HORARIO", servicoAs(hora).registrarLeitura("ABC123").situacao(), hora);
        }
        verify(presencas, never()).save(any(Presenca.class));
    }

    @Test
    void quartaAulaDepoisDoIntervaloEPermitida() {
        alunoComCartao("ATIVO");
        assertTrue(servicoAs("10:35:00").registrarLeitura("ABC123").aceita());
        verify(presencas).save(argThat(p -> p.numero_aula == 4));
    }

    @Test
    void segundaLeituraNaMesmaAulaEhDuplicada() {
        alunoComCartao("ATIVO");
        when(presencas.existeNaAula(eq(2026001), eq(1), any(), any())).thenReturn(true);
        var resultado = servicoAs("07:45:00").registrarLeitura("ABC123");
        assertFalse(resultado.aceita());
        assertEquals("DUPLICADA", resultado.situacao());
        verify(presencas, never()).save(any(Presenca.class));
    }

    @Test
    void presencaEmOutraAulaNaoBloqueiaAProxima() {
        alunoComCartao("ATIVO");
        when(presencas.existeNaAula(eq(2026001), eq(1), any(), any())).thenReturn(true);
        assertTrue(servicoAs("08:35:00").registrarLeitura("ABC123").aceita());
        verify(presencas).save(argThat(p -> p.numero_aula == 2));
    }

    @Test
    void cartaoDesconhecidoNaoGrava() {
        var resultado = servicoAs("07:45:00").registrarLeitura("INEXISTENTE");
        assertFalse(resultado.aceita());
        assertEquals("DESCONHECIDO", resultado.situacao());
        verifyNoInteractions(alunos, presencas);
    }

    @Test
    void cartaoBloqueadoNaoGrava() {
        alunoComCartao("BLOQUEADO");
        var resultado = servicoAs("07:45:00").registrarLeitura("ABC123");
        assertFalse(resultado.aceita());
        assertEquals("BLOQUEADO", resultado.situacao());
        verify(alunos, never()).buscarParaAtualizar(anyInt());
        verifyNoInteractions(presencas);
    }

    @Test
    void uidVazioNaoConsultaBanco() {
        var servico = servicoAs("07:45:00");
        assertEquals("INVALIDA", servico.registrarLeitura(null).situacao());
        assertEquals("INVALIDA", servico.registrarLeitura(" ").situacao());
        verifyNoInteractions(cartoes, alunos, presencas);
    }

    @Test
    void alunoInexistenteNaoGrava() {
        alunoComCartao("ATIVO");
        when(alunos.buscarParaAtualizar(2026001)).thenReturn(Optional.empty());
        assertEquals("ALUNO_INEXISTENTE", servicoAs("07:45:00").registrarLeitura("ABC123").situacao());
        verifyNoInteractions(presencas);
    }
}
