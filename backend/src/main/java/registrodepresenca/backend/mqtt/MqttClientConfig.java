package registrodepresenca.backend.mqtt;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class MqttClientConfig {

    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.port:8883}")
    private int port;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.topic.entrada}")
    private String topicoEntrada;

    private final MqttMessageHandler handler;
    private final MqttPublisher publisher;

    private Mqtt3AsyncClient client;

    public MqttClientConfig(
            MqttMessageHandler handler,
            MqttPublisher publisher) {

        this.handler = handler;
        this.publisher = publisher;
    }

    @PostConstruct
    public void conectar() {

        client = MqttClient.builder()
                .useMqttVersion3()
                .identifier(
                    "backend-presenca-" + UUID.randomUUID()
                )
                .serverHost(broker)
                .serverPort(port)
                .sslWithDefaultConfig()
                .automaticReconnectWithDefaultConfig()
                .buildAsync();

        publisher.definirCliente(client);

        client.connectWith()
                .simpleAuth()
                    .username(username)
                    .password(
                        password.getBytes(StandardCharsets.UTF_8)
                    )
                    .applySimpleAuth()
                .send()
                .whenComplete((ack, erro) -> {

                    if (erro != null) {
                        System.err.println(
                            "[MQTT] Falha na conexão: "
                            + erro.getMessage()
                        );
                        return;
                    }

                    System.out.println(
                        "[MQTT] Conectado ao HiveMQ Cloud!"
                    );

                    inscreverNoTopico();
                });
    }

    private void inscreverNoTopico() {

        client.subscribeWith()
                .topicFilter(topicoEntrada)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(mensagem -> {

                    String payload = new String(
                        mensagem.getPayloadAsBytes(),
                        StandardCharsets.UTF_8
                    );

                    System.out.println(
                        "[MQTT] Mensagem recebida: " + payload
                    );

                    handler.processar(payload);
                })
                .send()
                .whenComplete((ack, erro) -> {

                    if (erro != null) {
                        System.err.println(
                            "[MQTT] Falha na inscrição: "
                            + erro.getMessage()
                        );
                    } else {
                        System.out.println(
                            "[MQTT] Inscrito em: " + topicoEntrada
                        );
                    }
                });
    }

    @PreDestroy
    public void desconectar() {

        try {
            if (client != null && client.getState().isConnected()) {
                client.disconnect().join();

                System.out.println(
                    "[MQTT] Cliente desconectado."
                );
            }
        } catch (Exception e) {
            System.err.println(
                "[MQTT] Erro ao desconectar: " + e.getMessage()
            );
        }
    }
}