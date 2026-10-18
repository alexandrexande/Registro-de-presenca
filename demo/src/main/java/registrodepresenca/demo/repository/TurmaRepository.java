package registrodepresenca.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import registrodepresenca.demo.entity.Turma;

public interface TurmaRepository extends JpaRepository<Turma, Integer> {
}