package registrodepresenca.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import registrodepresenca.backend.entity.Presenca;

public interface PresencaRepository extends JpaRepository<Presenca, Integer> {

    @Query("SELECT MAX(p.data_hora) FROM Presenca p WHERE p.id_cartao = :cartao")
    OffsetDateTime ultimaLeituraGravada(@Param("cartao") int cartao);

    @Query("""
           SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
           FROM Presenca p
           WHERE p.matricula_aluno = :matricula
             AND p.data_hora >= :inicio
             AND p.data_hora < :fim
           """)
    boolean existeNoIntervalo(@Param("matricula") int matricula,
                              @Param("inicio") OffsetDateTime inicio,
                              @Param("fim") OffsetDateTime fim);

    @Query("""
           SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
           FROM Presenca p
           WHERE p.matricula_aluno = :matricula
             AND p.numero_aula = :aula
             AND p.data_hora >= :inicio
             AND p.data_hora < :fim
           """)
    boolean existeNaAula(@Param("matricula") int matricula,
                         @Param("aula") int aula,
                         @Param("inicio") OffsetDateTime inicio,
                         @Param("fim") OffsetDateTime fim);

    @Query("""
           SELECT p FROM Presenca p
           WHERE p.matricula_aluno = :matricula
             AND p.data_hora BETWEEN :inicio AND :fim
           """)
    List<Presenca> buscarPorAlunoNoPeriodo(@Param("matricula") int matricula,
                                           @Param("inicio") OffsetDateTime inicio,
                                           @Param("fim") OffsetDateTime fim);

    @Query("""
           SELECT p FROM Presenca p, Aluno a
           WHERE p.matricula_aluno = a.matricula_aluno
             AND a.id_turma = :turma
             AND p.data_hora >= :inicio
             AND p.data_hora < :fim
           ORDER BY p.data_hora
           """)
    List<Presenca> buscarPorTurmaNoDia(
            @Param("turma") int turma,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);
}
