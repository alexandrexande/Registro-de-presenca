package registrodepresenca.backend.controller;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.entity.Cartao;
import registrodepresenca.backend.entity.Turma;
import registrodepresenca.backend.repository.*;
import registrodepresenca.backend.seguranca.AcessoTurma;
import registrodepresenca.backend.seguranca.Sessao;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CadastroControllerTest {

    private AlunoRepository alunos;
    private TurmaRepository turmas;
    private CartaoRepository cartoes;
    private PresencaRepository presencas;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        alunos = mock(AlunoRepository.class);
        turmas = mock(TurmaRepository.class);
        cartoes = mock(CartaoRepository.class);
        presencas = mock(PresencaRepository.class);
        AcessoTurma acesso = new AcessoTurma(turmas, alunos);
        mvc = MockMvcBuilders.standaloneSetup(
                new AlunoController(alunos, turmas, acesso), new CartaoController(cartoes, alunos, acesso),
                new TurmaController(turmas), new ConsultaPresencaController(presencas, acesso))
                .defaultRequest(get("/").requestAttr(Sessao.ATRIBUTO, new Sessao.Usuario(1, "ADMIN")))
                .setControllerAdvice(new ErroController()).build();
    }

    @Test
    void listaTurmaDoRepositorio() throws Exception {
        Turma turma = new Turma();
        turma.id_turma = 1;
        turma.nome = "TESTE";
        when(turmas.findAll()).thenReturn(List.of(turma));
        mvc.perform(get("/api/turmas")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("TESTE"));
    }

    @Test
    void professorNaoCadastraAlunoNemCartao() throws Exception {
        var professor = new Sessao.Usuario(2, "PROFESSOR");
        mvc.perform(post("/api/alunos").requestAttr(Sessao.ATRIBUTO, professor)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricula\":1,\"idTurma\":1,\"nome\":\"Aluno\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/cartoes").requestAttr(Sessao.ATRIBUTO, professor)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricula\":1,\"uid\":\"ABC\"}"))
                .andExpect(status().isForbidden());
        verify(alunos, never()).saveAndFlush(any());
        verify(cartoes, never()).saveAndFlush(any());
    }

    @Test
    void cadastroAlunoValidaDadosETurma() throws Exception {
        mvc.perform(post("/api/alunos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricula\":1,\"idTurma\":1,\"nome\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/alunos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricula\":1,\"idTurma\":1,\"nome\":\"Aluno\"}"))
                .andExpect(status().isNotFound());
        verify(alunos, never()).saveAndFlush(any());
    }

    @Test
    void cadastraAlunoERejeitaMatriculaExistente() throws Exception {
        when(turmas.findById(1)).thenReturn(Optional.of(new Turma()));
        when(alunos.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        String dados = "{\"matricula\":1,\"idTurma\":1,\"nome\":\"Aluno\"}";
        mvc.perform(post("/api/alunos").contentType(MediaType.APPLICATION_JSON).content(dados))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nome_aluno").value("Aluno"));
        when(alunos.existsById(1)).thenReturn(true);
        mvc.perform(post("/api/alunos").contentType(MediaType.APPLICATION_JSON).content(dados))
                .andExpect(status().isConflict());
    }

    @Test
    void cartaoExigeAlunoEUidUnico() throws Exception {
        String dados = "{\"matricula\":1,\"uid\":\"ABC\"}";
        mvc.perform(post("/api/cartoes").contentType(MediaType.APPLICATION_JSON).content(dados))
                .andExpect(status().isNotFound());
        when(alunos.buscarParaAtualizar(1)).thenReturn(Optional.of(new Aluno()));
        when(turmas.findById(0)).thenReturn(Optional.of(new Turma()));
        when(cartoes.findByUid("ABC")).thenReturn(Optional.of(new Cartao()));
        mvc.perform(post("/api/cartoes").contentType(MediaType.APPLICATION_JSON).content(dados))
                .andExpect(status().isConflict());
        verify(cartoes, never()).saveAndFlush(any());
    }

    @Test
    void associaCartaoAoAluno() throws Exception {
        Aluno aluno = new Aluno();
        aluno.matricula_aluno = 1;
        when(turmas.findById(0)).thenReturn(Optional.of(new Turma()));
        when(alunos.buscarParaAtualizar(1)).thenReturn(Optional.of(aluno));
        mvc.perform(post("/api/cartoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricula\":1,\"uid\":\" ABC \"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.uid").value("ABC"));
        verify(alunos).saveAndFlush(argThat(a -> "ABC".equals(a.uid)));
        verify(cartoes).saveAndFlush(argThat(c -> c.matricula_aluno == 1 && "ATIVO".equals(c.situacao)));
    }

    @Test
    void conflitoDoBancoRetorna409() throws Exception {
        when(turmas.findById(0)).thenReturn(Optional.of(new Turma()));
        when(alunos.buscarParaAtualizar(1)).thenReturn(Optional.of(new Aluno()));
        when(cartoes.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));
        mvc.perform(post("/api/cartoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricula\":1,\"uid\":\"ABC\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void consultaPorDiaUsaFusoDeMaceio() throws Exception {
        when(turmas.findById(1)).thenReturn(Optional.of(new Turma()));
        when(alunos.findById(1)).thenReturn(Optional.of(new Aluno()));
        mvc.perform(get("/api/presencas/turma/1").param("dia", "2026-11-07"))
                .andExpect(status().isOk());
        verify(presencas).buscarPorTurmaNoDia(eq(1),
                eq(java.time.OffsetDateTime.parse("2026-11-07T00:00:00-03:00")),
                eq(java.time.OffsetDateTime.parse("2026-11-08T00:00:00-03:00")));
        mvc.perform(get("/api/presencas/aluno/1")
                .param("inicio", "2026-11-08T00:00:00-03:00")
                .param("fim", "2026-11-07T00:00:00-03:00"))
                .andExpect(status().isBadRequest());
    }
}
