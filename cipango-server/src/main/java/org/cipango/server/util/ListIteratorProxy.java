// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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

package org.cipango.server.util;

import java.util.ListIterator;

public class ListIteratorProxy<E> implements ListIterator<E>
{
	private ListIterator<E> _iterator;
	
	public ListIteratorProxy(ListIterator<E> iterator)
	{
		_iterator = iterator;
	}
	
	public void add(E o) 
	{
		_iterator.add(o);
	}

	public boolean hasNext() 
	{
		return _iterator.hasNext();
	}

	public boolean hasPrevious() 
	{
		return _iterator.hasPrevious();
	}

	public E next() 
	{
		return _iterator.next();
	}

	public int nextIndex() 
	{
		return _iterator.nextIndex();
	}

	public E previous() 
	{
		return _iterator.previous();
	}

	public int previousIndex() 
	{
		return _iterator.previousIndex();
	}

	public void remove() 
	{
		_iterator.remove();
	}

	public void set(E o) 
	{
		_iterator.set(o);
	}
}
