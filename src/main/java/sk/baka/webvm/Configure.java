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
package sk.baka.webvm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
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
import sk.baka.webvm.misc.NotificationDelivery;
import sk.baka.webvm.analyzer.ProblemAnalyzer;
import sk.baka.webvm.config.Bind;
import sk.baka.webvm.config.Config;

/**
 * The WebVM configuration. The configuration is not persistent.
 * @author Martin Vysny
 */
public final class Configure extends WebVMPage {

    public Configure() {
        border.add(new ConfigForm("configForm"));
    }

    private final class ConfigForm extends Form {

        private final Config config = new Config(WicketApplication.getConfig());

        public ConfigForm(final String componentName) {
            super(componentName);
            add(new FeedbackPanel("feedback"));
            for (final Field field : Config.class.getFields()) {
                final Bind annotation = field.getAnnotation(Bind.class);
                if (annotation == null) {
                    continue;
                }
                final FormComponent<?> f;
                if (Enum.class.isAssignableFrom(field.getType())) {
                    final Set<Enum> allConstants = EnumSet.allOf(field.getType().asSubclass(Enum.class));
                    f = new DropDownChoice<Enum>("mail.smtp.encryption", new PropertyModel(config, field.getName()), new ArrayList<Enum>(allConstants));
                } else if (annotation.password()) {
                    f = new PasswordTextField(annotation.key(), new PropertyModel(config, field.getName()));
                } else {
                    f = new TextField<Object>(annotation.key(), new PropertyModel<Object>(config, field.getName()));
                    if ((annotation.min() != Integer.MIN_VALUE) || (annotation.max() != Integer.MAX_VALUE)) {
                        // we know there will be only ints annotated
                        f.add((IValidator) new RangeValidator<Integer>(annotation.min(), annotation.max()));
                    }
                }
                f.setRequired(annotation.required());
                add(f);
            }
            add(new Button("submit"));
            add(new Button("sendTestMail") {

                @Override
                public void onSubmit() {
                    if (!NotificationDelivery.isEmailEnabled(config)) {
                        error("No SMTP host name is entered, mail notification is disabled");
                        return;
                    }
                    final ProblemAnalyzer pa = new ProblemAnalyzer();
                    pa.configure(config);
                    try {
                        NotificationDelivery.sendEmail(config, true, pa.getProblems(WicketApplication.getHistory().getVmstatHistory()));
                    } catch (Exception ex) {
                        error("Failed to send a message: " + ex.toString());
                    }
                }
            });
            add(new Button("sendTestJabber") {

                @Override
                public void onSubmit() {
                    if (!NotificationDelivery.isJabberEnabled(config)) {
                        error("No Jabber server is entered, jabber notification is disabled");
                        return;
                    }
                    final ProblemAnalyzer pa = new ProblemAnalyzer();
                    pa.configure(config);
                    try {
                        NotificationDelivery.sendJabber(config, true, pa.getProblems(WicketApplication.getHistory().getVmstatHistory()));
                    } catch (Exception ex) {
                        error("Failed to send a message: " + ex.toString());
                    }
                }
            });
        }

        @Override
        public final void onSubmit() {
            WicketApplication.setConfig(config);
        }
    }
}

