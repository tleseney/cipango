package org.cipango.server;

public class MessageTooLongException extends Exception
{

	public MessageTooLongException()
	{
	}

	public MessageTooLongException(String message)
	{
		super(message);
	}

	public MessageTooLongException(Throwable cause)
	{
		super(cause);
	}

	public MessageTooLongException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public MessageTooLongException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
