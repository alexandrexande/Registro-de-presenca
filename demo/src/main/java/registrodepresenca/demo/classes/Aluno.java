package registrodepresenca.demo.classes;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "ALUNO")
public class Aluno{
    @Id
    public int matricula_aluno;
    public int id_turma;
    public String nome_aluno;
    public String codrfid ;
    public LocalDateTime horaentrada;
    public LocalDateTime horasaida;
}