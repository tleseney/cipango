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

import org.cipango.diameter.AVPList;
import org.cipango.diameter.ApplicationId;
import org.cipango.diameter.DiameterCommand;
import org.cipango.diameter.ResultCode;
import org.cipango.diameter.Type;
import org.cipango.diameter.base.Common;
import org.cipango.diameter.base.Common.EnumDataFormat;


/**
 * Cx is defined in
 * <ul>
 *  <li>3GPP TS 29229: Cx and Dx interfaces based on the Diameter protocol;Protocol details</li>
 *  <li>3GPP TS 29228: IP Multimedia (IM) Subsystem Cx and Dx interfaces; Signalling flows
 *    and message contents</li>
 * </ul>
 *  
 * @see <a href="http://www.3gpp.org/ftp/Specs/html-info/29228.htm">TS 29.228</a>
 * @see <a href="http://www.3gpp.org/ftp/Specs/html-info/29229.htm">TS 29.229</a>
 */
public class Cx 
{
	public static final int CX_APPLICATION = 16777216;
	
	public static final ApplicationId CX_APPLICATION_ID = new ApplicationId(
			org.cipango.diameter.ApplicationId.Type.Auth, 
			CX_APPLICATION, 
			IMS.IMS_VENDOR_ID);

	//-------------------------------------------------------------------------
	//                         Diameter commands
	//-------------------------------------------------------------------------
	public static final int
		UAR_ORDINAL = 300,
		UAA_ORDINAL = 300,
		SAR_ORDINAL = 301,
		SAA_ORDINAL = 301,
		LIR_ORDINAL = 302,
		LIA_ORDINAL = 302,
		MAR_ORDINAL = 303,
		MAA_ORDINAL = 303,
		RTR_ORDINAL = 304,
		RTA_ORDINAL = 304,
		PPR_ORDINAL = 305,
		PPA_ORDINAL = 305;
	
	
	/**
	 * The User-Authorization-Request (UAR) command, indicated by the
	 * Command-Code field set to 300 and the 'R' bit set in the Command Flags
	 * field, is sent by a Diameter Multimedia client to a Diameter Multimedia
	 * server in order to request the authorization of the registration of a
	 * multimedia user.
	 * 
	 * <pre>
	 * < User-Authorization-Request> ::= < Diameter Header: 300, REQ, PXY, 16777216 >
	 * 		< Session-Id >
	 * 		{ Vendor-Specific-Application-Id }
	 * 		{ Auth-Session-State }
	 * 		{ Origin-Host }
	 * 		{ Origin-Realm }
	 * 		[ Destination-Host ]
	 * 		{ Destination-Realm }
	 * 		{ User-Name }
	 * 	   *[ Supported-Features ]
	 * 		{ Public-Identity }
	 * 		{ Visited-Network-Identifier }
	 * 		[ User-Authorization-Type ]
	 * 		[ UAR-Flags ]
	 * 	   *[ AVP ]
	 * 	   *[ Proxy-Info ]
	 * 	   *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand UAR = IMS.newRequest(UAR_ORDINAL,
			"User-Authorization-Request");

	/**
	 * The User-Authorization-Answer (UAA) command, indicated by the
	 * Command-Code field set to 300 and the 'R' bit cleared in the Command
	 * Flags field, is sent by a server in response to the
	 * User-Authorization-Request command. The Experimental-Result AVP may
	 * contain one of the values defined in section 6.2.
	 * 
	 * <pre>
	 * < User-Authorization-Answer> ::=	 < Diameter Header: 300, PXY, 16777216 >
	 *      < Session-Id >
	 *      { Vendor-Specific-Application-Id }
	 *      [ Result-Code ]
	 *      [ Experimental-Result ]
	 *      { Auth-Session-State }
	 *      { Origin-Host }
	 *      { Origin-Realm }
	 *     *[ Supported-Features ]
	 *      [ Server-Name ]
	 *      [ Server-Capabilities ]
	 *      [ Wildcarded-IMPU ]
	 *     *[ AVP ]
	 *     *[ Failed-AVP ]
	 *     *[ Proxy-Info ]
	 *     *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand UAA = IMS.newAnswer(UAA_ORDINAL,
			"User-Authorization-Answer");

	/**
	 * The Server-Assignment-Request (SAR) command, indicated by the
	 * Command-Code field set to 301 and the 'R' bit set in the Command Flags
	 * field, is sent by a Diameter Multimedia client to a Diameter Multimedia
	 * server in order to request it to store the name of the server that is
	 * currently serving the user.
	 * 
	 * <pre>
	 * < Server-Assignment-Request > ::= < Diameter Header: 301, REQ, PXY, 16777216 > 
	 * 		< Session-Id >
	 * 		{ Vendor-Specific-Application-Id } 
	 * 		{ Auth-Session-State } 
	 * 		{ Origin-Host } 
	 * 		{ Origin-Realm }
	 * 		[ Destination-Host ] 
	 * 		{ Destination-Realm } 
	 * 		[ User-Name ] 
	 * 		*[ Public-Identity ] 
	 * 		{ Server-Name } 
	 * 		{ Server-Assignment-Type } 
	 * 		{ User-Data-Already-Available } 
	 * 		*[ AVP ] 
	 * 		*[ Proxy-Info ]
	 * 		*[ Route-Record ]
	 */
	public static final DiameterCommand SAR = IMS.newRequest(SAR_ORDINAL,
			"Server-Assignment-Request");

