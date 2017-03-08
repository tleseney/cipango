package org.cipango.util;

import org.cipango.sip.SipRules;

import java.text.ParseException;
import java.util.BitSet;

public class Scanner {

    private String string;
    private int position;
    private int end;

    public Scanner(String s) {
        this.string = s;
        end = string.length();
    }

    public Scanner(String s, int start) {
        this(s);
        position = start;
        if (position > end)
            position = end;
    }

    public int getPosition() {
        return position;
    }

    public boolean eof() {
        return position >= end;
    }

    public char peek() {
        if (eof())
            return 0;
        return string.charAt(position);
    }

    public Scanner consume(int n) {
        position = position + n;
        if (position > end)
            position = end;
        return this;
    }

    public String read(BitSet validChars) {
        int start = position;
        while(position < end && validChars.get(string.charAt(position)))
            position++;
        return string.substring(start, position);
    }

    public String readUntil(BitSet separatorChars) {
        int start = position;
        while (position < end && !separatorChars.get(string.charAt(position)))
            position++;
        return string.substring(start, position);
    }

    public String readUntilSpace() {
        int start = position;
        while (position < end && !Character.isSpaceChar(string.charAt(position)))
            position++;
        return string.substring(start, position);
    }

    public int readInt() throws ParseException {
        int i = position;
        while (position < end && Character.isDigit(string.charAt(position)))
            position++;
        try {
            return Integer.parseInt(string.substring(i, position));
        } catch (Exception e) {
            throw new ParseException("Invalid number", i);
        }
    }

    public Scanner match(char c) throws ParseException {
        if (eof() || string.charAt(position) != c)
            throw new ParseException("Excepting " + c, position);

        position++;
        return this;
    }

    public Scanner skipSpace() {
        while (position < end && Character.isSpaceChar(string.charAt(position)))
            position++;
        return this;
    }

    public Scanner matchSpace() throws ParseException {
        int start = position;
        skipSpace();
        if (position == start)
            throw new ParseException("Expecting space", position);
        return this;
    }

    public String token() throws ParseException {
        String token = read(SipRules.TOKEN);
        if (token.length() == 0)
            throw new ParseException("Expecting token", position);
        return token;
    }



    public int indexOf(char c) {
        return string.indexOf(c, position);
    }
}
