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
package org.cipango.diameter.base;

import org.cipango.diameter.ApplicationId;
import org.cipango.diameter.DiameterCommand;
import org.cipango.diameter.Type;

import static org.cipango.diameter.Factory.*;

/**
 * @see <a href="http://www.rfc-editor.org/rfc/rfc3588.txt">RFC 3588</a>
 */
public class Accounting 
{
	public static final int ACCOUNTING_ORDINAL = 3;
	
	public static final ApplicationId ACCOUNTING_ID = new ApplicationId(
			ApplicationId.Type.Acct, ACCOUNTING_ORDINAL);
	
	public static final int 
		ACR_ORDINAL = 271,
		ACA_ORDINAL = 271;
	
	public static final int 
		ACCT_SESSION_ID_ORDINAL = 44,
		ACCT_MULTI_SESSION_ID_ORDINAL = 50,
		ACCT_INTERIM_INTERVAL_ORDINAL = 85,
		ACCOUNTING_SUB_SESSION_ID_ORDINAL = 287,
		ACCOUNTING_RECORD_TYPE_ORDINAL = 480,
		ACCOUNTING_REALTIME_REQUIRED_ORDINAL = 483,
		ACCOUNTING_RECORD_NUMBER_ORDINAL = 485;
	
	/**
	 * The Accounting-Request (ACR) command, indicated by the Command-Code
     * field set to 271 and the Command Flags' 'R' bit set, is sent by a
     * Diameter node, acting as a client, in order to exchange accounting
     * information with a peer.
	 * 
	 * <pre>
	 * < ACR > ::= < Diameter Header: 271, REQ, PXY >
     *          < Session-Id >
     *          { Origin-Host }
     *          { Origin-Realm }
     *          { Destination-Realm }
     *          { Accounting-Record-Type }
     *          { Accounting-Record-Number }
     *          [ Acct-Application-Id ]
     *          [ Vendor-Specific-Application-Id ]
     *          [ User-Name ]
     *          [ Accounting-Sub-Session-Id ]
     *          [ Acct-Session-Id ]
     *          [ Acct-Multi-Session-Id ]
     *          [ Acct-Interim-Interval ]
     *          [ Accounting-Realtime-Required ]
     *          [ Origin-State-Id ]
     *          [ Event-Timestamp ]
     *        * [ Proxy-Info ]
     *        * [ Route-Record ]
     *        * [ AVP ]
	 * </pre>
	 */
	public static final DiameterCommand ACR = newRequest(ACR_ORDINAL, "Accounting-Request");
	
	/**
	 * The Accounting-Answer (ACA) command, indicated by the Command-Code
     * field set to 271 and the Command Flags' 'R' bit cleared, is used to
     * acknowledge an Accounting-Request command.  The Accounting-Answer
     * command contains the same Session-Id and includes the usage AVPs only
     * if CMS is in use when sending this command.  Note that the inclusion
     * of the usage AVPs when CMS is not being used leads to unnecessarily
     * large answer messages, and can not be used as a server's proof of the
     * receipt of these AVPs in an end-to-end fashion.  If the Accounting-
     * Request was protected by end-to-end security, then the corresponding
     * ACA message MUST be protected by end-to-end security.
     * 
     * Only the target Diameter Server, known as the home Diameter Server,
     * SHOULD respond with the Accounting-Answer command.
     * 
     * One of Acct-Application-Id and Vendor-Specific-Application-Id AVPs
     * MUST be present.  If the Vendor-Specific-Application-Id grouped AVP
     * is present, it must have an Acct-Application-Id inside.
	 * 
	 * <pre>
	 * 
	 * < ACA > ::= < Diameter Header: 271, PXY >
     *           < Session-Id >
     *           { Result-Code }
     *           { Origin-Host }
     *           { Origin-Realm }
     *           { Accounting-Record-Type }
     *           { Accounting-Record-Number }
     *           [ Acct-Application-Id ]
     *           [ Vendor-Specific-Application-Id ]
     *           [ User-Name ]
     *           [ Accounting-Sub-Session-Id ]
     *           [ Acct-Session-Id ]
     *           [ Acct-Multi-Session-Id ]
     *           [ Error-Reporting-Host ]
     *           [ Acct-Interim-Interval ]
     *           [ Accounting-Realtime-Required ]
     *           [ Origin-State-Id ]
     *           [ Event-Timestamp ]
     *         * [ Proxy-Info ]
     *         * [ AVP ]
     * </pre>
	 */
	public static final DiameterCommand ACA = newAnswer(ACA_ORDINAL, "Accounting-Answer");
	