	/**
	 * The Server-Assignment-Answer (SAA) command, indicated by the Command-Code
	 * field set to 301 and the 'R' bit cleared in the Command Flags field, is
	 * sent by a server in response to the Server-Assignment-Request command.
	 * The Experimental-Result AVP may contain one of the values defined in
	 * section 6.2. If Result-Code or Experimental-Result does not inform about
	 * an error, the User-Data AVP shall contain the information that the S-CSCF
	 * needs to give service to the user.
	 * 
	 * <pre>
	 * < Server-Assignment-Answer > ::=	< Diameter Header: 301, PXY, 16777216 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     [ Result-Code ]
	 *     [ Experimental-Result ]
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     [ User-Name ]
	 *    *[ Supported-Features ]
	 *     [ User-Data ]
	 *     [ Charging-Information ]
	 *     [ Associated-Identities ]
	 *     [ Loose-Route-Indication ]
	 *    *[ SCSCF-Restoration-Info ]
	 *     [ Associated-Registered-Identities ]
	 *     [ Server-Name ]
	 *    *[ AVP ]
	 *    *[ Failed-AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand SAA = IMS.newAnswer(SAA_ORDINAL,
			"Server-Assignment-Answer");

	/**
	 * The Location-Info-Request (LIR) command, indicated by the Command-Code
	 * field set to 302 and the 'R' bit set in the Command Flags field, is sent
	 * by a Diameter Multimedia client to a Diameter Multimedia server in order
	 * to request name of the server that is currently serving the user.
	 * 
	 * <pre>
	 * < Location-Info-Request > ::= < Diameter Header: 302, REQ, PXY, 16777216 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     [ Destination-Host ]
	 *     { Destination-Realm }
	 *     [ Originating-Request ]
	 *    *[ Supported-Features ]
	 *     { Public-Identity }
	 *     [ User-Authorization-Type ]
	 *    *[ AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand LIR = IMS.newRequest(LIR_ORDINAL,
			"Location-Info-Request");
	
	/**
	 * The Location-Info-Answer (LIA) command, indicated by the Command-Code
	 * field set to 302 and the 'R' bit cleared in the Command Flags field, is
	 * sent by a server in response to the Location-Info-Request command. The
	 * Experimental-Result AVP may contain one of the values defined in section
	 * 6.2.
	 * 
	 * <pre>
	 * < Location-Info-Answer > ::= < Diameter Header: 302, PXY, 16777216 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     [ Result-Code ]
	 *     [ Experimental-Result ]
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *    *[ Supported-Features ]
	 *     [ Server-Name ]
	 *     [ Server-Capabilities ]
	 *     [ Wildcarded-PSI ]
	 *     [ Wildcarded-IMPU ]
	 *    *[ AVP ]
	 *    *[ Failed-AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand LIA = IMS.newAnswer(LIA_ORDINAL,
			"Location-Info-Answer");

	/**
	 * The Multimedia-Auth-Request (MAR) command, indicated by the Command-Code
	 * field set to 303 and the 'R' bit set in the Command Flags field, is sent
	 * by a Diameter Multimedia client to a Diameter Multimedia server in order
	 * to request security information.
	 * 
	 * <pre>
	 * < Multimedia-Auth-Request > ::=  < Diameter Header: 303, REQ, PXY, 16777216 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     { Destination-Realm }
	 *     [ Destination-Host ]
	 *     { User-Name }
	 *    *[ Supported-Features ]
	 *     { Public-Identity }
	 *     { SIP-Auth-Data-Item }
	 *     { SIP-Number-Auth-Items } 
	 *     { Server-Name }
	 *    *[ AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand MAR = IMS.newRequest(MAR_ORDINAL,
			"Multimedia-Auth-Request");
	
	/**
	 * The Multimedia-Auth-Answer (MAA) command, indicated by the Command-Code
	 * field set to 303 and the 'R' bit cleared in the Command Flags field, is
	 * sent by a server in response to the Multimedia-Auth-Request command. The
	 * Experimental-Result AVP may contain one of the values defined in section
	 * 6.2.
	 * 
	 * <pre>
	 * < Multimedia-Auth-Answer > ::= < Diameter Header: 303, PXY, 16777216 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     [ Result-Code ]
	 *     [ Experimental-Result ]
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     [ User-Name ]
	 *    *[ Supported-Features ]
	 *     [ Public-Identity ]
	 *     [ SIP-Number-Auth-Items ]
	 *    *[SIP-Auth-Data-Item ]
	 *     [ Wildcarded-IMPU ]
	 *    *[ AVP ]
	 *    *[ Failed-AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand MAA = IMS.newAnswer(MAA_ORDINAL,
			" Multimedia-Auth-Answer");

	/**
	 * The Registration-Termination-Request (RTR) command, indicated by the
	 * Command-Code field set to 304 and the 'R' bit set in the Command Flags
	 * field, is sent by a Diameter Multimedia server to a Diameter Multimedia
	 * client in order to request the de-registration of a user.
	 * 
	 * <pre>
	 * < Registration-Termination-Request > ::= < Diameter Header: 304, REQ, PXY, 16777216 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     { Destination-Host }
	 *     { Destination-Realm }
	 *     { User-Name }
	 *     [ Associated-Identities ]
	 *    *[ Supported-Features ]
	 *    *[ Public-Identity ]
	 *     { Deregistration-Reason }
	 *    *[ AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand RTR = IMS.newRequest(RTR_ORDINAL,
			"Registration-Termination-Request");

	/**
	 * The Registration-Termination-Answer (RTA) command, indicated by the
	 * Command-Code field set to 304 and the 'R' bit cleared in the Command
	 * Flags field, is sent by a client in response to the
	 * Registration-Termination-Request command. The Experimental-Result AVP may
	 * contain one of the values defined in section 6.2.
	 * 
	 * <pre>
	 * < Registration-Termination-Answer > ::= < Diameter Header: 304, PXY, 16777216 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     [ Result-Code ]
	 *     [ Experimental-Result ]
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     [ Associated-Identities ]
	 *    *[ Supported-Features ]
	 *    *[ AVP ]
	 *    *[ Failed-AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand RTA = IMS.newAnswer(RTA_ORDINAL,
			"Registration-Termination-Answer");

	/**
	 * The Push-Profile-Request (PPR) command, indicated by the Command-Code
	 * field set to 305 and the 'R' bit set in the Command Flags field, is sent
	 * by a Diameter Multimedia server to a Diameter Multimedia client in order
	 * to update the subscription data and for SIP Digest authentication the
	 * authentication data of a multimedia user in the Diameter Multimedia
	 * client whenever a modification has occurred in the subscription data or
	 * digest password that constitutes the data used by the client.
	 * 
	 * <pre>
	 * < Push-Profile-Request > ::=	< Diameter Header: 305, REQ, PXY, 16777216 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     { Auth-Session-State }
	 *     { Origin-Host }
	 *     { Origin-Realm }
	 *     { Destination-Host }
	 *     { Destination-Realm }
	 *     { User-Name }
	 *    *[ Supported-Features ]
	 *     [ User-Data ]
	 *     [ Charging-Information ]
	 *     [ SIP-Auth-Data-Item ]
	 *    *[ AVP ]
	 *    *[ Proxy-Info ]
	 *    *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand PPR = IMS.newRequest(PPR_ORDINAL,
			"Multimedia-Auth-Request");

	/**
	 * The Push-Profile-Answer (PPA) command, indicated by the Command-Code
	 * field set to 305 and the 'R' bit cleared in the Command Flags field, is
	 * sent by a client in response to the Push-Profile-Request command. The
	 * Experimental-Result AVP may contain one of the values defined in section
	 * 6.2.
	 * 
	 * <pre>
	 * < Push-Profile-Answer > ::= < Diameter Header: 305, PXY, 16777216 >
	 *     < Session-Id >
	 *     { Vendor-Specific-Application-Id }
	 *     [Result-Code ]
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
	public static final DiameterCommand PPA = IMS.newAnswer(PPA_ORDINAL,
			" Multimedia-Auth-Answer");

	//-------------------------------------------------------------------------
	//                               AVPs
	//-------------------------------------------------------------------------
	
	public static final int 
		VISITED_NETWORK_IDENTIFIER_ORDINAL = 600,
		PUBLIC_IDENTITY_ORDINAL = 601,
		SERVER_NAME_ORDINAL = 602,
		SERVER_CAPABILITIES_ORDINAL = 603,
		MANDATORY_CAPABILITIES_ORDINAL = 604,
		OPTIONAL_CAPABILITY_ORDINAL = 605,
		USER_DATA_ORDINAL = 606,
		SIP_NUMBER_AUTH_ITEMS_ORDINAL = 607,
		SIP_AUTHENTICATION_SCHEME_ORDINAL = 608,
		SIP_AUTHENTICATE_ORDINAL = 609,
		SIP_AUTHORIZATION_ORDINAL = 610,
		SIP_AUTHENTICATION_CONTEXT_ORDINAL = 611,
		SIP_AUTH_DATA_ITEM_ORDINAL = 612,
		SIP_ITEM_NUMBER_ORDINAL = 613,
		SERVER_ASSIGNMENT_TYPE_ORDINAL = 614,
		DERISTRATION_REASON_ORDINAL = 615,
		REASON_CODE_ORDINAL = 616,
		REASON_INFO_ORDINAL = 617,
		CHARGING_INFORMATION_ORDINAL = 618,
		PRIMARY_EVENT_CHARGING_FUNCTION_NAME_ORDINAL = 619,
		SECONDARY_EVENT_CHARGING_FUNCTION_NAME_ORDINAL = 620,
		PRIMARY_CHARGING_COLLECTION_FUNCTION_NAME_ORDINAL = 621,
		SECONDARY_CHARGING_COLLECTION_FUNCTION_NAME_ORDINAL = 622,
		USER_AUTHORIZATION_TYPE_ORDINAL = 623,
		USER_DATA_ALREADY_AVAILABLE_ORDINAL = 624,
		CONFIDENTIALITY_KEY_ORDINAL = 625,
		INTEGRITY_KEY_ORDINAL = 626,
		SUPPORTED_FEATURES_ORDINAL = 628,
		FEATURE_LIST_ID_ORDINAL = 629,
		FEATURE_LIST_ORDINAL = 630,
		SUPPORTED_APPLICATIONS_ORDINAL = 631,
		ASSOCIATED_IDENTITIES_ORDINAL = 632,
		ORIGININATING_REQUEST_ORDINAL = 633,
		WILCARDED_PSI_ORDINAL = 634,
		SIP_DIGEST_AUTHENTICATE_ORDINAL = 635,
		WILCARDED_IMPU_ORDINAL = 636,
		UAR_FLAGS_ORDINAL = 637,
		LOOSE_ROUTE_INDICATION_ORDINAL = 638,
		SCSCF_RESTORATION_INFO_ORDINAL = 639,
		PATH_ORDINAL = 640,
		CONTACT_ORDINAL = 641,
		SUBSCRIPTION_INFO_ORDINAL = 642,
		CALL_ID_SIP_HEADER_ORDINAL = 643,
		FROM_SIP_HEADER_ORDINAL = 644,
		TO_SIP_HEADER_ORDINAL = 645,
		RECORD_ROUTE_ORDINAL = 646,
		ASSOCIATED_REGISTERED_IDENTITIES_ORDINAL = 647,
		MULTIPLE_REGISTRATION_INDICATION_ORDINAL = 648,
		REGISTRATION_INFO_ORDINAL = 649;
		

	/**
	 * The Visited-Network-Identifier AVP is of type OctetString. This AVP contains an identifier that helps the home network to identify the visited network (e.g. the visited network domain name)
	 */
	public static final Type<byte[]> VISITED_NETWORK_IDENTIFIER = IMS.newIMSType("Visited-Network-Identifier", 
			VISITED_NETWORK_IDENTIFIER_ORDINAL, Common.__octetString);
	
