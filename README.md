# Sistema de Registro de Presença

Sistema de registro de presença escolar utilizando cartões RFID, ESP32, comunicação MQTT, backend Java com Spring Boot, banco de dados PostgreSQL e interface web.

O ESP32 realiza a leitura do cartão RFID e envia o UID ao backend por meio do HiveMQ. O backend valida o cartão, o aluno e o horário da aula, registra a presença no banco de dados e envia o resultado de volta ao dispositivo. A interface web permite que usuários autenticados acompanhem as presenças das turmas às quais têm acesso.

## Tecnologias utilizadas

- **Dispositivo:** ESP32.
- **Leitor RFID:** RC522, utilizando a biblioteca MFRC522.
- **Firmware:** MicroPython.
- **Comunicação:** MQTT com HiveMQ Cloud.
- **Backend:** Java 21 e Spring Boot.
- **Banco de dados:** PostgreSQL.
- **Frontend:** HTML, CSS e JavaScript.
- **Documentação da API:** Swagger / OpenAPI.
- **Relatórios:** integração com Google Sheets.

## Estrutura do projeto

```text
Registro-de-presenca/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/registrodepresenca/backend/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── entity/
│   │   │   │   ├── integracao/
│   │   │   │   ├── mqtt/
│   │   │   │   ├── repository/
│   │   │   │   ├── seguranca/
│   │   │   │   └── service/
│   │   │   └── resources/
│   │   └── test/java/registrodepresenca/backend/
│   │       ├── controller/
│   │       ├── integracao/
│   │       │   └── MontadorRelatorioMensalTest.java
│   │       └── service/
│   ├── build.gradle
│   └── gradlew
├── firmware/
│   ├── main.py
│   ├── mfrc522.py
│   ├── diagram.json
│   ├── libraries.txt
│   └── wokwi-project.txt
├── frontend/
│   ├── index.html
│   ├── css/style.css
│   └── js/app.js
└── README.md
```

## Funcionalidades

### Registro por RFID

1. O RC522 conectado ao ESP32 identifica o UID do cartão.
2. O firmware envia o UID ao tópico MQTT `escola/rfid/leituras`.
3. O backend recebe a mensagem e processa a leitura.
4. O sistema verifica se o cartão está cadastrado e ativo, se o aluno existe e se a leitura está dentro da janela permitida de uma aula.
5. Quando a leitura é aceita, a presença é gravada no PostgreSQL com a data, horário e número da aula.
6. O resultado é publicado no tópico `escola/rfid/resposta`; o ESP32 sinaliza a autorização usando os LEDs.

A configuração atual considera uma janela de **10 minutos antes do fim de cada aula**. Uma nova presença do mesmo aluno na mesma aula e no mesmo dia é tratada como duplicada.

### Gerenciamento de turmas e alunos

O backend disponibiliza operações para consultar turmas, consultar alunos e consultar presenças. O cadastro de alunos e a associação de cartões são operações administrativas.

### Autenticação e permissões

A aplicação possui autenticação por usuário e senha, com senha protegida por BCrypt e token com validade configurada. Administradores possuem acesso administrativo; professores visualizam as turmas associadas aos seus usuários. As rotas de cadastro e alteração administrativa exigem perfil de administrador.

### Interface web

A interface permite entrar no sistema, visualizar as turmas vinculadas ao usuário, abrir a chamada de uma turma, consultar presenças por data e aula e filtrar alunos presentes ou ausentes. A tela atualiza os dados periodicamente.

### Relatório mensal

O backend possui integração com Google Sheets para gerar/atualizar o relatório mensal de presenças. A integração depende das credenciais de serviço do Google e do identificador da planilha configurados no ambiente.

## Configuração

Antes de executar, configure os serviços e credenciais necessários sem inserir segredos diretamente no repositório.

### 1. Banco de dados

Configure a conexão PostgreSQL em `backend/src/main/resources/application.properties`, preferencialmente por variáveis de ambiente ou configuração externa.

A aplicação utiliza `spring.jpa.hibernate.ddl-auto=validate`; portanto, o banco precisa ter previamente as tabelas e colunas esperadas pelas entidades do projeto, incluindo a estrutura de aulas e o número da aula associado à presença.

