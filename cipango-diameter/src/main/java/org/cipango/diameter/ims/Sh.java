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
package org.cipango.diameter.ims;

import static org.cipango.diameter.Factory.newAnswer;
import static org.cipango.diameter.Factory.newRequest;

import java.util.Date;

import org.cipango.diameter.AVPList;
import org.cipango.diameter.ApplicationId;
import org.cipango.diameter.DiameterCommand;
import org.cipango.diameter.ResultCode;
import org.cipango.diameter.Type;
import org.cipango.diameter.base.Common;
import org.cipango.diameter.base.Common.EnumDataFormat;

/**
 * Sh is defined in 
 * <ul>
 *  <li>3GPP TS 23329: Sh Interface based on the Diameter protocol; Protocol details</li>
 *  <li>3GPP TS 23228: IP Multimedia (IM) Subsystem Sh interface; Signalling flows and message contents</li>
 * </ul>
 * 
 * @see <a href="http://www.3gpp.org/ftp/Specs/html-info/29328.htm">TS 29.328</a>
 * @see <a href="http://www.3gpp.org/ftp/Specs/html-info/29329.htm">TS 29.329</a>
 */
public class Sh 
{
	public static final int SH_APPLICATION = 16777217;
	
	public static final ApplicationId SH_APPLICATION_ID = new ApplicationId(
			org.cipango.diameter.ApplicationId.Type.Auth, 
			SH_APPLICATION, 
			IMS.IMS_VENDOR_ID);
	
	//-------------------------------------------------------------------------
	//                         Diameter commands
	//-------------------------------------------------------------------------
	
	public static final int 
		UDR_ORDINAL = 306, 
		UDA_ORDINAL = 306,
		PUR_ORDINAL = 307, 
		PUA_ORDINAL = 307,
		SNR_ORDINAL = 308,
		SNA_ORDINAL = 308,
		PNR_ORDINAL = 309,
		PNA_ORDINAL = 309;
	
	/**
	 * The User-Data-Request (UDR) command, indicated by the Command-Code field set to 306 and the 'R' bit 
	 * set in the Command Flags field, is sent by a Diameter client to a Diameter server in order to request user data.
	 * <p>
	 * <pre> {@code
	 * < User-Data -Request> ::= < Diameter Header: 306, REQ, PXY, 16777217 > 
	 * 		 < Session-Id >
	 * 		 { Vendor-Specific-Application-Id } 
	 * 		 { Auth-Session-State } 
	 * 		 { Origin-Host } 
	 * 		 { Origin-Realm }
	 * 		 [ Destination-Host ] 
	 * 		 { Destination-Realm } 
	 * 		*[ Supported-Features ] 
	 * 		 { User-Identity } 
	 * 		 [ Wildcarded-PSI ] 
	 * 		 [ Server-Name ] 
	 * 		*[ Service-Indication ] 
	 * 		*{ Data-Reference } 
	 * 		*[ Identity-Set ] 
	 * 		 [ Requested-Domain ] 
	 * 		 [ Current-Location ] 
	 * 		*[ AVP ] 
	 * 		*[ Proxy-Info ] 
	 * 		*[ Route-Record ]
	 * } </pre>
	 */
	public static final DiameterCommand UDR = newRequest(UDR_ORDINAL, "User-Data-Request");
	
	/**
	 * The User-Data-Answer (UDA) command, indicated by the Command-Code field set to 306 and 
	 * the 'R' bit cleared in the Command Flags field, is sent by a server in response to the 
	 * User-Data-Request command. The Experimental-Result AVP may contain one of the values 
	 * defined in section 6.2 or in 3GPP TS 29.229 [6].
	 * 
	 * <pre> {@code
	 * < User-Data-Answer > ::= < Diameter Header: 306, PXY, 16777217 > 
	 * 		 < Session-Id > 
	 * 		 { Vendor-Specific-Application-Id } 
	 * 		 [ Result-Code ]
	 * 		 [ Experimental-Result ] 
	 * 		 { Auth-Session-State } 
	 * 		 { Origin-Host } 
	 * 		 { Origin-Realm }
	 * 		*[ Supported-Features ] 
	 * 		 [ Wildcarded-PSI ]
	 * 		 [ User-Data ]
	 * 		*[ AVP ] 
	 * 		*[ Failed-AVP ] 
	 * 		*[ Proxy-Info ] 
	 * 		*[ Route-Record ]
	 * } </pre>
	 */
	public static final DiameterCommand UDA = newAnswer(UDA_ORDINAL, "User-Data-Answer");
	
