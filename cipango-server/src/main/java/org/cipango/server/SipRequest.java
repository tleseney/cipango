package org.cipango.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SipRequest extends SipMessage implements SipServletRequest 
{
	private static final Logger LOG = Log.getLogger(SipRequest.class);
	
	private SipMethod _sipMethod;
	private String _method;
	
	public boolean isRequest()
	{
		return true;
	}
	
	public void setMethod(SipMethod sipMethod, String method)
	{
		_sipMethod = sipMethod;
		_method = method;
	}
	
	protected boolean canSetContact()
	{
		return _sipMethod == SipMethod.REGISTER;
	}
	
	/**
	 * @see SipServletRequest#send()
	 */
	public void send() throws IOException
	{
		if (isCommitted())
			throw new IllegalStateException("request is committed");
		// TODO
	}
	
	public Address getTopRoute()
	{
		return (Address) _fields.get(SipHeader.ROUTE);
	}
	
	public Address removeTopRoute()
	{
		return (Address) _fields.removeFirst(SipHeader.ROUTE);
	}
	
	public void setPoppedRoute(Address route)
	{
		// TODO
	}
	
	@Override
	public SipServletRequest createCancel() 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * @see SipServletRequest#createResponse(int)
	 */
	public SipServletResponse createResponse(int status) 
	{
		return createResponse(status, null);
	}
	
	/**
	 * @see SipServletRequest#createResponse(int, String)
	 */
	public SipServletResponse createResponse(int status, String reason) 
	{
		if (SipMethod.ACK == _sipMethod)
			throw new IllegalStateException("Cannot create response to ACK");
		
		//TODO
		return new SipResponse(this, status, reason);
	}
	
	
	//
	
	public boolean isInvite()
	{
		return _sipMethod == SipMethod.INVITE;
	}
	
	public boolean isAck()
	{
		return _sipMethod == SipMethod.ACK;
	}
	
	public boolean isCancel()
	{
		return _sipMethod == SipMethod.CANCEL;
	}
	
	public String getParameter(String name) 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Enumeration getParameterNames() 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String[] getParameterValues(String name) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Map<String, String[]> getParameterMap() 
	{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getScheme() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getServerName() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public int getServerPort() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public String getRemoteHost() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Enumeration getLocales() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getRealPath(String path) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void addAuthHeader(SipServletResponse arg0, AuthInfo arg1) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void addAuthHeader(SipServletResponse arg0, String arg1, String arg2) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public B2buaHelper getB2buaHelper() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Address getInitialPoppedRoute() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ServletInputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public int getMaxForwards() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public Address getPoppedRoute() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Proxy getProxy() throws TooManyHopsException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Proxy getProxy(boolean arg0) throws TooManyHopsException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public BufferedReader getReader() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public SipApplicationRoutingRegion getRegion() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public URI getRequestURI() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public SipApplicationRoutingDirective getRoutingDirective()
			throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public URI getSubscriberURI() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean isInitial() 
	{
		return getTo().getParameter(AddressImpl.TAG) == null && !isCancel();
	}
	
	@Override
	public void pushPath(Address arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void pushRoute(SipURI arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void pushRoute(Address arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setMaxForwards(int arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setRequestURI(URI arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setRoutingDirective(SipApplicationRoutingDirective arg0,
			SipServletRequest arg1) throws IllegalStateException {
		// TODO Auto-generated method stub
		
	}
	
	
}
