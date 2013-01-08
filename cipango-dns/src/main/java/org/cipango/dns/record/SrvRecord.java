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
package org.cipango.dns.record;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.cipango.dns.Compression;
import org.cipango.dns.Name;
import org.cipango.dns.Type;
import org.cipango.dns.util.BufferUtil;

/**
 * @see <a href="http://www.faqs.org/rfcs/rfc2782.html">RFC 2782 - DNS SRV RR</a>
 */
public class SrvRecord extends Record implements AdditionalName
{
	private int _priority;
	private int _weight;
	private int _port;
	private Name _target;

	public SrvRecord()
	{

	}
	
	public SrvRecord(Name name)
	{
		setName(name);
	}

	public SrvRecord(String service, String protocol, String host)
	{
		setName(new Name("_" + service + "._" + protocol + "." + host));
	}

	@Override
	public Type getType()
	{
		return Type.SRV;
	}

	@Override
	public void doEncode(ByteBuffer b, Compression c) throws IOException
	{
		BufferUtil.put16(b, _priority);
		BufferUtil.put16(b, _weight);
		BufferUtil.put16(b, _port);
		c.encodeName(_target, b, true);
	}

	@Override
	public void doDecode(ByteBuffer b, Compression c, int dataLength) throws IOException
	{
		_priority = BufferUtil.get16(b);
		_weight = BufferUtil.get16(b);
		_port = BufferUtil.get16(b);
		_target = c.decodeName(b);

	}

	/**
	 * returns the priority of this target host. A client MUST attempt to contact the target host
	 * with the lowest-numbered priority it can reach; target hosts with the same priority SHOULD be
	 * tried in an order defined by the weight field. The range is 0-65535. This is a 16 bit
	 * unsigned integer in network byte order.
	 */
	public int getPriority()
	{
		return _priority;
	}

	public void setPriority(int priority)
	{
		_priority = priority;
	}

	/**
	 * A server selection mechanism. The weight field specifies a relative weight for entries with
	 * the same priority. Larger weights SHOULD be given a proportionately higher probability of
	 * being selected. The range of this number is 0-65535. This is a 16 bit unsigned integer in
	 * network byte order. Domain administrators SHOULD use Weight 0 when there isn't any server
	 * selection to do, to make the RR easier to read for humans (less noisy). In the presence of
	 * records containing weights greater than 0, records with weight 0 should have a very small
	 * chance of being selected.
	 * 
	 * In the absence of a protocol whose specification calls for the use of other weighting
	 * information, a client arranges the SRV RRs of the same Priority in the order in which target
	 * hosts, specified by the SRV RRs, will be contacted. The following algorithm SHOULD be used to
	 * order the SRV RRs of the same priority:
	 * 
	 * To select a target to be contacted next, arrange all SRV RRs (that have not been ordered yet)
	 * in any order, except that all those with weight 0 are placed at the beginning of the list.
	 * 
	 * Compute the sum of the weights of those RRs, and with each RR associate the running sum in
	 * the selected order. Then choose a uniform random number between 0 and the sum computed
	 * (inclusive), and select the RR whose running sum value is the first in the selected order
	 * which is greater than or equal to the random number selected. The target host specified in
	 * the selected SRV RR is the next one to be contacted by the client. Remove this SRV RR from
	 * the set of the unordered SRV RRs and apply the described algorithm to the unordered SRV RRs
	 * to select the next target host. Continue the ordering process until there are no unordered
	 * SRV RRs. This process is repeated for each Priority.
	 */
	public int getWeight()
	{
		return _weight;
	}

	public void setWeight(int weight)
	{
		_weight = weight;
	}

	/**
	 * The port on this target host of this service. The range is 0- 65535. This is a 16 bit
	 * unsigned integer in network byte order. This is often as specified in Assigned Numbers but
	 * need not be.
	 */
	public int getPort()
	{
		return _port;
	}

	public void setPort(int port)
	{
		_port = port;
	}

	/**
	 * returns the domain name of the target host. There MUST be one or more address records for
	 * this name, the name MUST NOT be an alias (in the sense of RFC 1034 or RFC 2181). Implementors
	 * are urged, but not required, to return the address record(s) in the Additional Data section.
	 * Unless and until permitted by future standards action, name compression is not to be used for
	 * this field.
	 * 
	 * A Target of "." means that the service is decidedly not available at this domain.
	 */
	public Name getTarget()
	{
		return _target;
	}

	public void setTarget(Name target)
	{
		_target = target;
	}

	@Override
	public String toString()
	{
		if (_target == null)
			return super.toString();
		return super.toString() + " " + _priority + " " + _weight + " " + _port + " " + _target;
	}

	public Name getAdditionalName()
	{
		return _target;
	}
	
	@Override
	public boolean doEquals(Record record)
	{
		return compare(_target, ((SrvRecord) record).getTarget()) && _port == ((SrvRecord) record).getPort();
	}

}
