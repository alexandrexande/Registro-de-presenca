package registrodepresenca.backend.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MqttPublisher {

    @Value("${mqtt.topic.resposta}")
    private String topicoResposta;

    private volatile Mqtt3AsyncClient client;

    public void definirCliente(Mqtt3AsyncClient client) {
        this.client = client;
    }

    public void publicar(String mensagem) {

        if (client == null || !client.getState().isConnected()) {

            System.err.println(
                "[MQTT] Não foi possível publicar: cliente desconectado."
            );

            return;
        }

        client.publishWith()
                .topic(topicoResposta)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(
                    mensagem.getBytes(StandardCharsets.UTF_8)
                )
                .send()
                .whenComplete((resultado, erro) -> {

                    if (erro != null) {

                        System.err.println(
                            "[MQTT] Erro ao publicar: "
                            + erro.getMessage()
                        );

                    } else {

                        System.out.println(
                            "[MQTT] Resposta publicada em: "
                            + topicoResposta
                        );
                    }
                });
    }
}