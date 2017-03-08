package org.cipango.sip;

import org.junit.Test;

import javax.servlet.sip.SipURI;
import java.text.ParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SipURIImplTest {

    String[] invalidURIs = {
            "",
            "foo:bar",
            "http://cipango.org",
            "sip",
            "sip:",
            "sip::",
            "sip:@",
            "sip:@atlanta.com",
            "sip:alice@",
            "sip:alice:pwd@",
            "sip:alice@:5060",
            "sip:alice@atlanta:",
            "sip:alice@atlanta:com",
            "sip:atlanta:",
            "sip:atlanta:com"
    };

    Object[][] uris = {
            { "sip:alice@atlanta.com", "alice" , null, "atlanta.com", -1},
            { "sip:alice:@atlanta.com:5070", "alice" , "", "atlanta.com", 5070},
            { "sip:alice:pwd@atlanta.com:5080", "alice" , "pwd", "atlanta.com", 5080},
            { "sip:%61lice@atlanta.com", "alice", null, "atlanta.com", -1 },
    };

    @Test
    public void testParse() {
        for (int i = 0; i < uris.length; i++) {

            try {
                SipURI uri = parseURI((String) uris[i][0]);

                assertEquals(uris[i][1], uri.getUser());
                assertEquals(uris[i][2], uri.getUserPassword());
                assertEquals(uris[i][3], uri.getHost());
                assertEquals(uris[i][4], uri.getPort());

            } catch (Exception e) {
                fail(uris[i][0] + ": " + e.getMessage());
            }
        }
    }

    @Test
    public void testInvalidURIs() throws Exception {

        for (String uri : invalidURIs) {
            try {
                parseURI(uri);
                fail("Expected invalid: " + uri);
            } catch (ParseException e) {
            }

        }
    }

    protected SipURI parseURI(String s) throws Exception {
        return SipURIImpl.parseURI(s);
    }
}
