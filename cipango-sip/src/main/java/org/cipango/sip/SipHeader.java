package org.cipango.sip;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.StringMap;

public enum SipHeader 
{
	VIA("Via", Type.VIA, true, true, false),
	MAX_FORWARDS("Max-Forwards"),
	ROUTE("Route", Type.ADDRESS, true, true, false),
	RECORD_ROUTE("Record-Route", Type.ADDRESS, true, true, false),
	FROM("From", Type.ADDRESS, true),
	TO("To", Type.ADDRESS, true),
	CALL_ID("Call-ID", Type.STRING, true),
	CSEQ("CSeq", Type.STRING, true),
	CONTACT("Contact"),
	
	ACCEPT("Accept", Type.PARAMETERABLE, false, true, true), 
	ACCEPT_CONTACT("Accept-Contact", Type.STRING, false, true, true),
	ACCEPT_ENCODING("Accept-Encoding", Type.PARAMETERABLE, false, true, true),
	ACCEPT_LANGUAGE("Accept-Language", Type.STRING, false, true, true),
	ACCEPT_RESOURCE_PRIORITY("Accept-Resource-Priority", Type.STRING, false, true, true),
	ALERT_INFO("Alert-Info", Type.PARAMETERABLE, false, true, true),
	ALLOW("Allow", Type.STRING, false, true, true),
	ALLOW_EVENTS("Allow-Events", Type.STRING, false, true, true),	
	AUTHENTICATION_INFO("Authentication-Info", Type.STRING, false, true, true),
	AUTHORIZATION("Authorization"), 
	
	CALL_INFO("Call-Info", Type.STRING, false, true, true),
	
	CONTENT_DISPOSITION("Content-Disposition", Type.PARAMETERABLE, false, false, false),
	CONTENT_ENCODING("Content-Encoding", Type.STRING, false, true, true),
	CONTENT_LANGUAGE("Content-Language", Type.STRING, false, true, true),
	CONTENT_LENGTH("Content-Length"),
	CONTENT_TYPE("Content-Type", Type.PARAMETERABLE, false, false, false),
	
	DATE("Date"),
	ERROR_INFO("Error-Info", Type.PARAMETERABLE, false, true, true),
	EVENT("Event", Type.PARAMETERABLE, false, false, false),
	EXPIRES("Expires"),
	
	HISTORY_INFO("History-Info"),
	IDENTITY("Identity"),
	IDENTITY_INFO("Identity-Info"),
	IN_REPLY_TO("In-Reply-To"),
	JOIN("Join", Type.PARAMETERABLE, false, false, false), 
	
	MIME_VERSION("MIME-Version"),
	MIN_EXPIRES("Min-Expires"),
	MIN_SE("Min-SE"),
	ORGANIZATION("Organization"),
	P_ACCESS_NETWORK_INFO("P-Access-Network-Info"),
	P_ASSERTED_IDENTITY("P-Asserted-Identity", Type.STRING, false, true, false),
	P_ASSOCIATED_URI("P-Associated-URI", Type.STRING, false, true, false),
	P_CALLED_PARTY_ID("P-Called-Party-ID"),
	P_CHARGING_FUNCTION_ADDRESSES("P-Charging-Function-Addresses"),
	P_CHARGING_VECTOR("P-Charging-Vector"),
	P_MEDIA_AUTHORIZATION("P-Media-Authorization"), 
	P_PREFERRED_IDENTITY("P-Preferred-Identity", Type.STRING, false, true, true),
	P_USER_DATABASE("P-User-Database"),
	P_VISITED_NETWORK_ID("P-Visited-Network-ID"),
	PATH("Path"), 
	PRIORITY("Priority"),
	PRIVACY("Privacy"), 
	PROXY_AUTHENTICATE("Proxy-Authenticate"),
	PROXY_AUTHORIZATION("Proxy-Authorization"),
	PROXY_REQUIRE("Proxy-Require", Type.STRING, false, true, true),
	RACK("RAck", Type.STRING, true),
	REASON("Reason", Type.PARAMETERABLE, false, true, true),
	