	/**
	 * The Supported-Features AVP is of type Grouped. If this AVP is present it may inform the destination host about 
	 * the features that the origin host supports. 
	 * The Feature-List AVP contains a list of supported features of the origin host. 
	 * The Vendor-Id AVP and the Feature-List AVP shall together identify which feature list is 
	 * carried in the Supported-Features AVP.
	 * <p>
	 * Where a Supported-Features AVP is used to identify features that have been defined by 3GPP, the Vendor-Id AVP 
	 * shall contain the vendor ID of 3GPP. Vendors may define proprietary features, but it is strongly recommended 
	 * that the possibility is used only as the last resort. Where the Supported-Features AVP is used to identify 
	 * features that have been defined by a vendor other than 3GPP, it shall contain the vendor ID of the specific 
	 * vendor in question.
	 * <p>
	 * If there are multiple feature lists defined by the same vendor, the Feature-List-ID AVP shall differentiate 
	 * those lists from one another. The destination host shall use the value of the Feature-List-ID AVP to identify 
	 * the feature list.
	 * 
	 * <pre> {code
	 * Supported-Features ::= < AVP header: 628 10415 > 
	 * 		{ Vendor-Id } 
	 * 		{ Feature-List-ID } 
	 * 		{ Feature-List }
	 * 		*[AVP]
	 * } </pre>
	 */
	public static final Type<AVPList> SUPPORTED_FEATURES = IMS.newIMSType("Supported-Features", 
			SUPPORTED_FEATURES_ORDINAL, Common.__grouped);
		
