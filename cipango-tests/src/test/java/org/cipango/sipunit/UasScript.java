// ========================================================================
// Copyright 2010 NEXCOM Systems
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
package org.cipango.sipunit;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.SipMethods;

public abstract class UasScript
{
	public static class RingingForbidden extends UaRunnable
	{
		/**
		 * <pre>
		 * Remote                    Sip unit
		 * | INVITE                     |
		 * |--------------------------->|
		 * |                        180 |
		 * |<---------------------------|
		 * |                        403 |
		 * |<---------------------------|
		 * | ACK                        |
		 * |--------------------------->|
		 * </pre>
		 */
		public RingingForbidden(TestAgent userAgent)
		{
			super(userAgent);
		}

		@Override
		public void doTest() throws Throwable
		{
			SipServletRequest request = waitForInitialRequest();
			_ua.createResponse(request, SipServletResponse.SC_RINGING).send();
			Thread.sleep(250);
			_ua.createResponse(request, SipServletResponse.SC_FORBIDDEN).send();
		}
	};

	public static class RingingNotFound extends UaRunnable
	{
		/**
		 * <pre>
		 * Remote                    Sip unit
		 * | INVITE                     |
		 * |--------------------------->|
		 * |                        180 |
		 * |<---------------------------|
		 * |                        404 |
		 * |<---------------------------|
		 * | ACK                        |
		 * |--------------------------->|
		 * </pre>
		 */
		public RingingNotFound(TestAgent userAgent)
		{
			super(userAgent);
		}

		@Override
		public void doTest() throws Throwable
		{
			SipServletRequest request = waitForInitialRequest();
			_ua.createResponse(request, SipServletResponse.SC_RINGING).send();
			Thread.sleep(250);
			_ua.createResponse(request, SipServletResponse.SC_NOT_FOUND).send();
		}
	};
	
	public static class NotFound extends UaRunnable
	{

		/**
		 * <pre>
		 * Remote                    Sip unit
		 * | INVITE                     |
		 * |--------------------------->|
		 * |                        404 |
		 * |<---------------------------|
		 * | ACK                        |
		 * |--------------------------->|
		 * </pre>
		 */
		public NotFound(TestAgent userAgent)
		{
			super(userAgent);
		}

		@Override
		public void doTest() throws Throwable
		{
			SipServletRequest request = waitForInitialRequest();
			_ua.createResponse(request, SipServletResponse.SC_NOT_FOUND).send();
		}
	};
	
	
	public static class RingingOkBye extends UaRunnable
	{
		/**
		 * <pre>
		 * Remote                    Sip unit
		 * | INVITE                     |
		 * |--------------------------->|
		 * |                        180 |
		 * |<---------------------------|
		 * |                        200 |
		 * |<---------------------------|
		 * | ACK                        |
		 * |--------------------------->|
		 * | BYE                        |
		 * |--------------------------->|
		 * |                        200 |
		 * |<---------------------------|
		 * </pre>
		 */
		public RingingOkBye(TestAgent userAgent)
		{
			super(userAgent);
		}

		@Override
		public void doTest() throws Throwable
		{
			SipServletRequest request = waitForInitialRequest();
			_ua.createResponse(request, SipServletResponse.SC_RINGING).send();
			Thread.sleep(500);
			_ua.createResponse(request, SipServletResponse.SC_OK).send();
			request = _dialog.waitForRequest();
			assert request.getMethod().equals(SipMethods.ACK);
			request = _dialog.waitForRequest();
			assert request.getMethod().equals(SipMethods.BYE);
			_ua.createResponse(request, SipServletResponse.SC_OK).send();
		}
	};

	public static class RingingCanceled extends UaRunnable
	{
		/**
		 * <pre>
		 * Remote                    Sip unit
		 * | INVITE                     |
		 * |--------------------------->|
		 * |                        180 |
		 * |<---------------------------|
		 * | CANCEL                     |
		 * |--------------------------->|
		 * |                 200/CANCEL |
		 * |<---------------------------|
		 * |                 487/INVITE |
		 * |<---------------------------|
		 * | ACK                        |
		 * |--------------------------->|
		 * </pre>
		 */
		public RingingCanceled(TestAgent userAgent)
		{
			super(userAgent);
		}

		@Override
		public void doTest() throws Throwable
		{
			SipServletRequest invite = waitForInitialRequest();
			_ua.createResponse(invite, SipServletResponse.SC_RINGING).send();
	        SipServletRequest cancel  = _dialog.waitForRequest();
			assert cancel.getMethod().equals(SipMethods.CANCEL);
		}
	};
	
	public static class OkBye extends UaRunnable
	{

		/**
		 * <pre>
		 * Remote                    Sip unit
		 * | INVITE                     |
		 * |--------------------------->|
		 * |                        200 |
		 * |<---------------------------|
		 * | ACK                        |
		 * |--------------------------->|
		 * | BYE                        |
		 * |--------------------------->|
		 * |                        200 |
		 * |<---------------------------|
		 * </pre>
		 */
		public OkBye(TestAgent userAgent)
		{
			super(userAgent);
		}

		@Override
		public void doTest() throws Throwable
		{
			SipServletRequest request = waitForInitialRequest();
			_ua.createResponse(request, SipServletResponse.SC_OK).send();
			request = _dialog.waitForRequest();
			assert request.getMethod().equals(SipMethods.ACK);
			request = _dialog.waitForRequest();
			assert request.getMethod().equals(SipMethods.BYE);
			_ua.createResponse(request, SipServletResponse.SC_OK).send();
		}
	};
}
