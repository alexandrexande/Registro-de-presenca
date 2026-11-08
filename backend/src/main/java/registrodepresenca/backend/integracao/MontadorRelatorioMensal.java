package registrodepresenca.backend.integracao;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.entity.Presenca;

public final class MontadorRelatorioMensal {
    private MontadorRelatorioMensal(){}

    public static List<List<Object>> montar(List<Aluno> alunos,List<Presenca> presencas,YearMonth mes,ZoneId fuso){

        Set<Integer> matriculasDaTurma = alunos.stream().map(a -> a.matricula_aluno).collect(Collectors.toSet());

        Map<Integer, Set<Integer>> diasPorAluno = new HashMap<>();

        TreeSet<Integer> diasLetivos = new TreeSet<>();

        for(Presenca p : presencas){
            if(p.data_hora == null || !ehEntrada(p)){
                continue;
            }

            if(!matriculasDaTurma.contains(p.matricula_aluno)){
                continue;
            }

            LocalDate data = p.data_hora.atZoneSameInstant(fuso).toLocalDate();
            if(!YearMonth.from(data).equals(mes)){
                continue;
            }

            diasPorAluno.computeIfAbsent(p.matricula_aluno, k -> new HashSet<>()).add(data.getDayOfMonth());
            diasLetivos.add(data.getDayOfMonth());    
        }

        List<List<Object>> tabela = new ArrayList<>();

        //Cabeçalho
        List<Object> cabecalho = new ArrayList<>();
        cabecalho.add("Matricula");
        cabecalho.add("Nome");
        for(int dia : diasLetivos){
            cabecalho.add(String.format("%02d/%02d", dia, mes.getMonthValue()));
        }
        cabecalho.add("Presencas");
        cabecalho.add("Frequencia (%)");
        tabela.add(cabecalho);

        //Rows dos alunos
        List<Aluno> ordenados = new ArrayList<>(alunos);
        ordenados.sort(Comparator.comparing((Aluno a) -> vazioSeNulo(a.nome_aluno), String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(a -> a.matricula_aluno));

        for(Aluno aluno : ordenados){
            Set<Integer> diasDoAluno = diasPorAluno.getOrDefault(aluno.matricula_aluno, Set.of());

            List<Object> linha = new ArrayList<>();
            linha.add(aluno.matricula_aluno);   
            linha.add(vazioSeNulo(aluno.nome_aluno));

            int presentes = 0;

            for(int dia : diasLetivos){
                boolean veio = diasDoAluno.contains(dia);

                if(veio){
                    presentes++;
                }
                linha.add(veio ? "P" : "F");
            }

            linha.add(presentes);
            linha.add(diasLetivos.isEmpty() ? "" : Math.round(presentes * 1000 / diasLetivos.size()) / 10);

            tabela.add(linha);
        }

        return tabela;
    }

    private static boolean ehEntrada(Presenca p) {
        return p.situacao != null && "ENTRADA".equalsIgnoreCase(p.situacao.trim());
    }
 
    private static String vazioSeNulo(String texto) {
        return texto == null ? "" : texto;
    }
}