	REFER_SUB("Refer-Sub"),
	REFER_TO("Refer-To", Type.ADDRESS, false, false, false),
	REFERRED_BY("Referred-By", Type.ADDRESS, false, false, false),
	REJECT_CONTACT("Reject-Contact", Type.STRING, false, true, true),
	REPLACES("Replaces"),
	REPLY_TO("Reply-To"),
	REQUEST_DISPOSITION("Request-Disposition"),
	REQUIRE("Require", Type.STRING, false, true, true), 
	RESOURCE_PRIORITY("Resource-Priority"),
	RETRY_AFTER("Retry-After", Type.PARAMETERABLE, false, false, false), 
	
	RSEQ("RSeq", Type.STRING, true),
	SECURITY_CLIENT("Secury-Client"), 
	SECURITY_SERVER("Security-Server"),
	SECURITY_VERIFY("Security-Verify"),
	SERVER("Server"),
	SERVICE_ROUTE("Service-Route"), 
	SESSION_EXPIRES("Session-Expires"),
	SIP_ETAG("SIP-ETag"),
	SIP_IF_MATCH("SIP-If-Match"),
	SUBJECT("Subject"),
	SUBSCRIPTION_STATE("Subscription-State", Type.PARAMETERABLE, false, false, false),
	SUPPORTED("Supported"),
	TARGET_DIALOG("Target-Dialog"),
	TIMESTAMP("Timestamp"),
	
	UNSUPPORTED("Unsupported", Type.STRING, false, true, true),
	USER_AGENT("User-Agent"),
	
	WARNING("Warning", Type.STRING, false, true, true),
	WWW_AUTHENTICATE("WWW-Authenticate");
	
	public static final StringMap<SipHeader> CACHE = new StringMap<SipHeader>(true);
	//public static final StringMap<SipHeader> COMPACT_CACHE = new StringMap<SipHeader>(true);
	public static final Map<SipHeader, Byte> REVERSE_COMPACT_CACHE = new EnumMap<SipHeader, Byte>(SipHeader.class);
	
	static 
	{
		for (SipHeader header : SipHeader.values())
			CACHE.put(header.toString(), header);
		
		REVERSE_COMPACT_CACHE.put(ACCEPT_CONTACT, new Byte((byte) 'a'));
		REVERSE_COMPACT_CACHE.put(REFERRED_BY, new Byte((byte) 'b'));
		REVERSE_COMPACT_CACHE.put(CONTENT_TYPE, new Byte((byte) 'c'));
		REVERSE_COMPACT_CACHE.put(REQUEST_DISPOSITION, new Byte((byte) 'd'));
		REVERSE_COMPACT_CACHE.put(CONTENT_ENCODING, new Byte((byte) 'e'));
		REVERSE_COMPACT_CACHE.put(FROM, new Byte((byte) 'f'));
		
		REVERSE_COMPACT_CACHE.put(CALL_ID, new Byte((byte) 'i'));
		REVERSE_COMPACT_CACHE.put(REJECT_CONTACT, new Byte((byte) 'j'));
		REVERSE_COMPACT_CACHE.put(SUPPORTED, new Byte((byte) 'k'));
		REVERSE_COMPACT_CACHE.put(CONTENT_LENGTH, new Byte((byte) 'l'));
		REVERSE_COMPACT_CACHE.put(CONTACT, new Byte((byte) 'm'));
		REVERSE_COMPACT_CACHE.put(IDENTITY, new Byte((byte) 'n'));
		REVERSE_COMPACT_CACHE.put(EVENT, new Byte((byte) 'o'));
		
		REVERSE_COMPACT_CACHE.put(REFER_TO, new Byte((byte) 'r'));
		REVERSE_COMPACT_CACHE.put(SUBJECT, new Byte((byte) 's'));
		REVERSE_COMPACT_CACHE.put(TO, new Byte((byte) 't'));
		REVERSE_COMPACT_CACHE.put(ALLOW_EVENTS, new Byte((byte) 'u'));
		REVERSE_COMPACT_CACHE.put(VIA, new Byte((byte) 'v'));
		
		REVERSE_COMPACT_CACHE.put(SESSION_EXPIRES, new Byte((byte) 'x'));
		REVERSE_COMPACT_CACHE.put(IDENTITY, new Byte((byte) 'y'));
		
		for (Map.Entry<SipHeader, Byte> entry : REVERSE_COMPACT_CACHE.entrySet())
			CACHE.put("" + (char) entry.getValue().byteValue(), entry.getKey());
		
	}
	