	/**
	 * The Server-Capabilities AVP is of type Grouped. This AVP contains information to assist the 
	 * I-CSCF in the selection of an S-CSCF.
	 * 
	 * <pre>
	 * Server-Capabilities ::= <AVP header: 603 10415>
	 *    *[Mandatory-Capability]
	 *    *[Optional-Capability]
	 *    *[Server-Name]
	 *    *[AVP]
	 * </pre>
	 */
	public static final Type<AVPList> SERVER_CAPABILITIES = IMS.newIMSType("Server-Capabilities", 
			SERVER_CAPABILITIES_ORDINAL, Common.__grouped);
	
	/**
	 * The Mandatory-Capability AVP is of type Unsigned32. The value included in this AVP can be used
	 * to represent a single determined mandatory capability of an S-CSCF. Each mandatory capability
	 * available in an individual operator's network shall be allocated a unique value.  The allocation
	 * of these values to individual capabilities is an operator issue.
	 */
	public static final Type<Integer> MANDATORY_CAPABILITIES = IMS.newIMSType("Mandatory-Capability", 
			MANDATORY_CAPABILITIES_ORDINAL, Common.__unsigned32);
	
	/**
	 * The Optional-Capability AVP is of type Unsigned32. The value included in this AVP can be used 
	 * to represent a single determined optional capability of an S-CSCF. Each optional capability 
	 * available in an individual operator's network shall be allocated a unique value.  The allocation
	 * of these values to individual capabilities is an operator issue.
	 */
	public static final Type<Integer> OPTIONAL_CAPABILITIES = IMS.newIMSType("Optional-Capability", 
			OPTIONAL_CAPABILITY_ORDINAL, Common.__unsigned32);
	
	/**
	 * The SIP-Number-Auth-Items AVP is of type Unsigned32.
	 * When used in a request, the SIP-Number-Auth-Items indicates the number of authentication 
	 * vectors the S-CSCF is requesting. This can be used, for instance, when the client is requesting
	 * several pre-calculated authentication vectors. In the answer message, the SIP-Number-Auth-Items
	 * AVP indicates the actual number of SIP-Auth-Data-Item AVPs provided by the Diameter server.
	 */
	public static final Type<Integer> SIP_NUMBER_AUTH_ITEMS = IMS.newIMSType("SIP-Number-Auth-Items", 
			SIP_NUMBER_AUTH_ITEMS_ORDINAL, Common.__unsigned32);
	
	/**
	 * The Authentication-Scheme AVP is of type UTF8String and indicates the authentication scheme used
	 * in the authentication of SIP messages. 
	 */
	public static final Type<String> SIP_AUTHENTICATION_SCHEME = IMS.newIMSType("Authentication-Scheme", 
			SIP_AUTHENTICATION_SCHEME_ORDINAL, Common.__utf8String);
	
