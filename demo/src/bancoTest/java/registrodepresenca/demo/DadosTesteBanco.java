package registrodepresenca.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import registrodepresenca.demo.classes.*;
import registrodepresenca.demo.repository.*;
import registrodepresenca.demo.service.PresencaService;

public class DadosTesteBanco {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DemoApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setAdditionalProfiles("banco");
        try (var ctx = app.run(args)) {
            var tx = new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class));
            tx.executeWithoutResult(status -> {
                var alunos = ctx.getBean(AlunoRepository.class);
                var cartoes = ctx.getBean(CartaoRepository.class);
                var ativo = cartoes.findByUid("TESTE-ATIVO-01");
                var bloqueado = cartoes.findByUid("TESTE-BLOQUEADO-01");
                if (ativo.isPresent() || bloqueado.isPresent()) {
                    if (ativo.isEmpty() || bloqueado.isEmpty()
                            || ativo.get().matricula_aluno != 1900000101
                            || bloqueado.get().matricula_aluno != 1900000102) {
                        throw new IllegalStateException("UIDs de teste já usados ou exemplos incompletos. Nenhum dado foi alterado.");
                    }
                    System.out.println("Os dois cartões de exemplo já existem. Nenhum registro duplicado foi criado.");
                    return;
                }
                if (alunos.existsById(1900000101) || alunos.existsById(1900000102)) {
                    throw new IllegalStateException("Matrículas reservadas para os exemplos já estão em uso.");
                }
                Usuario usuario = new Usuario();
                usuario.nome = "TESTE - Professor demonstração";
                usuario.perfil = "PROFESSOR";
                usuario.senha_hash = "SEM_LOGIN_TESTE";
                ctx.getBean(UsuarioRepository.class).saveAndFlush(usuario);

                Turma turma = new Turma();
                turma.nome = "TESTE - Presença";
                turma.turma = "TESTE";
                turma.ano = java.time.LocalDate.now().getYear();
                turma.id_usuario = usuario.id_usuario;
                ctx.getBean(TurmaRepository.class).saveAndFlush(turma);

                criarAluno(alunos, cartoes, turma.id_turma, 1900000101,
                        "TESTE - Aluno ativo", "TESTE-ATIVO-01", "ATIVO");
                criarAluno(alunos, cartoes, turma.id_turma, 1900000102,
                        "TESTE - Aluno bloqueado", "TESTE-BLOQUEADO-01", "BLOQUEADO");

                var service = ctx.getBean(PresencaService.class);
                var entrada = service.registrarLeitura("TESTE-ATIVO-01");
                var saida = service.registrarLeitura("TESTE-ATIVO-01");
                var recusada = service.registrarLeitura("TESTE-BLOQUEADO-01");
                if (!entrada.aceita() || !"ENTRADA".equals(entrada.situacao())
                        || !saida.aceita() || !"SAIDA".equals(saida.situacao()) || recusada.aceita()) {
                    throw new IllegalStateException("Resultado inesperado. Os exemplos serão desfeitos.");
                }
            });
            System.out.println("Exemplos disponíveis: matrículas 1900000101 e 1900000102.");
        }
    }

    private static void criarAluno(AlunoRepository alunos, CartaoRepository cartoes,
                                    int turma, int matricula, String nome, String uid, String situacao) {
        Aluno aluno = new Aluno();
        aluno.matricula_aluno = matricula;
        aluno.id_turma = turma;
        aluno.nome_aluno = nome;
        aluno = alunos.saveAndFlush(aluno);
        Cartao cartao = new Cartao();
        cartao.matricula_aluno = matricula;
        cartao.uid = uid;
        cartao.situacao = situacao;
        cartoes.saveAndFlush(cartao);
        aluno.uid = uid;
        alunos.saveAndFlush(aluno);
    }
}
