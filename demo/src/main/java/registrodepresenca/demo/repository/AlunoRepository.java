package registrodepresenca.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import registrodepresenca.demo.classes.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Integer> {

    List<Aluno> findByTurmaIdTurma(Integer idTurma);
}