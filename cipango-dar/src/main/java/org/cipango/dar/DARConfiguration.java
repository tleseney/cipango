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

package org.cipango.dar;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.servlet.sip.ar.SipRouteModifier;

public class DARConfiguration
{
	private Properties _properties;

	public DARConfiguration(URI uri) throws Exception
	{
		URL url;
		if (!uri.isAbsolute())
		{
			File file = new File(uri.toString()).getCanonicalFile();
			url = file.toURI().toURL();
		} 
		else
		{
			url = uri.toURL();
		}
		_properties = new Properties();
		InputStream is = url.openStream();
		_properties.load(is);
		is.close();
	}
	
	public DARConfiguration(URL url) throws Exception
	{
		_properties = new Properties();
		InputStream is = url.openStream();
		_properties.load(is);
		is.close();
	}

	public void configure(DefaultApplicationRouter dar) throws ParseException
	{
		Map<String, RouterInfo[]> infoMap = new HashMap<String, RouterInfo[]>();

		Enumeration<Object> e = _properties.keys();
		while (e.hasMoreElements())
		{
			String key = e.nextElement().toString();
			String infos = _properties.get(key).toString().trim();

			List<RouterInfo> list = new ArrayList<RouterInfo>();
			int li = infos.indexOf('(');
			while (li >= 0)
			{
				int ri = infos.indexOf(')', li);
				if (ri < 0)
					throw new ParseException(infos, li);

				String info = infos.substring(li + 1, ri);
	
				li = infos.indexOf('(', ri);
				InfoIterator it = new InfoIterator(info);

				String name = it.next();
				String identity = it.next();
				SipApplicationRoutingRegion region = valueOf(it.next());
				String uri = it.next();
				SipRouteModifier routeModifier = SipRouteModifier.valueOf(it.next().toUpperCase());
				@SuppressWarnings("unused")
				String stateInfo = it.next();

				RouterInfo sri = new RouterInfo(
						name, identity, region, uri, routeModifier);
				list.add(sri);
			}
			infoMap.put(key, list.toArray(new RouterInfo[0]));
		}
		dar.setRouterInfos(infoMap);
	}

	public SipApplicationRoutingRegion valueOf(String region)
	{
		if (SipApplicationRoutingRegion.NEUTRAL_REGION.getLabel().equalsIgnoreCase(region))
			return SipApplicationRoutingRegion.NEUTRAL_REGION;
		else if (SipApplicationRoutingRegion.ORIGINATING_REGION.getLabel().equalsIgnoreCase(region))
			return SipApplicationRoutingRegion.ORIGINATING_REGION;
		else if (SipApplicationRoutingRegion.TERMINATING_REGION.getLabel().equalsIgnoreCase(region))
			return SipApplicationRoutingRegion.TERMINATING_REGION;
		return null;
	}

	class InfoIterator implements Iterator<String>
	{
		private String _info;
		private int i;
		private String token;

		public InfoIterator(String info)
		{
			_info = info;
		}

		public boolean hasNext()
		{
			if (token == null)
			{
				int li = _info.indexOf('"', i);
				if (li != -1)
				{
					int ri = _info.indexOf('"', li + 1);
					if (ri != -1)
					{
						token = _info.substring(li + 1, ri);
						i = ri + 1;
					}
				}
			}
			return token != null;
		}

		public String next()
		{
			if (hasNext())
			{
				String s = token;
				token = null;
				return s;
			} else
			{
				return null;
			}
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
}