	/**
	 * The Profile-Update-Request (PUR) command, indicated by the Command-Code field set to 307 and
	 * the 'R' bit set in the Command Flags field, is sent by a Diameter client to a Diameter server
	 * in order to update user data in the server. Message Format
	 * 
	 * <pre>
	 * < Profile-Update-Request > ::= < Diameter Header: 307, REQ, PXY, 16777217 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     [ Destination-Host ]
	 *     { Destination-Realm }
	 *    *[ Supported-Features ]
	 *     { User-Identity }
	 *     [ Wildcarded-PSI ]
	 *     [ Wildcarded-IMPU ]
	 *     { Data-Reference }
	 *     { User-Data }
	 *    *[ AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand PUR = newRequest(PUR_ORDINAL, "Profile-Update-Request");
	
	/**
	 * The Profile-Update-Answer (PUA) command, indicated by the Command-Code field set to 307 and 
	 * the 'R' bit cleared in the Command Flags field, is sent by a server in response to the 
	 * Profile-Update-Request command. The Experimental-Result AVP may contain one of the values 
	 * defined in section 6.2 or in 3GPP TS 29.229 [6].
	 * 
	 * <pre>
	 * < Profile-Update-Answer > ::= < Diameter Header: 307, PXY, 16777217 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     [ Result-Code ]
	 *     [ Experimental-Result ]
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     [ Wildcarded-PSI ]
	 *     [ Wildcarded-IMPU ]
	 *    *[ Supported-Features ]
	 *    *[ AVP ]
	 *    *[ Failed-AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand PUA = newAnswer(PUA_ORDINAL, "Profile-Update-Answer");
	
	
	/**
	 * The Subscribe-Notifications-Request (SNR) command, indicated by the Command-Code field set to
	 * 308 and the 'R' bit set in the Command Flags field, is sent by a Diameter client to a Diameter
	 * server in order to request notifications of changes in user data.
	 * 
	 * <pre>
	 * < Subscribe-Notifications-Request > ::=	< Diameter Header: 308, REQ, PXY, 16777217 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     [ Destination-Host ]
	 *     { Destination-Realm }
	 *    *[ Supported-Features ]
	 *     { User-Identity }
	 *     [ Wildcarded-PSI ]
	 *     [ Wildcarded-IMPU ]
	 *    *[ Service-Indication ]
	 *     [ Send-Data-Indication ]
	 *     [ Server-Name ]
	 *     { Subs-Req-Type }
	 *    *{ Data-Reference }
	 *    *[ Identity-Set ]
	 *     [ Expiry-Time ]
	 *    *[ DSAI-Tag ]
	 *    *[ AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand SNR = newRequest(SNR_ORDINAL, "Subscribe-Notifications-Request");
	
	/**
	 * The Subscribe-Notifications-Answer command, indicated by the Command-Code field set to 308
	 * and the 'R' bit cleared in the Command Flags field, is sent by a server in response to the 
	 * Subscribe-Notifications-Request command. The Result-Code or Experimental-Result AVP may contain
	 * one of the values defined in section 6.2 or in 3GPP TS 29.229 [6].
	 * 
	 * <pre>
	 * < Subscribe-Notifications-Answer > ::= < Diameter Header: 308, PXY, 16777217 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     { Auth-Session-State }
	 *     [ Result-Code ]
	 *     [ Experimental-Result ]
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     [ Wildcarded-PSI ]
	 *     [ Wildcarded-IMPU ]
	 *    *[ Supported-Features ]
	 *     [ User-Data ]
	 *     [ Expiry-Time ]
	 *    *[ AVP ]
	 *    *[ Failed-AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand SNA = newAnswer(SNA_ORDINAL, "Subscribe-Notifications-Answer");
	
	/**
	 * The Push-Notification-Request (PNR) command, indicated by the Command-Code field set to 309 
	 * and the 'R' bit set in the Command Flags field, is sent by a Diameter server to a Diameter 
	 * client in order to notify changes in the user data in the server. 
	 * 
	 * <pre>
	 * < Push-Notification-Request > ::= < Diameter Header:  309, REQ, PXY, 16777217 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     { Destination-Host }
	 *     { Destination-Realm }
	 *    *[ Supported-Features ]
	 *     { User-Identity }
	 *     [ Wildcarded-PSI ]
	 *     [ Wildcarded-IMPU ]
	 *     { User-Data }
	 *    *[ AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand PNR = newRequest(PNR_ORDINAL, "Push-Notification-Request");
	
	/**
	 * The Push-Notifications-Answer (PNA) command, indicated by the Command-Code field set to 309
	 * and the 'R' bit cleared in the Command Flags field, is sent by a client in response to the 
	 * Push-Notification-Request command. The Experimental-Result AVP may contain one of the values
	 * defined in section 6.2 or in 3GPP TS 29.229 [6].
	 * 
	 * <pre>
	 * < Push-Notification-Answer > ::=< Diameter Header: 309, PXY, 16777217 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     [ Result-Code ]
	 *     [ Experimental-Result ]
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *    *[ Supported-Features ]
	 *    *[ AVP ]
	 *    *[ Failed-AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand PNA = newAnswer(PNA_ORDINAL, "Push-Notifications-Answer");
	
	
	//-------------------------------------------------------------------------
	//                                AVPs
	//-------------------------------------------------------------------------
	
	public static final int 
		USER_IDENTITY_ORDINAL = 700,
		MSISDN_ORDINAL = 701, 
		USER_DATA_ORDINAL = 702,
		DATA_REFERENCE_ORDINAL = 703,
		SERVICE_INDICATION_ORDINAL = 704,
		SUBS_REQ_TYPE_ORDINAL = 705,
		REQUESTED_DOMAIN_ORDINAL = 706,
		CURRENT_LOCATION_ORDINAL = 707,
		IDENTITY_SET_ORDINAL = 708,
		EXPIRY_TIME_ORDINAL = 709,
		SEND_DATA_INDICATION_ORDINAL = 710,
		DSAI_TAG_ORDINAL = 711;
	
	/**
	 * The User-Identity AVP is of type Grouped. This AVP contains either a Public- Identity AVP or 
	 * an MSISDN AVP. 
	 * 
	 * <pre> {@code
	 * User-Identity ::= <AVP header: 700 10415> 
	 * 		[Public-Identity]
     * 		[MSISDN] 
     * 		*[AVP]
     * } </pre>
     * 
     * @see Cx#PUBLIC_IDENTITY
     * @see Sh#MSISDN
	 */
	public static final Type<AVPList> USER_IDENTITY = IMS.newIMSType("User-Identity", 
			USER_IDENTITY_ORDINAL, Common.__grouped);
	
