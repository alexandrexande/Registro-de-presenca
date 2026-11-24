package registrodepresenca.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import registrodepresenca.backend.entity.Presenca;
import registrodepresenca.backend.repository.PresencaRepository;
import registrodepresenca.backend.seguranca.AcessoTurma;
import registrodepresenca.backend.seguranca.Sessao;

@RestController
@RequestMapping("/api/presencas")
public class ConsultaPresencaController {

    private final PresencaRepository presencas;

    private final AcessoTurma acesso;

    public ConsultaPresencaController(PresencaRepository presencas, AcessoTurma acesso) {
        this.presencas = presencas;
        this.acesso = acesso;
    }

    @GetMapping("/aluno/{matricula}")
    public List<Presenca> porAluno(
            @PathVariable int matricula,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim,
            HttpServletRequest request) {
        acesso.conferirAluno(Sessao.de(request), matricula);
        if (inicio.isAfter(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Início deve ser anterior ao fim");
        }
        return presencas.buscarPorAlunoNoPeriodo(matricula, inicio, fim);
    }

    @GetMapping("/turma/{idTurma}")
    public List<Presenca> porTurma(
            @PathVariable int idTurma,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            HttpServletRequest request) {
        acesso.conferirTurma(Sessao.de(request), idTurma);
        ZoneId fuso = ZoneId.of("America/Maceio");
        OffsetDateTime inicio = dia.atStartOfDay(fuso).toOffsetDateTime();
        OffsetDateTime fim = dia.plusDays(1).atStartOfDay(fuso).toOffsetDateTime();
        return presencas.buscarPorTurmaNoDia(idTurma, inicio, fim);
    }
}
