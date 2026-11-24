package registrodepresenca.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import registrodepresenca.backend.entity.Aula;
import registrodepresenca.backend.repository.AulaRepository;
import registrodepresenca.backend.seguranca.Sessao;
import registrodepresenca.backend.service.PresencaService;

@RestController
@RequestMapping("/api/aulas")
public class AulaController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final AulaRepository aulas;
    private final int janelaMinutos;

    public AulaController(AulaRepository aulas, @Value("${presenca.janela-minutos:10}") int janelaMinutos) {
        this.aulas = aulas;
        this.janelaMinutos = janelaMinutos;
    }

    @GetMapping
    public List<AulaDto> listar() {
        return aulas.listarEmOrdem().stream().map(this::paraDto).toList();
    }

    @PutMapping("/{numero}")
    public AulaDto alterar(@PathVariable int numero, @RequestBody AulaDto dados, HttpServletRequest request) {
        Sessao.exigirAdmin(request, "Só o administrador altera os horários das aulas");
        Aula aula = aulas.findById(numero).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada"));
        LocalTime inicio;
        LocalTime fim;
        try {
            inicio = LocalTime.parse(dados.inicio().trim());
            fim = LocalTime.parse(dados.fim().trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe início e fim no formato HH:mm");
        }
        if (!inicio.isBefore(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O início precisa ser antes do fim");
        }
        for (Aula outra : aulas.findAll()) {
            if (outra.numero != numero && inicio.isBefore(outra.fim) && outra.inicio.isBefore(fim)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "O horário se sobrepõe à " + outra.nome);
            }
        }
        if (dados.nome() != null && !dados.nome().isBlank()) {
            aula.nome = dados.nome().trim();
        }
        aula.inicio = inicio;
        aula.fim = fim;
        return paraDto(aulas.saveAndFlush(aula));
    }

    private AulaDto paraDto(Aula aula) {
        return new AulaDto(aula.numero, aula.nome, aula.inicio.format(HORA), aula.fim.format(HORA),
                PresencaService.inicioDaJanela(aula, janelaMinutos).format(HORA), janelaMinutos);
    }

    public record AulaDto(int numero, String nome, String inicio, String fim, String janelaInicio, int janelaMinutos) {}
}