	/**
	 * The SIP-Authenticate AVP is of type OctetString and contains specific parts of the data portion 
	 * of the WWW-Authenticate or Proxy-Authenticate SIP headers that are to be present in a SIP 
	 * response. The identification and encoding of the specific parts are defined in 3GPP TS 29.228 [1].
	 */
	public static final Type<byte[]> SIP_AUTHENTICATE = IMS.newIMSType("SIP-Authenticate",
			SIP_AUTHENTICATE_ORDINAL, Common.__octetString);
	
	/**
	 * The SIP-Authorization AVP is of type OctetString and contains specific parts of the data portion 
	 * of the Authorization or Proxy-Authorization SIP headers suitable for inclusion in a SIP request.
	 * The identification and encoding of the specific parts are defined in 3GPP TS 29.228 [1]. 
	 */
	public static final Type<byte[]> SIP_AUTHORIZATION = IMS.newIMSType("SIP-Authorization",
			SIP_AUTHORIZATION_ORDINAL, Common.__octetString);
	
	/**
	 * The SIP-Authentication-Context AVP is of type OctectString, and contains authentication-related
	 * information relevant for performing the authentication but that is not part of the SIP 
	 * authentication headers.
	 * 
	 * Some mechanisms (e.g. PGP, digest with quality of protection set to auth-int defined in IETF 
	 * RFC 2617, digest with predictive nonces or sip access digest) request that part or the whole 
	 * SIP request is passed to the entity performing the authentication. In such cases the 
	 * SIP-Authentication-Context AVP would be carrying such information.
	 */
	public static final Type<byte[]> SIP_AUTHENTICATION_CONTEXT = IMS.newIMSType("SIP-Authentication-Context",
			SIP_AUTHENTICATION_CONTEXT_ORDINAL, Common.__octetString);
	
	/**
	 * The SIP-Auth-Data-Item is of type Grouped, and contains the authentication and/or authorization
	 * information for the Diameter client.
	 * 
	 * <pre>
	 * SIP-Auth-Data-Item :: = < AVP Header : 612 10415 >
	 *    [ SIP-Item-Number ]
	 *    [ SIP-Authentication-Scheme ]
	 *    [ SIP-Authenticate ]
	 *    [ SIP-Authorization ]
	 *    [ SIP-Authentication-Context ]
	 *    [ Confidentiality-Key ]
	 *    [ Integrity-Key ]
	 *    [ SIP-Digest-Authenticate ]
	 *    [ Framed-IP-Address ]
	 *    [ Framed-IPv6-Prefix ]
	 *    [ Framed-Interface-Id ]
	 *   *[ Line-Identifier ]
	 *   *[AVP]
	 * </pre>
	 */
	public static final Type<AVPList> SIP_AUTH_DATA_ITEM = IMS.newIMSType("SIP-Auth-Data-Item", 
			SIP_AUTH_DATA_ITEM_ORDINAL, Common.__grouped);
	
	/**
	 * The SIP-Item-Number AVP is of type Unsigned32, and is included in a
	 * SIP-Auth-Data-Item grouped AVP in circumstances where there are multiple
	 * occurrences of SIP-Auth-Data-Item AVP, and the order in which they should
	 * be processed is significant. In this scenario, SIP-Auth-Data-Item AVP
	 * with a low SIP-Item-Number value should be processed before
	 * SIP-Auth-Data-Items AVPs with a high SIP-Item-Number value.
	 */
	public static final Type<Integer> SIP_ITEM_NUMBER = IMS.newIMSType("SIP-Item-Number", 
			SIP_ITEM_NUMBER_ORDINAL, Common.__unsigned32);
	
	public static enum ServerAssignmentType
	{
		/**
		 * This value is used to request from HSS the user profile assigned to one or more public 
	     * identities, without affecting the registration state of those identities.
		 */
		NO_ASSIGNMENT, 
		/**
		 * The request is generated as a consequence of a first registration of an identity.
		 */
		REGISTRATION, 
		/**
		 * The request corresponds to the re-registration of an identity.
		 */
		RE_REGISTRATION, 
		/**
		 * The request is generated because the S-CSCF received an INVITE for
		 * a public identity that is not registered.
		 */
		UNREGISTERED_USER, 
		/**
		 * The SIP registration timer of an identity has expired.
		 */
		TIMEOUT_DEREGISTRATION, 
		/**
		 * The S-CSCF has received a user initiated de-registration request.
		 */
		USER_DEREGISTRATION,
		/**
		 * The SIP registration timer of an identity has expired. The S-CSCF
		 * keeps the user data stored in the S-CSCF and requests HSS to store
		 * the S-CSCF name.
		 */
		TIMEOUT_DEREGISTRATION_STORE_SERVER_NAME, 
		/**
		 * The S-CSCF has received a user initiated de-registration request. The
		 * S-CSCF keeps the user data stored in the S-CSCF and requests HSS to
		 * store the S-CSCF name.
		 */
		USER_DEREGISTRATION_STORE_SERVER_NAME, 
		/**
		 *  The S-CSCF, due to administrative reasons, has performed the de-registration of an identity.
		 */
		ADMINISTRATIVE_DEREGISTRATION,
		/**
		 * The authentication of a user has failed.
		 */
		AUTHENTICATION_FAILURE, 
		/**
		 * The authentication timeout has expired.
		 */
		AUTHENTICATION_TIMEOUT, 
		/**
		 * The S-CSCF has requested user profile information from the HSS and
		 * has received a volume of data higher than it can accept.
		 */
		DEREGISTRATION_TOO_MUCH_DATA
	}
	
