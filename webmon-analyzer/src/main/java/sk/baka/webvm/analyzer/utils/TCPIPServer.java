package sk.baka.webvm.analyzer.utils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import sk.baka.webvm.analyzer.*;
import sk.baka.webvm.analyzer.dump.HTMLDump;
import sk.baka.webvm.analyzer.dump.TextDump;

/**
 *
 * @author Martin Vysny
 */
public class TCPIPServer {

    public final int port;
    private final IHistorySampler sampler;
    private final int httpPort;
    private ExecutorService executor;

    public TCPIPServer(int port, int httpPort, IHistorySampler sampler) {
        this.httpPort = httpPort;
        Checks.checkNotNull("sampler", sampler);
        this.port = port;
        this.sampler = sampler;
    }
    private volatile ServerSocket serverSocket = null;
    private volatile ServerSocket serverHttpSocket = null;

    public synchronized void start() throws IOException {
        if (serverSocket != null) {
            return;
        }
        serverSocket = new ServerSocket(port);
        serverHttpSocket = new ServerSocket(httpPort);
        executor = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger id = new AtomicInteger();
            @Override public Thread newThread(@NotNull Runnable r) {
                final Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName(TCPIPServer.class.getSimpleName() + " - WebMon Server Socket thread #" + id.incrementAndGet());
                return t;
            }
        });
        executor.submit(new Runnable() {
            @Override public void run() {
                try {
                    while (true) {
                        final Socket s = serverSocket.accept();
                        executor.submit(new Runnable() {
                            @Override public void run() {
                                try {
                                    handle(s);
                                } catch (Exception e) {
                                    if (serverSocket != null) {
                                        log.log(Level.SEVERE, "WebMon Listening thread failed", e);
                                    }
                                }
                            }
                        });
                    }
                } catch (Throwable t) {
                    if (serverSocket != null) {
                        log.log(Level.SEVERE, "WebMon Listening thread failed", t);
                    }
                }
                log.info("WebMon Socket Server stopped");
                TCPIPServer.this.stop();
            }
        });
        executor.submit(new Runnable() {
            @Override public void run() {
                try {
                    while (true) {
                        final Socket s = serverHttpSocket.accept();
                        executor.submit(new Runnable() {
                            @Override public void run() {
                                try {
                                    s.getInputStream().skip(s.getInputStream().available());
                                    s.getOutputStream().write(("HTTP/1.0 200 OK\n"
                                            + "Date: Fri, 31 Dec 1999 23:59:59 GMT\n"
                                            + "Content-Type: text/html\n\n").getBytes());
                                    s.getOutputStream().write(new HTMLDump().dump(sampler.getVmstatHistory()).getBytes("UTF-8"));
                                    s.close();
                                } catch (Exception e) {
                                    if (serverSocket != null) {
                                        log.log(Level.SEVERE, "WebMon Listening thread failed", e);
                                    }
                                }
                            }
                        });
                    }
                } catch (Throwable t) {
                    if (serverSocket != null) {
                        log.log(Level.SEVERE, "WebMon Listening thread failed", t);
                    }
                }
                log.info("WebMon Socket Server stopped");
                TCPIPServer.this.stop();
            }
        });
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
        executor.shutdown();
        executor = null;
    }

    private void handle(Socket s) throws IOException {
        try {
            s.getOutputStream().write(new TextDump().dump(sampler.getVmstatHistory()).getBytes("UTF-8"));
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
                        w.println("getResourceAsStreamBase64 java/lang/String.class  - calls Thread.currentThread().getContextClassLoader().getResources() and dumps each URL here");
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
                        getResourceAsStream(s, w, args[1], false);
                    } else if ("getResourceAsStreamBase64".equals(cmd)) {
                        getResourceAsStream(s, w, args[1], true);
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

    private void getResourceAsStream(Socket s, PrintWriter w, String arg, boolean base64) throws IOException {
        final Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(arg);
        if (urls == null || !urls.hasMoreElements()) {
            w.println("Resource " + arg + " not found");
        } else {
            int count = 0;
            while (urls.hasMoreElements()) {
                final URL url = urls.nextElement();
                w.println(url);
                w.flush();
                if (!base64) {
                    MiscUtils.copy(url.openStream(), s.getOutputStream());
                    s.getOutputStream().flush();
                } else {
                    w.println(Base64Coder.encodeLines(MiscUtils.toByteArray(url.openStream())));
                }
                w.println();
                w.flush();
                count++;
            }
            w.println(count + " resource(s) found");
        }
    }

    public static void main(String[] args) throws Exception {
        Main.main(args);
    }
}
