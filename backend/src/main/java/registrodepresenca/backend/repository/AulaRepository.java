package registrodepresenca.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import registrodepresenca.backend.entity.Aula;

public interface AulaRepository extends JpaRepository<Aula, Integer> {

    @Query("SELECT a FROM Aula a ORDER BY a.numero")
    List<Aula> listarEmOrdem();
}
