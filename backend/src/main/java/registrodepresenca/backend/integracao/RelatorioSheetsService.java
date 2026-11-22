package registrodepresenca.backend.integracao;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
 
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchClearValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
 
import registrodepresenca.backend.entity.Aluno;
import registrodepresenca.backend.entity.Aula;
import registrodepresenca.backend.entity.Presenca;
import registrodepresenca.backend.entity.Turma;
import registrodepresenca.backend.repository.AlunoRepository;
import registrodepresenca.backend.repository.AulaRepository;
import registrodepresenca.backend.repository.PresencaRepository;
import registrodepresenca.backend.repository.TurmaRepository;

@Service
public class RelatorioSheetsService {
    
    private static final ZoneId FUSO = ZoneId.of("America/Maceio");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int LINHAS_DO_TOPO = 3;

    private final Sheets sheets;
    private final TurmaRepository turmaRepository;
    private final AlunoRepository alunoRepository;
    private final PresencaRepository presencaRepository;
    private final AulaRepository aulaRepository;

    @Value("${google.sheets.spreadsheet-id}")
    private String idPlanilha;

    @Value("${google.credentials.path:}")
    private String caminhoCredencial;

    public RelatorioSheetsService(
        Sheets sheets,
        TurmaRepository turmaRepository,
        AlunoRepository alunoRepository,
        PresencaRepository presencaRepository,
        AulaRepository aulaRepository){
            this.sheets = sheets;
            this.turmaRepository = turmaRepository;
            this.alunoRepository = alunoRepository;
            this.presencaRepository = presencaRepository;
            this.aulaRepository = aulaRepository;
    }

    public record ResultadoRelatorio(String mes, List<String> abas, int abasCriadas, int turmasSemAlunos, String planilha){}

    public synchronized ResultadoRelatorio gerarMensal(YearMonth mes) throws IOException{
        if(caminhoCredencial == null || caminhoCredencial.isBlank()){
            throw new IOException("as credenciais do Google não estão configuradas (defina GOOGLE_CREDENTIALS_PATH com o caminho do arquivo .json)");
        }
        String mesTexto = String.format("%02d/%d", mes.getMonthValue(), mes.getYear());
        String link = "https://docs.google.com/spreadsheets/d/" + idPlanilha;

        List<Turma> turmas = turmaRepository.findAll();
        Map<Integer, List<Aluno>> alunosPorTurma = alunoRepository.findAll().stream().collect(Collectors.groupingBy(a -> a.id_turma));

        OffsetDateTime inicio = mes.atDay(1).atStartOfDay(FUSO).toOffsetDateTime();
        OffsetDateTime fim = mes.plusMonths(1).atDay(1).atStartOfDay(FUSO).toOffsetDateTime();
        String geradoEm = OffsetDateTime.now(FUSO).format(DATA_HORA);

        //Monta em memória a tabela para cada turma
        Map<String, List<List<Object>>> tabelas = new LinkedHashMap<>();

        List<Aula> grade = aulaRepository.listarEmOrdem();

        for(Turma turma : turmas){
            List<Aluno> alunos = alunosPorTurma.get(turma.id_turma);

            if(alunos == null || alunos.isEmpty()){
                continue;
            }

            List<Presenca> presencas = presencaRepository.buscarPorTurmaNoDia(turma.id_turma, inicio, fim);
            List<List<Object>> linhas = new ArrayList<>();

            linhas.add(List.of("Turma: " + vazioSeNulo(turma.nome) + " " + vazioSeNulo(turma.turma) + " (" + turma.ano + ") - " + mesTexto));
            linhas.add(List.of("Gerado em: " + geradoEm));
            linhas.add(List.of(""));
            linhas.addAll(MontadorRelatorioMensal.montar(alunos, presencas, mes, FUSO, grade));

            tabelas.put(nomeDaAba(turma), linhas);
        }

        if(tabelas.isEmpty()){
            return new ResultadoRelatorio(mesTexto, List.of(), 0, turmas.size(), link);
        }

        int abasCriadas = criarAbasQueFaltam(tabelas.keySet());

        Map<String, Integer> idsDasAbas = buscarIdsDasAbas();

        formatar(tabelas, idsDasAbas);

        List<String> abasParaLimpar = tabelas.keySet().stream().map(nome -> "'" + nome + "'").toList();
        sheets.spreadsheets().values().batchClear(idPlanilha, new BatchClearValuesRequest().setRanges(abasParaLimpar)).execute();

        List<ValueRange> dados = new ArrayList<>();
        for(Map.Entry<String, List<List<Object>>> aba : tabelas.entrySet()){
            dados.add(new ValueRange().setRange("'" + aba.getKey() + "'!A1").setValues(aba.getValue()));
        }

        sheets.spreadsheets().values().batchUpdate(idPlanilha, new BatchUpdateValuesRequest().setValueInputOption("RAW").setData(dados)).execute();

        return new ResultadoRelatorio(mesTexto, new ArrayList<>(tabelas.keySet()), abasCriadas, turmas.size() - tabelas.size(), link);
    }

