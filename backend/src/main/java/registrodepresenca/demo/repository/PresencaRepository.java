package registrodepresenca.demo.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import registrodepresenca.demo.classes.Presenca;

public interface PresencaRepository extends JpaRepository<Presenca, Integer> {

    @Query(
            """
           SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
           FROM Presenca p
           WHERE p.matricula_aluno = :matricula
             AND p.data_hora >= :inicio
             AND p.data_hora < :fim
           """)
    boolean existeNoIntervalo(
            @Param("matricula") int matricula,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);

    @Query(
            """
           SELECT p FROM Presenca p
           WHERE p.matricula_aluno = :matricula
             AND p.data_hora BETWEEN :inicio AND :fim
           """)
    List<Presenca> buscarPorAlunoNoPeriodo(
            @Param("matricula") int matricula,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);
}
