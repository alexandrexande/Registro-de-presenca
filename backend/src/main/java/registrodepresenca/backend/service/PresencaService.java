package registrodepresenca.backend.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.entity.Cartao;
import registrodepresenca.backend.entity.Presenca;
import registrodepresenca.backend.repository.AlunoRepository;
import registrodepresenca.backend.repository.CartaoRepository;
import registrodepresenca.backend.repository.PresencaRepository;

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
            return negada("INVALIDA", "UID não informado");
        }

        Cartao cartao = cartaoRepository.findByUid(uid.trim()).orElse(null);
        if (cartao == null) {
            return negada("DESCONHECIDO", "Cartao naoo cadastrado");
        }
        if (cartao.situacao == null || !"ATIVO".equalsIgnoreCase(cartao.situacao.trim())) {
            return negada("BLOQUEADO", "Cartao não  ativo");
        }

        Aluno aluno = alunoRepository.buscarParaAtualizar(cartao.matricula_aluno).orElse(null);
        if (aluno == null) {
            return negada("ALUNO_INEXISTENTE", "Alugo nao encontrado");
        }

        ZoneId fuso = ZoneId.of("America/Maceio");

        OffsetDateTime agora = OffsetDateTime.now(fuso);

        OffsetDateTime limite = agora.minusMinutes(45);
        OffsetDateTime ultimaPresenca = presencaRepository.ultimaLeituraGravada(cartao.id_cartao);
        if ((ultimaPresenca != null && ultimaPresenca.isAfter(limite))
                || (aluno.horasaida != null && aluno.horasaida.isAfter(limite.toLocalDateTime()))) {
            return negada("DUPLICADA", "Aguarde 45 minutos entre leituras do cartão");
        }

        LocalDate hoje = agora.toLocalDate();

        OffsetDateTime inicio = hoje.atStartOfDay(fuso).toOffsetDateTime();

        OffsetDateTime fim = hoje.plusDays(1).atStartOfDay(fuso).toOffsetDateTime();

        boolean jaEntrouHoje =
                presencaRepository.existeNoIntervalo(aluno.matricula_aluno, inicio, fim);
        String situacao = jaEntrouHoje ? "SAIDA" : "ENTRADA";

        if (jaEntrouHoje) {
            aluno.horasaida = agora.toLocalDateTime();
        } else {
            Presenca presenca = new Presenca();
            presenca.matricula_aluno = aluno.matricula_aluno;
            presenca.id_cartao = cartao.id_cartao;
            presenca.data_hora = agora;
            presenca.situacao = situacao;
            presencaRepository.save(presenca);

            aluno.horaentrada = agora.toLocalDateTime();
            aluno.horasaida = null;
        }

        alunoRepository.save(aluno);

        String mensagem = jaEntrouHoje ? "Saída registrada" : "Entrada registrada";
        return new Resultado(true, situacao, mensagem, aluno.nome_aluno, agora);
    }

    private Resultado negada(String situacao, String mensagem) {
        return new Resultado(false, situacao, mensagem, null, null);
    }

    public record Resultado(
            boolean aceita,
            String situacao,
            String mensagem,
            String aluno,
            OffsetDateTime dataHora) {}
}
