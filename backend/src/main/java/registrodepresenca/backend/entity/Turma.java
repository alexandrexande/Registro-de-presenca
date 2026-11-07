package registrodepresenca.demo.classes;

import jakarta.persistence.*;

@Entity
@Table(name = "\"TURMA\"")
public class Turma{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id_turma;
    public String nome;
    public String turma;
    public int ano;
    public int id_usuario;
}