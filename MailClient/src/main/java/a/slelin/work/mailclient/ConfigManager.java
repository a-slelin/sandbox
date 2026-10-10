package a.slelin.work.mailclient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigManager {

    private static final String CONFIG_NAME = "config.properties";

    private final Properties props = new Properties();

    public ConfigManager() throws IOException {
        load();
    }

    private void load() throws IOException {
        Path external = Path.of(CONFIG_NAME);
        try (InputStream in = Files.isRegularFile(external)
                ? Files.newInputStream(external)
                : getClass().getResourceAsStream("/" + CONFIG_NAME)) {
            if (in == null) {
                throw new FileNotFoundException(CONFIG_NAME + " not found");
            }
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    public String getSmtpHost() {
        return props.getProperty("smtp.host").trim();
    }

    public int getSmtpPort() {
        return Integer.parseInt(props.getProperty("smtp.port").trim());
    }

    public String getUser() {
        return props.getProperty("email.user").trim();
    }

    public String getPassword() {
        return props.getProperty("email.password");
    }

    public String getImapHost() {
        return props.getProperty("imap.host").trim();
    }

    public int getImapPort() {
        return Integer.parseInt(props.getProperty("imap.port").trim());
    }
}
