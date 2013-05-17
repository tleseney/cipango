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

package org.cipango.util;

/**
 * Usual heap based priority queue with nodes keeping track of their position for quicker
 * removal. 
 */
public class PriorityQueue
{
	private static final int DEFAULT_INITIAL_CAPACITY = 11;
	
	protected Node[] _heap;
	protected int _size;
	
	public PriorityQueue()
	{
		this(DEFAULT_INITIAL_CAPACITY);
	}
	
	public PriorityQueue(int initialSize)
	{
		_heap = new Node[initialSize];
	}
	
	public int getSize()
	{
		return _size;
	}
	
	public void offer(Node node)
	{
		if (node == null)
			throw new NullPointerException();
		if (node._position > 0)
			remove(node);
		
		if (++_size == _heap.length)
		{
			Node[] newHeap = new Node[_heap.length*2];
			System.arraycopy(_heap, 0, newHeap, 0, _heap.length);
			_heap = newHeap;
		}
		if (_size == 1)
			set(_size, node);
		else
			siftUp(_size, node);
	}
	
	public Node poll()
	{
		if (_size == 0)
			return null;
		Node node = _heap[1]; node._position = -1;
		
		Node x = _heap[_size];
		_heap[_size--] = null;
		
		if (_size != 0)
			siftDown(1, x);
		return node;
	}
	
	public Node peek()
	{
		return _size > 0 ? _heap[1] : null;
	}
	
	private void siftUp(int k, Node x)
	{
		while (k>1)
		{
			int j = k >>> 1;
			Node parent = _heap[j];
			if (parent._value <= x._value)
				break;
			set(k, parent); 
			k = j;
		}
		set(k, x);
	}
	
	private void siftDown(int k, Node x)
	{
		int j; 
		while ((j = k<<1) <= _size)
		{
			Node child = _heap[j];
			if (j < _size && child._value > _heap[j+1]._value)
				child = _heap[++j];
			if (x._value <= child._value)
				break;
			set(k, child);
			k = j;

		}
		set(k, x);
	}
	
	public void remove(Node node)
	{
		if (node._position != -1)
			removeAt(node._position);
		node._position = -1;
	}
	
	private void removeAt(int i)
	{
		if (i == 0 || i > _size)
			throw new IllegalArgumentException("0<" + i + "<=" + _size);
		
		if (i == _size)
			_heap[_size--] = null; 
		else
		{
			Node node = _heap[_size];
			_heap[_size--] = null;
			siftDown(i, node);
			if (_heap[i] == node)
				siftUp(i, node);
		}
	}
	
	private final void set(int k, Node node)
	{
		_heap[k] = node;
		node._position = k;
	}
	
	public void offer(Node node, long time) 
	{
		if (node._position > 0)
		{
			 long oldTime = _heap[node._position]._value;
	        _heap[node._position]._value = time;
	        
	        if (oldTime < time)
				siftDown(node._position, node);
			else
				siftUp(node._position, node);
		}
		else 
		{
			node._value = time;
			offer(node);
		}
	}

	protected Node[] asArray()
	{
		Node[] nodes = new Node[_size];
		System.arraycopy(_heap, 1, nodes, 0, _size);
		return nodes;
	}
	
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append('[');
		for (int i = 1; i <= _size; i++)
		{
			buf.append(_heap[i]); 
			if (i< _size)
				buf.append(',');
		}
		buf.append(']');
		return buf.toString();
	}
	
	public static class Node
	{
		private int _position = -1;
		protected long _value;
		
		public Node(long value)
		{
			_value = value;
		}
		
		public long getValue()
		{
			return _value;
		}
		
		protected int getPosition()
		{
			return _position; 
		}
		/*
		public String toString()
		{
			return Long.toString(_value);
		}
		*/
	}
}
