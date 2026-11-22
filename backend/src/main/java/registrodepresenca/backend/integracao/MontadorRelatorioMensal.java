package registrodepresenca.backend.integracao;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
import registrodepresenca.backend.entity.Aula;
import registrodepresenca.backend.entity.Presenca;

public final class MontadorRelatorioMensal {
    private MontadorRelatorioMensal(){}

    /** Linhas do cabeçalho da tabela: dia, aula e horário. */
    public static final int LINHAS_DE_CABECALHO = 3;

    // Chave das presenças antigas, sem aula: contam como presente em todas as aulas do dia
    private static final int DIA_INTEIRO = 0;
    // Usada só quando não há grade cadastrada: uma coluna por dia
    private static final int QUALQUER_AULA = -1;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Uma coluna por dia letivo e por aula. Cada célula mostra "P" (presente) ou "F" (faltou).
     * O cabeçalho tem três linhas: dia, aula e horário. A frequência é calculada sobre as aulas.
     */
    public static List<List<Object>> montar(List<Aluno> alunos,List<Presenca> presencas,YearMonth mes,ZoneId fuso,List<Aula> aulas){

        List<Integer> numeros = new ArrayList<>();
        List<String> nomes = new ArrayList<>();
        List<String> horarios = new ArrayList<>();
        for(Aula aula : aulas){
            numeros.add(aula.numero);
            nomes.add(aula.nome);
            horarios.add(aula.inicio.format(HORA) + "–" + aula.fim.format(HORA));
        }
        if(numeros.isEmpty()){
            numeros.add(QUALQUER_AULA);
            nomes.add("Dia");
            horarios.add("");
        }
        int colunasPorDia = numeros.size();

        Set<Integer> matriculasDaTurma = alunos.stream().map(a -> a.matricula_aluno).collect(Collectors.toSet());

        // aluno -> dia do mês -> aulas em que esteve presente
        Map<Integer, Map<Integer, Set<Integer>>> aulasPorAlunoEDia = new HashMap<>();

        TreeSet<Integer> diasLetivos = new TreeSet<>();

        for(Presenca p : presencas){
            if(p.data_hora == null || !"PRESENTE".equals(p.situacao)){
                continue;
            }

            if(!matriculasDaTurma.contains(p.matricula_aluno)){
                continue;
            }

            LocalDate data = p.data_hora.atZoneSameInstant(fuso).toLocalDate();
            if(!YearMonth.from(data).equals(mes)){
                continue;
            }

            int aula = p.numero_aula == null ? DIA_INTEIRO : p.numero_aula;
            aulasPorAlunoEDia
                .computeIfAbsent(p.matricula_aluno, k -> new HashMap<>())
                .computeIfAbsent(data.getDayOfMonth(), k -> new HashSet<>())
                .add(aula);
            diasLetivos.add(data.getDayOfMonth());
        }

        List<List<Object>> tabela = new ArrayList<>();

        //Cabeçalho: linha 1 = dia, linha 2 = aula, linha 3 = horário (com os títulos das colunas fixas)
        List<Object> linhaDia = new ArrayList<>(List.of("", ""));
        List<Object> linhaAula = new ArrayList<>(List.of("", ""));
        List<Object> linhaHorario = new ArrayList<>(List.of("Matricula", "Nome"));
        for(int dia : diasLetivos){
            for(int i = 0; i < colunasPorDia; i++){
                linhaDia.add(i == 0 ? String.format("%02d/%02d", dia, mes.getMonthValue()) : "");
                linhaAula.add(nomes.get(i));
                linhaHorario.add(horarios.get(i));
            }
        }
        linhaDia.addAll(List.of("", ""));
        linhaAula.addAll(List.of("", ""));
        linhaHorario.add("Aulas assistidas");
        linhaHorario.add("Frequencia (%)");
        tabela.add(linhaDia);
        tabela.add(linhaAula);
        tabela.add(linhaHorario);

        //Rows dos alunos
        List<Aluno> ordenados = new ArrayList<>(alunos);
        ordenados.sort(Comparator.comparing((Aluno a) -> vazioSeNulo(a.nome_aluno), String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(a -> a.matricula_aluno));

        for(Aluno aluno : ordenados){
            Map<Integer, Set<Integer>> diasDoAluno = aulasPorAlunoEDia.getOrDefault(aluno.matricula_aluno, Map.of());

            List<Object> linha = new ArrayList<>();
            linha.add(aluno.matricula_aluno);
            linha.add(vazioSeNulo(aluno.nome_aluno));

            int assistidas = 0;

            for(int dia : diasLetivos){
                Set<Integer> doDia = diasDoAluno.get(dia);
                for(int numero : numeros){
                    boolean veio = estevePresente(doDia, numero);
                    if(veio){
                        assistidas++;
                    }
                    linha.add(veio ? "P" : "F");
                }
            }

            linha.add(assistidas);
            linha.add(diasLetivos.isEmpty() ? "" : Math.round(assistidas * 1000.0 / (diasLetivos.size() * colunasPorDia)) / 10.0);

            tabela.add(linha);
        }

        return tabela;
    }

    private static boolean estevePresente(Set<Integer> aulasDoDia, int numero){
        if(aulasDoDia == null || aulasDoDia.isEmpty()){
            return false;
        }
        return numero == QUALQUER_AULA || aulasDoDia.contains(DIA_INTEIRO) || aulasDoDia.contains(numero);
    }

    private static String vazioSeNulo(String texto) {
        return texto == null ? "" : texto;
    }
}
