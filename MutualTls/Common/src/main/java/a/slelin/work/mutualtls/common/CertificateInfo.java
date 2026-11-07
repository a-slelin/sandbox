package a.slelin.work.mutualtls.common;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Человекочитаемые сведения о сертификате X.509: кому и кем выдан, срок действия, ключ, назначение, отпечаток.
 * Используется и для вывода в консоль, и для карточек в GUI.
 *
 * @param subject           поля «Кому выдан» (CN, OU, O, ...) в порядке «от частного к общему»
 * @param issuer            поля «Кем выдан»
 * @param serialNumber      серийный номер в HEX
 * @param notBefore         начало срока действия
 * @param notAfter          окончание срока действия
 * @param publicKey         алгоритм и размер открытого ключа, например {@code RSA 2048}
 * @param signatureAlgorithm алгоритм подписи CA, например {@code SHA256withRSA}
 * @param certificateAuthority является ли сертификат удостоверяющим центром
 * @param selfSigned        самоподписанный (subject == issuer)
 * @param alternativeNames  subjectAltName: DNS-имена, IP, e-mail
 * @param keyUsage          разрешённые операции с ключом
 * @param extendedKeyUsage  назначение: TLS-сервер, TLS-клиент, ...
 * @param sha256            SHA-256 отпечаток всего сертификата
 */
