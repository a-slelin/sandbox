package a.slelin.work.mutualtls.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * Чтение PEM-файлов, которые выдаёт OpenSSL: сертификатов X.509 и закрытых ключей в формате PKCS#8.
 * <p>
 * PEM — это DER (двоичный ASN.1), закодированный в Base64 и обёрнутый строками {@code -----BEGIN ...-----}.
 * Сертификаты Java умеет читать из PEM сама, а ключ приходится «распаковывать» вручную.
 */
public final class PemReader {

    private static final String PKCS8_BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_END = "-----END PRIVATE KEY-----";

    /** Алгоритмы, которые пробуем по очереди: PKCS#8 хранит OID алгоритма, но KeyFactory нужен конкретный. */
    private static final List<String> KEY_ALGORITHMS = List.of("RSA", "EC", "Ed25519", "RSASSA-PSS");

    private PemReader() {
    }

    /**
     * Читает сертификат X.509 (PEM или DER).
     */
    public static X509Certificate readCertificate(Path file) {
        requireFile(file);
        try (InputStream in = Files.newInputStream(file)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        } catch (IOException | GeneralSecurityException e) {
            throw new CertificateLoadException("Не удалось прочитать сертификат " + file.getFileName()
                                               + ": " + e.getMessage(), e);
        }
    }

    /**
     * Читает закрытый ключ PKCS#8 без пароля ({@code -----BEGIN PRIVATE KEY-----}).
     * Именно такой ключ создаёт {@code openssl genpkey}.
     */
    public static PrivateKey readPrivateKey(Path file) {
        requireFile(file);
        String pem;
        try {
            pem = Files.readString(file);
        } catch (IOException e) {
            throw new CertificateLoadException("Не удалось прочитать ключ " + file.getFileName(), e);
        }

        if (pem.contains("BEGIN RSA PRIVATE KEY") || pem.contains("BEGIN EC PRIVATE KEY")) {
            throw new CertificateLoadException("Ключ " + file.getFileName() + " в старом формате (PKCS#1/SEC1). "
                                               + "Преобразуйте: openssl pkcs8 -topk8 -nocrypt -in old.key -out "
                                               + file.getFileName());
        }
        if (pem.contains("BEGIN ENCRYPTED PRIVATE KEY")) {
            throw new CertificateLoadException("Ключ " + file.getFileName() + " зашифрован паролем. "
                                               + "Создайте ключ без пароля (openssl genpkey без -aes256)");
        }
        int begin = pem.indexOf(PKCS8_BEGIN);
        int end = pem.indexOf(PKCS8_END);
        if (begin < 0 || end < begin) {
            throw new CertificateLoadException("В файле " + file.getFileName() + " нет блока " + PKCS8_BEGIN);
        }

        String base64 = pem.substring(begin + PKCS8_BEGIN.length(), end).replaceAll("\\s", "");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(base64));

        for (String algorithm : KEY_ALGORITHMS) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(spec);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException ignored) {
                // Ключ другого алгоритма — пробуем следующий
            }
        }
        throw new CertificateLoadException("Неизвестный алгоритм ключа в " + file.getFileName());
    }

    private static void requireFile(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new CertificateLoadException("Нет файла " + file.toAbsolutePath().normalize()
                                               + " — создайте сертификаты (см. MutualTls/certs/HELP.md)");
        }
    }
}
