package registrodepresenca.backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import registrodepresenca.backend.entity.Cartao;

public interface CartaoRepository extends JpaRepository<Cartao, Integer> {

    Optional<Cartao> findByUid(String uid);
}