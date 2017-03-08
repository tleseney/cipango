package org.cipango.util;

import org.junit.Test;

import java.util.BitSet;

import static org.junit.Assert.*;

public class StringUtilTest {

    @Test
    public void testEquals() {
        assertTrue(StringUtil.startsWithIgnoreCase("", ""));
        assertTrue(StringUtil.startsWithIgnoreCase("SiP:localhost", "sip:"));
        assertFalse(StringUtil.startsWithIgnoreCase("sips:localhost", "sip:"));
    }

    @Test
    public void testEncode() {

        BitSet bs = new BitSet();
        bs.set('a', 'z');
        assertEquals("francois", StringUtil.encode("francois", bs));
        assertEquals("fran%c3%a7ois%20%49", StringUtil.encode("françois I", bs));
    }

    @Test
    public void testDecode() throws Exception {
        assertEquals("françois", StringUtil.decode("fran%c3%a7ois"));
        assertEquals("françois", StringUtil.decode("françois"));
    }
}