public record CertificateInfo(Map<String, String> subject, Map<String, String> issuer, String serialNumber,
                              Instant notBefore, Instant notAfter, String publicKey, String signatureAlgorithm,
                              boolean certificateAuthority, boolean selfSigned, List<String> alternativeNames,
                              List<String> keyUsage, List<String> extendedKeyUsage, String sha256) {

    /** Названия битов keyUsage в порядке RFC 5280. */
    private static final String[] KEY_USAGE = {"digitalSignature", "nonRepudiation", "keyEncipherment",
            "dataEncipherment", "keyAgreement", "keyCertSign", "cRLSign", "encipherOnly", "decipherOnly"};

    /** Наиболее распространённые OID расширения extendedKeyUsage. */
    private static final Map<String, String> EXTENDED_KEY_USAGE = Map.of(
            "1.3.6.1.5.5.7.3.1", "TLS-сервер (serverAuth)",
            "1.3.6.1.5.5.7.3.2", "TLS-клиент (clientAuth)",
            "1.3.6.1.5.5.7.3.3", "Подпись кода (codeSigning)",
            "1.3.6.1.5.5.7.3.4", "E-mail (emailProtection)",
            "1.3.6.1.5.5.7.3.8", "Метки времени (timeStamping)");

    /** Короткие имена атрибутов, которые Java выводит как OID. */
    private static final Map<String, String> ATTRIBUTE_NAMES = Map.of(
            "1.2.840.113549.1.9.1", "EMAIL",
            "EMAILADDRESS", "EMAIL");

    /**
     * Разбирает сертификат.
     */
    public static CertificateInfo of(X509Certificate cert) {
        return new CertificateInfo(
                parseName(cert.getSubjectX500Principal()),
                parseName(cert.getIssuerX500Principal()),
                cert.getSerialNumber().toString(16).toUpperCase(),
                cert.getNotBefore().toInstant(),
                cert.getNotAfter().toInstant(),
                describeKey(cert.getPublicKey()),
                cert.getSigAlgName(),
                cert.getBasicConstraints() >= 0,
                cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal()),
                alternativeNames(cert),
                keyUsage(cert),
                extendedKeyUsage(cert),
                fingerprint(cert));
    }

    /** CN владельца — «Кому выдан». */
    public String subjectName() {
        return subject.getOrDefault("CN", "—");
    }

    /** CN удостоверяющего центра — «Кем выдан». */
    public String issuerName() {
        return issuer.getOrDefault("CN", "—");
    }

    /** Подразделение владельца (OU) — у нас здесь учебная группа. */
    public String subjectUnit() {
        return subject.getOrDefault("OU", "—");
    }

    public String subjectOrganization() {
        return subject.getOrDefault("O", "—");
    }

    /** Действителен ли сертификат в момент {@code now}. */
    public boolean isValidAt(Instant now) {
        return !now.isBefore(notBefore) && !now.isAfter(notAfter);
    }

    /** Сколько дней осталось до окончания срока (отрицательное — уже истёк). */
    public long daysLeft(Instant now) {
        return Duration.between(now, notAfter).toDays();
    }

    /** Отпечаток в две строки — для консоли. */
    public String sha256TwoLines() {
        return sha256Lines(16);
    }

    /**
     * Отпечаток, разбитый на строки по {@code bytesPerLine} байт («AB:CD:...»).
     */
    public String sha256Lines(int bytesPerLine) {
        int charsPerLine = bytesPerLine * 3;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < sha256.length(); i += charsPerLine) {
            if (!out.isEmpty()) {
                out.append('\n');
            }
            // Каждый байт занимает «XX:», у последнего байта строки двоеточие отбрасываем
            out.append(sha256, i, Math.min(sha256.length(), i + charsPerLine - 1));
        }
        return out.toString();
    }

    /** Серийный номер группами по 4 символа — так его проще читать и сравнивать. */
    public String serialNumberGrouped() {
        return serialNumber.replaceAll("(.{4})(?!$)", "$1 ");
    }

    // ---------- Разбор полей ----------

    /**
     * DN «CN=..., OU=..., O=...» → упорядоченная карта. В X.500 порядок RDN «от общего к частному»,
     * LdapName возвращает его в обратном виде, поэтому разворачиваем, чтобы CN оказался первым.
     */
    private static Map<String, String> parseName(X500Principal principal) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            List<Rdn> rdns = new ArrayList<>(new LdapName(principal.getName(X500Principal.RFC2253)).getRdns());
            for (Rdn rdn : rdns.reversed()) {
                String type = rdn.getType().toUpperCase();
                result.putIfAbsent(ATTRIBUTE_NAMES.getOrDefault(type, type), valueOf(rdn.getValue()));
            }
        } catch (InvalidNameException e) {
            result.put("DN", principal.getName());
        }
        return result;
    }

    /** Значения нестандартных атрибутов (например, e-mail) приходят как DER-байты. */
    private static String valueOf(Object value) {
        if (value instanceof byte[] der && der.length > 2) {
            // DER: тег (1 байт) + длина (1 байт для коротких строк) + сама строка
            return new String(der, 2, der.length - 2, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }

    private static String describeKey(PublicKey key) {
        return switch (key) {
            case RSAPublicKey rsa -> "RSA " + rsa.getModulus().bitLength() + " бит";
            case ECPublicKey ec -> "EC " + ec.getParams().getOrder().bitLength() + " бит";
            default -> key.getAlgorithm();
        };
    }

    private static List<String> alternativeNames(X509Certificate cert) {
        List<String> names = new ArrayList<>();
        try {
            Collection<List<?>> entries = cert.getSubjectAlternativeNames();
            if (entries != null) {
                for (List<?> entry : entries) {
                    String prefix = switch ((Integer) entry.get(0)) {
                        case 1 -> "email:";
                        case 2 -> "DNS:";
                        case 6 -> "URI:";
                        case 7 -> "IP:";
                        default -> "";
                    };
                    names.add(prefix + entry.get(1));
                }
            }
        } catch (CertificateParsingException ignored) {
            // Расширение повреждено — просто не показываем его
        }
        return names;
    }

    private static List<String> keyUsage(X509Certificate cert) {
        boolean[] bits = cert.getKeyUsage();
        List<String> usages = new ArrayList<>();
        if (bits != null) {
            for (int i = 0; i < bits.length && i < KEY_USAGE.length; i++) {
                if (bits[i]) {
                    usages.add(KEY_USAGE[i]);
                }
            }
        }
        return usages;
    }

    private static List<String> extendedKeyUsage(X509Certificate cert) {
        try {
            List<String> oids = cert.getExtendedKeyUsage();
            return oids == null ? List.of() : oids.stream().map(o -> EXTENDED_KEY_USAGE.getOrDefault(o, o)).toList();
        } catch (CertificateParsingException e) {
            return List.of();
        }
    }

    private static String fingerprint(X509Certificate cert) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
            return HexFormat.ofDelimiter(":").withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            return "—";
        }
    }
}
