package a.slelin.work.mutualtls.common;

import java.io.PrintStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Вывод сведений о сертификатах в консоль в виде аккуратных «карточек».
 */
public final class CertificatePrinter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final String LINE = "─".repeat(78);

    private CertificatePrinter() {
    }

    /**
     * Печатает весь набор: собственный сертификат и доверенный CA.
     */
    public static void print(PrintStream out, CertificateBundle bundle) {
        out.println();
        out.println("Набор сертификатов «" + bundle.name() + "» из " + bundle.directory());
        print(out, "Собственный сертификат (" + bundle.name() + ".pem)", bundle.certificate());
        print(out, "Доверенный CA (ca.pem)", bundle.ca());
        out.println(bundle.isSignedByCa()
                ? "  [OK] " + bundle.name() + ".pem подписан доверенным CA"
                : "  [!!] " + bundle.name() + ".pem НЕ подписан ca.pem — собеседник его отвергнет!");
        out.println();
    }

    /**
     * Печатает цепочку, полученную в TLS-сессии (например, сертификаты собеседника).
     */
    public static void print(PrintStream out, String title, Certificate[] chain) {
        if (chain == null || chain.length == 0) {
            out.println(title + ": (нет сертификатов)");
            return;
        }
        for (int i = 0; i < chain.length; i++) {
            if (chain[i] instanceof X509Certificate x509) {
                print(out, chain.length == 1 ? title : title + " [" + (i + 1) + "/" + chain.length + "]", x509);
            }
        }
    }

    public static void print(PrintStream out, String title, X509Certificate certificate) {
        CertificateInfo info = CertificateInfo.of(certificate);
        Instant now = Instant.now();

        out.println("┌" + LINE);
        out.println("│ " + title);
        out.println("├" + LINE);
        row(out, "Кому выдан", info.subjectName());
        row(out, "Группа (OU)", info.subjectUnit());
        row(out, "Организация", info.subjectOrganization());
        row(out, "Subject", join(info.subject()));
        row(out, "Кем выдан", info.issuerName() + (info.selfSigned() ? "  (самоподписанный)" : ""));
        row(out, "Действителен", DATE.format(info.notBefore()) + " — " + DATE.format(info.notAfter())
                                 + (info.isValidAt(now) ? "  [OK] ещё " + info.daysLeft(now) + " дн." : "  [!!] НЕ действителен"));
        row(out, "Серийный №", info.serialNumber());
        row(out, "Ключ / подпись", info.publicKey() + " / " + info.signatureAlgorithm());
        row(out, "Тип", info.certificateAuthority() ? "Удостоверяющий центр (CA)" : "Конечный сертификат");
        if (!info.alternativeNames().isEmpty()) {
            row(out, "Альт. имена", String.join(", ", info.alternativeNames()));
        }
        if (!info.extendedKeyUsage().isEmpty()) {
            row(out, "Назначение", String.join(", ", info.extendedKeyUsage()));
        }
        row(out, "Key usage", list(info.keyUsage()));
        String[] sha = info.sha256TwoLines().split("\n");
        row(out, "SHA-256", sha[0]);
        row(out, "", sha[1]);
        out.println("└" + LINE);
    }

    private static void row(PrintStream out, String key, String value) {
        out.printf("│ %-15s %s%n", key, value);
    }

    private static String join(Map<String, String> name) {
        return name.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "));
    }

    /** Склеивает список через запятую или возвращает прочерк. */
    public static String list(List<String> values) {
        return values.isEmpty() ? "—" : String.join(", ", values);
    }
}