	/**
	 * The Server-Assignment-Type AVP is of type Enumerated, and indicates the type of server update being performed 
	 * in a Server-Assignment-Request operation. 
	 */
	public static final Type<ServerAssignmentType> SERVER_ASSIGNMENT_TYPE = IMS.newIMSType(
			"Server-Assignment-Type", SERVER_ASSIGNMENT_TYPE_ORDINAL, new EnumDataFormat<ServerAssignmentType>(ServerAssignmentType.class));

	/**
	 * The Deregistration-Reason AVP is of type Grouped, and indicates the
	 * reason for a de-registration operation.
	 * 
	 * <pre>
	 * Deregistration-Reason :: = < AVP Header : 615 10415 > 
	 *     { Reason-Code } 
	 *     [ Reason-Info ] 
	 *    *[AVP]
	 * </pre>
	 */
	public static final Type<AVPList> DERISTRATION_REASON = IMS.newIMSType("Deregistration-Reason", 
			DERISTRATION_REASON_ORDINAL, Common.__grouped);
	
	public static enum ReasonCode
	{
		PERMANENT_TERMINATION,
		NEW_SERVER_ASSIGNED,
		SERVER_CHANGE,
		REMOVE_SCSCF;
	}
	
	/**
	 * The Reason-Code AVP is of type Enumerated, and defines the reason for the network initiated de-registration.
	 * @see ReasonCode
	 */
	public static final Type<ReasonCode> REASON_CODE = IMS.newIMSType("Reason-Code",
			REASON_CODE_ORDINAL, new EnumDataFormat<ReasonCode>(ReasonCode.class));
	
	/**
	 * The Reason-Info AVP is of type UTF8String, and contains textual
	 * information to inform the user about the reason for a de-registration.
	 */
	public static final Type<String> REASON_INFO = IMS.newIMSType("Reason-Info", 
			REASON_INFO_ORDINAL, Common.__utf8String);
	
	public static enum UserAuthorizationType
	{
		/**
		 * This value is used in case of the initial registration or
		 * re-registration. I-CSCF determines this from the Expires field or
		 * expires parameter in Contact field in the SIP REGISTER method if it
		 * is not equal to zero. This is the default value.
		 */
		REGISTRATION,
		/**
		 * This value is used in case of the de-registration. I-CSCF determines
		 * this from the Expires field or expires parameter in Contact field in
		 * the SIP REGISTER method if it is equal to zero.
		 */
		DE_REGISTRATION,
		/**
		 * This value is used in case of initial registration, re-registration
		 * or terminating SIP request and when the I-CSCF explicitly requests
		 * S-CSCF capability information from the HSS. The I-CSCF shall use this
		 * value when the user's current S-CSCF, which is stored in the HSS,
		 * cannot be contacted and a new S-CSCF needs to be selected
		 */
		REGISTRATION_AND_CAPABILITIES;
	}

	/**
	 * The User-Authorization-Type AVP is of type Enumerated, and indicates the
	 * type of user authorization being performed in a User Authorization
	 * operation, i.e. UAR command.
	 */
	public static final Type<UserAuthorizationType> USER_AUTHORIZATION_TYPE = IMS.newIMSType("User-Authorization-Type",
			USER_AUTHORIZATION_TYPE_ORDINAL, new EnumDataFormat<UserAuthorizationType>(UserAuthorizationType.class));
	
	
	public static enum UserDataAlreadyAvailable
	{
		/** (0) The S-CSCF does not have the data that it needs to serve the user */
		USER_DATA_NOT_AVAILABLE,
		
		/** (1) The S-CSCF already has the data that it needs to serve the user */
		USER_DATA_ALREADY_AVAILABLE
	}
	
	/**
	 * The User-Data-Already-Available AVP is of type Enumerated, and indicates to the HSS whether or not the S-CSCF already has the part of the user profile that it needs to serve the user. The following values are defined:
	 * @see UserDataAlreadyAvailable
	 */
	public static final Type<UserDataAlreadyAvailable> USER_DATA_ALREADY_AVAILABLE = IMS.newIMSType(
			"User-Data-Already-Available", USER_DATA_ALREADY_AVAILABLE_ORDINAL, new EnumDataFormat<UserDataAlreadyAvailable>(UserDataAlreadyAvailable.class));

	/**
	 * The Confidentiality-Key is of type OctetString, and contains the Confidentiality Key (CK).
	 */
	public static final Type<byte[]> CONFIDENTIALITY_KEY = IMS.newIMSType("Confidentiality-Key",
			CONFIDENTIALITY_KEY_ORDINAL, Common.__octetString);
	
	/**
	 * The Integrity-Key is of type OctetString, and contains the Integrity Key (IK).
	 */
	public static final Type<byte[]> INTEGRITY_KEY = IMS.newIMSType("Integrity-Key",
			INTEGRITY_KEY_ORDINAL, Common.__octetString);
	
	/**
	 * The Associated-Identities AVP is of type Grouped and it contains the
	 * private user identities associated to an IMS subscription.
	 * 
	 * <pre>
	 * Associated-Identities ::= < AVP header: 632, 10415 >
	 *     *[ User-Name ]
	 *     *[ AVP ]
	 * </pre>
	 */
	public static final Type<AVPList> ASSOCIATED_IDENTITIES = IMS.newIMSType("Associated-Identities", 
			ASSOCIATED_IDENTITIES_ORDINAL, Common.__grouped).setMandatory(false);
	
	public static enum OriginatingRequest
	{
		/** (0) This value informs the HSS that it should check originating unregistered services for the public identity. */
		ORIGINATING;
	}
	
