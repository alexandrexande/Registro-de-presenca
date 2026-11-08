package registrodepresenca.backend.mqtt;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Component;

import registrodepresenca.backend.service.PresencaService;

@Component
public class MqttMessageHandler {

    private final PresencaService presencaService;
    private final MqttPublisher publisher;
    private final JsonMapper mapper;

    public MqttMessageHandler(
            PresencaService presencaService,
            MqttPublisher publisher,
            JsonMapper mapper) {

        this.presencaService = presencaService;
        this.publisher = publisher;
        this.mapper = mapper;
    }

    public void processar(String payload) {

        try {

            // Converte o JSON recebido
            JsonNode json = mapper.readTree(payload);

            // Extrai o UID do cartão
            String uid = json.hasNonNull("uid")
                    ? json.get("uid").asText()
                    : null;

            System.out.println(
                "[MQTT] UID recebido: " + uid
            );

            // Executa a regra de negócio existente
            PresencaService.Resultado resultado =
                    presencaService.registrarLeitura(uid);

            // Monta a resposta
            ObjectNode resposta = mapper.createObjectNode();

            resposta.put(
                "autorizado",
                resultado.aceita()
            );

            resposta.put(
                "situacao",
                resultado.situacao()
            );

            resposta.put(
                "mensagem",
                resultado.mensagem()
            );

            resposta.put(
                "aluno",
                resultado.aluno() != null
                    ? resultado.aluno()
                    : "DESCONHECIDO"
            );

            if (resultado.dataHora() != null) {
                resposta.put(
                    "dataHora",
                    resultado.dataHora().toString()
                );
            }

            // Publica o resultado para o ESP32
            publisher.publicar(
                mapper.writeValueAsString(resposta)
            );

            System.out.println(
                "[MQTT] Resultado: " + resultado.mensagem()
            );

        } catch (Exception e) {

            System.err.println(
                "[MQTT] Erro ao processar mensagem: "
                + e.getMessage()
            );

            // Informa ao ESP32 que a leitura não foi processada
            try {
                ObjectNode erro = mapper.createObjectNode();

                erro.put("autorizado", false);
                erro.put("situacao", "ERRO");
                erro.put("mensagem", "Erro ao processar leitura");
                erro.put("aluno", "DESCONHECIDO");

                publisher.publicar(
                    mapper.writeValueAsString(erro)
                );

            } catch (Exception ex) {
                System.err.println(
                    "[MQTT] Erro ao enviar resposta: "
                    + ex.getMessage()
                );
            }
        }
    }
}