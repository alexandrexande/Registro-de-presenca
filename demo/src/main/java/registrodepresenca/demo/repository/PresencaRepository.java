package registrodepresenca.demo.repository;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import registrodepresenca.demo.entity.Presenca;

public interface PresencaRepository extends JpaRepository<Presenca, Integer> {

    // Aluno já tem presença no intervalo [inicio, fim)?
    boolean existsByAlunoMatriculaAlunoAndDataHoraGreaterThanEqualAndDataHoraLessThan(
            Integer matriculaAluno, OffsetDateTime inicio, OffsetDateTime fim);

    // Presenças de um aluno em um período
    List<Presenca> findByAlunoMatriculaAlunoAndDataHoraBetween(
            Integer matriculaAluno, OffsetDateTime inicio, OffsetDateTime fim);
}