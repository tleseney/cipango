// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

package org.cipango.util;

import java.text.ParseException;
import java.util.BitSet;

public class StringScanner
{
	private int _position;
	private int _end;
	private int _mark;
	private String _string;
	
	public StringScanner(String s)
	{
		_string = s;
		_end = _string.length();
	}
	
	public boolean eof()
	{
		return (_position >= _end);
	}
	
	public int position()
	{
		return _position;
	}
	
	public void position(int i)
	{
		if (i < 0 || i >= _end)
			throw new IndexOutOfBoundsException();
		_position = i;
	}
	
	public StringScanner end()
	{
		_position = _end;
		return this;
	}
	
	public void reset()
	{
		_position = _mark;
	}
	
	public StringScanner skipWhitespace() 
	{
		while (_position < _end)
		{
			switch (_string.charAt(_position))
			{
			case ' ':
			case '\t':
			case '\r':
			case '\n':
				_position++;
				break;
			default: 
				return this;
			}
		}
		return this;
	}
	
	public StringScanner skipBackWhitespace() 
	{
		while (_position > 0)
		{
			switch (_string.charAt(_position-1))
			{
			case ' ':
			case '\t':
			case '\r':
			case '\n':
				_position--;
				break;
			default: 
				return this;
			}
		}
		return this;
	}
	
	public StringScanner skipToChar(char c)
	{
		_position = _string.indexOf(c, _position);
		if (_position < 0)
			_position = _end;
		
		return this;
	}
	
	public StringScanner skipToOneOf(BitSet bs)
	{
		while (_position < _end && !bs.get(_string.charAt(_position)))
			_position++;
		return this;
	}
	
	public StringScanner skipChars(BitSet bs) 
	{
		while (_position < _end && bs.get(_string.charAt(_position)))
			_position++;
		
		return this;
	}
	
	public StringScanner skipChar() 
	{
		_position++;
		return this;
	}
	
	public StringScanner readSWSChar(char c) throws ParseException
	{
		skipWhitespace();
		readChar(c);
		skipWhitespace();
		return this;
	}
	
	public StringScanner readChar(char c) throws ParseException
	{
		if (eof()) throw new ParseException("expected " + c, _position);		
		if (_string.charAt(_position) != c) throw new ParseException(_string.charAt(_position) + " instead of " + c, _position);
	
		_position++;
		return this;
	}
	
	public StringScanner readString(String s) throws ParseException
	{
		for (int i = 0; i < s.length(); i++)
		{
			if (eof() || _string.charAt(_position) != s.charAt(i))
				throw new ParseException("expected " + s, _position);
			_position++;
		}
		return this;
	}
	
	public StringScanner mark()
	{
		_mark = _position;
		return this;
	}
	
	public String sliceFromMark()
	{
		return _string.substring(_mark, _position);
	}
	
	public char charAt(int i)
	{
		return _string.charAt(i);
	}
	
	public String readToSpace()
	{
		int m = _position;
		while (_position < _end && !Character.isWhitespace(_string.charAt(_position)))
				_position++;
		
		return _string.substring(m, _position);
	}
	
	public char peekChar()
	{
		return _string.charAt(_position);
	}
	
	public String readTo(BitSet bs)
	{
		int m = _position;
		while (_position < _end && !bs.get(_string.charAt(_position)))
				_position++;
		
		return _string.substring(m, _position);
	}
	
	public int readInt(int base)
	{
		int value = 0;
		
		while (!eof())
		{
            char c = _string.charAt(_position);
            
            int digit = c - '0';
            
            if (digit<0 || digit>=base || digit>=10)
            {
                digit=10+c-'A';
                if (digit<10 || digit>=base)
                    digit=10+c-'a';
            }
            
            if (digit<0 || digit>=base)
                return value;
            value=value*base+digit;
            _position++;
        }
        return value;
	}
	
	public int readInt()
	{
		return readInt(10);
	}
	
	public String readQuoted() throws ParseException
	{
		int m = _position;
		
		readChar('"');

		while (_position < _end)
		{
			if (_string.charAt(_position) == '"' && _string.charAt(_position-1) != '\\')
				return _string.substring(m, ++_position);
			_position++;
		}
		throw new ParseException("cannot find closing quote", _position);
	}
	
	public String readFromMark()
	{
		return _string.substring(_mark, _position);
	}
	
	// --
	
	
	
	
	/*
	private final void skip(Charset charset)
	{
		while (_position < _end && charset.contains(_string.charAt(_position)))
			_position++;
	}
	
	String readToken() throws ParseException
	{
		int start = _position;
		
		skip(SipGrammar.__token);
		
		if (_position > start)
			return _string.substring(start, _position);
		
		throw new ParseException(_string, start);
	}
	
	public String get(int start, int end)
	{
		return _string.substring(start, end);
	}
	*/
}