    private int criarAbasQueFaltam(Set<String> nomesNecessarios) throws IOException{
        Set<String> existentes = buscarIdsDasAbas().keySet();

        List<Request> pedidos = new ArrayList<>();

        for(String nome : nomesNecessarios){
            if(!existentes.contains(nome)){
                pedidos.add(new Request().setAddSheet(new AddSheetRequest().setProperties(new SheetProperties().setTitle(nome))));
            }
        }

        if(!pedidos.isEmpty()){
            sheets.spreadsheets().batchUpdate(idPlanilha, new BatchUpdateSpreadsheetRequest().setRequests(pedidos)).execute();
        }
        return pedidos.size();
    }

    private Map<String, Integer> buscarIdsDasAbas() throws IOException {
        return sheets.spreadsheets().get(idPlanilha).execute().getSheets().stream().collect(Collectors.toMap(
            aba -> aba.getProperties().getTitle(),aba -> aba.getProperties().getSheetId()));
    }

    private void formatar(Map<String, List<List<Object>>> tabelas, Map<String, Integer> idsDasAbas) throws IOException{
        List<Request> pedidos = new ArrayList<>();

        for(Map.Entry<String, List<List<Object>>> aba : tabelas.entrySet()){
            int sheetId = idsDasAbas.get(aba.getKey());
            List<List<Object>> linhas = aba.getValue();
            int totalColunas = linhas.stream().mapToInt(List :: size).max().orElse(1);

            //Tamanho da aba
            pedidos.add(new Request().setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
                .setProperties(new SheetProperties()
                        .setSheetId(sheetId)
                        .setGridProperties(new GridProperties()
                                .setRowCount(Math.max(linhas.size() + 10, 30))
                                .setColumnCount(Math.max(totalColunas + 2, 10))
                                .setFrozenRowCount(LINHAS_DO_TOPO + MontadorRelatorioMensal.LINHAS_DE_CABECALHO)
                                .setFrozenColumnCount(2)))
                .setFields("gridProperties(rowCount,columnCount,frozenRowCount,frozenColumnCount)")));

            pedidos.add(negrito(sheetId, 0, 1));                            //Título
            for(int i = 0; i < MontadorRelatorioMensal.LINHAS_DE_CABECALHO; i++){
                pedidos.add(negrito(sheetId, LINHAS_DO_TOPO + i, totalColunas));    //Cabeçalho da tabela: dia, aula e horário
            }
        }

        sheets.spreadsheets().batchUpdate(idPlanilha, new BatchUpdateSpreadsheetRequest().setRequests(pedidos)).execute();
    }

    private static Request negrito(int sheetId, int linha, int colunas) {
        return new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(linha)
                        .setEndRowIndex(linha + 1)
                        .setStartColumnIndex(0)
                        .setEndColumnIndex(colunas))
                .setCell(new CellData().setUserEnteredFormat(
                        new CellFormat().setTextFormat(new TextFormat().setBold(true))))
                .setFields("userEnteredFormat.textFormat.bold"));
    }
 
    // "id - nome", sem os caracteres que o Google não aceita em nomes de aba.
    private static String nomeDaAba(Turma turma) {
        String base = turma.id_turma + " - " + vazioSeNulo(turma.nome);
        String limpo = base.replaceAll("[:\\\\/?*\\[\\]']", " ").trim();
        
        return limpo.length() > 100 ? limpo.substring(0, 100) : limpo;
    }
 
    private static String vazioSeNulo(String texto) {
        return texto == null ? "" : texto;
    }
}