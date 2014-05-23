package org.cipango.server.nio;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;

import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.handler.AbstractSipHandler;
import org.junit.Ignore;

import org.junit.Assert;

@Ignore
public class TestSipHandler extends AbstractSipHandler
{
	private Throwable _e;
	private AtomicInteger _msgReceived = new AtomicInteger(0);
	private TestHandler _testHandler;
				

	@Override
	public void handle(SipMessage message) throws IOException, ServletException
	{
		try
		{
			doHandle(message);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			_e = e;
		}
		finally
		{
			_msgReceived.incrementAndGet();
			synchronized (_msgReceived)
			{
				_msgReceived.notify();
			}
		}
	}
	
	public void doHandle(SipMessage message) throws Throwable
	{
		if (_testHandler != null)
			_testHandler.doHandle(message);
		else
			throw new UnsupportedOperationException("No test handler set");
	}
	
	
	public void assertDone() throws Exception
	{
		assertDone(1);
	}
	
	public void assertDone(int msgExpected) throws Exception
	{
		throwPossibleException();
		
		long end = System.currentTimeMillis() + 5000;
		
		synchronized (_msgReceived)
		{
			while (end > System.currentTimeMillis() && _msgReceived.get() < msgExpected)
			{
				try
				{
					_msgReceived.wait(end - System.currentTimeMillis());
				}
				catch (InterruptedException e)
				{
				}
			}
		}
		throwPossibleException();
		
		if (_msgReceived.get() != msgExpected)
			Assert.fail("Received " + _msgReceived + " messages when expected " + msgExpected);
	}
	
	private void throwPossibleException() throws Exception
	{
		if (_e != null)
		{
			if (_e instanceof Error)
				throw (Error) _e;
			else
				throw (Exception) _e;
		}
	}

	public TestHandler getTestHandler()
	{
		return _testHandler;
	}

	public void setTestHandler(TestHandler testHandler)
	{
		_testHandler = testHandler;
	}
	
	public static interface TestHandler
	{
		public void doHandle(SipMessage message) throws Throwable;
	}
	
	public static class ServerHandler implements TestHandler
	{
		private int _statusCode;
		
		public ServerHandler(int statusCode)
		{
			_statusCode = statusCode;
		}

		@Override
		public void doHandle(SipMessage message) throws Throwable
		{
			SipRequest request = (SipRequest) message;
			request.createResponse(_statusCode).send();
		}
		
	}
	
	
	public static class TimeoutHandler implements TestHandler
	{

		@Override
		public void doHandle(SipMessage message) throws Throwable
		{
		}
		
	}
		
	public static class ClientHandler implements TestHandler
	{
		private int _statusCode;
		
		public ClientHandler(int expectedCode)
		{
			_statusCode = expectedCode;
		}
		
		@Override
		public void doHandle(SipMessage message) throws Throwable
		{
			SipResponse answer = (SipResponse) message;
			assertEquals(_statusCode, answer.getStatus());
		}
		
	}

}