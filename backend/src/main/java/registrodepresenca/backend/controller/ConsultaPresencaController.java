package registrodepresenca.backend.controller;

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

@RestController
@RequestMapping("/api/presencas")
public class ConsultaPresencaController {

    private final PresencaRepository presencas;

    public ConsultaPresencaController(PresencaRepository presencas) {
        this.presencas = presencas;
    }

    @GetMapping("/aluno/{matricula}")
    public List<Presenca> porAluno(
            @PathVariable int matricula,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim) {
        if (inicio.isAfter(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Início deve ser anterior ao fim");
        }
        return presencas.buscarPorAlunoNoPeriodo(matricula, inicio, fim);
    }

    @GetMapping("/turma/{idTurma}")
    public List<Presenca> porTurma(
            @PathVariable int idTurma,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia) {
        ZoneId fuso = ZoneId.of("America/Maceio");
        OffsetDateTime inicio = dia.atStartOfDay(fuso).toOffsetDateTime();
        OffsetDateTime fim = dia.plusDays(1).atStartOfDay(fuso).toOffsetDateTime();
        return presencas.buscarPorTurmaNoDia(idTurma, inicio, fim);
    }
}
