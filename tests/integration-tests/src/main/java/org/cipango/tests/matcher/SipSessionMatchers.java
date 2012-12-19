package org.cipango.tests.matcher;

import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSession.State;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class SipSessionMatchers
{


	@Factory
	public static <T> Matcher<SipSession> hasState(State state)
	{
		return new HasState(state);
	}
	
	@Factory
	public static <T> Matcher<SipSession> isReadyToInvalidate()
	{
		return new IsReadyToInvalidate(true);
	}
	
	@Factory
	public static <T> Matcher<SipSession> isNotReadyToInvalidate()
	{
		return new IsReadyToInvalidate(false);
	}

	
	public static class HasState extends TypeSafeMatcher<SipSession>
	{
		private State _state;
		
		public HasState(State state)
		{
			_state = state;
		}

		public void describeTo(Description description)
		{
			description.appendText(" has state " + _state);
		}

		@Override
		protected boolean matchesSafely(SipSession session)
		{
			return session.getState() == _state;
		}

		@Override
		protected void describeMismatchSafely(SipSession item, Description mismatchDescription)
		{
			mismatchDescription.appendText(" has state " + item.getState());
		}
	}
	
	public static class IsReadyToInvalidate extends TypeSafeMatcher<SipSession>
	{
		private boolean _ready;
		
		public IsReadyToInvalidate(boolean ready)
		{
			_ready = ready;
		}

		public void describeTo(Description description)
		{
			description.appendText(" is " + (_ready ? "not" : "") + " ready to invalidate");
		}

		@Override
		protected boolean matchesSafely(SipSession session)
		{
			return session.isReadyToInvalidate() == _ready;
		}
	}
}
