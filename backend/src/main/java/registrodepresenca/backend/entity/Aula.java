package registrodepresenca.backend.entity;

import java.time.LocalTime;

import jakarta.persistence.*;

@Entity
@Table(name = "\"AULA\"")
public class Aula{
    @Id
    public int numero;
    public String nome;
    public LocalTime inicio;
    public LocalTime fim;
}
