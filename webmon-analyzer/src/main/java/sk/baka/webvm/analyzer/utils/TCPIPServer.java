package sk.baka.webvm.analyzer.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.HistorySampler;
import sk.baka.webvm.analyzer.IHistorySampler;
import sk.baka.webvm.analyzer.SamplerConfig;
import sk.baka.webvm.analyzer.TextDump;

/**
 *
 * @author Martin Vysny
 */
public class TCPIPServer {

    public final int port;
    private final HistorySampler sampler;

    public TCPIPServer(int port, HistorySampler sampler) {
        this.port = port;
        this.sampler = sampler;
    }
    private volatile ServerSocket serverSocket = null;

    public synchronized void start() throws IOException {
        if (serverSocket != null) {
            return;
        }
        serverSocket = new ServerSocket(port);
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    setName(TCPIPServer.class.getSimpleName() + " - WebMon Server Socket thread");
                    while (true) {
                        listen();
                    }
                } catch (Throwable t) {
                    if (serverSocket != null) {
                        log.log(Level.SEVERE, "WebMon Listening thread failed", t);
                    }
                }
                log.info("WebMon Socket Server stopped");
                TCPIPServer.this.stop();
            }
        };
        t.setDaemon(true);
        t.start();
    }
    private static final Logger log = Logger.getLogger(TCPIPServer.class.getName());

    public synchronized void stop() {
        if (serverSocket == null) {
            return;
        }
        final ServerSocket ss = serverSocket;
        serverSocket = null;
        try {
            ss.close();
        } catch (Exception ex) {
            log.log(Level.INFO, "Failed to close server socket", ex);
        }
    }

    private void listen() throws IOException {
        final Socket s = serverSocket.accept();
        try {
            s.getOutputStream().write(TextDump.dump(sampler.getVmstatHistory()).getBytes("UTF-8"));
            s.getOutputStream().flush();
        } finally {
            try {
                s.close();
            } catch (Exception ex) {
                log.log(Level.INFO, "Failed to close socket", ex);
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        final int port = 50000;
        final int historySize = 20;
        final SamplerConfig cfg = new SamplerConfig(historySize, 1000, 0);
        final HistorySampler sampler = new HistorySampler(cfg, IHistorySampler.HISTORY_PROBLEMS, MgmtUtils.getMemoryInfoProvider(), null, null);
        sampler.start();
        final TCPIPServer server = new TCPIPServer(port, sampler);
        server.start();
        System.out.println("Listening on port " + port + ", press Enter to stop");
        System.in.read();
        server.stop();
        sampler.stop();
    }
}
