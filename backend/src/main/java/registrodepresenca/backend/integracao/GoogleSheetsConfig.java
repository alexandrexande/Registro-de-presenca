package registrodepresenca.backend.integracao;

import java.io.FileInputStream;
import java.util.List;

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
    @Value("${google.credentials.path}") private String caminhoCredencial;

    @Bean public Sheets sheets() throws Exception{
        GoogleCredentials credencial;
        try(FileInputStream arquivo = new FileInputStream(caminhoCredencial)){
            credencial = GoogleCredentials.fromStream(arquivo).createScoped(List.of("https://www.googleapis.com/auth/spreadsheets"));
        }

        return new Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            new HttpCredentialsAdapter(credencial)).setApplicationName("Registro de Presenca").build();
    }
}