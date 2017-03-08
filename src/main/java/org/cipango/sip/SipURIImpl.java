package org.cipango.sip;

import org.cipango.util.Scanner;
import org.cipango.util.StringUtil;
import org.cipango.util.TypeUtils;

import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;
import java.text.ParseException;
import java.util.BitSet;
import java.util.Iterator;

public class SipURIImpl implements SipURI {

    private static final BitSet PARAM_SEPARATORS = SipRules.fromChars(";=?");

    enum Param {
        TRANSPORT, TTL, MADDR, METHOD, USER, LR;

        private String string;
        Param() { string = name().toLowerCase(); }
        public String asString() { return string; }
        @Override public String toString() { return asString(); }
    }

    private String user;
    private String password;
    private String host;

    private boolean secure;
    private int port;

    protected SipURIImpl() {
        this.port = -1;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public void setUser(String s) {
        this.user = user;
    }

    @Override
    public String getUserPassword() {
        return password;
    }

    @Override
    public void setUserPassword(String s) {
        this.password = password;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        if (StringUtil.isEmpty(host))
            throw new IllegalArgumentException("host cannot be empty");

        if (!SipRules.isValid(host, SipRules.HOSTNAME))
            throw new IllegalArgumentException("invalid host");

        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        if (port < 0)
            port = -1;

        if (port > 65535)
            throw new IllegalArgumentException("invalid port");

        this.port = port;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public void setSecure(boolean b) {
        this.secure = true;
    }

    @Override
    public String getTransportParam() {
        return getParameter(Param.TRANSPORT.asString());
    }

    @Override
    public void setTransportParam(String transport) {
        setParameter(Param.TRANSPORT.asString(), transport);
    }

    @Override
    public String getMAddrParam() {
        return getParameter(Param.MADDR.asString());
    }

    @Override
    public void setMAddrParam(String maddr) {
        setParameter(Param.MADDR.asString(), maddr);
    }

    @Override
    public String getMethodParam() {
        return getParameter(Param.METHOD.asString());
    }

    @Override
    public void setMethodParam(String method) {
        setParameter(Param.METHOD.asString(), method);
    }

    @Override
    public int getTTLParam() {
        return TypeUtils.toInt(getParameter(Param.TTL.asString()), -1);
    }

    @Override
    public void setTTLParam(int ttl) {
        if (ttl < 0)
            removeParameter(Param.TTL.asString());
        else
            setParameter(Param.TTL.asString(), String.valueOf(ttl));
    }

    @Override
    public String getUserParam() {
        return getParameter(Param.USER.asString());
    }

    @Override
    public void setUserParam(String user) {
        setParameter(Param.USER.asString(), user);
    }

    @Override
    public boolean getLrParam() {
        return "".equals(getParameter(Param.LR.asString()));
    }

    @Override
    public void setLrParam(boolean b) {
        setParameter(Param.LR.asString(), "");
    }

    @Override
    public String getHeader(String s) {
        return null;
    }

    @Override
    public void setHeader(String s, String s1) {

    }

    @Override
    public void removeHeader(String s) {

    }

    @Override
    public Iterator<String> getHeaderNames() {
        return null;
    }

    @Override
    public String getScheme() {
        return secure ? SipScheme.SIPS.asString() : SipScheme.SIP.asString();
    }

    @Override
    public boolean isSipURI() {
        return true;
    }

    @Override
    public String getParameter(String s) {
        return null;
    }

    @Override
    public void setParameter(String s, String s1) {

    }

    @Override
    public void removeParameter(String s) {

    }

    @Override
    public Iterator<String> getParameterNames() {
        return null;
    }

    @Override
    public URI clone() {
        return null;
    }

    public static SipURIImpl parseURI(String s) throws ParseException {

        SipURIImpl uri = new SipURIImpl();

        int i = 0;
        if (StringUtil.startsWithIgnoreCase(s, "sip:")) {
            i = 4;
        } else if (StringUtil.startsWithIgnoreCase(s, "sips:")) {
            i = 5;
            uri.secure = true;
        } else {
            throw new ParseException("invalid scheme", 0);
        }

        Scanner scanner = new Scanner(s, i);

        if (scanner.indexOf('@') != -1) { // user/password

            uri.user = StringUtil.decode(scanner.read(SipRules.ESCAPED_USER));
            if (StringUtil.isEmpty(uri.user))
                throw new ParseException("Empty user", scanner.getPosition());

            if (scanner.peek() == ':') {
                scanner.consume(1);
                uri.password = StringUtil.decode(scanner.read(SipRules.ESCAPED_PASSWORD));
            }

            scanner.match('@');
        }

        uri.host = scanner.read(SipRules.HOSTNAME); // TODO IPv6

        if (StringUtil.isEmpty(uri.host))
            throw new ParseException("Empty host", scanner.getPosition());

        if (scanner.peek() == ':') {
            scanner.consume(1);
            uri.port = scanner.readInt();
        }

        while (scanner.peek() == ';') {

            scanner.consume(1);
            String name = scanner.readUntil(PARAM_SEPARATORS);
            String value = "";

            if (scanner.peek() == '=') {
                scanner.consume(1);
                value = scanner.readUntil(PARAM_SEPARATORS);
            }
        }

        return uri;
    }
}
