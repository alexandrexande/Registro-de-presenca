package registrodepresenca.backend.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "\"PRESENCA\"")
public class Presenca{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id_presenca;
    public int matricula_aluno;
    public int id_cartao;
    public OffsetDateTime data_hora;
    public String situacao;
    // Aula em que a leitura foi feita. Nulo nas presenças antigas, de antes da grade de aulas
    public Integer numero_aula;
}