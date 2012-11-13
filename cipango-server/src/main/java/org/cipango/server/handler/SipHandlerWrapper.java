package org.cipango.server.handler;

import java.io.IOException;

import javax.servlet.ServletException;

import org.cipango.server.SipHandler;
import org.cipango.server.SipMessage;
import org.cipango.server.SipServer;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;

@ManagedObject("SipHandler wrapping another SipHandler")
public class SipHandlerWrapper extends AbstractSipHandler
{
    protected SipHandler _handler;

    public SipHandlerWrapper()
    {
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the handler.
     */
    @ManagedAttribute(value="Wrapped Handler", readonly=true)
    public SipHandler getHandler()
    {
        return _handler;
    }



    /* ------------------------------------------------------------ */
    /**
     * @param handler Set the {@link SipHandler} which should be wrapped.
     */
    public void setHandler(SipHandler handler)
    {
        if (isStarted())
            throw new IllegalStateException(STARTED);

        if (handler!=null)
            handler.setServer(getServer());
        
        updateBean(_handler,handler);
        _handler=handler;
    }

    /* ------------------------------------------------------------ */
    @Override
    public void handle(SipMessage message) throws IOException, ServletException
    {
        if (_handler!=null && isStarted())
        {
            _handler.handle(message);
        }
    }


    /* ------------------------------------------------------------ */
    @Override
    public void setServer(SipServer server)
    {
        if (server==getServer())
            return;
        
        if (isStarted())
            throw new IllegalStateException(STARTED);

        super.setServer(server);
        SipHandler h=getHandler();
        if (h!=null)
            h.setServer(server);
    }


    /* ------------------------------------------------------------ */
    @Override
    public void destroy()
    {
        if (!isStopped())
            throw new IllegalStateException("!STOPPED");
        SipHandler child=getHandler();
        if (child!=null)
        {
            setHandler(null);
            //child.destroy();
        }
        super.destroy();
    }

}
