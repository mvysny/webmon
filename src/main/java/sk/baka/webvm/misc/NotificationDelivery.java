/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebMon is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebMon.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.misc;

import com.google.inject.Inject;
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
import sk.baka.webvm.analyzer.ProblemReport;
import sk.baka.webvm.config.Config;

/**
 * Delivers miscellaneous notifications (mail, jabber).
 * @author Martin Vysny
 */
public final class NotificationDelivery extends BackgroundService {

    /**
     * Creates new deliverer.
     */
    public NotificationDelivery() {
        super("Notificator", 1);
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
        mail.setSubject("WebMon: Problems notification" + (testing ? " (testing mail)" : ""));
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

    /**
     * Sends a Jabber message with the Problems analysis.
     * @param config use this config.
     * @param testing if true then a (testing) string is appended to the message
     * @param reports use these reports
     * @throws org.jivesoftware.smack.XMPPException when send fails
     */
    public static void sendJabber(final Config config, final boolean testing, final List<ProblemReport> reports) throws XMPPException {
        if (!isJabberEnabled(config)) {
            return;
        }
        XMPPConnection connection = new XMPPConnection(config.jabberServer);
        connection.connect();
        try {
            connection.login(config.jabberUsername, config.jabberPassword);
            final StringTokenizer t = new StringTokenizer(config.jabberRecipients, ",");
            for (; t.hasMoreTokens();) {
                final String recipient = t.nextToken().trim();
                final Chat chat = connection.getChatManager().createChat(recipient, "WebMon", new MessageListener() {

                    public void processMessage(Chat chat, Message message) {
                        // do nothing
                    }
                });
                chat.sendMessage("WebMon Problems report: " + (testing ? "(testing)" : "") + "\n" + ProblemReport.toString(reports, "\n"));
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Delivers given report asynchronously.
     * @param reports the reports to deliver.
     */
    public void deliverAsync(final List<ProblemReport> reports) {
        try {
            asyncQueue.put(reports);
        } catch (InterruptedException ex) {
            // we do not have a space-bound queue
            throw new AssertionError(ex);
        }
    }
    private final BlockingQueue<List<ProblemReport>> asyncQueue = new LinkedBlockingQueue<List<ProblemReport>>();

    @Override
    protected void started(ScheduledExecutorService executor) {
        executor.execute(new Notificator());
    }
    @Inject
    private volatile Config config;

    /**
     * Notifies the deliverer that the config has been changed.
     */
    public void configChanged() {
        // you're probably wondering what the hell does this mean.
        // This method is invoked when a property of a config has been changed. This change was performed in thread A
        // (a thread which invokes this method), however the fields are read in thread B. To force the thread B to see changes
        // done by thread A we have to form a happens-before relationship. Actions preceding a write to a volatile field happens-before
        // subsequent read of the same field. Therefore, if we write something to the "config" field, other threads
        // reading the "config" field will see the changes. Therefore, we will just write the same value.
        // This is actually not completely thread-safe as the thread B actually MAY see changes made by thread B. Well,
        // nothing's perfect :-) And we actually don't mind - all values in the Config class are independent from each other
        // (so there is nothing to break if we see incomplete changes), and the objects themselves are thread-safe.
        final Config cfg = config;
        config = cfg;
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
                    LOG.log(Level.SEVERE, "Failed to send email", ex);
                }
                try {
                    NotificationDelivery.sendJabber(config, false, reports);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Failed to send jabber message", ex);
                }
            }
        }
    }
    private static final Logger LOG = Logger.getLogger(NotificationDelivery.class.getName());
}
