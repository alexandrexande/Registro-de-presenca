package registrodepresenca.backend.integracao;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.entity.Aula;
import registrodepresenca.backend.entity.Presenca;
import static org.junit.jupiter.api.Assertions.*;

class MontadorRelatorioMensalTest {

    private static final ZoneId FUSO = ZoneId.of("America/Maceio");

    private static Aula aula(int numero, String inicio, String fim) {
        Aula aula = new Aula();
        aula.numero = numero;
        aula.nome = numero + "ª aula";
        aula.inicio = LocalTime.parse(inicio);
        aula.fim = LocalTime.parse(fim);
        return aula;
    }

    private static Aluno aluno(int matricula, String nome) {
        Aluno aluno = new Aluno();
        aluno.matricula_aluno = matricula;
        aluno.nome_aluno = nome;
        return aluno;
    }

    private static Presenca presenca(int matricula, Integer aula, String quando) {
        Presenca p = new Presenca();
        p.matricula_aluno = matricula;
        p.numero_aula = aula;
        p.data_hora = OffsetDateTime.parse(quando);
        p.situacao = "PRESENTE";
        return p;
    }

    @Test
    void separaUmaColunaPorAulaComHorarioNoCabecalho() {
        var tabela = MontadorRelatorioMensal.montar(
                List.of(aluno(222, "Bruno"), aluno(111, "Ana")),
                List.of(presenca(111, 1, "2026-11-23T07:45:00-03:00"),
                        presenca(111, 2, "2026-11-23T08:35:00-03:00"),
                        presenca(111, 1, "2026-11-24T07:45:00-03:00"),
                        presenca(222, null, "2026-11-23T07:40:00-03:00")),
                YearMonth.of(2026, 11), FUSO,
                List.of(aula(1, "07:00", "07:50"), aula(2, "07:50", "08:40")));

        assertEquals(MontadorRelatorioMensal.LINHAS_DE_CABECALHO + 2, tabela.size());
        assertEquals(List.of("", "", "23/11", "", "24/11", "", "", ""), tabela.get(0));
        assertEquals(List.of("", "", "1ª aula", "2ª aula", "1ª aula", "2ª aula", "", ""), tabela.get(1));
        assertEquals(List.of("Matricula", "Nome", "07:00–07:50", "07:50–08:40", "07:00–07:50", "07:50–08:40",
                "Aulas assistidas", "Frequencia (%)"), tabela.get(2));
        // Ana: dia 23 nas duas aulas, dia 24 só na 1ª
        assertEquals(List.of(111, "Ana", "P", "P", "P", "F", 3, 75.0), tabela.get(3));
        // Bruno: presença antiga, sem aula, vale para as duas aulas do dia 23
        assertEquals(List.of(222, "Bruno", "P", "P", "F", "F", 2, 50.0), tabela.get(4));
    }

    @Test
    void semGradeUsaUmaColunaPorDia() {
        var tabela = MontadorRelatorioMensal.montar(
                List.of(aluno(111, "Ana")),
                List.of(presenca(111, null, "2026-11-23T07:45:00-03:00")),
                YearMonth.of(2026, 11), FUSO, List.of());
        assertEquals(List.of(111, "Ana", "P", 1, 100.0), tabela.get(3));
    }
}
