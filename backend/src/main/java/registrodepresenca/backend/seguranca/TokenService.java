package registrodepresenca.backend.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import registrodepresenca.backend.entity.Usuario;

/** Token no formato base64url(id|expiracao|perfil).base64url(HMAC-SHA256). */
@Component
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String ALGORITMO = "HmacSHA256";

    private final byte[] chave;
    private final long validadeSegundos;

    public TokenService(@Value("${auth.secret:}") String segredo,
                        @Value("${auth.validade-horas:8}") long validadeHoras) {
        if (segredo == null || segredo.isBlank()) {
            byte[] aleatorio = new byte[32];
            new SecureRandom().nextBytes(aleatorio);
            this.chave = aleatorio;
            log.warn("AUTH_SECRET não definido: usando segredo temporário. Os logins caem a cada reinício.");
        } else {
            this.chave = segredo.getBytes(StandardCharsets.UTF_8);
        }
        this.validadeSegundos = validadeHoras * 3600;
    }

    public String gerar(Usuario usuario) {
        long expira = Instant.now().getEpochSecond() + validadeSegundos;
        String perfil = usuario.perfil == null ? "" : usuario.perfil;
        String corpo = b64(usuario.id_usuario + "|" + expira + "|" + perfil);
        return corpo + "." + assinar(corpo);
    }

    public Optional<Sessao.Usuario> validar(String token) {
        try {
            if (token == null) return Optional.empty();
            int ponto = token.indexOf('.');
            if (ponto <= 0) return Optional.empty();
            String corpo = token.substring(0, ponto);
            byte[] esperado = assinar(corpo).getBytes(StandardCharsets.UTF_8);
            byte[] recebido = token.substring(ponto + 1).getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(esperado, recebido)) return Optional.empty();
            String texto = new String(Base64.getUrlDecoder().decode(corpo), StandardCharsets.UTF_8);
            String[] partes = texto.split("\\|", 3);
            if (partes.length < 3) return Optional.empty();
            if (Long.parseLong(partes[1]) < Instant.now().getEpochSecond()) return Optional.empty();
            return Optional.of(new Sessao.Usuario(Integer.parseInt(partes[0]), partes[2]));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private String assinar(String corpo) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(chave, ALGORITMO));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível assinar o token", e);
        }
    }

    private static String b64(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }
}
