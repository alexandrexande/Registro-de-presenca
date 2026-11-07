package registrodepresenca.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import registrodepresenca.backend.service.PresencaService;

@RestController
@RequestMapping("/api/presencas")
public class PresencaController {

    private final PresencaService presencaService;

    public PresencaController(PresencaService presencaService) {
        this.presencaService = presencaService;
    }

    @PostMapping("/leitura")
    public PresencaService.Resultado registrar(@RequestBody(required = false) Leitura leitura) {
        return presencaService.registrarLeitura(leitura == null ? null : leitura.uid());
    }

    public record Leitura(String uid) {}
}