### 2. MQTT / HiveMQ

Configure no `application.properties` os dados do broker HiveMQ:

- endereço do broker;
- porta TLS;
- usuário e senha;
- tópico de entrada: `escola/rfid/leituras`;
- tópico de resposta: `escola/rfid/resposta`.

O firmware em `firmware/main.py` também precisa utilizar os dados correspondentes do broker e os mesmos tópicos. Não publique credenciais reais no GitHub.

### 3. Google Sheets

Configure as credenciais da conta de serviço do Google e o ID da planilha. O caminho do arquivo de credenciais pode ser informado pela variável de ambiente `GOOGLE_CREDENTIALS_PATH`.

Sem as credenciais do Google Sheets, o restante da aplicação pode funcionar, mas a geração do relatório mensal ficará indisponível.

### 4. Segredo de autenticação

Defina a variável de ambiente `AUTH_SECRET` com um valor longo e aleatório. A validade do token é configurada pela propriedade `auth.validade-horas` (8 horas na configuração atual).

## Como executar o backend

É necessário ter o Java 21 instalado.

No Windows, abra o terminal na pasta `backend` e execute:

```powershell
.\gradlew.bat bootRun
```

No Linux ou macOS:

```bash
./gradlew bootRun
```

Por padrão, o backend utiliza a porta `8081`.

A interface web é disponibilizada pelo próprio backend. Depois de iniciar a aplicação, acesse:

```text
http://localhost:8081/
```

A documentação interativa da API fica disponível em:

```text
http://localhost:8081/swagger-ui/index.html
```

## Testes

Para executar todos os testes automatizados, entre na pasta `backend` e execute:

```powershell
.\gradlew.bat test
```

No Linux ou macOS:

```bash
./gradlew test
```

O teste do montador do relatório mensal está localizado no pacote correspondente ao backend:

```text
backend/src/test/java/registrodepresenca/backend/integracao/MontadorRelatorioMensalTest.java
```

## Principais rotas da API

As rotas abaixo representam os recursos principais da aplicação. As permissões exigidas dependem do usuário autenticado.

| Método | Rota | Finalidade |
|---|---|---|
| `POST` | `/api/login` | Autenticar usuário |
| `GET` | `/api/turmas` | Listar turmas disponíveis ao usuário |
| `GET` | `/api/alunos` | Listar alunos dentro do escopo de acesso |
| `GET` | `/api/alunos/turma/{idTurma}` | Listar alunos de uma turma |
| `POST` | `/api/alunos` | Cadastrar aluno (administrador) |
| `POST` | `/api/cartoes` | Associar cartão a aluno (administrador) |
| `GET` | `/api/cartoes/{uid}` | Consultar cartão |
| `POST` | `/api/presencas/leitura` | Processar leitura de cartão |
| `GET` | `/api/presencas/turma/{idTurma}` | Consultar presenças de uma turma |
| `GET` | `/api/presencas/aluno/{matricula}` | Consultar presenças de um aluno por período |
| `GET` | `/api/aulas` | Listar horários das aulas |
| `PUT` | `/api/aulas/{numero}` | Alterar horário de aula (administrador) |
| `POST` | `/api/relatorios/mensal` | Gerar relatório mensal |

Para detalhes dos parâmetros, formatos de requisição e respostas, consulte o Swagger.

## Firmware e simulação

A pasta `firmware` contém o código MicroPython do ESP32, a implementação do driver MFRC522 e os arquivos utilizados na simulação do circuito no Wokwi. Os pinos do RC522 e dos LEDs estão definidos em `firmware/main.py`; o diagrama do circuito está em `firmware/diagram.json`.

## Observações

- Não versione senhas, tokens, arquivos de credenciais ou outros segredos.
- O backend depende de um PostgreSQL configurado com o esquema compatível com as entidades atuais.
- O relatório mensal depende da configuração válida do Google Sheets.
- Os horários das aulas e a janela de leitura devem estar consistentes com a grade cadastrada no banco.
