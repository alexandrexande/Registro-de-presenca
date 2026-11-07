package registrodepresenca.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import registrodepresenca.demo.classes.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Integer> {

    @Query("SELECT a FROM Aluno a WHERE a.id_turma = :idTurma")
    List<Aluno> buscarPorTurma(@Param("idTurma") int idTurma);
}
