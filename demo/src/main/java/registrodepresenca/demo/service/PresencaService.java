package registrodepresenca.demo.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import registrodepresenca.demo.classes.Aluno;
import registrodepresenca.demo.classes.Cartao;
import registrodepresenca.demo.classes.Presenca;
import registrodepresenca.demo.repository.AlunoRepository;
import registrodepresenca.demo.repository.CartaoRepository;
import registrodepresenca.demo.repository.PresencaRepository;

@Service
public class PresencaService {

    private final CartaoRepository cartaoRepository;
    private final AlunoRepository alunoRepository;
    private final PresencaRepository presencaRepository;

    public PresencaService(
            CartaoRepository cartaoRepository,
            AlunoRepository alunoRepository,
            PresencaRepository presencaRepository) {
        this.cartaoRepository = cartaoRepository;
        this.alunoRepository = alunoRepository;
        this.presencaRepository = presencaRepository;
    }

    @Transactional
    public Resultado registrarLeitura(String uid) {
        if (uid == null || uid.isBlank()) {
            return negada("UID não informado");
        }

        Cartao cartao = cartaoRepository.findByUid(uid.trim()).orElse(null);
        if (cartao == null) {
            return negada("Cartao naoo cadastrado");
        }
        if (cartao.situacao == null || !"ATIVO".equalsIgnoreCase(cartao.situacao.trim())) {
            return negada("Cartao não  ativo");
        }

        Aluno aluno = alunoRepository.findById(cartao.matricula_aluno).orElse(null);
        if (aluno == null) {
            return negada("Alugo nao encontrado");
        }

        ZoneId fuso = ZoneId.systemDefault();

        OffsetDateTime agora = OffsetDateTime.now(fuso);

        LocalDate hoje = agora.toLocalDate();

        OffsetDateTime inicio = hoje.atStartOfDay(fuso).toOffsetDateTime();

        OffsetDateTime fim = hoje.plusDays(1).atStartOfDay(fuso).toOffsetDateTime();

        boolean jaEntrouHoje =
                presencaRepository.existeNoIntervalo(aluno.matricula_aluno, inicio, fim);
        String situacao = jaEntrouHoje ? "SAIDA" : "ENTRADA";

        Presenca presenca = new Presenca();
        presenca.matricula_aluno = aluno.matricula_aluno;
        presenca.id_cartao = cartao.id_cartao;
        presenca.data_hora = agora;
        presenca.situacao = situacao;
        presencaRepository.save(presenca);

        if (jaEntrouHoje) {
            aluno.horasaida = agora.toLocalDateTime();
        } else {
            aluno.horaentrada = agora.toLocalDateTime();
            aluno.horasaida = null;
        }

        alunoRepository.save(aluno);

        String mensagem = jaEntrouHoje ? "Saída registrada" : "Entrada registrada";
        return new Resultado(true, situacao, mensagem, aluno.nome_aluno, agora);
    }

    private Resultado negada(String mensagem) {
        return new Resultado(false, "NEGADA", mensagem, null, null);
    }

    public record Resultado(
            boolean aceita,
            String situacao,
            String mensagem,
            String aluno,
            OffsetDateTime dataHora) {}
}
