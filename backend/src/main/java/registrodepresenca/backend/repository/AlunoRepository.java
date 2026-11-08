package registrodepresenca.backend.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import registrodepresenca.backend.entity.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Integer> {

    @Query("SELECT a FROM Aluno a WHERE a.id_turma = :idTurma")
    List<Aluno> buscarPorTurma(@Param("idTurma") int idTurma);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Aluno a WHERE a.matricula_aluno = :matricula")
    Optional<Aluno> buscarParaAtualizar(@Param("matricula") int matricula);
}
