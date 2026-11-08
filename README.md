# Colinha dos testes

Swagger: http://localhost:8081/swagger-ui/index.html

Em cada rota, clique em **Try it out**, preencha os campos e clique em **Execute**.

Para um cadastro novo, siga a ordem abaixo: turma → aluno → cartão → leitura.
Os cadastros e as leituras aceitas ficam salvos no banco real.

## 1. Listar turmas

**GET `/api/turmas`**

É uma lista, só clicar em **Execute**. Confira se a turma `17` existe antes de cadastrar o aluno.

## 2. Cadastrar aluno

**POST `/api/alunos`**

```json
{
  "matricula": 444444444,
  "idTurma": 17,
  "nome": "maluco"
}
```

Pode dar qualquer nome. A matrícula precisa ser nova e estar entre `1` e `2147483647`.
Não precisa ter 10 números. `2222222222` não funciona porque ultrapassa o limite de `int`.
Se trocar a matrícula, use a mesma no cadastro do cartão.

- `201`: cadastrado.
- `400`: dados inválidos.
- `404`: turma não existe.
- `409`: matrícula já cadastrada.

## 3. Listar alunos

**GET `/api/alunos`**

É uma lista, só clicar em **Execute**.

## 4. Listar alunos por turma

**GET `/api/alunos/turma/{idTurma}`**

```text
idTurma: 17
```

## 5. Cadastrar cartão

**POST `/api/cartoes`**

```json
{
  "matricula": 444444444,
  "uid": "Teste-macaco"
}
```

O aluno precisa existir e estar sem cartão associado. O UID também precisa ser novo.

- `201`: cartão criado e associado ao aluno.
- `400`: dados inválidos.
- `404`: aluno não existe.
- `409`: UID já cadastrado, aluno já tem cartão ou outro conflito com o banco.

Se já cadastrou, não precisa cadastrar de novo: use a consulta ou registre uma leitura.

## 6. Consultar cartão

**GET `/api/cartoes/{uid}`**

```text
uid: Teste-macaco
```

Retorna os dados do cartão. Se não existir, retorna `404`.

## 7. Registrar leitura

**POST `/api/presencas/leitura`**

```json
{
  "uid": "Teste-macaco"
}
```

- Primeira leitura aceita do dia: `ENTRADA`.
- Repetir em menos de 30 segundos: `DUPLICADA`, sem gravar outra presença.
- Nova leitura depois do intervalo: `SAIDA`.
- Cartão inexistente: `DESCONHECIDO`.
- Cartão que não está ativo: `BLOQUEADO`.

## 8. Presenças por turma e dia

**GET `/api/presencas/turma/{idTurma}`**

```text
idTurma: 11
dia: 2026-11-02
```

Esse exemplo só retorna dados se houver presenças de alunos dessa turma nesse dia.
Para ver o aluno cadastrado acima, use a turma `17` e a data em que fez a leitura.
`200` com `[]` significa que não encontrou presenças para os filtros informados.

## 9. Presenças por aluno e período

**GET `/api/presencas/aluno/{matricula}`**

```text
matricula: 1900000101
inicio: 2025-11-02T00:00:00-03:00
fim: 2026-12-25T23:59:59.999999-03:00
```

Os horários precisam estar completos, incluindo o fuso `-03:00`.
Para consultar o aluno cadastrado acima, troque a matrícula por `444444444`
e use um período que inclua o dia da leitura.
