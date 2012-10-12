package org.cipango.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;

import javax.servlet.sip.Address;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipSession;

import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;
import org.cipango.server.transaction.Transaction;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipFields;
import org.cipango.sip.SipHeader;
import org.cipango.sip.Via;

public abstract class SipMessage implements SipServletMessage
{
	protected SipFields _fields = new SipFields();
	
	private long _timeStamp;
	private SipConnection _connection;
	
	private boolean _committed = false;
	
	protected Session _session;
	
	protected boolean isSystemHeader(SipHeader header)
	{
		return (header.isSystem() || header == SipHeader.CONTACT && !canSetContact());
	}
	
	public SipFields getFields()
	{
		return _fields;
	}
	
	public void setTimeStamp(long ts)
	{
		_timeStamp = ts;
	}
	
	public long getTimeStamp()
	{
		return _timeStamp;
	}
	
	public void setConnection(SipConnection connection)
	{
		_connection = connection;
	}
	
	public SipConnection getConnection()
	{
		return _connection;
	}
	
	public void setSession(Session session)
	{
		_session = session;
	}
	
	public abstract boolean isRequest();

	protected abstract boolean canSetContact();
	public abstract Transaction getTransaction(); 
	
	public Via getTopVia()
	{
		return (Via) _fields.get(SipHeader.VIA);
	}
	
	public String getToTag()
	{
		AddressImpl to = (AddressImpl) _fields.get(SipHeader.TO);
		return to.getTag();
	}
	
	public Session session()
	{
		return _session;
	}
	
	public ApplicationSession appSession()
	{
		return _session.appSession();
	}
	
	public void addAcceptLanguage(Locale arg0) {
		// TODO Auto-generated method stub
		
	}

	public void addAddressHeader(String arg0, Address arg1, boolean arg2) 
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * @see SipServletMessage#addHeader(String, String)
	 */
	public void addHeader(String name, String value) 
	{
		if (isCommitted())
			throw new IllegalStateException("Message is committed");
		
		SipHeader header = SipHeader.CACHE.get(name);
		if (isSystemHeader(header))
			throw new IllegalArgumentException(name + " is a system header");
		
		if (value == null || name == null)
			throw new NullPointerException("name or value is null");
		
		_fields.add(name, value);
	}

	@Override
	public void addParameterableHeader(String arg0, Parameterable arg1,
			boolean arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Locale getAcceptLanguage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Locale> getAcceptLanguages() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Address getAddressHeader(String arg0) throws ServletParseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<Address> getAddressHeaders(String arg0)
			throws ServletParseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipApplicationSession getApplicationSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipApplicationSession getApplicationSession(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see SipServletMessage#getCallId()
	 */
	public String getCallId() 
	{
		return _fields.getString(SipHeader.CALL_ID);
	}

	@Override
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getContent() throws IOException, UnsupportedEncodingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Locale getContentLanguage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getContentLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getExpires() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see SipServletMessage#getFrom()
	 */
	public Address getFrom() 
	{
		return (Address) _fields.get(SipHeader.FROM); // RO
	}

	/**
	 * @see SipServletMessage#getHeader(String)
	 */
	public String getHeader(String name) 
	{
		if (name == null)
			throw new NullPointerException("name is null");
		return _fields.getString(name);
	}

	@Override
	public HeaderForm getHeaderForm() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<String> getHeaders(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInitialRemoteAddr() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getInitialRemotePort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getInitialTransport() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocalAddr() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocalPort() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public Parameterable getParameterableHeader(String arg0)
			throws ServletParseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<? extends Parameterable> getParameterableHeaders(
			String arg0) throws ServletParseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] getRawContent() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see SipServletMessage#getRemoteAddr()
	 */
	public String getRemoteAddr() 
	{
		return _connection != null ? _connection.getRemoteAddress().getHostAddress() : null;
	}

	/**
	 * @see SipServletMessage#getRemotePort()
	 */
	public int getRemotePort() 
	{
		return _connection != null ? _connection.getRemotePort() : -1;
	}

	@Override
	public String getRemoteUser() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see SipServletMessage#getSession()
	 */
	public SipSession getSession() 
	{
		return _session;
	}

	/**
	 * 
	 */
	public SipSession getSession(boolean create) 
	{
		return getSession();
	}

	@Override
	public Address getTo() 
	{
		return (Address) _fields.get(SipHeader.TO);
	}

	/**
	 * @see SipServletMessage#getTransport()
	 */
	public String getTransport() 
	{
		if (_connection == null)
			return null;
		return _connection.getTransport().getName();
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see SipServletMessage#isCommitted()
	 */
	public boolean isCommitted() 
	{
		return _committed;
	}
	
	public void setCommitted(boolean b)
	{
		_committed = b;
	}

	@Override
	public boolean isSecure() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isUserInRole(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeHeader(String arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setAcceptLanguage(Locale arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAddressHeader(String arg0, Address arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContent(Object arg0, String arg1)
			throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContentLanguage(Locale arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContentLength(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContentType(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setExpires(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setHeaderForm(HeaderForm arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setParameterableHeader(String arg0, Parameterable arg1) {
		// TODO Auto-generated method stub
		
	}

}
