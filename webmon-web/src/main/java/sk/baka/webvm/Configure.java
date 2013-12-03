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
package sk.baka.webvm;

import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.validator.RangeValidator;
import sk.baka.webvm.analyzer.IHistorySampler;
import sk.baka.webvm.analyzer.IProblemAnalyzer;
import sk.baka.webvm.analyzer.utils.NotificationDelivery;
import sk.baka.webvm.analyzer.config.Bind;
import sk.baka.webvm.analyzer.config.Config;

/**
 * The WebMon configuration. The configuration is not persistent.
 * @author Martin Vysny
 */
public final class Configure extends WebVMPage {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the configure page.
     */
    public Configure() {
        // submitting forms in a page with multiple forms fails: http://issues.apache.org/jira/browse/WICKET-2134
        border.add(new ConfigForm("problemsForm", Config.GROUP_PROBLEMS));
        border.add(new MailForm("mailForm"));
        border.add(new JabberForm("jabberForm"));
    }

    @SuppressWarnings("unchecked")
    private static void bind(final MarkupContainer c, final Object bean, final int group) {
        for (final Field field : bean.getClass().getFields()) {
            final Bind annotation = field.getAnnotation(Bind.class);
            if (annotation == null) {
                continue;
            }
            if (group >= 0 && annotation.group() != group) {
                continue;
            }
            final FormComponent<?> f;
            if (Enum.class.isAssignableFrom(field.getType())) {
                final Class<? extends Enum> clazz = field.getType().asSubclass(Enum.class);
                final EnumSet<? extends Enum> allConstants = EnumSet.allOf(clazz);
                f = new DropDownChoice<Enum>(annotation.key(), new PropertyModel<Enum>(bean, field.getName()), new ArrayList<Enum>(allConstants));
            } else if (annotation.password()) {
                f = new PasswordTextField(annotation.key(), new PropertyModel<String>(bean, field.getName()));
            } else {
                final TextField<Object> tf = new TextField<Object>(annotation.key(), new PropertyModel<Object>(bean, field.getName()));
                if ((annotation.min() != Integer.MIN_VALUE) || (annotation.max() != Integer.MAX_VALUE)) {
                    // we know there will be only ints annotated
                    tf.add((IValidator) new RangeValidator<Integer>(annotation.min(), annotation.max()));
                }
                f = tf;
            }
            f.setRequired(annotation.required());
            c.add(f);
        }
    }

    /**
     * A basic configuration form, handles retrieval of config object.
     */
    protected static class ConfigForm extends Form<Config> {

        private static final long serialVersionUID = 1L;
        /**
         * The configuration object.
         */
        @Inject
        private Config config;

        /**
         * Creates new form.
         * @param componentName wicket id
         * @param group bind only this group to the form, -1 to all groups
         */
        public ConfigForm(final String componentName, final int group) {
            super(componentName);
            add(new FeedbackPanel("feedback"));
            bind(this, config, group);
            add(new Button("submit"));
        }

        @Override
        public final void onSubmit() {
            WicketApplication.configChanged();
        }
    }

    /**
     * A mail form, supports sending of a test mail.
     */
    private static class MailForm extends ConfigForm {

        private static final long serialVersionUID = 1L;

        public MailForm(String componentName) {
            super(componentName, Config.GROUP_MAIL);
            add(new Button("sendTestMail") {

                private static final long serialVersionUID = 1L;
                @Inject
                private IProblemAnalyzer pa;
                @Inject
                private IHistorySampler hs;
                @Inject
                private Config config;

                @Override
                public void onSubmit() {
                    if (!NotificationDelivery.isEmailEnabled(config)) {
                        error("No SMTP host name is entered, mail notification is disabled");
                        return;
                    }
                    try {
                        NotificationDelivery.sendEmail(config, true, pa.getProblems(hs.getVmstatHistory()));
                    } catch (Exception ex) {
                        error("Failed to send a message: " + ex.toString());
                    }
                }
            });
        }
    }

    /**
     * A Jabber configuration form, supports sending a simple notification Jabber message.
     */
    private static class JabberForm extends ConfigForm {

        private static final long serialVersionUID = 1L;

        public JabberForm(String componentName) {
            super(componentName, Config.GROUP_JABBER);
            add(new Button("sendTestJabber") {

                private static final long serialVersionUID = 1L;
                @Inject
                private IProblemAnalyzer pa;
                @Inject
                private Config config;
                @Inject
                private IHistorySampler hs;

                @Override
                public void onSubmit() {
                    if (!NotificationDelivery.isJabberEnabled(config)) {
                        error("No Jabber server is entered, jabber notification is disabled");
                        return;
                    }
                    try {
                        NotificationDelivery.sendJabber(config, true, pa.getProblems(hs.getVmstatHistory()));
                    } catch (Exception ex) {
                        error("Failed to send a message: " + ex.toString());
                    }
                }
            });
        }
    }
}

