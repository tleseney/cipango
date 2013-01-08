// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================
package org.cipango.dns;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.cipango.dns.record.ARecord;
import org.cipango.dns.record.AaaaRecord;
import org.cipango.dns.record.CnameRecord;
import org.cipango.dns.record.GenericRecord;
import org.cipango.dns.record.NaptrRecord;
import org.cipango.dns.record.NsRecord;
import org.cipango.dns.record.PtrRecord;
import org.cipango.dns.record.Record;
import org.cipango.dns.record.SoaRecord;
import org.cipango.dns.record.SrvRecord;
import org.cipango.dns.util.BufferUtil;

public enum Type
{
	/**
	 * host address
	 */
	A(1, ARecord.class),
	/**
	 * an authoritative name server
	 */
	NS(2, NsRecord.class),
	/**
	 * mail destination (Obsolete - use MX)
	 * 
	 * @deprecated
	 */
	MD(3),
	/**
	 * a mail forwarder (Obsolete - use MX)
	 * 
	 * @deprecated
	 */
	MF(4),
	/**
	 * the canonical name for an alias
	 */
	CNAME(5, CnameRecord.class),
	/**
	 * marks the start of a zone of authority
	 */
	SOA(6, SoaRecord.class),
	/**
	 * a mailbox domain name (EXPERIMENTAL)
	 */
	MB(7),
	/**
	 * a mail group member (EXPERIMENTAL)
	 */
	MG(8),
	/**
	 * a mail rename domain name (EXPERIMENTAL)
	 */
	MR(9),
	/**
	 * a null RR (EXPERIMENTAL)
	 */
	NULL(10),
	/**
	 * a well known service description
	 */
	WKS(11),
	/**
	 * a domain name pointer
	 */
	PTR(12, PtrRecord.class),
	/**
	 * host information
	 */
	HINFO(13),
	/**
	 * mailbox or mail list information
	 */
	MINFO(14),
	/**
	 * mail exchange
	 */
	MX(15),
	/**
	 * text strings
	 */
	TXT(16),
	/** 
	 * Responsible person 
	 */
	RP(17),
	/** AFS cell database */
	AFSDB(18),
	/** X.25 calling address */
	X25(19),
	/** ISDN calling address */
	ISDN(20),
	/** Router */
	RT(21),
	/** NSAP address */
	NSAP(22),
	/** 
	 * Reverse NSAP address
	 * @deprecated
	 * */
	NSAP_PTR(23),
	/** Signature */
	SIG(24),
	/** Key */
	KEY(25),
	/** X.400 mail mapping */
	PX(26),
	/** Geographical position (withdrawn) */
	GPOS(27),
	/** IPv6 address */
	AAAA(28, AaaaRecord.class),
	/** Location */
	LOC(29),
	/** Next valid name in zone */
	NXT(30),
	/** Endpoint identifier */
	EID(31),
	/** Nimrod locator */
	NIMLOC(32),
	/** Server selection */
	SRV(33, SrvRecord.class),
	/** ATM address */
	ATMA(34),
	/** Naming authority pointer */
	NAPTR(35, NaptrRecord.class),
	/** Key exchange */
	KX(36),
	/** Certificate */
	CERT(37),
	/** IPv6 address (experimental) */
	A6(38),
	/** Non-terminal name redirection */
	DNAME(39),
	/** Options - contains EDNS metadata */
	OPT(41),
	/** Address Prefix List */
	APL(42),
	/** Delegation Signer */
	DS(43),
	/** SSH Key Fingerprint */
	SSHFP(44),
	/** IPSEC key */
	IPSECKEY(45),
	/** Resource Record Signature */
	RRSIG(46),
	/** Next Secure Name */
	NSEC(47),
	/** DNSSEC Key */
	DNSKEY(48),
	/** Dynamic Host Configuration Protocol (DHCP) ID */
	DHCID(49),
	/** Next SECure, 3rd edition, RFC 5155 */
	NSEC3(50),
	NSEC3PARAM(51),
	/** Sender Policy Framework (experimental) */
	SPF(99),
	/** Transaction key - used to compute a shared secret or exchange a key */
	TKEY(249),
	/** Transaction signature */
	TSIG(250),
	/** Incremental zone transfer */
	IXFR(251),
	/** Zone transfer */
	AXFR(252),
	/** Transfer mailbox records */
	MAILB(253),
	/** Transfer mail agent records */
	MAILA(254),
	/** Matches any type */
	ANY(255);

	private int _value;
	private Class<? extends Record> _class;

	private Type(int value)
	{
		_value = value;
	}

	private Type(int value, Class<? extends Record> clazz)
	{
		_value = value;
		_class = clazz;
	}

	public int getValue()
	{
		return _value;
	}

	public void encode(ByteBuffer b)
	{
		BufferUtil.put16(b, _value);
	}

	public Record newRecord() throws IOException
	{
		try
		{
			if (_class == null)
				return new GenericRecord(this);
			return _class.newInstance();
		}
		catch (Exception e)
		{
			throw new IOException("Could not create record with class " + _class + ":" + e);
		}
	}

	public static Type getType(int value) throws IOException
	{
		for (Type t : Type.values())
			if (t.getValue() == value)
				return t;
		throw new IOException("Could not found type with value: " + value);
	}

}
