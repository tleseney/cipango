package org.cipango.sip;

import java.nio.ByteBuffer;


import org.eclipse.jetty.util.Utf8StringBuilder;

public class SipParser 
{
	enum State
	{
		START,
		METHOD,
		RESPONSE_VERSION,
		SPACE1,
		URI,
		SPACE2,
		REQUEST_VERSION,
		HEADER,
		HEADER_NAME,	
		HEADER_IN_NAME,	
		HEADER_VALUE,
		HEADER_IN_VALUE,
		END,
		CONTENT;
	}
	
	
	private SipMessageHandler _handler;
	private State _state = State.START;
	
	private SipMethod _method;
	private String _methodString;
	private String _uri;
	private SipVersion _version;
	
	private byte _eol;
	
	private long _contentLength;
	
	private String _headerString;
	private String _valueString;
	private SipHeader _header;
	
    private final StringBuilder _string = new StringBuilder();
    private final Utf8StringBuilder _utf8 = new Utf8StringBuilder();
    
	private int _length;

	public SipParser(SipMessageHandler eventHandler)
	{
		_handler = eventHandler;
	}
	
	public State getState()
	{
		return _state;
	}
	
	public boolean isState(State state)
	{
		return _state == state;
	}
	
	private void quickStart(ByteBuffer buffer)
	{
		while (_state == State.START && buffer.hasRemaining())
		{
			byte ch = buffer.get();
			if (_eol == SipGrammar.CR && ch == SipGrammar.LF)
			{
				_eol = SipGrammar.LF;
				continue;
			}
			_eol = 0;
			
			if (ch > SipGrammar.SPACE || ch < 0)
			{				
				if (Character.isDigit(ch))
				{
					_version = SipVersion.lookAheadGet(buffer);
					if (_version != null)
					{
						buffer.position(buffer.position()+_version.asString().length()+1);
	                    _state = State.SPACE1;
	                    return;
					}
					_string.setLength(0);
					_string.append((char) ch);
					_state = State.RESPONSE_VERSION;
				}
				else 
				{
					_method = SipMethod.lookAheadGet(buffer);
	                if (_method!=null)
	                {
	                    _methodString = _method.asString();
	                    buffer.position(buffer.position()+_methodString.length()+1);
	                    _state=State.SPACE1;
	                    return;
	                }
	                _string.setLength(0);
					_string.append((char) ch);
					_state = State.METHOD;
				}
				return;
			}
		}
	}
	
	private boolean parseLine(ByteBuffer buffer)
	{
		while (_state.ordinal() < State.HEADER.ordinal() && buffer.hasRemaining())
		{
			byte ch = buffer.get();
			
			if (_eol == SipGrammar.CR && ch == SipGrammar.LF)
			{
				_eol = SipGrammar.LF;
				continue;
			}
			_eol = 0;
			
			switch (_state)
			{
				case METHOD:
					if (ch == SipGrammar.SPACE)
					{
						_methodString = takeString();
						SipMethod method = SipMethod.CACHE.get(_methodString);
						if (method != null)
							_methodString = method.toString();
						_state = State.SPACE1;
					}
					else if (ch < SipGrammar.SPACE && ch >= 0)
					{
						badMessage(buffer, "No URI");
						return true;
					}
					else
					{
						_string.append((char) ch);
					}
					break;
					
				case SPACE1:
					if (ch > SipGrammar.SPACE || ch < 0)
					{
						// TODO response
						_state = State.URI;
						_utf8.reset();
						_utf8.append(ch);
					}
					else if (ch < SipGrammar.SPACE)
					{
						badMessage(buffer, "No URI");
						return true;
					}
					break;
				case URI:
					if (ch == SipGrammar.SPACE)
					{
						_uri = _utf8.toString();
						_utf8.reset();
						_state = State.SPACE2;
					}
					else 
						_utf8.append(ch);
					break;
				case SPACE2:
					if (ch > SipGrammar.SPACE || ch < 0)
					{
						_string.setLength(0);
						_string.append((char) ch);
						// TODO response
						
						_state = State.REQUEST_VERSION;
						
						if (buffer.position() > 0 && buffer.hasArray())
						{
							_version = SipVersion.lookAheadGet(buffer.array(), buffer.arrayOffset()+buffer.position()-1, 
									buffer.arrayOffset()+buffer.limit());
							if (_version != null)
							{
								_string.setLength(0);
								buffer.position(buffer.position()+_version.asString().length()-1);
								_eol = buffer.get();
								_state = State.HEADER;
								_handler.startRequest(_methodString, _uri, _version);
							}
						}
					}
					break;
				case REQUEST_VERSION:
					if (ch == SipGrammar.CR || ch == SipGrammar.LF)
					{
						String version = takeString();
						_version = SipVersion.CACHE.get(version);
						
						if (_version == null)
						{
							badMessage(buffer, "Unknown Version");
							return true;
						}
						_eol = ch;
						_state = State.HEADER;
						_handler.startRequest(_methodString, _uri, _version);
					}
					else
					{
						_string.append((char) ch);
					}
					break;
			}
		}
		return false;
	}
	
