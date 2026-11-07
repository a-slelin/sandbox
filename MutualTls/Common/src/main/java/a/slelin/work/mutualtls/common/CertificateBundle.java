package a.slelin.work.mutualtls.common;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Набор сертификатов одной стороны соединения:
 * <ul>
 *     <li><b>identity</b> — собственный сертификат и закрытый ключ (им сторона представляется);</li>
 *     <li><b>ca</b> — корневой сертификат, которому сторона доверяет (им проверяется собеседник).</li>
 * </ul>
 * Файлы ищутся в одной папке: {@code <name>.pem}, {@code <name>.key} и {@code ca.pem}.
 *
 * @param name        роль: {@code server} или {@code client}
 * @param directory   папка с файлами
 * @param certificate собственный сертификат
 * @param privateKey  закрытый ключ к нему
 * @param ca          доверенный корневой сертификат
 */
public record CertificateBundle(String name, Path directory, X509Certificate certificate, PrivateKey privateKey,
                                X509Certificate ca) {

    /** Пустой пароль: хранилища живут только в памяти и наружу не сохраняются. */
    private static final char[] NO_PASSWORD = new char[0];

    /**
     * Загружает набор {@code name} из папки {@code directory}.
     *
     * @throws CertificateLoadException если файла нет или он повреждён
     */
    public static CertificateBundle load(Path directory, String name) {
        Path dir = directory.toAbsolutePath().normalize();
        X509Certificate certificate = PemReader.readCertificate(dir.resolve(name + ".pem"));
        PrivateKey key = PemReader.readPrivateKey(dir.resolve(name + ".key"));
        X509Certificate ca = PemReader.readCertificate(dir.resolve("ca.pem"));
        return new CertificateBundle(name, dir, certificate, key, ca);
    }

    /**
     * Папка сертификатов по умолчанию: {@code certs/} в текущей папке, а если её нет —
     * {@code MutualTls/certs/} (удобно при запуске из корня песочницы).
     */
    public static Path defaultDirectory() {
        Path local = Path.of("certs");
        Path fromRoot = Path.of("MutualTls", "certs");
        return !Files.isDirectory(local) && Files.isDirectory(fromRoot) ? fromRoot : local;
    }

    /**
     * Собирает {@link SSLContext} для mTLS.
     * <p>
     * {@code KeyManager} отдаёт собеседнику наш сертификат и подписывает рукопожатие нашим ключом,
     * {@code TrustManager} проверяет сертификат собеседника: он должен быть подписан {@link #ca()}.
     */
    public SSLContext createSslContext() {
        try {
            // Хранилище «кто я»: ключ + наш сертификат. Корневой CA собеседнику не отправляем — он у него уже есть
            KeyStore identity = KeyStore.getInstance("PKCS12");
            identity.load(null, null);
            identity.setKeyEntry(name, privateKey, NO_PASSWORD, new Certificate[]{certificate});

            // Хранилище «кому я доверяю»: только наш CA, системные корневые сертификаты не используются
            KeyStore trust = KeyStore.getInstance("PKCS12");
            trust.load(null, null);
            trust.setCertificateEntry("ca", ca);

            KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keys.init(identity, NO_PASSWORD);
            TrustManagerFactory trusted = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trusted.init(trust);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keys.getKeyManagers(), trusted.getTrustManagers(), null);
            return context;
        } catch (Exception e) {
            throw new CertificateLoadException("Не удалось создать SSLContext: " + e.getMessage(), e);
        }
    }

    /**
     * Проверяет, что закрытый ключ — пара к открытому ключу из сертификата.
     * <p>
     * Подписываем тестовые данные закрытым ключом и проверяем подпись открытым ключом сертификата. Если ключ
     * «левый», собеседник отвергнет рукопожатие: подпись CertificateVerify не сойдётся.
     */
    public boolean isKeyMatchingCertificate() {
        try {
            byte[] data = "mtls-key-check".getBytes(StandardCharsets.UTF_8);
            // Алгоритм подписи выбираем по типу ключа
            String algorithm = switch (privateKey.getAlgorithm()) {
                case "RSA" -> "SHA256withRSA";
                case "EC" -> "SHA256withECDSA";
                default -> privateKey.getAlgorithm(); // Ed25519 и другие подписывают «как есть»
            };
            Signature signer = Signature.getInstance(algorithm);
            signer.initSign(privateKey);
            signer.update(data);
            byte[] signature = signer.sign();

            Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(certificate.getPublicKey());
            verifier.update(data);
            return verifier.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /**
     * Проверяет, что собственный сертификат действительно подписан нашим CA. Если нет — собеседник его отвергнет,
     * поэтому лучше сообщить об этом сразу, ещё до подключения.
     */
    public boolean isSignedByCa() {
        try {
            certificate.verify(ca.getPublicKey());
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