	public static enum AccountingRecordType
	{
		/**
		 * An Accounting Event Record is used to indicate that a one-time
		 * event has occurred (meaning that the start and end of the event
		 * are simultaneous).  This record contains all information relevant
		 * to the service, and is the only record of the service.
		 */
		EVENT_RECORD, 
		      
		/**
		 * An Accounting Start, Interim, and Stop Records are used to
		 * indicate that a service of a measurable length has been given.  An
		 * Accounting Start Record is used to initiate an accounting session,
		 * and contains accounting information that is relevant to the
		 * initiation of the session.
		 */
		START_RECORD,

		/**
		 * An Interim Accounting Record contains cumulative accounting
		 * information for an existing accounting session.  Interim
		 * Accounting Records SHOULD be sent every time a re-authentication
		 * or re-authorization occurs. Further, additional interim record
		 * triggers MAY be defined by application-specific Diameter
		 * applications.  The selection of whether to use INTERIM_RECORD
		 * records is done by the Acct-Interim-Interval AVP.
		 */
		INTERIM_RECORD,
		      
		 /**
		  * An Accounting Stop Record is sent to terminate an accounting
		  * session and contains cumulative accounting information relevant to
		  * the existing session.
		  */
		 STOP_RECORD;
	}
	
	/** 
	 * The Accounting-Record-Type AVP (AVP Code 480) is of type Enumerated and contains 
	 * the type of accounting record being sent.
	 */
	public static final Type<AccountingRecordType> ACCOUNTING_RECORD_TYPE = newType(
			"Accounting-Record-Type", 
			ACCOUNTING_RECORD_TYPE_ORDINAL, 
			new Common.EnumDataFormat<AccountingRecordType>(AccountingRecordType.class, 1));
	
	/**
	 * The Acct-Interim-Interval AVP (AVP Code 85) is of type Unsigned32 and
     * is sent from the Diameter home authorization server to the Diameter
     * client.  The client uses information in this AVP to decide how and
     * when to produce accounting records.  With different values in this
     * AVP, service sessions can result in one, two, or two+N accounting
     * records, based on the needs of the home-organization.  The following
     * accounting record production behavior is directed by the inclusion of
     * this AVP:
     * 
     * 1. The omission of the Acct-Interim-Interval AVP or its inclusion
     * with Value field set to 0 means that EVENT_RECORD, START_RECORD,
     * and STOP_RECORD are produced, as appropriate for the service.
     * 
     * 2. The inclusion of the AVP with Value field set to a non-zero value
     * means that INTERIM_RECORD records MUST be produced between the
     * START_RECORD and STOP_RECORD records.  The Value field of this AVP
     * is the nominal interval between these records in seconds.  The
     * Diameter node that originates the accounting information, known as
     * the client, MUST produce the first INTERIM_RECORD record roughly
     * at the time when this nominal interval has elapsed from the
     * START_RECORD, the next one again as the interval has elapsed once
     * more, and so on until the session ends and a STOP_RECORD record is
     * produced.
     *
     * The client MUST ensure that the interim record production times
     * are randomized so that large accounting message storms are not
     * created either among records or around a common service start
     * time.
	 */
	public static final Type<Integer> ACCT_INTERIM_INTERVAL = Common.newUnsigned32Type(
			"Acct-Interim-Interval",
			ACCT_INTERIM_INTERVAL_ORDINAL);
	
