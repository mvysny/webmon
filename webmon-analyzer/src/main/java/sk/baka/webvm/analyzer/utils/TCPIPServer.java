package sk.baka.webvm.analyzer.utils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Enumeration;
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
    private final IHistorySampler sampler;

    public TCPIPServer(int port, IHistorySampler sampler) {
        Checks.checkNotNull("sampler", sampler);
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
            final PrintWriter w = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "ASCII"));
            final BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
            while (true) {
                try {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    final String[] args = line.split("\\s+");
                    if (line.length() == 0 || args.length == 0) {
                        continue;
                    }
                    final String cmd = args[0];
                    if ("help".equals(cmd)) {
                        w.println("help - displays this help");
                        w.println("getResources java/lang/String.class  - calls Thread.currentThread().getContextClassLoader().getResources()");
                        w.println("getResourceAsStream java/lang/String.class  - calls Thread.currentThread().getContextClassLoader().getResources() and dumps each URL here");
                    } else if ("getResources".equals(cmd)) {
                        final Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(args[1]);
                        if (urls == null || !urls.hasMoreElements()) {
                            w.println("Resource " + args[1] + " not found");
                        } else {
                            int count = 0;
                            while (urls.hasMoreElements()) {
                                w.println(urls.nextElement());
                                count++;
                            }
                            w.println(count + " resource(s) found");
                        }
                    } else if ("getResourceAsStream".equals(cmd)) {
                        final Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(args[1]);
                        if (urls == null || !urls.hasMoreElements()) {
                            w.println("Resource " + args[1] + " not found");
                        } else {
                            int count = 0;
                            while (urls.hasMoreElements()) {
                                final URL url = urls.nextElement();
                                w.println(url);
                                w.flush();
                                MiscUtils.copy(url.openStream(), s.getOutputStream());
                                s.getOutputStream().flush();
                                w.println();
                                w.flush();
                                count++;
                            }
                            w.println(count + " resource(s) found");
                        }
                    } else {
                        w.println("Invalid command '" + cmd + "' - type 'help' for help");
                    }
                } catch (Throwable t) {
                    w.println(MiscUtils.getStacktrace(t));
                }
                w.println();
                w.flush();
            }
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
        final HistorySampler sampler = new HistorySampler(cfg, IHistorySampler.HISTORY_PROBLEMS, null, null);
        sampler.start();
        final TCPIPServer server = new TCPIPServer(port, sampler);
        server.start();
        System.out.println("Listening on port " + port + ", press Enter to stop");
        System.in.read();
//        long hu=System.currentTimeMillis();
//        while(System.currentTimeMillis()-hu < 10000) {
//            byte[] huhu = new byte[6000];
//        }
//        System.in.read();
        server.stop();
        sampler.stop();
    }
}
