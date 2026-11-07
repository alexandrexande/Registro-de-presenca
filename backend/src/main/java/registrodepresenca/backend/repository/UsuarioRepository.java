package registrodepresenca.backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import registrodepresenca.backend.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByNome(String nome);
}