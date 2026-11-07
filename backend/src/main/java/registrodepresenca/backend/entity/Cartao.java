package registrodepresenca.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "\"CARTAO\"")
public class Cartao{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id_cartao;
    public int matricula_aluno;
    public String uid;
    public String situacao;
}