package org.cipango.util;

public class TypeUtils {

    public static int toInt(String s, int defaultValue) {
        if (s == null)
            return defaultValue;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
