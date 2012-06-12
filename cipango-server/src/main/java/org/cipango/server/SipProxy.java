package org.cipango.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import javax.servlet.sip.Proxy;
import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

public class SipProxy implements Proxy
{

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cancel(String[] arg0, int[] arg1, String[] arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<ProxyBranch> createProxyBranches(List<? extends URI> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getAddToPath() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getNoCancel() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SipServletRequest getOriginalRequest() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getParallel() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SipURI getPathURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProxyBranch getProxyBranch(URI arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ProxyBranch> getProxyBranches() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getProxyTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getRecordRoute() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SipURI getRecordRouteURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getRecurse() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getSequentialSearchTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getStateful() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getSupervised() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void proxyTo(URI arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void proxyTo(List<? extends URI> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAddToPath(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNoCancel(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOutboundInterface(InetSocketAddress arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOutboundInterface(InetAddress arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setParallel(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setProxyTimeout(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRecordRoute(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRecurse(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSequentialSearchTimeout(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStateful(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSupervised(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startProxy() {
		// TODO Auto-generated method stub
		
	}

}
