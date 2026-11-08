package registrodepresenca.backend.controller;

import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import registrodepresenca.backend.integracao.RelatorioSheetsService;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private static final Logger log = LoggerFactory.getLogger(RelatorioController.class);

    private final RelatorioSheetsService relatorioService;

    public RelatorioController(RelatorioSheetsService relatorioService) {
        this.relatorioService = relatorioService;
    }

    // Exemplos: POST /api/relatorios/mensal            (mês atual)
    //           POST /api/relatorios/mensal?mes=2026-11
    @PostMapping("/mensal")
    public ResponseEntity<?> gerarMensal(@RequestParam(required = false) String mes) {
        YearMonth alvo;
        try {
            alvo = (mes == null || mes.isBlank())
                    ? YearMonth.now(ZoneId.of("America/Maceio"))
                    : YearMonth.parse(mes.trim());
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erro", "Use o formato AAAA-MM, por exemplo 2026-11"));
        }

        try {
            return ResponseEntity.ok(relatorioService.gerarMensal(alvo));
        } catch (IOException e) {
            log.error("Falha ao gerar o relatório mensal de {}", alvo, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("erro", "Falha ao atualizar a planilha: " + e.getMessage()));
        }
    }
}