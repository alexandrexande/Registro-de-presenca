package registrodepresenca.demo.classes;

import jakarta.persistence.*;

@Entity
@Table(name = "USUARIO")
public class Usuario{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id_usuario;
    public String nome;
    public String senha_hash;
    public String perfil;
}