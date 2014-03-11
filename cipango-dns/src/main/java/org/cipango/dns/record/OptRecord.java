package org.cipango.dns.record;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.cipango.dns.Compression;
import org.cipango.dns.Type;
import org.cipango.dns.util.BufferUtil;

/**
 * An OPT is called a pseudo-RR because it pertains to a particular transport level message and not
 * to any actual DNS data. OPT RRs shall never be cached, forwarded, or stored in or loaded from
 * master files.
 * 
 * <pre>
 *                 +0 (MSB)                            +1 (LSB)
 *     +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 *  0: |                          OPTION-CODE                          |
 *     +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 *  2: |                         OPTION-LENGTH                         |
 *     +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 *  4: |                                                               |
 *     /                          OPTION-DATA                          /
 *     /                                                               /
 *     +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 * </pre>
 * 
 * OPTION-CODE (Assigned by IANA.) 
 * 
 * OPTION-LENGTH Size (in octets) of OPTION-DATA.
 * 
 * OPTION-DATA Varies per OPTION-CODE.
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2671.html">RFC 2671 - Extension Mechanisms for DNS
 *      (EDNS0)</a>
 */
public class OptRecord extends Record
{
	
	public OptRecord()
	{
		
	}
	
	/**
	 * 
	 * @param maxPayloadSize
	 * @param extendedRcode 
	 * @param version  Indicates the implementation level of whoever sets it. Should be 0 for cipango-dns
	 * @param z Set to zero by senders and ignored by receivers, unless modified in a subsequent specification.
	 */
	public OptRecord(int maxPayloadSize, int extendedRcode, int version, int z)
	{
		setDnsClass(BufferUtil.check16("max payload size", maxPayloadSize));
		BufferUtil.check8("Extended RCODE", extendedRcode);
		BufferUtil.check8("Version", version);
		BufferUtil.check16("z", z);
		setTtl((extendedRcode << 24) + (version << 16) + z);
	}
	
	public OptRecord(int maxPayloadSize)
	{
		this(maxPayloadSize, 0, 0, 0);
	}
	
	public int getMaxPayloadSize() {
		return getDnsClass();
	}
	
	public int getExtendedRCode() {
		return (getTtl() >> 24) &0xFF;
	}
	
	public int getVersion() {
		return (getTtl() >> 16) &0xFF;
	}
	
	public int getZ() {
		return getTtl() &0xFFFF;
	}
	
	@Override
	public Type getType()
	{
		return Type.OPT;
	}
	
	@Override
	public void doEncode(ByteBuffer b, Compression c) throws IOException
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void doDecode(ByteBuffer b, Compression c, int dataLength) throws IOException
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean doEquals(Record record)
	{
		// TODO Auto-generated method stub
		return true;
	}
	
}
