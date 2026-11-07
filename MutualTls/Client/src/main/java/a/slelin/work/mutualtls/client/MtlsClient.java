package a.slelin.work.mutualtls.client;

import a.slelin.work.mutualtls.client.ui.MainFrame;
import a.slelin.work.mutualtls.client.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * Точка входа mTLS-клиента со Swing-интерфейсом.
 */
public final class MtlsClient {

    private MtlsClient() {
    }

    public static void main(String[] args) {
        ClientConfig config = ClientConfig.parse(args);
        SwingUtilities.invokeLater(() -> {
            Theme.apply(config.dark());
            new MainFrame(config).setVisible(true);
        });
    }
}
