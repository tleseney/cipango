package org.cipango.sip;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.ArrayTernaryTrie;
import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.Trie;

import java.util.Map;

public enum SipVersion {

    SIP_2_0("SIP/2.0");

    private String string;

    SipVersion(String string) {
        this.string = string;
    }

    public String asString() {
        return string;
    }

    @Override public String toString() {
        return asString();
    }

    public static Trie<SipVersion> CACHE = new ArrayTrie<>();

    static {
        CACHE.put(SIP_2_0.asString(), SIP_2_0);
    }
}
