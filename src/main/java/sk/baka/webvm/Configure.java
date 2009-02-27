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
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.PropertyModel;
import sk.baka.webvm.config.Bind;
import sk.baka.webvm.config.Config;

/**
 * The WebVM configuration. The configuration is not persistent.
 * @author Martin Vysny
 */
public final class Configure extends WebPage {

    public Configure(PageParameters params) {
        final AppBorder border = new AppBorder("appBorder");
        add(border);
        border.add(new ConfigForm("configForm"));
    }

    private final class ConfigForm extends Form {

        private final Config config = new Config();

        public ConfigForm(final String componentName) {
            super(componentName);
            for (final Field field : Config.class.getFields()) {
                final Bind annotation = field.getAnnotation(Bind.class);
                if (annotation == null) {
                    continue;
                }
                if (Enum.class.isAssignableFrom(field.getType())) {
                    final Set<Enum> allConstants = EnumSet.allOf(field.getType().asSubclass(Enum.class));
                    add(new DropDownChoice<Enum>("mail.smtp.encryption", new PropertyModel(config, field.getName()), new ArrayList<Enum>(allConstants)));
                } else {
                    add(new TextArea(annotation.key(), new PropertyModel(config, field.getName())));
                }
            }
        }

        public final void onSubmit() {
        }
    }
}

