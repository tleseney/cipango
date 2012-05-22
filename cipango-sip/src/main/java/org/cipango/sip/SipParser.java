package org.cipango.sip;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.StringUtil;

public class SipParser 
{
	enum State
	{
		START,
		METHOD,
		SPACE1,
		URI,
		SPACE2,
		REQUEST_VERSION,
		HEADER,
		HEADER_NAME,	
		//HEADER_IN_NAME,	
		
		HEADER_VALUE,
		END,
		CONTENT;
	}
	
	private EventHandler _handler;
	private State _state = State.START;
	
	private String _field0;
	private String _field1;
	private byte _eol;
	
	private long _contentLength = -1;
	
	private SipHeader _header;
	
	public SipParser(EventHandler eventHandler)
	{
		_handler = eventHandler;
	}
	
	public void reset()
	{
		_state = State.START;
		_contentLength = -1;
	}
	
	public void parseAll(ByteBuffer buffer) throws IOException
	{
		if (_state == State.END)
			reset();
		if (_state != State.START)
			throw new IllegalStateException("!START");
		
		while (_state != State.END && buffer.hasRemaining())
		{
			int remaining = buffer.remaining();
			parseNext(buffer);
			if (remaining == buffer.remaining())
				break;
		}
	}
	
	public boolean parseNext(ByteBuffer buffer) throws IOException
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
	
	
	public State getState()
	{
		return _state;
	}
	
	public interface EventHandler
	{
		boolean startRequest(String method, String uri, String version) throws IOException;
		boolean parsedHeader(SipHeader header, String name, String value) throws IOException;
		boolean headerComplete() throws IOException;
		boolean messageComplete(ByteBuffer content) throws IOException;
	}
	
}
