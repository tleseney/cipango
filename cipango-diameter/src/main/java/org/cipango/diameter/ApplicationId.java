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
package org.cipango.diameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.cipango.diameter.api.DiameterServletMessage;
import org.cipango.diameter.base.Common;

/**
 * The Application Identifier is used to identify a specific Diameter Application. There are
 * standards-track application ids and vendor specific application ids.
 * <p>
 * IANA [IANA] has assigned the range 0x00000001 to 0x00ffffff for standards-track applications; and
 * 0x01000000 - 0xfffffffe for vendor specific applications, on a first-come, first-served basis.
 * The following values are allocated.
 * <ul>
 * <li>Diameter Common Messages 0
 * <li>NASREQ 1
 * <li>Mobile-IP 2
 * <li>Diameter Base Accounting 3
 * <li>Relay 0xffffffff
 * </ul>
 * <p>
 * Assignment of standards-track application IDs are by Designated Expert with Specification
 * Required [IANA].
 * <p>
 * Both Application-Id and Acct-Application-Id AVPs use the same Application Identifier space.
 * <p>
 * Vendor-Specific Application Identifiers, are for Private Use. Vendor-Specific Application
 * Identifiers are assigned on a First Come, First Served basis by IANA.
 * <p>
 * IETF applications ID: {@link org.cipango.diameter.base.Accounting#ACCOUNTING_ID Accounting}
 * <br/>
 * 3GPP applications ID: {@link org.cipango.diameter.ims.Sh#SH_APPLICATION_ID Sh},
 * {@link org.cipango.diameter.ims.Cx#CX_APPLICATION_ID Cx},
 * {@link org.cipango.diameter.ims.Zh#ZH_APPLICATION_ID Zh},
 * {@link org.cipango.diameter.ims.Zh#ZN_APPLICATION_ID Zn}
 */
public class ApplicationId
{
	public static enum Type
	{
		Acct, Auth
	}

	private int _id;
	private Type _type;
	private List<Integer> _vendors;

	public ApplicationId(Type type, int id, List<Integer> vendors)
	{
		_id = id;
		_type = type;
		_vendors = vendors;
	}

	public ApplicationId(Type type, int id, int vendor)
	{
		this(type, id, Collections.singletonList(vendor));
	}

	public ApplicationId(Type type, int id)
	{
		this(type, id, null);
	}

	public int getId()
	{
		return _id;
	}

	public boolean isAuth()
	{
		return (_type == Type.Auth);
	}

	public boolean isAcct()
	{
		return (_type == Type.Acct);
	}

	public boolean isVendorSpecific()
	{
		return _vendors != null && _vendors.size() != 0;
	}

	public List<Integer> getVendors()
	{
		return _vendors;
	}

	public AVP<?> getAVP()
	{
		AVP<Integer> appId;
		if (_type == Type.Auth)
			appId = new AVP<Integer>(Common.AUTH_APPLICATION_ID, _id);
		else
			appId = new AVP<Integer>(Common.ACCT_APPLICATION_ID, _id);

		if (_vendors != null && _vendors.size() > 0)
		{
			AVP<AVPList> vsai = new AVP<AVPList>(Common.VENDOR_SPECIFIC_APPLICATION_ID, new AVPList());
			for (Integer vendorId : _vendors)
			{
				vsai.getValue().add(Common.VENDOR_ID, vendorId);
			}
			vsai.getValue().add(appId);
			return vsai;
		}
		else
			return appId;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof ApplicationId))
			return false;
		ApplicationId other = (ApplicationId) o;

		boolean same = _id == other.getId() && _type == other._type
				&& isVendorSpecific() == other.isVendorSpecific();
		if (isVendorSpecific())
			return same && getVendors().equals(other.getVendors());
		return same;

	}

	@Override
	public String toString()
	{
		return _id + (isVendorSpecific() ? _vendors.toString() : "");
	}

	public static ApplicationId ofAVP(DiameterServletMessage message)
	{
		Integer appId = message.get(Common.ACCT_APPLICATION_ID);
		if (appId != null)
			return new ApplicationId(ApplicationId.Type.Acct, appId);

		appId = message.get(Common.AUTH_APPLICATION_ID);

		if (appId != null)
			return new ApplicationId(ApplicationId.Type.Auth, appId);

		AVPList list = message.get(Common.VENDOR_SPECIFIC_APPLICATION_ID);

		if (list == null)
			return null;

		List<Integer> vendors = new ArrayList<Integer>();
		Iterator<AVP<Integer>> it = list.getAVPs(Common.VENDOR_ID);
		while (it.hasNext())
			vendors.add(it.next().getValue());

		appId = list.getValue(Common.ACCT_APPLICATION_ID);
		if (appId != null)
			return new ApplicationId(ApplicationId.Type.Acct, appId, vendors);

		appId = list.getValue(Common.AUTH_APPLICATION_ID);
		return new ApplicationId(ApplicationId.Type.Auth, appId, vendors);
	}

}
