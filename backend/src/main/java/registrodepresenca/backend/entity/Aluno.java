package registrodepresenca.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "\"ALUNO\"")
public class Aluno{
    @Id
    public int matricula_aluno;
    public int id_turma;
    public String nome_aluno;
    public String uid;
    // Não existem na tabela ALUNO do banco atual; @Transient evita o erro de validação do schema
    @Transient
    public LocalDateTime horaentrada;
    @Transient
    public LocalDateTime horasaida;
}