	private Type _type;
	private String _string;
	private byte[] _bytesColonSpace;
	
	private boolean _system;
	private boolean _list;
	private boolean _merge;
	
	SipHeader(String s, Type type, boolean system, boolean list, boolean merge)
	{
		_string = s;
		_type = type;
		_system = system;
		_bytesColonSpace = StringUtil.getBytes(s+": ", StringUtil.__UTF8);
		_list = list;
		_merge = merge;
	}
	
	SipHeader(String s, Type type, boolean system)
	{
		this(s, type, system, false, false);
	}
	
	SipHeader(String s)
	{
		this(s, Type.STRING, false);
	}
	
	public boolean isSystem()
	{
		return _system;
	}
	
	public Type getType()
	{
		return _type;
	}
	
	public byte[] getBytesColonSpace()
	{
		return _bytesColonSpace;
	}
	
	public String asString()
	{
		return _string;
	}
	
	public boolean isList()
	{
		return _list;
	}

	public boolean isMerge()
	{
		return _merge;
	}
	
	@Override
	public String toString()
	{
		return _string;
	}
	
	public enum Type { STRING, PARAMETERABLE, ADDRESS, VIA }
	
	private final static SipHeader[] __hashed = new SipHeader[4096];
	private final static int __maxHashed;
	
	static
	{
		int max = 0;
		Map<Integer, SipHeader> hashes = new HashMap<Integer, SipHeader>();
		
		for (SipHeader header : SipHeader.values())
		{
			String s = header.asString();
			max = Math.max(max, s.length());
			int h = 0;
			for (char c : s.toCharArray())
				h = 31*h + ((c >= 'a') ? (c - 'a' + 'A') : c);
			int hash = h % __hashed.length;
			if (hash < 0)
				hash = -hash;
			if (hashes.containsKey(hash))
			{
				System.err.println("duplicate hash " + header + " " + hashes.get(hash));
				System.exit(1);
			}
			hashes.put(hash, header);
			__hashed[hash] = header;
		}
		__maxHashed = max;
	}
	
	public static SipHeader lookAheadGet(byte[] bytes, int position, int limit)
	{
		int h=0;
        byte b=0;
        limit=Math.min(position+__maxHashed,limit);
        for (int i=position;i<limit;i++)
        {
            b=bytes[i];
            if (b==':'||b==' ')
                break;
            h= 31*h+ ((b>='a')?(b-'a'+'A'):b);
        }
        if (b!=':'&&b!=' ')
            return null;

        int hash=h%__hashed.length;
        if (hash<0)hash=-hash;
        
        SipHeader header=__hashed[hash];
        
        if (header!=null)
        {
            String s=header.asString();
            for (int i=s.length();i-->0;)
            {
                b=bytes[position+i];
                char c=s.charAt(i);
                if (c!=b && Character.toUpperCase(c)!=(b>='a'?(b-'a'+'A'):b))
                    return null;
            }
        }
        
        return header;
	}
	
	public static String getFormattedName(String name)
	{
		
		if (name.length() == 1)
		{
			SipHeader header = CACHE.get(name);
			return header == null ? name : header.toString();
		}
		return name;
			
//		SipHeader header = CACHE.get(name);
//		return header == null ? name : header.toString();
	}

}
