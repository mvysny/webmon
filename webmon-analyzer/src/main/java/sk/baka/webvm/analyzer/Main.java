package sk.baka.webvm.analyzer;

import sk.baka.webvm.analyzer.utils.TCPIPServer;

/**
 * @author mavi
 */
public class Main {
    public static void main(String[] args) throws Exception {
        final SamplerConfig cfg = new SamplerConfig(20, 1000, 0);
        final IHistorySampler hs = new HistorySampler(cfg, IHistorySampler.HISTORY_PROBLEMS, null, null);
        hs.start();
        final TCPIPServer webmonServer = new TCPIPServer(5455, 5456, hs);
        webmonServer.start();
        System.out.println("========================================================");
        System.out.println("Webmon is running:");
        System.out.println("  * Point your browser to http://localhost:5456 to obtain a dump");
        System.out.println("  * nc localhost 5455   to gain access to a simple console");
        System.out.println("========================================================");
        System.out.println("Press Enter to terminate");
        System.in.read();
        webmonServer.stop();
        hs.stop();
    }
}