	/**
	 * The MSISDN AVP is of type OctetString. This AVP contains an MSISDN, in international number 
	 * format as described in ITU-T Rec E.164 [8], encoded as a TBCD-string, i.e. digits from 0 through 9 
	 * are encoded 0000 to 1001; 1111 is used as a filler when there is an odd number of digits; bits 8 to 5 
	 * of octet n encode digit 2n; bits 4 to 1 of octet n encode digit 2(n-1)+1.
	 */
	public static final Type<byte[]> MSISDN = IMS.newIMSType("MSISDN", 
			MSISDN_ORDINAL, Common.__octetString);
	
	/**
	 * The User-Data AVP is of type OctetString. This AVP contains the user data requested 
	 * in the UDR/UDA, SNR/SNA and PNR/PNA operations and the data to be modified in the 
	 * PUR/PUA operation. The exact content and format of this AVP is described in 3GPP 
	 * TS 29.328 [1] Annex C as Sh-Data.
	 */
	public static final Type<byte[]> USER_DATA = IMS.newIMSType("User-Data",
			USER_DATA_ORDINAL, Common.__octetString);
	
	public static enum DataReference implements Common.CustomEnumValues
	{
		RepositoryData(0), 
		IMSPublicIdentity(10),
		IMSUserState(11),
		SCSCFName(12),
		InitialFilterCriteria(13),
		LocationInformation(14),
		UserState(15),
		ChargingInformation(16),
		MSISDN(17),
		PSIActivation(18),
		DSAI(19),
		AliasesRepositoryData(20);
		
