package registrodepresenca.backend.integracao;

import java.io.FileInputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

@Configuration 
public class GoogleSheetsConfig{
    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsConfig.class);

    @Value("${google.credentials.path:}") private String caminhoCredencial;

    @Bean public Sheets sheets() throws Exception{
        com.google.api.client.http.HttpRequestInitializer inicializador = null;

        if(caminhoCredencial != null && !caminhoCredencial.isBlank()){
            try(FileInputStream arquivo = new FileInputStream(caminhoCredencial)){
                GoogleCredentials credencial = GoogleCredentials.fromStream(arquivo).createScoped(List.of("https://www.googleapis.com/auth/spreadsheets"));
                inicializador = new HttpCredentialsAdapter(credencial);
            }
        } else {
            log.warn("GOOGLE_CREDENTIALS_PATH não definido: o relatório mensal ficará indisponível.");
        }

        return new Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            inicializador).setApplicationName("Registro de Presenca").build();
    }
}