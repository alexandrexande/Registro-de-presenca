package registrodepresenca.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import registrodepresenca.backend.entity.Turma;

public interface TurmaRepository extends JpaRepository<Turma, Integer> {

    @Query("SELECT t FROM Turma t WHERE t.id_usuario = :idUsuario ORDER BY t.nome, t.turma")
    List<Turma> buscarPorUsuario(@Param("idUsuario") int idUsuario);
}