package org.cipango.server.handler;

import java.io.IOException;

import javax.servlet.ServletException;

import org.cipango.server.SipHandler;
import org.cipango.server.SipMessage;
import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.component.Container;

public class SipContextHandlerCollection extends AbstractSipHandler implements Container.Listener
{

	private SipAppContext[] _sipContexts;
	private SipHandler _handler;
	 
	public SipContextHandlerCollection(@Name("contexts") HandlerCollection contexts)
	{
		contexts.addBean(this);
	}
	
	
	@Override
	public void handle(SipMessage message) throws IOException, ServletException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beanAdded(Container parent, Object child)
	{
		if (child instanceof SipAppContext)
		{
			setSipContexts(ArrayUtil.addToArray(getSipContexts(), (SipAppContext) child, SipAppContext.class));
		}	
	}

	@Override
	public void beanRemoved(Container parent, Object child)
	{
		if (child instanceof SipAppContext)
		{
			setSipContexts(ArrayUtil.removeFromArray(getSipContexts(), (SipAppContext) child));
		}
	}

	public SipAppContext getContext(String name)
	{
		if (_sipContexts != null)
		{
			for (int i = 0; i < _sipContexts.length; i++)
			{
				if (_sipContexts[i].getName().equals(name))	
					return _sipContexts[i];
			}
		}
		return null;
	}

	public SipAppContext[] getSipContexts()
	{
		return _sipContexts;
	}


	public void setSipContexts(SipAppContext[] sipContexts)
	{
		_sipContexts = sipContexts;
	}

}
