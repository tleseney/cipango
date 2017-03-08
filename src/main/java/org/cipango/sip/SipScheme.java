package org.cipango.sip;

import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.Trie;

public enum SipScheme {

    SIP("sip"), SIPS("sips");

    public static final Trie<SipScheme> CACHE = new ArrayTrie<>();

    static {
        for (SipScheme scheme : SipScheme.values()) {
            CACHE.put(scheme.asString(), scheme);
        }
    }

    private String string;

    SipScheme(String s) {
        string = s;
    }

    public String asString() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

}