	private boolean parseHeaders(ByteBuffer buffer)
	{
		while (_state.ordinal() < State.END.ordinal() && buffer.hasRemaining())
		{
			byte ch = buffer.get();
			if (_eol == SipGrammar.CR && ch == SipGrammar.LF)
			{
				_eol = SipGrammar.LF;
				continue;
			}
			_eol = 0;
			
			switch (_state)
			{
				case HEADER:
					switch (ch)
					{
						case SipGrammar.COLON:
						case SipGrammar.SPACE:
						case SipGrammar.TAB:
							// TODO continuation
							_state = State.HEADER_VALUE;
							break;
							
						default:
							
							if (_headerString != null || _valueString != null)
							{
								if (_header == SipHeader.CONTENT_LENGTH)
								{
									try
									{
										_contentLength = Long.parseLong(_valueString);
									}
									catch (NumberFormatException e)
									{
										badMessage(buffer, "Invalid Content-Length");
										return true;
									}
								}
								_handler.parsedHeader(_header, _headerString, _valueString);
							}
							
							_headerString = _valueString = null;
							_header = null;
							
							if (ch == SipGrammar.CR || ch == SipGrammar.LF)
							{
								_eol = ch;
								_state = State.END;
							}
							else
							{
								if (buffer.remaining() > 6 && buffer.hasArray())
								{
									_header = SipHeader.lookAheadGet(buffer.array(),buffer.arrayOffset()+buffer.position()-1,buffer.arrayOffset()+buffer.limit());
									
									if (_header != null)
									{
										_headerString = _header.asString();
										buffer.position(buffer.position()+_headerString.length());
										_state = buffer.get(buffer.position()-1) == ':' ? State.HEADER_VALUE : State.HEADER_NAME;
										break;
									}
								}
								_state = State.HEADER_NAME;
								_string.setLength(0);
								_string.append((char) ch);
								_length = -1;
							}
					}
					
					break;
					
				case HEADER_NAME:
					switch (ch)
					{
						case SipGrammar.CR:
						case SipGrammar.LF:
							consumeCRLF(ch, buffer);
							_headerString = takeLengthString();
							_header = SipHeader.CACHE.get(_headerString);
							_state = State.HEADER;
							
							break;
							
						case SipGrammar.COLON:
							if (_headerString == null)
							{
								_headerString = takeLengthString();
								_header = SipHeader.CACHE.get(_headerString);
							}
							_state = State.HEADER_VALUE;
							break;
						case SipGrammar.SPACE:
						case SipGrammar.TAB:
							_string.append((char) ch);
							break;
							
						default:
							if (_header != null)
							{
								_string.setLength(0);
								_string.append(_header.asString());
								_string.append(' ');
								_length = _string.length();
								_header = null;
								_headerString = null;
							}
							_string.append((char) ch);
							_length = _string.length();
							_state = State.HEADER_IN_NAME;
					}
					
					break;
					
				case HEADER_IN_NAME:
					
					switch (ch)
					{
						case SipGrammar.CR:
						case SipGrammar.LF:
							consumeCRLF(ch, buffer);
							_headerString = takeString();
							_length = -1;
							_header = SipHeader.CACHE.get(_headerString);
							_state = State.HEADER;
							break;
						
						case SipGrammar.COLON:
							if (_headerString == null)
							{
								_headerString = takeString();
								_header = SipHeader.CACHE.get(_headerString);
							}
							_length = -1;
							_state = State.HEADER_VALUE;
							break;
						case SipGrammar.SPACE:
						case SipGrammar.TAB:
							_state = State.HEADER_NAME;
							_string.append((char) ch);
							break;
						default:
							_string.append((char) ch);
							_length++;
					}
					break;
					
				case HEADER_VALUE:
					switch (ch)
					{
						case SipGrammar.CR:
						case SipGrammar.LF:
							consumeCRLF(ch, buffer);
							
							if (_length > 0)
							{
								if (_valueString != null)
									_valueString += " " + takeLengthString();
								else
									_valueString = takeLengthString();
							}
							_state = State.HEADER;
							break;
						case SipGrammar.SPACE:
						case SipGrammar.TAB:
							break;
						default:
							_string.append((char) ch);
							_length = _string.length();
							_state = State.HEADER_IN_VALUE;
					}
					
					break;
				case HEADER_IN_VALUE:
					switch (ch)
					{
						case SipGrammar.CR:
						case SipGrammar.LF:
							consumeCRLF(ch, buffer);
							
							if (_length > 0)
							{
								if (_valueString != null)
									_valueString += " " + takeString();
								else
									_valueString = takeString();
							}
							_length = -1;
							_state = State.HEADER;
							break;
						case SipGrammar.SPACE:
						case SipGrammar.TAB:
							_string.append((char) ch);
							_state = State.HEADER_VALUE;
							break;
						default:
							_string.append((char) ch);
							_length++;
					}
					break;
			}
		}
		return false;
	}
	
