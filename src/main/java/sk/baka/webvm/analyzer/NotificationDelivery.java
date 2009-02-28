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
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import sk.baka.webvm.config.Config;

/**
 * Delivers miscellaneous notifications (mail, jabber).
 * @author Martin Vysny
 */
public final class NotificationDelivery {

    private NotificationDelivery() {
        throw new AssertionError();
    }

    /**
     * Sends a mail with given report.
     * @param config the mail server configuration.
     * @param testing if true then a testing mail is sent
     * @param reports the current reports
     * @throws org.apache.commons.mail.EmailException if sending mail fails.
     */
    public static void sendEmail(final Config config, final boolean testing, final List<ProblemReport> reports) throws EmailException {
        if (config.mailSmtpHost == null) {
            return;
        }
        final Email mail = new SimpleEmail();
        configure(mail, config);
        mail.setSubject("WebVM: Problems notification" + (testing ? " (testing mail)" : ""));
        mail.setMsg(ProblemReport.toString(reports, "\n"));
        mail.send();
    }

    private static void configure(final Email mail, final Config config) throws EmailException {
        mail.setHostName(config.mailSmtpHost);
        config.mailSmtpEncryption.activate(mail);
        if (mail.isSSL()) {
            mail.setSslSmtpPort(Integer.toString(config.mailSmtpPort));
        } else {
            mail.setSmtpPort(config.mailSmtpPort);
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
}
