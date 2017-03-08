package org.cipango.sip;

import java.util.BitSet;

public class SipRules {

    public static final char SP = 0x20;

    public static final byte ESCAPE_CHAR = '%';

    public static final String ALPHA_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String DIGIT_CHARS = "0123456789";
    public static final String ALPHANUM_CHARS = ALPHA_CHARS + DIGIT_CHARS;
    public static final String MARK_CHARS = "-_.!~*'()";
    public static final String UNRESERVED_CHARS = ALPHANUM_CHARS + MARK_CHARS;

    public static final String PASSWORD_CHARS = UNRESERVED_CHARS + "&=+$,";
    public static final String USER_CHARS = UNRESERVED_CHARS + "&=+$,;?/";
    public static final String HOSTNAME_CHARS =ALPHANUM_CHARS + "-.";
    public static final String PARAM_CHARS = "[]/:&+$" +  UNRESERVED_CHARS;

    public static final String TOKEN_CHARS = "-.!%*_+`'~" + ALPHA_CHARS;

    public static final BitSet USER = fromChars(USER_CHARS);
    public static final BitSet ESCAPED_USER = fromChars(USER_CHARS + ESCAPE_CHAR);

    public static final BitSet PASSWORD = fromChars(PASSWORD_CHARS);
    public static final BitSet ESCAPED_PASSWORD = fromChars(PASSWORD_CHARS + ESCAPE_CHAR);

    public static final BitSet HOSTNAME = fromChars(HOSTNAME_CHARS);

    public static final BitSet ESCAPED_PARAM = fromChars(PARAM_CHARS + ESCAPE_CHAR);


    public static final BitSet TOKEN = fromChars(TOKEN_CHARS);


    public static final BitSet fromChars(String s) {
        BitSet bs = new BitSet(256);
        for (int i = 0; i < s.length(); i++)
            bs.set(s.charAt(i));
        return bs;
    }

    public static boolean isValid(String s, BitSet bs) {
        for (int i = 0; i < s.length(); i++) {
            if (!bs.get(s.charAt(i)))
                return false;
        }
        return true;
    }
}
