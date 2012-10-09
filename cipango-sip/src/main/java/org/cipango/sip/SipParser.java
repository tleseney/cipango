package org.cipango.sip;

import java.nio.ByteBuffer;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Utf8StringBuilder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

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
	
	private SipHeader _header;
	private String _headerString;
	private String _valueString;

	private State _state = State.START;
	
	private SipMethod _method;
	private String _methodString;
	private String _uri;
	private SipVersion _version;
	
	private byte _eol;
	private long _contentLength;
	private ByteBuffer _content;
	
    private final StringBuilder _string = new StringBuilder();
    private final Utf8StringBuilder _utf8 = new Utf8StringBuilder();
 
	private int _length;

	private static final Logger LOG = Log.getLogger(SipParser.class);
	
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
			if (Character.isDigit(buffer.get(buffer.position())))
			{
				_version = SipVersion.lookAheadGet(buffer);
				if (_version != null)
				{
					buffer.position(buffer.position()+_version.asString().length()+1);
                    _state = State.SPACE1;
                    return;
				}
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
			}
			byte ch = buffer.get();
			if (_eol == SipGrammar.CR && ch == SipGrammar.LF)
			{
				_eol = SipGrammar.LF;
				continue;
			}
			_eol = 0;
			
			if (ch > SipGrammar.SPACE || ch < 0)
			{				
				_string.setLength(0);
				_string.append((char) ch);
				_state = Character.isDigit(ch) ? State.RESPONSE_VERSION : State.METHOD;
				return;
			}
		}
	}
	
	private boolean parseLine(ByteBuffer buffer)
	{
		boolean returnFromParse = false;
		
		while (_state.ordinal() < State.HEADER.ordinal() && buffer.hasRemaining() && !returnFromParse)
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
								returnFromParse |= _handler.startRequest(_methodString, _uri, _version);
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
						returnFromParse |= _handler.startRequest(_methodString, _uri, _version);
					}
					else
					{
						_string.append((char) ch);
					}
					break;
			}
		}
		return returnFromParse;
	}
	
	private boolean parseHeaders(ByteBuffer buffer)
	{
		boolean returnFromParse = false;
		
		while (_state.ordinal() < State.END.ordinal() && buffer.hasRemaining() && !returnFromParse)
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
							_length = -1;
							_string.setLength(0);
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
								returnFromParse |= _handler.parsedHeader(_header, _headerString, _valueString);
							}
							
							_headerString = _valueString = null;
							_header = null;
							
							if (ch == SipGrammar.CR || ch == SipGrammar.LF)
							{
								consumeCRLF(ch, buffer);
								if (_contentLength == 0)
								{
									returnFromParse |= _handler.headerComplete();
									_state = State.END;
									returnFromParse |= _handler.messageComplete(null);
								}
								else
								{
									returnFromParse |= _handler.headerComplete();
									_state = State.CONTENT;
								}
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
		return returnFromParse;
	}
	
	public boolean parseNext(ByteBuffer buffer)
	{
		try
		{
			switch (_state)
			{
				case START:
					_version = null;
					_method = null;
					_methodString = null;
					_uri = null;
					_header = null;
					_contentLength = -1;
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

			if (_state == State.CONTENT)
			{
				// Eat the last LF symbol before content if necessary.
				if (_eol == SipGrammar.CR && buffer.hasRemaining() && buffer.get(buffer.position()) == SipGrammar.LF)
				{
					buffer.get();
					_eol=0;
				}
	            
				ByteBuffer content = getContent(buffer);
				if (!content.isReadOnly() && buffer.hasRemaining())
				{
					int remaining = (int) Math.min(buffer.remaining(), _contentLength - content.position());
					buffer.get(content.array(), content.position(), remaining);
					content.position(content.position() + remaining);

					if (content.position() == content.limit())
					{
						content.flip();
						content = content.asReadOnlyBuffer();
					}
				}

				if (content.isReadOnly())
				{
					_state = State.END;
					releaseContent();
					if (_handler.messageComplete(content))
						return true;
				}
			}
			return false;
		}
		catch (Exception e)
		{
			LOG.warn(e);
			_handler.badMessage(400, e.toString());
			return true;
		}
	}
	
	private void badMessage(ByteBuffer buffer, String reason)
	{
		BufferUtil.clear(buffer);
		_state = State.END;
		_handler.badMessage(400, reason);
	}
	
	private ByteBuffer getContent(ByteBuffer buffer)
	{
		if (_content == null)
		{
            if (_contentLength == -1)
			{
            	 _content = buffer.asReadOnlyBuffer();
			}
            else if (buffer.remaining() >= _contentLength)
			{
				_content = buffer.asReadOnlyBuffer();
				if (_content.remaining() > _contentLength)
					_content.limit(_content.position() + (int)_contentLength);
				buffer.position(_content.position() + (int)_contentLength);
			}
			else
				_content = ByteBuffer.allocate((int)_contentLength);
		}
		return _content;
	}
	
	private void releaseContent()
	{
		_content = null;
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
	}
	
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
