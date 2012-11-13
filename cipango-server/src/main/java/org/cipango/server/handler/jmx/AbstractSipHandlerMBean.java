package org.cipango.server.handler.jmx;

import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.jmx.ObjectMBean;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class AbstractSipHandlerMBean extends ObjectMBean
{
    private static final Logger LOG = Log.getLogger(AbstractSipHandlerMBean.class);

    /* ------------------------------------------------------------ */
    public AbstractSipHandlerMBean(Object managedObject)
    {
        super(managedObject);
    }

    /* ------------------------------------------------------------ */
    @Override
    public String getObjectContextBasis()
    {
        if (_managed != null )
        {
            String basis = null;
            if (_managed instanceof SipAppContext)
            {
            	SipAppContext handler = (SipAppContext)_managed;
                String context = handler.getName();
                
                if (context!=null)
                    return context;
            }
//            else if (_managed instanceof AbstractSipHandler)
//            {
//            	AbstractSipHandler handler = (AbstractSipHandler)_managed;
//                SipServer server = handler.getServer();
//                if (server != null)
//                {
//                	SipAppContext context = 
//                        AbstractHandlerContainer.findContainerOf(server, SipAppContext.class, handler);
//                    
//                    if (context != null)
//                        basis = getContextName(context);
//                }
//            }
            if (basis != null)
                return basis;
        }
        return super.getObjectContextBasis();
    }


}