	/**
	 * The Originating-Request AVP is of type Enumerated and indicates to the
	 * HSS that the request is related to an AS originating SIP request in the
	 * Location-Information-Request operation.
	 * @see OriginatingRequest
	 */
	public static final Type<OriginatingRequest> ORIGININATING_REQUEST = IMS.newIMSType("Originating-Request", 
			ORIGININATING_REQUEST_ORDINAL, new EnumDataFormat<OriginatingRequest>(OriginatingRequest.class));


	/**
	 * The SIP-Digest-Authenticate is of type Grouped and it contains a
	 * reconstruction of either the SIP WWW-Authenticate or Proxy-Authentication
	 * header fields specified in IETF RFC 2617 [14].
	 * 
	 * <pre>
	 * SIP-Digest-Authenticate ::= < AVP Header: 635 10415>
	 *    { Digest-Realm }
	 *    [ Digest-Algorithm ] 
	 *    { Digest-QoP } 
	 *    { Digest-HA1} 
	 *   *[ AVP ]
	 * </pre>
	 */
	public static final Type<AVPList> SIP_DIGEST_AUTHENTICATE = IMS.newIMSType("SIP-Digest-Authenticate", 
			SIP_DIGEST_AUTHENTICATE_ORDINAL, Common.__grouped).setMandatory(false);
	
	/**
	 * The Wildcarded-PSI AVP is of type UTF8String. This AVP contains a
	 * Wildcarded PSI stored in the HSS. The syntax of the contents of this AVP
	 * is described in 3GPP TS 23.003 [13].
	 */
	public static final Type<String> WILCARDED_PSI = IMS.newIMSType("Wildcarded-PSI", 
			WILCARDED_PSI_ORDINAL, Common.__utf8String).setMandatory(false);
	
	/**
	 * The Public-Identity AVP is of type UTF8String. This AVP contains the
	 * public identity of a user in the IMS. The syntax of this AVP corresponds
	 * either to a SIP URL (with the format defined in IETF RFC 3261 [3] and
	 * IETF RFC 2396 [4]) or a TEL URL (with the format defined in IETF RFC 3966
	 * [8])
	 */
	public static final Type<String> PUBLIC_IDENTITY = IMS.newIMSType("Public-Identity", 
			PUBLIC_IDENTITY_ORDINAL, Common.__utf8String);
	
	/**
	 * The Wildcarded-IMPU AVP is of type UTF8String. This AVP contains a
	 * Wildcarded Public User Identity stored in the HSS. The syntax of the
	 * contents of this AVP is described in 3GPP TS 23.003 [13].
	 */
	public static final Type<String> WILCARDED_IMPU = IMS.newIMSType("Wildcarded-IMPU", 
			WILCARDED_IMPU_ORDINAL, Common.__utf8String).setMandatory(false);

	/**
	 * The UAR-Flags AVP is of type Unsigned32 and it contains a bit mask. The
	 * meaning of the bits is defined in the following table:
	 * <ul>
	 * <li>Bit 0: IMS Emergency Registration: This bit, when set, indicates that
	 * the request corresponds to an IMS Emergency Registration.</li>
	 * </ul>
	 * Note: Bits not defined in this table shall be cleared by the sending
	 * I-CSCF and discarded by the receiving HSS.
	 */
	public static final Type<Integer> UAR_FLAGS = IMS.newIMSType("UAR-Flags", 
			UAR_FLAGS_ORDINAL, Common.__unsigned32).setMandatory(false);
	
	/**
	 * The Server-Name AVP is of type UTF8String. This AVP contains a SIP-URL (as defined in IETF 
	 * RFC 3261 [3] and IETF RFC 2396 [4]), used to identify a SIP server (e.g. S-CSCF name).
	 */
	public static final Type<String> SERVER_NAME = IMS.newIMSType("Server-Name", 
			SERVER_NAME_ORDINAL, Common.__utf8String);
	
	
	
	//-------------------------------------------------------------------------
	//                           Result codes
	//-------------------------------------------------------------------------
	
	public static final int
		DIAMETER_FIRST_REGISTRATION_ORDINAL = 2001,
		DIAMETER_SUBSEQUENT_REGISTRATION_ORDINAL = 2002,
		DIAMETER_UNREGISTERED_SERVICE_ORDINAL = 2003,
		DIAMETER_SUCCESS_SERVER_NAME_NOT_STORED_ORDINAL = 2004,
		
		DIAMETER_ERROR_USER_UNKNOWN_ORDINAL = 5001,
		DIAMETER_ERROR_IDENTITIES_DONT_MATCH_ORDINAL = 5002,
		DIAMETER_ERROR_IDENTITY_NOT_REGISTERED_ORDINAL = 5003,
		DIAMETER_ERROR_ROAMING_NOT_ALLOWED_ORDINAL = 5004,
		DIAMETER_ERROR_IDENTITY_ALREADY_REGISTERED_ORDINAL = 5005,
		DIAMETER_ERROR_AUTH_SCHEME_NOT_SUPPORTED_ORDINAL = 5006,
		DIAMETER_ERROR_IN_ASSIGNMENT_TYPE_ORDINAL = 5007,
		DIAMETER_ERROR_TOO_MUCH_DATA_ORDINAL = 5008,
		DIAMETER_ERROR_NOT_SUPPORTED_USER_DATA_ORDINAL = 5009,
		DIAMETER_ERROR_FEATURE_UNSUPPORTED_ORDINAL = 5011;
			