	/**
	 * The Accounting-Record-Number AVP (AVP Code 485) is of type Unsigned32
   	 * and identifies this record within one session.  As Session-Id AVPs
   	 * are globally unique, the combination of Session-Id and Accounting-
     * Record-Number AVPs is also globally unique, and can be used in
     * matching accounting records with confirmations.  An easy way to
     * produce unique numbers is to set the value to 0 for records of type
     * EVENT_RECORD and START_RECORD, and set the value to 1 for the first
     * INTERIM_RECORD, 2 for the second, and so on until the value for
     * STOP_RECORD is one more than for the last INTERIM_RECORD.
	 */
	public static final Type<Integer> ACCOUNTING_RECORD_NUMBER = Common.newUnsigned32Type(
			"Accounting-Record-Number",
			ACCOUNTING_RECORD_NUMBER_ORDINAL);
	
	/**
	 * The Acct-Session-Id AVP (AVP Code 44) is of type OctetString is only
     * used when RADIUS/Diameter translation occurs.  This AVP contains the
     * contents of the RADIUS Acct-Session-Id attribute.
	 */
	public static final Type<byte[]> ACCT_SESSION_ID = Common.newOctetStringType(
			"Acct-Session-Id", 
			ACCT_SESSION_ID_ORDINAL);
	
	/**
	 * The Acct-Multi-Session-Id AVP (AVP Code 50) is of type UTF8String,
     * following the format specified in Section 8.8.  The Acct-Multi-
     * Session-Id AVP is used to link together multiple related accounting
     * sessions, where each session would have a unique Session-Id, but the
     * same Acct-Multi-Session-Id AVP.  This AVP MAY be returned by the
     * Diameter server in an authorization answer, and MUST be used in all
     * accounting messages for the given session.
	 */
	public static final Type<String> ACCT_MULTI_SESSION_ID = Common.newUTF8StringType(
			"Acct-Multi-Session-Id",
			ACCT_MULTI_SESSION_ID_ORDINAL);
	
	/**
	 * The Accounting-Sub-Session-Id AVP (AVP Code 287) is of type
     * Unsigned64 and contains the accounting sub-session identifier.  The
     * combination of the Session-Id and this AVP MUST be unique per sub-
     * session, and the value of this AVP MUST be monotonically increased by
     * one for all new sub-sessions.  The absence of this AVP implies no
     * sub-sessions are in use, with the exception of an Accounting-Request
     * whose Accounting-Record-Type is set to STOP_RECORD.  A STOP_RECORD
     * message with no Accounting-Sub-Session-Id AVP present will signal the
     * termination of all sub-sessions for a given Session-Id.
	 */
	public static final Type<Integer> ACCOUNTING_SUB_SESSION_ID = Common.newUnsigned32Type(
			"Accounting-Sub-Session-Id", 
			ACCOUNTING_SUB_SESSION_ID_ORDINAL);
	
	public static enum AccountingRealtimeRequired 
	{
		/**
		 * The AVP with Value field set to DELIVER_AND_GRANT means that the
         * service MUST only be granted as long as there is a connection to
         * an accounting server.  Note that the set of alternative accounting
         * servers are treated as one server in this sense.  Having to move
         * the accounting record stream to a backup server is not a reason to
         * discontinue the service to the user.
		 */
		DELIVER_AND_GRANT,
		
		/**
		 * The AVP with Value field set to GRANT_AND_STORE means that service
         * SHOULD be granted if there is a connection, or as long as records
         * can still be stored as described in Section 9.4.
         * This is the default behavior if the AVP isn't included in the
         * reply from the authorization server.
		 */
		GRANT_AND_STORE,
		
		/**
		 * The AVP with Value field set to GRANT_AND_LOSE means that service
         * SHOULD be granted even if the records can not be delivered or
         * stored.
		 */
		GRAND_AND_LOSE;
	}
	
	public static final Type<AccountingRealtimeRequired> ACCOUNTING_REALTIME_REQUIRED = newType(
			"Accounting-Realtime-Required",
			ACCOUNTING_REALTIME_REQUIRED_ORDINAL,
			new Common.EnumDataFormat<AccountingRealtimeRequired>(AccountingRealtimeRequired.class, 1));
}
