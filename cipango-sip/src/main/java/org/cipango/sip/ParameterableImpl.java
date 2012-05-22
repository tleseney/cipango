package org.cipango.sip;

import javax.servlet.sip.Parameterable;

public class ParameterableImpl extends Parameters implements Parameterable
{
	private String _value;

	/**
	 * @see Parameterable#getValue()
	 */
	public String getValue() 
	{
		return _value;
	}

	/**
	 * @see Parameterable#setValue(String)
	 */
	public void setValue(String value) 
	{
		_value = value;		
	}

	@Override
	public Parameterable clone()
	{
		return this;
	}
}
