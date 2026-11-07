package registrodepresenca.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import registrodepresenca.demo.classes.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByNome(String nome);
}