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
package sk.baka.webvm.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple binder which transfers properties from a bean to a Property instance.
 * @author Martin Vysny
 */
public final class Binder {

    /**
     * Copies all properties annotated by {@link Bind} to the view.
     *
     * @param bean
     *            the bean to copy from/to
     * @param properties the properties to copy from/to
     * @param beanToMap
     *            if <code>true</code> then data from the bean are copied into
     *            the view. If <code>false</code> then view's data will be
     *            copied into the bean.
     * @param validate
     *            if <code>true</code> then validation constraints are
     *            checked.
     * @return an empty map if no validation constraints were violated; a map of
     *         a property key to an error message if violation occurred.
     */
    public static Map<String, String> bindBeanMap(final Object bean,
            final Properties properties, final boolean beanToMap, final boolean validate) {
        if (bean == null) {
            throw new IllegalArgumentException("bean");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }
        final Map<String, String> result = new HashMap<String, String>();
        try {
            for (final Field field : bean.getClass().getFields()) {
                final Bind annotation = field.getAnnotation(Bind.class);
                if (annotation == null) {
                    continue;
                }
                if (beanToMap) {
                    bindBeanToMap(field, bean, validate, result, properties);
                } else {
                    bindMapToBean(field, properties, result, validate, bean);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read/write a field", e);
        }
        return result;
    }

    /**
     * Logs all warnings to given logger.
     * @param log log warnings here
     * @param warnings the warnings.
     */
    public static void log(final Logger log, final Map<String, String> warnings) {
        for (final Map.Entry<String, String> e : warnings.entrySet()) {
            log.log(Level.WARNING, e.getKey() + ": " + e.getValue());
        }
    }

    private static void bindBeanToMap(final Field sourceField, final Object sourceBean, final boolean validate, final Map<String, String> validation, final Properties target) throws IllegalArgumentException, IllegalAccessException {
        final Bind annotation = sourceField.getAnnotation(Bind.class);
        final Object fieldValue = sourceField.get(sourceBean);
        if (validate) {
            validation.putAll(checkValidity(fieldValue, annotation));
        }
        if (fieldValue != null) {
            target.put(annotation.key(), fieldValue.toString());
        } else {
            target.remove(annotation.key());
        }
    }

    private static void bindMapToBean(final Field targetField, final Properties source, final Map<String, String> validation, final boolean validate, final Object targetBean) throws IllegalArgumentException, IllegalAccessException {
        final Bind annotation = targetField.getAnnotation(Bind.class);
        final Class<?> fieldClass = primitiveToClass(targetField.getType());
        final String value = source.getProperty(annotation.key());
        if (value == null) {
            return;
        }
        final Object fieldValue;
        try {
            fieldValue = stringToValue(value.trim(), fieldClass);
        } catch (final Exception ex) {
            validation.put(annotation.key(), "Failed to parse " + value + ": " + ex.toString());
            return;
        }
        if (validate) {
            validation.putAll(checkValidity(fieldValue, annotation));
        }
        targetField.set(targetBean, fieldValue);
        return;
    }

    private static Object stringToValue(final String value, final Class<?> requiredClass) {
        if (requiredClass == String.class) {
            return value;
        }
        if (requiredClass == Integer.class) {
            return Integer.parseInt(value);
        }
        if (Enum.class.isAssignableFrom(requiredClass)) {
            return Enum.valueOf(requiredClass.asSubclass(Enum.class), value);
        }
        throw new RuntimeException("Unsupported class: " + requiredClass);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> checkValidity(final Object object,
            Bind annotation) {
        if (object instanceof Number) {
            final int intVal = ((Number) object).intValue();
            if (intVal < annotation.min()) {
                final String errMsg = annotation.key() + ": Number too small: " + intVal + " must be at least " + annotation.min();
                return Collections.singletonMap(annotation.key(), errMsg);
            }
            if (intVal > annotation.max()) {
                final String errMsg = annotation.key() + ": number too big: " + intVal + " must be at most " + annotation.min();
                return Collections.singletonMap(annotation.key(), errMsg);
            }
        }
        return Collections.emptyMap();
    }
    private final static Map<Class<?>, Class<?>> primitiveToClass = new HashMap<Class<?>, Class<?>>();


    static {
        primitiveToClass.put(int.class, Integer.class);
        primitiveToClass.put(double.class, Double.class);
        primitiveToClass.put(float.class, Float.class);
        primitiveToClass.put(boolean.class, Boolean.class);
        primitiveToClass.put(byte.class, Byte.class);
        primitiveToClass.put(char.class, Character.class);
        primitiveToClass.put(long.class, Long.class);
    }

    private static Class<?> primitiveToClass(final Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            return clazz;
        }
        return primitiveToClass.get(clazz);
    }

    /**
     * Copies all bindable properties from given bean to given bean.
     *
     * @param <T>
     *            the bean type
     * @param from
     *            source bean
     * @param to
     *            target bean
     */
    public static <T> void copy(T from, T to) {
        try {
            for (final Field field : from.getClass().getFields()) {
                final Bind annotation = field.getAnnotation(Bind.class);
                if (annotation == null) {
                    continue;
                }
                if (!Modifier.isPublic(field.getModifiers())) {
                    continue;
                }
                final Object fieldValue = field.get(from);
                field.set(to, fieldValue);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Binder() {
        throw new AssertionError();
    }
}
