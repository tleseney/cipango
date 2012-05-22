package org.cipango.sip;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.sip.Parameterable;

import org.cipango.util.StringScanner;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParametersTest 
{
	@Test
	public void testParse() throws Exception
	{
		String s = ";   p1 =   v1;    p2=   \"v2\"  ;  p3; p4=v4";
		Parameters parameters = new Parameters();
		parameters.parseParameters(new StringScanner(s));
		
		assertEquals("v1", parameters.getParameter("p1"));
		assertEquals("v2", parameters.getParameter("p2"));
		assertEquals("", parameters.getParameter("p3"));
		assertEquals("v4", parameters.getParameter("p4"));
		
	}
}
