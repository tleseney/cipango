// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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
package org.cipango.server.session;

import java.security.SecureRandom;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class HashIdManager extends AbstractLifeCycle implements IdManager
{
	private static final Logger LOG = Log.getLogger(HashIdManager.class);
	
	private final Set<String> _ids = new ConcurrentSkipListSet<String>();
	private String _prefix;
	private String _postfix;
	private Random _random;
	
	@Override
    protected void doStart() throws Exception
    {
       initRandom();
    }
	
	@Override
    protected void doStop() throws Exception
    {
       int size = _ids.size();
       if (size > 0)
    	   LOG.warn("Id manager has {} elements at stop", size);
    }
	
	public void initRandom()
	{
		if (_random == null)
		{
			try
			{
				_random = new SecureRandom();
			}
			catch (Exception e)
			{
				LOG.warn("Could not generate SecureRandom for session-id randomness", e);
				_random = new Random();
			}
		}
		else
			_random.setSeed(_random.nextLong() ^ System.currentTimeMillis() ^ hashCode()
					^ Runtime.getRuntime().freeMemory());
	}
	
	@Override
	public String newId()
	{
		String id = null;
		boolean added;
		do
		{
			long r = _random.nextLong();
			if (r<0)
				r = -r;
			id = StringUtil.toBase62String2(r);
			if (_prefix != null)
				id = _prefix + id;
			if (_postfix != null)
				id = id + _postfix;
			added = _ids.add(id);
			if (!added)
				LOG.warn("Id already in use: " + id); 
		}
		while (!added);
		return id;
	}

	@Override
	public void releaseId(String id)
	{
		_ids.remove(id);
	}

	@Override
	public String getPrefix()
	{
		return _prefix;
	}

	@Override
	public void setPrefix(String prefix)
	{
		_prefix = prefix;
	}

	public Random getRandom()
	{
		return _random;
	}

	public void setRandom(Random random)
	{
		_random = random;
	}

	@Override
	public boolean idInUse(String id)
	{
		return _ids.contains(id);
	}

	@Override
	public String getPostfix()
	{
		return _postfix;
	}

	@Override
	public void setPostfix(String postfix)
	{
		_postfix = postfix;
	}

}
