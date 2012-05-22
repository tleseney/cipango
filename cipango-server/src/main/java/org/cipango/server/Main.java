package org.cipango.server;

import java.util.zip.CRC32;

import org.cipango.server.nio.UdpConnector;
import org.eclipse.jetty.util.TypeUtil;

public class Main 
{
	public static final byte[] intToByteArray(int value) { return new byte[]{ (byte)(value >>> 24), (byte)(value >> 16 & 0xff), (byte)(value >> 8 & 0xff), (byte)(value & 0xff) }; }
	
	public static int hashCode(String s) {
		int hash = 5381;


			char[] a = s.toCharArray();
			int i = 0;
			for (; i<a.length; i++) {
				hash = ((hash << 5) + hash) + a[i]; /* hash * 33 + c */
			}
		
		return hash;
	}
	
	public static int hashCode2(String s) {
		int hash = 5381;


			byte[] a = s.getBytes();
			int i = 0;
			for (; i<a.length; i++) {
				hash = ((hash << 5) + hash) + a[i]; /* hash * 33 + c */
			}
		
		return hash;
	}
	public static void main(String[] args) throws Exception
	{		
		CRC32 crc = new CRC32();
		crc.update("a".getBytes());
		
		System.out.println(Integer.toHexString(hashCode("/fjerlkfjrelkfjlj")));
		System.out.println(Integer.toHexString(hashCode2("ded")));
		System.out.println(TypeUtil.toHexString(intToByteArray(hashCode2("ded"))));
		System.out.println(Integer.toHexString("nena".hashCode()));
		
		
		/*
		
		SipServer server = new SipServer();
		
		UdpConnector connector = new UdpConnector();
		connector.setHost("192.168.2.127");
		
		server.addConnector(connector);
		
		server.start();
		*/
	}
}