	public boolean parseNext(ByteBuffer buffer)
	{
		try
		{
			switch (_state)
			{
				case START:
					_methodString = null;
					_header = null;
					quickStart(buffer);
					break;
				case END:
					return false;
			}
			if (_state.ordinal() < State.HEADER.ordinal())
				if (parseLine(buffer))
					return true;
			
			if (_state.ordinal() < State.END.ordinal())
				if (parseHeaders(buffer))
					return true;
			
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return true;
		}
	}
	
	private void badMessage(ByteBuffer buffer, String reason)
	{
		
	}
	
	private String takeString()
	{
		String s = _string.toString();
		_string.setLength(0);
		
		return s;
	}
	
	private String takeLengthString()
	{
		_string.setLength(_length);
		String s = _string.toString();
		_string.setLength(0);
		_length = -1;
		
		return s;
	}
	
	private void consumeCRLF(byte ch, ByteBuffer buffer)
	{
		_eol = ch;
		if (_eol == SipGrammar.CR && buffer.hasRemaining() && buffer.get(buffer.position()) == SipGrammar.LF)
		{
			buffer.get();
			_eol = 0;
		}
	}
	
	public void reset()
	{
		_state = State.START;
		_contentLength = -1;
	}
	
	public static void main(String[] args) throws Exception
	{	
		SipMessageHandler handler= new SipMessageHandler() 
		{	
			@Override
			public boolean startRequest(String method, String uri, SipVersion version)
					 {
				// TODO Auto-generated method stub
				return false;
			}
			
			public void badMessage(int status, String reason)
			{
				System.out.println("bad message: " + reason);
			}
			
			@Override
			public boolean parsedHeader(SipHeader header, String name, String value)
					 {
				System.out.println("header: (" + name + ") = (" + value + ")");
				return false;
			}
			
			@Override
			public boolean messageComplete(ByteBuffer content)  {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean headerComplete() {
				// TODO Auto-generated method stub
				return false;
			}
		};
		SipParser parser = new SipParser(handler);
		
		ByteBuffer buffer = ByteBuffer.wrap("INVITE sip:localhost SIP/2.0\r\nazerty: xxx\r\n\r\n".getBytes());
		
		
			parser.parseNext(buffer);
		
				System.out.println(parser);
	}
	
	/*
	public void parseAll(ByteBuffer buffer) throws IOException
	{
		if (_state == State.END)
			reset();
		if (_state != State.START)
			throw new IllegalStateException("!START");
		
		while (_state != State.END && buffer.hasRemaining())
		{
			int remaining = buffer.remaining();
			parseNextOld(buffer);
			if (remaining == buffer.remaining())
				break;
		}
	}
	
	
	public boolean parseNextOld(ByteBuffer buffer) throws IOException
	{
		int start = -1;
		State startState = null;
		
		try
		{
			if (_state == State.END)
				return false;
			
			byte c;
			int length = -1;
			
			while (_state.ordinal() < State.END.ordinal() && buffer.hasRemaining())
			{
				c = buffer.get();
				
				if (_eol == SipGrammar.CR && c == SipGrammar.LF)
				{
					_eol = SipGrammar.LF;
					continue;
				}
				_eol = 0;
				
				switch (_state)
				{
					case START:
						if (c > SipGrammar.SPACE || c < 0)
						{
							start = buffer.position() - 1;
							startState = _state;
							
							_state = State.METHOD;
						}
						break;
					case METHOD:
						if (c == SipGrammar.SPACE)
						{
							SipMethod method = SipMethod.CACHE.get(buffer, start, buffer.position()-start-1);
                            _field0=method==null?BufferUtil.toString(buffer,start,buffer.position()-start-1,StringUtil.__ISO_8859_1_CHARSET):method.toString();
                            _state = State.SPACE1;
						}
						else if (c < SipGrammar.SPACE && c >= 0)
						{
							throw SipException.badRequest();
						}
						break;
						
					case SPACE1:
						if (c > SipGrammar.SPACE || c < 0)
						{
							start = buffer.position()-1;
							startState = _state;
							_state = State.URI;
						}
						else if (c < SipGrammar.SPACE)
						{
							throw SipException.badRequest();
						}
						break;
					case URI:
						if (c == SipGrammar.SPACE)
						{
							_field1=BufferUtil.toString(buffer,start,buffer.position()-start-1,StringUtil.__UTF8_CHARSET);
							start=-1;
                            _state=State.SPACE2;
						}
						break;
					case SPACE2:
						if (c > SipGrammar.SPACE || c < 0)
						{
							start = buffer.position()-1;
							startState = _state;
							_state = State.REQUEST_VERSION;
						}
						else if (c < SipGrammar.SPACE)
						{
							throw SipException.badRequest();
						}
						break;
					case REQUEST_VERSION:
						if (c == SipGrammar.CR || c == SipGrammar.LF)
						{
							String version = BufferUtil.toString(buffer,start,buffer.position()-start-1,StringUtil.__UTF8_CHARSET);
							start = -1;
							
							_handler.startRequest(_field0, _field1, version);
							_eol = c;
							_state = State.HEADER;
							_field0 = _field1 = null;
							continue;
						}
						break;
						
					case HEADER:
						switch (c)
						{
							case SipGrammar.SPACE:
							case SipGrammar.TAB:
							{
								length = -1;
								_state = State.HEADER_VALUE;
								break;
							}
							
							default:
							{
								// header
								if (_field0 != null || _field1 != null)
								{
									if (_header == SipHeader.CONTENT_LENGTH)
									{
										try
										{
											_contentLength = Long.parseLong(_field1);
										}
										catch (NumberFormatException e)
										{
											throw SipException.badRequest();
										}
									}
									_handler.parsedHeader(_header, _field0, _field1);
								}
								_field0 = _field1 = null;
								_header = null;
								// _value = null;
								
								if (c == SipGrammar.CR || c == SipGrammar.LF)
								{
									_eol = c;
									
									if (_contentLength == 0)
									{
										_handler.headerComplete();
										_state = State.END;
										_handler.messageComplete(BufferUtil.EMPTY_BUFFER);
									}
									else
									{
										_state = State.CONTENT;
										_handler.headerComplete();
									}
									//System.out.println("end of header");
								}
								else
								{
									start = buffer.position()-1; 
									startState = _state;
									length = 1;
									_state = State.HEADER_NAME;
								}
							}
								
						}
						break;
						
					case HEADER_NAME:
						switch (c)
						{
							case SipGrammar.CR:
							case SipGrammar.LF:
								_eol = c;
								_header = SipHeader.CACHE.get(buffer, start, length);
								_field0 = _header == null ? BufferUtil.toString(buffer, start, length, StringUtil.__UTF8_CHARSET) : _header.toString();	
								start = length = - 1;
								_state = State.HEADER;
								break;
								
							case SipGrammar.COLON:
								_header = SipHeader.CACHE.get(buffer, start, length);
								_field0 = _header == null ? BufferUtil.toString(buffer, start, length, StringUtil.__UTF8_CHARSET) : _header.toString();	
								
								//System.out.println("header: " + _field0);
								
								start = length = - 1;
								_state = State.HEADER_VALUE;
								break;
							case SipGrammar.SPACE:
							case SipGrammar.TAB:
								break;
							default:
								length = buffer.position()-start;
						}
						break;		
						
					case HEADER_VALUE:
						switch (c)
						{
							case SipGrammar.CR:
							case SipGrammar.LF:
								_eol = c;
								if (length > 0)
								{
									_field1 = BufferUtil.toString(buffer, start, length, StringUtil.__UTF8_CHARSET);
									//System.out.println("value: " + _field1);
									start = length = -1;
								}
								_state = State.HEADER;
								break;
							case SipGrammar.SPACE:
							case SipGrammar.TAB:
								break;
							default:
								if (start == -1)
								{
									start = buffer.position()-1;
									startState = _state;
								}
								length = buffer.position() - start;
						}
						break;
							
				} 
			} 
			// end header loop
			
			if (_state == State.CONTENT)
			{
				int remaining = buffer.remaining();
				if (remaining > 0)
				{
					if (_eol == SipGrammar.CR && buffer.get(buffer.position()) == SipGrammar.LF)
					{
						_eol = buffer.get();
						remaining--;
					}
					_eol = 0;
					if (_contentLength == -1)
					{
						System.out.println("eof");
						ByteBuffer content = buffer.asReadOnlyBuffer();
						_state = State.END;
						_handler.messageComplete(content);
						
					}
					else if (remaining >= _contentLength)
					{
						System.out.println("length");
						ByteBuffer content = buffer.asReadOnlyBuffer();
						if (content.remaining() > _contentLength)
							content.limit(content.position() + (int)_contentLength);
				
						_state = State.END;
						_handler.messageComplete(content);
					}
				}
			}
			
			
			return true;
		}
		finally
		{
			if (start >=0)
			{
				buffer.position(start);
				_state = startState;
			}
		}
	}
	*/

	
	public String toString()
	{
		return String.format("%s{s=%s}",
                getClass().getSimpleName(),
                _state);
	}
	
	public interface SipMessageHandler
	{
		boolean startRequest(String method, String uri, SipVersion version);
		boolean parsedHeader(SipHeader header, String name, String value);
		boolean headerComplete();
		boolean messageComplete(ByteBuffer content);
		
		void badMessage(int status, String reason);
	}
	
}
