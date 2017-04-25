package org.cipango.server;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;

public abstract class SipRequest extends SipMessage implements SipServletRequest {

    private String method;
    private URI requestURI;
    private String version;



}
