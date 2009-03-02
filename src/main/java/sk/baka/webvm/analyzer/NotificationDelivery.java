/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebVM.
 *
 * WebVM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebVM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebVM.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer;

import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import sk.baka.webvm.config.Config;
import sk.baka.webvm.misc.BackgroundService;

/**
 * Delivers miscellaneous notifications (mail, jabber).
 * @author Martin Vysny
 */
public final class NotificationDelivery extends BackgroundService {

    /**
     * Creates new deliverer.
     */
    public NotificationDelivery() {
        super(1);
    }

    /**
     * Checks if sending mail is enabled in given config object.
     * @param config the configuration object
     * @return true if {@link Config#mailSmtpHost} is non-empty, false otherwise.
     */
    public static boolean isEmailEnabled(final Config config) {
        return !isBlank(config.mailSmtpHost);
    }

    private static boolean isBlank(final String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * Checks if sending mail is enabled in given config object.
     * @param config the configuration object
     * @return true if {@link Config#mailSmtpHost} is non-empty, false otherwise.
     */
    public static boolean isJabberEnabled(final Config config) {
        return !isBlank(config.jabberServer);
    }

    /**
     * Sends a mail with given report.
     * @param config the mail server configuration.
     * @param testing if true then a testing mail is sent
     * @param reports the current reports
     * @throws org.apache.commons.mail.EmailException if sending mail fails.
     */
    public static void sendEmail(final Config config, final boolean testing, final List<ProblemReport> reports) throws EmailException {
        if (!isEmailEnabled(config)) {
            return;
        }
        final HtmlEmail mail = new HtmlEmail();
        configure(mail, config);
        mail.setSubject("WebVM: Problems notification" + (testing ? " (testing mail)" : ""));
        mail.setMsg(ProblemReport.toString(reports, "\n"));
        mail.setHtmlMsg("<html><body>\n" + ProblemReport.toHtml(reports) + "\n</body></html>");
        mail.send();
    }

    private static void configure(final Email mail, final Config config) throws EmailException {
        mail.setHostName(config.mailSmtpHost);
        config.mailSmtpEncryption.activate(mail);
        if (config.mailSmtpPort > 0) {
            if (mail.isSSL()) {
                mail.setSslSmtpPort(Integer.toString(config.mailSmtpPort));
            } else {
                mail.setSmtpPort(config.mailSmtpPort);
            }
        }
        if (config.mailSmtpUsername != null) {
            mail.setAuthentication(config.mailSmtpUsername, config.mailSmtpPassword);
        }
        mail.setFrom(config.mailFrom);
        if (config.mailTo == null) {
            config.mailTo = "";
        }
        // add recipients
        final StringTokenizer t = new StringTokenizer(config.mailTo, ",");
        for (; t.hasMoreTokens();) {
            mail.addTo(t.nextToken().trim());
        }
    }

    public static void sendJabber(final Config config, final boolean testing, final List<ProblemReport> reports) throws XMPPException {
        XMPPConnection connection = new XMPPConnection(config.jabberServer);
        connection.connect();
        try {
            connection.login(config.jabberUsername, config.jabberPassword);
            final StringTokenizer t = new StringTokenizer(config.jabberRecipients, ",");
            for (; t.hasMoreTokens();) {
                final String recipient = t.nextToken().trim();
                final Chat chat = connection.getChatManager().createChat(recipient, "WebVM", new MessageListener() {

                    public void processMessage(Chat chat, Message message) {
                        // do nothing
                    }
                });
                chat.sendMessage("WebVM Problems report: " + (testing ? "(testing)" : "") + "\n" + ProblemReport.toString(reports, "\n"));
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Delivers given report asynchronously.
     * @param reports the reports to deliver.
     */
    public void deliverAsync(final List<ProblemReport> reports) throws InterruptedException {
        asyncQueue.put(reports);
    }
    private final BlockingQueue<List<ProblemReport>> asyncQueue = new LinkedBlockingQueue<List<ProblemReport>>();

    @Override
    protected void started(ScheduledExecutorService executor) {
        executor.execute(new Notificator());
    }
    private volatile Config config;

    /**
     * Sets the new configuration file.
     * @param config the new config file.
     */
    public void configure(final Config config) {
        this.config = new Config(config);
    }

    @Override
    protected void stopped() {
        // do nothing
    }

    private class Notificator implements Runnable {

        public void run() {
            while (true) {
                final List<ProblemReport> reports;
                try {
                    reports = asyncQueue.take();
                } catch (InterruptedException ex) {
                    // we are terminating.
                    return;
                }
                try {
                    NotificationDelivery.sendEmail(config, false, reports);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, "Failed to send email", ex);
                }
                try {
                    NotificationDelivery.sendJabber(config, false, reports);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, "Failed to send jabber message", ex);
                }
            }
        }
    }
    private static final Logger log = Logger.getLogger(NotificationDelivery.class.getName());
}
