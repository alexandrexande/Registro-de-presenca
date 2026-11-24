package registrodepresenca.backend.controller;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import registrodepresenca.backend.entity.Aula;
import registrodepresenca.backend.repository.AulaRepository;
import registrodepresenca.backend.seguranca.Sessao;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AulaControllerTest {

    private AulaRepository aulas;
    private MockMvc mvc;
    private Aula primeira;
    private Aula segunda;

    private static Aula aula(int numero, String inicio, String fim) {
        Aula aula = new Aula();
        aula.numero = numero;
        aula.nome = numero + "ª aula";
        aula.inicio = LocalTime.parse(inicio);
        aula.fim = LocalTime.parse(fim);
        return aula;
    }

    @BeforeEach
    void preparar() {
        aulas = mock(AulaRepository.class);
        primeira = aula(1, "07:00", "07:50");
        segunda = aula(2, "07:50", "08:40");
        when(aulas.listarEmOrdem()).thenReturn(List.of(primeira, segunda));
        when(aulas.findAll()).thenReturn(List.of(primeira, segunda));
        when(aulas.findById(1)).thenReturn(Optional.of(primeira));
        when(aulas.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        mvc = MockMvcBuilders.standaloneSetup(new AulaController(aulas, 10))
                .setControllerAdvice(new ErroController()).build();
    }

    private static String corpo(String inicio, String fim) {
        return "{\"numero\":1,\"nome\":\"1ª aula\",\"inicio\":\"" + inicio + "\",\"fim\":\"" + fim + "\"}";
    }

    @Test
    void qualquerUsuarioListaAGradeComAJanela() throws Exception {
        mvc.perform(get("/api/aulas").requestAttr(Sessao.ATRIBUTO, new Sessao.Usuario(2, "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inicio").value("07:00"))
                .andExpect(jsonPath("$[0].janelaInicio").value("07:40"))
                .andExpect(jsonPath("$[0].janelaMinutos").value(10));
    }

    @Test
    void professorNaoAlteraHorario() throws Exception {
        mvc.perform(put("/api/aulas/1").requestAttr(Sessao.ATRIBUTO, new Sessao.Usuario(2, "PROFESSOR"))
                .contentType(MediaType.APPLICATION_JSON).content(corpo("07:10", "07:50")))
                .andExpect(status().isForbidden());
        verify(aulas, never()).saveAndFlush(any());
    }

    @Test
    void adminAlteraEValidaOHorario() throws Exception {
        var admin = new Sessao.Usuario(1, "ADMIN");
        mvc.perform(put("/api/aulas/1").requestAttr(Sessao.ATRIBUTO, admin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo("07:10", "07:50")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.inicio").value("07:10"));
        mvc.perform(put("/api/aulas/1").requestAttr(Sessao.ATRIBUTO, admin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo("08:00", "07:50")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/aulas/1").requestAttr(Sessao.ATRIBUTO, admin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo("abc", "07:50")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/aulas/1").requestAttr(Sessao.ATRIBUTO, admin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo("07:00", "08:00")))
                .andExpect(status().isConflict());
    }
}
