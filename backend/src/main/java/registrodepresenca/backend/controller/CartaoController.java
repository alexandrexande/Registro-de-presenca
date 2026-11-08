package registrodepresenca.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.entity.Cartao;
import registrodepresenca.backend.repository.AlunoRepository;
import registrodepresenca.backend.repository.CartaoRepository;

@RestController
@RequestMapping("/api/cartoes")
public class CartaoController {

    private final CartaoRepository cartoes;
    private final AlunoRepository alunos;

    public CartaoController(CartaoRepository cartoes, AlunoRepository alunos) {
        this.cartoes = cartoes;
        this.alunos = alunos;
    }

    @GetMapping("/{uid}")
    public Cartao buscar(@PathVariable String uid) {
        return cartoes.findByUid(uid.trim()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Cartão não encontrado"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Cartao cadastrar(@RequestBody CadastroCartao dados) {
        if (dados.matricula() <= 0 || dados.uid() == null || dados.uid().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe matrícula e UID");
        }
        Aluno aluno = alunos.buscarParaAtualizar(dados.matricula()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        String uid = dados.uid().trim();
        if (cartoes.findByUid(uid).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "UID já cadastrado");
        }
        if (aluno.uid != null && !aluno.uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aluno já possui um cartão associado");
        }
        Cartao cartao = new Cartao();
        cartao.matricula_aluno = aluno.matricula_aluno;
        cartao.uid = uid;
        cartao.situacao = "ATIVO";
        cartoes.saveAndFlush(cartao);
        aluno.uid = uid;
        alunos.saveAndFlush(aluno);
        return cartao;
    }

    public record CadastroCartao(int matricula, String uid) {}
}