		private int _value;
		DataReference(int value) { _value = value; }
		public int getValue() { return _value; }
	}
	
	/**
	 * The Data-Reference AVP is of type Enumerated, and indicates the type of the requested user 
	 * data in the operation UDR and SNR.
	 */
	public static final Type<DataReference> DATA_REFERENCE = IMS.newIMSType("Data-Reference", 
			DATA_REFERENCE_ORDINAL, new Common.CustomEnumDataFormat<DataReference>(DataReference.class));
	
	/**
	 * The Service-Indication AVP is of type OctetString. This AVP contains the Service Indication 
	 * that identifies a service or a set of services in an AS and the related repository data in 
	 * the HSS. Standardized values of Service-Indication identifying a standardized service or 
	 * set of services in the AS and standardized format of the related repository data are defined
	 * in 3GPP TS 29.364 [10].
	 */
	public static final Type<byte[]> SERVICE_INDICATION = IMS.newIMSType("Service-Indication",
			SERVICE_INDICATION_ORDINAL, Common.__octetString);
	
	public static enum IdentitySet
	{
		ALL_IDENTITIES, 
		REGISTERED_IDENTITIES,
		IMPLICIT_IDENTITIES,
		ALIAS_IDENTITIES;
	}
	
	/**
	 * The Identity-Set AVP is of type Enumerated and indicates the requested set of IMS Public
	 * Identities.
	 */
	public static Type<IdentitySet> IDENTITY_SET = IMS.newIMSType("Identity-Set", 
			IDENTITY_SET_ORDINAL, new EnumDataFormat<IdentitySet>(IdentitySet.class)).setMandatory(false);

	public static enum RequestedDomain
	{
		CS_Domain, 
		PS_Domain;
	}
	
	/**
	 * The Requested-Domain AVP is of type Enumerated, and indicates the access domain for which 
	 * certain data (e.g. user state) are requested.
	 */
	public static Type<RequestedDomain> REQUESTED_DOMAIN = IMS.newIMSType("Requested-Domain", 
			REQUESTED_DOMAIN_ORDINAL, new EnumDataFormat<RequestedDomain>(RequestedDomain.class));

	public static enum SubsReqType
	{
		Subscribe, 
		Unsubscribe;
	}
	
	/**
	 * The Subs-Req-Type AVP is of type Enumerated, and indicates the type of the 
	 * subscription-to-notifications request.
	 */
	public static Type<SubsReqType> SUBS_REQ_TYPE = IMS.newIMSType("Subs-Req-Type", 
			SUBS_REQ_TYPE_ORDINAL, new EnumDataFormat<SubsReqType>(SubsReqType.class));

	public static enum CurrentLocation
	{
		/**
		 * retrieval has to be initiated or not:
		 */
		DoNotNeedInitiateActiveLocationRetrieval, 
		/**
		 * It is requested that an active location retrieval is initiated.
		 */
		InitiateActiveLocationRetrieval;
	}
	
