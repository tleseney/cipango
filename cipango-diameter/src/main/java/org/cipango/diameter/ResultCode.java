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

import org.cipango.diameter.base.Common;

/**
 * A result code consits of
 * <ul>
 * <li>a four-digit code that indicates the outcome of the corresponding request.
 * <li>Eventually a vendor-ID if it has not defined by IETF.
 * </ul>
 * 
 * The summary of result codes is [RFC 3588]:
 * <ul>
 * <li>1xxx (Informational)
 * <li>2xxx (Success)
 * <li>3xxx (Protocol Errors)
 * <li>4xxx (Transient Failures)
 * <li>5xxx (Permanent Failure)
 * </ul>
 * 
 * IETF result codes are defined in {@link org.cipango.diameter.base.Common}.
 * 
 * 3GPP result codes are defined in {@link org.cipango.diameter.ims.Cx},
 * {@link org.cipango.diameter.ims.Sh}, {@link org.cipango.diameter.ims.Zh}.
 */
public class ResultCode
{
	private int _vendorId;
	private int _code;
	private String _name;

	public ResultCode(int vendorId, int code, String name)
	{
		_code = code;
		_name = name;
		_vendorId = vendorId;
	}

	public int getCode()
	{
		return _code;
	}

	public String getName()
	{
		return _name;
	}

	/**
	 * Return <code>true</code> if code is 1XXX.
	 * 
	 * Errors that fall within this category are used to inform the requester that a request could
	 * not be satisfied, and additional action is required on its part before access is granted.
	 * 
	 * @return <code>true</code> if code is 1XXX.
	 */
	public boolean isInformational()
	{
		return (_code / 1000) == 1;
	}

	/**
	 * Returns <code>true</code> if code is 2XXX.
	 * 
	 * Errors that fall within the Success category are used to inform a peer that a request has
	 * been successfully completed.
	 * 
	 * @return <code>true</code> if code is 2XXX.
	 */
	public boolean isSuccess()
	{
		return (_code / 1000) == 2;
	}

	/**
	 * Returns <code>true</code> if code is 3XXX.
	 * 
	 * Errors that fall within the Protocol Error category SHOULD be treated on a per-hop basis, and
	 * Diameter proxies MAY attempt to correct the error, if it is possible. Note that these and
	 * only these errors MUST only be used in answer messages whose 'E' bit is set.
	 * 
	 * 
	 * @return <code>true</code> if code is 3XXX.
	 */
	public boolean isProtocolError()
	{
		return (_code / 1000) == 3;
	}

	/**
	 * Returns <code>true</code> if code is 4XXX.
	 * 
	 * Errors that fall within the transient failures category are used to inform a peer that the
	 * request could not be satisfied at the time it was received, but MAY be able to satisfy the
	 * request in the future.
	 * 
	 * 
	 * @return <code>true</code> if code is 4XXX.
	 */
	public boolean isTransientFailure()
	{
		return (_code / 1000) == 4;
	}

	/**
	 * Returns <code>true</code> if code is 5XXX.
	 * 
	 * Errors that fall within the permanent failures category are used to inform the peer that the
	 * request failed, and should not be attempted again.
	 * 
	 * 
	 * @return <code>true</code> if code is 5XXX.
	 */
	public boolean isPermanentFailure()
	{
		return (_code / 1000) == 5;
	}

	public int getVendorId()
	{
		return _vendorId;
	}

	/**
	 * Returns <code>true</code> if the AVP is an {@link Common#EXPERIMENTAL_RESULT_CODE}.
	 */
	public boolean isExperimentalResultCode()
	{
		return _vendorId != Common.IETF_VENDOR_ID;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		if (_name != null)
			sb.append(_name).append(' ');
		sb.append("(").append(_vendorId).append('/').append(_code).append(")");
		return sb.toString();
	}

	public AVP<?> getAVP()
	{
		if (_vendorId == Common.IETF_VENDOR_ID)
			return new AVP<Integer>(Common.RESULT_CODE, _code);
		else
		{
			AVPList expRc = new AVPList();
			expRc.add(Common.VENDOR_ID, _vendorId);
			expRc.add(Common.EXPERIMENTAL_RESULT_CODE, _code);
			return new AVP<AVPList>(Common.EXPERIMENTAL_RESULT, expRc);
		}
	}
}
