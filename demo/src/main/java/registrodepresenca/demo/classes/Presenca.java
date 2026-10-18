package registrodepresenca.demo.classes;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "PRESENCA")
public class Presenca{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id_presenca;
    public int matricula_aluno;
    public int id_cartao;
    public OffsetDateTime data_hora;
    public String situacao;
}