	/**
	 * The Current-Location AVP is of type Enumerated, and indicates whether an active location 
	 */
	public static Type<CurrentLocation> CURRENT_LOCATION = IMS.newIMSType("Current-Location", 
			CURRENT_LOCATION_ORDINAL, new EnumDataFormat<CurrentLocation>(CurrentLocation.class));

	/**
	 * The Expiry-Time AVP is of type Time.  This AVP contains the expiry time of subscriptions to 
	 * notifications in the HSS.
	 */
	public static final Type<Date> EXPIRY_TIME = IMS.newIMSType("Expiry-Time", 
			EXPIRY_TIME_ORDINAL, Common.__date).setMandatory(false);
	
	public static enum SendDataIndication
	{
		USER_DATA_NOT_REQUESTED, 
		USER_DATA_REQUESTED;
	}
	
	/**
	 * The Send-Data-Indication AVP is of type Enumerated. If present it indicates that the sender
	 * requests the User-Data.
	 */
	public static Type<SendDataIndication> SEND_DATA_INDICATION = IMS.newIMSType("Send-Data-Indication", 
			SEND_DATA_INDICATION_ORDINAL, new EnumDataFormat<SendDataIndication>(SendDataIndication.class)).setMandatory(false);

	/**
	 * The DSAI-Tag AVP is of type OctetString. This AVP contains the DSAI-Tag identifying the 
	 * instance of the Dynamic Service Activation Information being accessed for the Public 
	 * Identity.
	 */
	public static final Type<byte[]> DSAI_TAG = IMS.newIMSType("DSAI-Tag",
			DSAI_TAG_ORDINAL, Common.__octetString);
	
	
	//-------------------------------------------------------------------------
	//                           Result codes
	//-------------------------------------------------------------------------
	
	public static final int
		DIAMETER_ERROR_USER_DATA_NOT_RECOGNIZED_ORDINAL = 5100,
		DIAMETER_ERROR_OPERATION_NOT_ALLOWED_ORDINAL = 5101,
		DIAMETER_ERROR_USER_DATA_CANNOT_BE_READ_ORDINAL = 5102,
		DIAMETER_ERROR_USER_DATA_CANNOT_BE_MODIFIED_ORDINAL = 5103,
		DIAMETER_ERROR_USER_DATA_CANNOT_BE_NOTIFIED_ORDINAL = 5104,
		DIAMETER_ERROR_TRANSPARENT_DATA_OUT_OF_SYNC_ORDINAL = 5105,
		DIAMETER_ERROR_SUBS_DATA_ABSENT_ORDINAL = 5106,
		DIAMETER_ERROR_NO_SUBSCRIPTION_TO_DATA_ORDINAL = 5107,
		DIAMETER_ERROR_DSAI_NOT_AVAILABLE_ORDINAL = 5108,
		DIAMETER_USER_DATA_NOT_AVAILABLE_ORDINAL = 4100,
		DIAMETER_PRIOR_UPDATE_IN_PROGRESS_ORDINAL = 4101;
	
	/**
	 * The data received by the AS is not supported or recognized.
	 */
	public static final ResultCode DIAMETER_ERROR_USER_DATA_NOT_RECOGNIZED = IMS.newImsResultCode(
			DIAMETER_ERROR_USER_DATA_NOT_RECOGNIZED_ORDINAL, "DIAMETER_ERROR_USER_DATA_NOT_RECOGNIZED");
	
	/**
	 * The requested operation is not allowed for the user
	 */
	public static final ResultCode DIAMETER_ERROR_OPERATION_NOT_ALLOWED = IMS.newImsResultCode(
			DIAMETER_ERROR_OPERATION_NOT_ALLOWED_ORDINAL, "DIAMETER_ERROR_OPERATION_NOT_ALLOWED");
		