	/**
	 * The HSS informs the I-CSCF that:
	 * -	The user is authorized to register this public identity;
	 * -	A S-CSCF shall be assigned to the user.
	 */
	public static final ResultCode DIAMETER_FIRST_REGISTRATION = IMS.newImsResultCode(
			DIAMETER_FIRST_REGISTRATION_ORDINAL, "DIAMETER_FIRST_REGISTRATION");
	
	/**
	 * The HSS informs the I-CSCF that:
	 * -	The user is authorized to register this public identity;
	 * -	A S-CSCF is already assigned and there is no need to select a new one.
	 */
	public static final ResultCode DIAMETER_SUBSEQUENT_REGISTRATION = IMS.newImsResultCode(
			DIAMETER_SUBSEQUENT_REGISTRATION_ORDINAL, "DIAMETER_SUBSEQUENT_REGISTRATION");
	
	/**
	 * The HSS informs the I-CSCF that:
	 * -	The public identity is not registered but has services related to unregistered state;
	 * -	A S-CSCF shall be assigned to the user.
	 */
	public static final ResultCode DIAMETER_UNREGISTERED_SERVICE = IMS.newImsResultCode(
			DIAMETER_UNREGISTERED_SERVICE_ORDINAL, "DIAMETER_UNREGISTERED_SERVICE");
	
	/**
	 * The HSS informs to the S-CSCF that: 
	 * -	The de-registration is completed;
	 * -	The S-CSCF name is not stored in the HSS.
	 */
	public static final ResultCode DIAMETER_SUCCESS_SERVER_NAME_NOT_STORED = IMS.newImsResultCode(
			DIAMETER_SUCCESS_SERVER_NAME_NOT_STORED_ORDINAL, "DIAMETER_SUCCESS_SERVER_NAME_NOT_STORED");
	
	/**
	 * A message was received for a user that is unknown.
	 */
	public static final ResultCode DIAMETER_ERROR_USER_UNKNOWN = IMS.newImsResultCode(
			DIAMETER_ERROR_USER_UNKNOWN_ORDINAL, "DIAMETER_ERROR_USER_UNKNOWN");
	
	/**
	 * A message was received with a public identity and a private identity for
	 * a user, and the server determines that the public identity does not
	 * correspond to the private identity.
	 */
	public static final ResultCode DIAMETER_ERROR_IDENTITIES_DONT_MATCH = IMS.newImsResultCode(
			DIAMETER_ERROR_IDENTITIES_DONT_MATCH_ORDINAL, "DIAMETER_ERROR_IDENTITIES_DONT_MATCH");
	
	/**
	 * A query for location information is received for a public identity that
	 * has not been registered before. The user to which this identity belongs
	 * cannot be given service in this situation.
	 */
	public static final ResultCode DIAMETER_ERROR_IDENTITY_NOT_REGISTERED = IMS.newImsResultCode(
			DIAMETER_ERROR_IDENTITY_NOT_REGISTERED_ORDINAL, "DIAMETER_ERROR_IDENTITY_NOT_REGISTERED");
	
	/**
	 * The user is not allowed to roam in the visited network.
	 */
	public static final ResultCode DIAMETER_ERROR_ROAMING_NOT_ALLOWED = IMS.newImsResultCode(
			DIAMETER_ERROR_ROAMING_NOT_ALLOWED_ORDINAL, "DIAMETER_ERROR_ROAMING_NOT_ALLOWED ");
	
	/**
	 * The identity has already a server assigned and the registration status
	 * does not allow that it is overwritten
	 */
	public static final ResultCode DIAMETER_ERROR_IDENTITY_ALREADY_REGISTERED = IMS
			.newImsResultCode(
					DIAMETER_ERROR_IDENTITY_ALREADY_REGISTERED_ORDINAL,
					"DIAMETER_ERROR_IDENTITY_ALREADY_REGISTERED");

	/**
	 * The authentication scheme indicated in an authentication request is not
	 * supported.
	 */
	public static final ResultCode DIAMETER_ERROR_AUTH_SCHEME_NOT_SUPPORTED = IMS
			.newImsResultCode(DIAMETER_ERROR_AUTH_SCHEME_NOT_SUPPORTED_ORDINAL,
					"DIAMETER_ERROR_AUTH_SCHEME_NOT_SUPPORTED");

	/**
	 * The identity being registered has already the same server assigned and
	 * the registration status does not allow the server assignment type.
	 */
	public static final ResultCode DIAMETER_ERROR_IN_ASSIGNMENT_TYPE = IMS
			.newImsResultCode(DIAMETER_ERROR_IN_ASSIGNMENT_TYPE_ORDINAL,
					"DIAMETER_ERROR_IN_ASSIGNMENT_TYPE");

	/**
	 * The volume of the data pushed to the receiving entity exceeds its
	 * capacity. NOTE: This error code is also used in 3GPP TS 29.329 [11].
	 */
	public static final ResultCode DIAMETER_ERROR_TOO_MUCH_DATA = IMS
			.newImsResultCode(DIAMETER_ERROR_TOO_MUCH_DATA_ORDINAL,
					"DIAMETER_ERROR_TOO_MUCH_DATA");

	/**
	 * A request application message was received indicating that the origin
	 * host requests that the command pair would be handled using a feature
	 * which is not supported by the destination host.
	 */
	public static final ResultCode DIAMETER_ERROR_FEATURE_UNSUPPORTED = IMS
			.newImsResultCode(DIAMETER_ERROR_FEATURE_UNSUPPORTED_ORDINAL,
					"DIAMETER_ERROR_FEATURE_UNSUPPORTED");
}
