package registrodepresenca.backend.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.entity.Aula;
import registrodepresenca.backend.entity.Cartao;
import registrodepresenca.backend.entity.Presenca;
import registrodepresenca.backend.repository.AlunoRepository;
import registrodepresenca.backend.repository.AulaRepository;
import registrodepresenca.backend.repository.CartaoRepository;
import registrodepresenca.backend.repository.PresencaRepository;

@Service
public class PresencaService {

    private final CartaoRepository cartaoRepository;
    private final AlunoRepository alunoRepository;
    private final PresencaRepository presencaRepository;
    private final AulaRepository aulaRepository;
    private final Clock relogio;
    private final int janelaMinutos;

    public PresencaService(
            CartaoRepository cartaoRepository,
            AlunoRepository alunoRepository,
            PresencaRepository presencaRepository,
            AulaRepository aulaRepository,
            Clock relogio,
            @Value("${presenca.janela-minutos:10}") int janelaMinutos) {
        this.cartaoRepository = cartaoRepository;
        this.alunoRepository = alunoRepository;
        this.presencaRepository = presencaRepository;
        this.aulaRepository = aulaRepository;
        this.relogio = relogio;
        this.janelaMinutos = janelaMinutos;
    }

    /** Início da janela em que o cartão vale: os últimos minutos da aula, sem passar do começo dela. */
    public static LocalTime inicioDaJanela(Aula aula, int janelaMinutos) {
        LocalTime inicio = aula.fim.minusMinutes(janelaMinutos);
        return inicio.isBefore(aula.inicio) ? aula.inicio : inicio;
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

        OffsetDateTime agora = OffsetDateTime.now(relogio);
        LocalTime hora = agora.toLocalTime().truncatedTo(ChronoUnit.SECONDS);

        Aula aula = aulaDaLeitura(hora);
        if (aula == null) {
            return negada("FORA_DO_HORARIO", "Fora da janela de presença das aulas");
        }

        LocalDate hoje = agora.toLocalDate();
        OffsetDateTime inicio = hoje.atStartOfDay(relogio.getZone()).toOffsetDateTime();
        OffsetDateTime fim = hoje.plusDays(1).atStartOfDay(relogio.getZone()).toOffsetDateTime();

        if (presencaRepository.existeNaAula(aluno.matricula_aluno, aula.numero, inicio, fim)) {
            return negada("DUPLICADA", "Presença já registrada na " + aula.nome);
        }

        Presenca presenca = new Presenca();
        presenca.matricula_aluno = aluno.matricula_aluno;
        presenca.id_cartao = cartao.id_cartao;
        presenca.data_hora = agora;
        presenca.situacao = "PRESENTE";
        presenca.numero_aula = aula.numero;
        presencaRepository.save(presenca);
        return new Resultado(true, presenca.situacao, "Presença registrada na " + aula.nome, aluno.nome_aluno, agora);
    }

    private Aula aulaDaLeitura(LocalTime hora) {
        for (Aula aula : aulaRepository.listarEmOrdem()) {
            if (!hora.isBefore(inicioDaJanela(aula, janelaMinutos)) && !hora.isAfter(aula.fim)) {
                return aula;
            }
        }
        return null;
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
