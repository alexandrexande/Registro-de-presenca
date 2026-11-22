from machine import Pin
from mfrc522 import MFRC522
import network
import time
import ujson
from umqtt.simple import MQTTClient


SSID = "Wokwi-GUEST"
WIFI_PASSWORD = ""

MQTT_CLIENT_ID = "esp32_rfid_client"
MQTT_BROKER = "7a323450744041aaa8ea29e6eec6ace9.s1.eu.hivemq.cloud" 
MQTT_PORT = 8883

MQTT_USER = "registeriot6767"        
MQTT_PASSWORD = "auramaisego67"        

MQTT_TOPIC_ENVIO = "escola/rfid/leituras"     # ESP32 envia para o Back
MQTT_TOPIC_RESPOSTA = "escola/rfid/resposta"  # ESP32 escuta do Back

SCK_PIN = 18
MOSI_PIN = 23
MISO_PIN = 19
RST_PIN = 21
SDA_PIN = 5

rdr = MFRC522(
    SCK_PIN,
    MOSI_PIN,
    MISO_PIN,
    RST_PIN,
    SDA_PIN
)

led_verde = Pin(25, Pin.OUT)
led_vermelho = Pin(26, Pin.OUT)

led_verde.value(0)
led_vermelho.value(0)

ultimo_uid = None
ultimo_tempo_leitura = 0
INTERVALO_RELEITURA = 8

ultimo_log_erro = 0
INTERVALO_LOG_ERRO = 5


def conectar_wifi():
    print("Conectando ao WiFi...")
    wifi = network.WLAN(network.STA_IF)
    wifi.active(True)
    
    tentativas = 0
    max_tentativas_gerais = 5

    while not wifi.isconnected():
        wifi.connect(SSID, WIFI_PASSWORD)
        print("Tentando estabelecer conexão...")
        
        # Aguarda até 5 segundos por tentativa
        inicio = time.time()
        while time.time() - inicio < 5:
            if wifi.isconnected():
                break
            time.sleep(0.5)
            
        if wifi.isconnected():
            break
            
        tentativas += 1
        print(f"Tentativa {tentativas} falhou. Reiniciando interface de rede...")
        
        # Reinicia o rádio Wi-Fi do ESP32 para destravar o simulador
        wifi.active(False)
        time.sleep(1)
        wifi.active(True)
        
        if tentativas >= max_tentativas_gerais:
            print("AVISO: Dificuldade extrema na rede. Tentando novamente em loop...")
            tentativas = 0 

    print("WiFi conectado com sucesso!")
    # print("Configuração de rede:", wifi.ifconfig())


# PROCEDIMENTO QUANDO O BACK RESPONDER

def mqtt_callback(topic, msg):
    print("\n[MQTT] Resposta recebida do Backend:")
    print("Tópico:", topic.decode())
    print("Mensagem:", msg.decode())

    try:
        resposta = ujson.loads(msg)
        
        autorizado = resposta.get("autorizado", False)
        nome_aluno = resposta.get("aluno", "DESCONHECIDO")

        if autorizado:
            print(f"-> Acesso LIBERADO para: {nome_aluno}")
            led_verde.value(1)
            time.sleep(2)
            led_verde.value(0)
        else:
            print(f"-> Acesso NEGADO para o UID informado.")
            led_vermelho.value(1)
            time.sleep(2)
            led_vermelho.value(0)

    except Exception as e:
        print("Erro ao processar JSON de resposta do backend:", e)


def conectar_mqtt():
    print("Conectando ao HiveMQ Cloud (SSL/TLS)...")

    client = MQTTClient(
        client_id=MQTT_CLIENT_ID,
        server=MQTT_BROKER,
        port=MQTT_PORT,
        user=MQTT_USER,
        password=MQTT_PASSWORD,
        keepalive=60,
        ssl=True, 
        ssl_params={'server_hostname': MQTT_BROKER}
    )

    client.set_callback(mqtt_callback)
    client.connect()
    
    client.subscribe(MQTT_TOPIC_RESPOSTA)
    
    print("MQTT conectado e inscrito no canal de resposta!")
    return client


def uid_to_str(uid):
    return ":".join("{:02X}".format(i) for i in uid[:4])

def checksum_uid_valido(uid):
    if uid is None:
        return False
    if len(uid) < 5:
        return False
    checksum = uid[0] ^ uid[1] ^ uid[2] ^ uid[3]
    return checksum == uid[4]

def obter_uid():
    stat, tag_type = rdr.request(rdr.REQALL)

    if stat != rdr.OK:
        return None, None, "sem_cartao"

    stat, uid = rdr.anticoll()

    if stat == rdr.OK and checksum_uid_valido(uid):
        uid_str = uid_to_str(uid)
        return uid_str, uid, "leitura_normal"

    if uid is not None:
        uid_parcial = tuple(uid[:3])
        
        uids_parciais_wokwi = {
            (1, 3, 4): "01:02:03:04",      
            (17, 51, 68): "11:22:33:44",   
            (85, 119, 204): "55:66:77:88", 
            (170, 204, 0): "AA:BB:CC:DD",  
            (192, 238, 72): "C0:FF:EE:99",
            (4, 17, 34): "04:11:22:33:44:55:66"
        }
        
        if uid_parcial in uids_parciais_wokwi:
            uid_str = uids_parciais_wokwi[uid_parcial]
            return uid_str, uid, "leitura_parcial_wokwi"

    return None, uid, "uid_incompleto"

# ENVIO DA LEITURA PARA O BACKEND VIA HIVEMQ

def enviar_leitura(client, uid_str, origem_leitura):
    try:
        payload = {
            "uid": uid_str,
            "origem_leitura": origem_leitura
        }
        mensagem = ujson.dumps(payload)
        
        print("--------------------------------")
        print("Enviando UID lido para o backend:", mensagem)
        client.publish(MQTT_TOPIC_ENVIO, mensagem)
        print("Aguardando veredito do backend...")
        
    except Exception as erro:
        print("Erro ao publicar leitura:", erro)

conectar_wifi()
client = conectar_mqtt()

print("Sistema iniciado (Modo Cliente/Servidor).")
print("Aproxime o cartão no leitor e aguarde...")

while True:
    
    try:
        client.check_msg()
    except OSError as e:
        if e.args[0] != -1:
            print("Erro MQTT real:", e)
    except Exception as e:
        print("Outro erro ao checar mensagens:", e)

    uid_str, uid_bruto, origem_leitura = obter_uid()
    tempo_atual = time.time()

    if uid_str is not None:
        if uid_str != ultimo_uid or tempo_atual - ultimo_tempo_leitura >= INTERVALO_RELEITURA:
            
            # Envia o UID para o backend processar
            enviar_leitura(client, uid_str, origem_leitura)

            ultimo_uid = uid_str
            ultimo_tempo_leitura = tempo_atual

        time.sleep(0.3)

    else:
        if origem_leitura == "uid_incompleto":
            if tempo_atual - ultimo_log_erro >= INTERVALO_LOG_ERRO:
                print("Cartão detectado, mas leitura incompleta.")
                print("UID bruto recebido:", uid_bruto)
                ultimo_log_erro = tempo_atual

                led_vermelho.value(1)
                time.sleep(2)
                led_vermelho.value(0)

            print("Aproxime o cartão no leitor e aguarde...")

        rdr.init()
        time.sleep(0.3)

    time.sleep(0.1)