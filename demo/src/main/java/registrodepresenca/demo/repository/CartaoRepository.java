package registrodepresenca.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import registrodepresenca.demo.classes.Cartao;

public interface CartaoRepository extends JpaRepository<Cartao, Integer> {

    Optional<Cartao> findByUid(String uid);
}