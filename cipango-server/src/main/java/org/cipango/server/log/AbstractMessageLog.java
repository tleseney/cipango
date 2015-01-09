// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

package org.cipango.server.log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.TimeZone;

import org.cipango.server.MessageTooLongException;
import org.cipango.server.SipConnection;
import org.cipango.server.SipMessage;
import org.cipango.server.SipMessageGenerator;
import org.eclipse.jetty.util.DateCache;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public abstract class AbstractMessageLog extends AbstractLifeCycle implements AccessLog
{
	private static final Logger LOG = Log.getLogger(AbstractMessageLog.class);
	public static final int MAXIMAL_BUFFER_LENGTH = 16 * 1024 * 1024;
	
	private DateCache _logDateCache;
    private String _logDateFormat = "yyyy-MM-dd HH:mm:ss"; //"yyyy-MM-dd HH:mm:ss.SSS ZZZ";
    private Locale _logLocale     = Locale.getDefault();
    private String _logTimeZone   = TimeZone.getDefault().getID();
    private SipMessageGenerator _generator;
    private StringBuilder _buf = new StringBuilder();
    private ByteBuffer _buffer;
    
	public static final int IN = 0;
	public static final int OUT = 1;
    
    protected void doStart() throws Exception 
    {	
		try
		{
			_logDateCache = new DateCache(_logDateFormat, _logLocale, _logTimeZone);
			
			_generator = new SipMessageGenerator();
			_buffer = ByteBuffer.allocate(64 * 1024);
			
			super.doStart();
	        
		}
		catch (Exception e) 
		{
			LOG.warn("Unable to log SIP messages: " + e.getMessage());
		}
	}
    
    public void messageReceived(SipMessage message, SipConnection connection)
	{
    	if (!isStarted()) return;
    	
    	try
    	{
    		doLog(message, IN, connection);
    	}
    	catch (Exception e)
    	{
    		LOG.warn("Failed to log message", e);
    	}
	}
    
	public void messageSent(SipMessage message, SipConnection connection)
	{
    	if (!isStarted()) return;
    	
    	try
    	{
    		doLog(message, OUT, connection);
    	}
		catch (Exception e)
		{
			LOG.warn("Failed to log message", e);
		}
	}
	
	public abstract void doLog(SipMessage message, int direction, SipConnection connection) throws IOException;
	
	protected String generateInfoLine(int direction, SipConnection connection, long date)
	{
		_buf.setLength(0);
        _buf.append(_logDateCache.format(date));
        if (direction == IN)
			_buf.append(" IN  ");
		else
            _buf.append(" OUT ");
		
		_buf.append(connection.getConnector().getTransport());
        _buf.append(" ");
        _buf.append(connection.getLocalAddress());
        _buf.append(':');
        _buf.append(connection.getLocalPort());
        
        if (direction == IN)
        	_buf.append(" < ");
        else
        	_buf.append(" > ");
        
        _buf.append(connection.getRemoteAddress());
        _buf.append(':');
        _buf.append(connection.getRemotePort());
        _buf.append(StringUtil.__LINE_SEPARATOR);
        _buf.append(StringUtil.__LINE_SEPARATOR);
        return _buf.toString();
	}
	
	protected ByteBuffer generateMessage(SipMessage message)
	{
		_buffer.clear();
		while (true)
		{
			try
			{
				_generator.generateMessage(_buffer, message);
				break;
			}
			catch (MessageTooLongException e)
			{
				if (_buffer.capacity() < MAXIMAL_BUFFER_LENGTH)
					_buffer = ByteBuffer.allocate(_buffer.capacity() * 2);
				else
					LOG.warn("Failed to log message", e);
			}
		}
		return _buffer;
	}
}