	/**
	 * The requested user data is not allowed to be read.
	 */
	public static final ResultCode DIAMETER_ERROR_USER_DATA_CANNOT_BE_READ = IMS.newImsResultCode(
			DIAMETER_ERROR_USER_DATA_CANNOT_BE_READ_ORDINAL, "DIAMETER_ERROR_USER_DATA_CANNOT_BE_READ");
		
	/**
	 * The requested user data is not allowed to be modified.
	 */
	public static final ResultCode DIAMETER_ERROR_USER_DATA_CANNOT_BE_MODIFIED = IMS.newImsResultCode(
			DIAMETER_ERROR_USER_DATA_CANNOT_BE_MODIFIED_ORDINAL, "DIAMETER_ERROR_USER_DATA_CANNOT_BE_MODIFIED");
	
	/**
	 * The requested user data is not allowed to be notified on changes.
	 */
	public static final ResultCode DIAMETER_ERROR_USER_DATA_CANNOT_BE_NOTIFIED = IMS.newImsResultCode(
			DIAMETER_ERROR_USER_DATA_CANNOT_BE_NOTIFIED_ORDINAL, "DIAMETER_ERROR_USER_DATA_CANNOT_BE_NOTIFIED");
	
	/**
	 * The request to update the repository data at the HSS could not be
	 * completed because the requested update is based on an out-of-date version
	 * of the repository data. That is, the sequence number in the Sh-Update
	 * Request message, does not match with the immediate successor of the
	 * associated sequence number stored for that repository data at the HSS. It
	 * is also used where an AS tries to create a new set of repository data
	 * when the identified repository data already exists in the HSS.
	 */
	public static final ResultCode DIAMETER_ERROR_TRANSPARENT_DATA_OUT_OF_SYNC = IMS.newImsResultCode(
			DIAMETER_ERROR_TRANSPARENT_DATA_OUT_OF_SYNC_ORDINAL, "DIAMETER_ERROR_TRANSPARENT_DATA OUT_OF_SYNC");
	
	/**
	 * The Application Server requested to subscribe to changes to Repository Data that is not present in the HSS.
	 */
	public static final ResultCode DIAMETER_ERROR_SUBS_DATA_ABSENT = IMS.newImsResultCode(
			DIAMETER_ERROR_SUBS_DATA_ABSENT_ORDINAL, "DIAMETER_ERROR_SUBS_DATA_ABSENT");
	
	/**
	 * The AS received a notification of changes of some information to which it is not subscribed
	 */
	public static final ResultCode DIAMETER_ERROR_NO_SUBSCRIPTION_TO_DATA = IMS.newImsResultCode(
			DIAMETER_ERROR_NO_SUBSCRIPTION_TO_DATA_ORDINAL, "DIAMETER_ERROR_NO_SUBSCRIPTION_TO_DATA");
	
	/**
	 * The Application Server addressed a DSAI not configured in the HSS.
	 */
	public static final ResultCode DIAMETER_ERROR_DSAI_NOT_AVAILABLE  = IMS.newImsResultCode(
			DIAMETER_ERROR_DSAI_NOT_AVAILABLE_ORDINAL, "DIAMETER_ERROR_DSAI_NOT_AVAILABLE ");
	
	/**
	 * The requested user data is not available at this time to satisfy the requested operation.
	 */
	public static final ResultCode DIAMETER_USER_DATA_NOT_AVAILABLE = IMS.newImsResultCode(
			DIAMETER_USER_DATA_NOT_AVAILABLE_ORDINAL, "DIAMETER_USER_DATA_NOT_AVAILABLE");
	
	/**
	 * The request to update the repository data at the HSS could not be
	 * completed because the related repository data is currently being updated
	 * by another entity.
	 */
	public static final ResultCode DIAMETER_PRIOR_UPDATE_IN_PROGRESS = IMS.newImsResultCode(
			DIAMETER_PRIOR_UPDATE_IN_PROGRESS_ORDINAL, "DIAMETER_PRIOR_UPDATE_IN_PROGRESS");
	

	
}
