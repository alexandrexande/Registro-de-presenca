package registrodepresenca.demo;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import registrodepresenca.demo.classes.*;
import registrodepresenca.demo.repository.*;
import registrodepresenca.demo.service.PresencaService;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.hikari.connection-timeout=10000", "spring.datasource.hikari.maximum-pool-size=2"})
@ActiveProfiles("banco")
@Transactional
@Commit
class PresencaBancoTest {
 @Autowired PresencaService service;
 @Autowired UsuarioRepository usuarios;
 @Autowired TurmaRepository turmas;
 @Autowired AlunoRepository alunos;
 @Autowired CartaoRepository cartoes;
 @Autowired PresencaRepository presencas;
 @Autowired EntityManager em;

 Cartao criarCartao(String situacao) {
  Usuario u = new Usuario(); u.nome="TESTE_CONEXAO_RN"; u.senha_hash="teste-sem-login"; u.perfil="PROFESSOR";
  usuarios.saveAndFlush(u);
  Turma t = new Turma(); t.nome="TESTE_CONEXAO_RN"; t.turma="TESTE"; t.ano=2026; t.id_usuario=u.id_usuario;
  turmas.saveAndFlush(t);
  Aluno a = new Aluno(); a.matricula_aluno=ThreadLocalRandom.current().nextInt(1000000000,2000000000);
  while(alunos.existsById(a.matricula_aluno)) a.matricula_aluno++;
  a.nome_aluno="TESTE_CONEXAO_RN"; a.id_turma=t.id_turma; alunos.saveAndFlush(a);
  Cartao c = new Cartao(); c.matricula_aluno=a.matricula_aluno;
  c.uid=UUID.randomUUID().toString().replace("-", ""); c.situacao=situacao; cartoes.saveAndFlush(c);
  a=alunos.findById(a.matricula_aluno).orElseThrow(); a.uid=c.uid; alunos.saveAndFlush(a);
  System.out.println("Registro de teste: matrícula="+c.matricula_aluno+", UID="+c.uid+", cartão="+c.situacao);
  return c;
 }
 long quantidade(Cartao c) {
  em.flush();
  return em.createQuery("select count(p) from Presenca p where p.matricula_aluno=:id",Long.class)
    .setParameter("id",c.matricula_aluno).getSingleResult();
 }
 Aluno recarregar(Cartao c) { em.flush(); em.clear(); return alunos.findById(c.matricula_aluno).orElseThrow(); }
 @Test void uidVazio() {
  assertFalse(service.registrarLeitura(null).aceita());
  assertFalse(service.registrarLeitura("  ").aceita());
 }
 @Test void cartaoDesconhecido() {
  var r=service.registrarLeitura(UUID.randomUUID().toString());
  assertFalse(r.aceita()); assertEquals("Cartão não cadastrado",r.mensagem());
 }
 @Test void cartaoBloqueadoNaoGrava() {
  Cartao c=criarCartao("BLOQUEADO");
  assertFalse(service.registrarLeitura(c.uid).aceita());
  assertEquals(0L,quantidade(c)); assertNull(recarregar(c).horaentrada);
 }
 @Test void primeiraLeituraGravaEntrada() {
  Cartao c=criarCartao("ATIVO"); var r=service.registrarLeitura(" "+c.uid+" ");
  assertTrue(r.aceita()); assertEquals("ENTRADA",r.situacao()); assertEquals(1L,quantidade(c));
  Aluno a=recarregar(c); assertNotNull(a.horaentrada); assertNull(a.horasaida);
  Presenca p=em.createQuery("select p from Presenca p where p.matricula_aluno=:id",Presenca.class)
   .setParameter("id",c.matricula_aluno).getSingleResult();
  assertEquals(c.id_cartao,p.id_cartao); assertEquals("ENTRADA",p.situacao); assertNotNull(p.data_hora);
 }
 @Test void segundaLeituraGravaSaida() {
  Cartao c=criarCartao("ATIVO"); service.registrarLeitura(c.uid);
  var entrada=recarregar(c).horaentrada;
  var r=service.registrarLeitura(c.uid); assertTrue(r.aceita()); assertEquals("SAIDA",r.situacao());
  assertEquals(2L,quantidade(c)); Aluno a=recarregar(c);
  assertEquals(entrada,a.horaentrada); assertNotNull(a.horasaida); assertFalse(a.horasaida.isBefore(a.horaentrada));
 }
 @Test void novoDiaReiniciaEntrada() {
  Cartao c=criarCartao("ATIVO");
  OffsetDateTime ontem=OffsetDateTime.now().minusDays(1);
  Presenca p=new Presenca(); p.matricula_aluno=c.matricula_aluno; p.id_cartao=c.id_cartao;
  p.data_hora=ontem; p.situacao="SAIDA"; presencas.saveAndFlush(p);
  Aluno a=alunos.findById(c.matricula_aluno).orElseThrow(); a.horaentrada=ontem.minusHours(1).toLocalDateTime(); a.horasaida=ontem.toLocalDateTime(); alunos.saveAndFlush(a);
  assertEquals("ENTRADA",service.registrarLeitura(c.uid).situacao());
  a=recarregar(c); assertNull(a.horasaida); assertEquals(OffsetDateTime.now().toLocalDate(),a.horaentrada.toLocalDate());
 }
 @Test void terceiraLeituraMantemSaida() {
  Cartao c=criarCartao("ATIVO"); service.registrarLeitura(c.uid); service.registrarLeitura(c.uid);
  assertEquals("SAIDA",service.registrarLeitura(c.uid).situacao()); assertEquals(3L,quantidade(c));
 }
}
