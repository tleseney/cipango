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

import java.util.Date;

import org.cipango.diameter.ApplicationId;
import org.cipango.diameter.DiameterCommand;
import org.cipango.diameter.ResultCode;
import org.cipango.diameter.Type;
import org.cipango.diameter.base.Common;
import org.cipango.diameter.base.Common.EnumDataFormat;

/**
 * Zh and Zn is defined in
 * <ul>
 *  <li>3GPP TS 29109: Generic Authentication Architecture (GAA);
 *      Zh and Zn Interfaces based on the Diameter protocol;</li>
 * </ul>
 *  
 * @see <a href="http://www.3gpp.org/ftp/Specs/html-info/29109.htm">TS 29.109</a>
 */
public class Zh
{
	public static final int ZH_APPLICATION = 16777221;
	public static final int ZN_APPLICATION = 16777220;

	public static final ApplicationId ZH_APPLICATION_ID = new ApplicationId(
			org.cipango.diameter.ApplicationId.Type.Auth, ZH_APPLICATION,
			IMS.IMS_VENDOR_ID);

	public static final ApplicationId ZN_APPLICATION_ID = new ApplicationId(
			org.cipango.diameter.ApplicationId.Type.Auth, ZN_APPLICATION,
			IMS.IMS_VENDOR_ID);

	//-------------------------------------------------------------------------
	//                         Diameter commands
	//-------------------------------------------------------------------------
	
	public static final int
		BIR_ORDINAL = 310,
		BIA_ORDINAL = 310;

	/**
	 * <pre>
	 * < Bootstrapping-Info-Request > ::=< Diameter Header: 310, REQ, PXY, 16777220 >
	 *    < Session-Id >
	 *    { Vendor-Specific-Application-Id }
	 *    { Origin-Host }	; Address of NAF
	 *    { Origin-Realm }	; Realm of NAF
	 *    { Destination-Realm }	; Realm of BSF
	 *    [ Destination-Host ]	; Address of the BSF 
	 *   * [ GAA-Service-Identifier ]	; Service identifiers
	 *    { Transaction-Identifier }	; B-TID
	 *    { NAF-Id }	; NAF_ID
	 *    [ GBA_U-Awareness-Indicator ]	; GBA_U awareness of the NAF
	 *   *[ AVP ]
	 *   *[ Proxy-Info ]
	 *   *[ Route-Record ]
	 * </pre>
	 */
	public static final DiameterCommand BIR = IMS.newRequest(BIR_ORDINAL,
	"Bootstrapping-Info-Request");
	
	/**
	 * <pre>
	 * < Boostrapping-Info-Answer > ::= < Diameter Header: 310, PXY, 16777220 >
	 *    < Session-Id >
	 *    { Vendor-Specific-Application-Id }
	 *    [ Result-Code ]	
	 *    [ Experimental-Result]
	 *    { Origin-Host }	; Address of BSF
	 *    { Origin-Realm }	; Realm of BSF
	 *    [ User-Name ]	; IMPI
	 *    [ ME-Key-Material ]	; Required
	 *    [ UICC-Key-Material ]	; Conditional
	 *    [ Key-ExpiryTime ]	; Time of expiry
	 *    [ BootstrapInfoCreationTime ]	; Bootstrapinfo creation time
	 *    [ GBA-UserSecSettings ]	; Selected USSs
	 *    [ GBA-Type ]	; GBA type used in bootstrapping
	 *   *[ AVP ]
	 *   *[ Proxy-Info ]
	 *   *[ Route-Record ]
     * </pre>
	 */
	public static final DiameterCommand BIA = IMS.newAnswer(BIA_ORDINAL,
	"Bootstrapping-Info-Answer");
	
	// -------------------------------------------------------------------------
	// Result codes
	// -------------------------------------------------------------------------

	public static final int DIAMETER_ERROR_IDENTITY_UNKNOWN_ORDINAL = 5401,
			DIAMETER_ERROR_NOT_AUTHORIZED_ORDINAL = 5402,
			DIAMETER_ERROR_TRANSACTION_IDENTIFIER_INVALID_ORDINAL = 5403;

	/**
	 * A message was received by the HSS for an IMPI or IMPU that is unknown.
	 */
	public static final ResultCode DIAMETER_ERROR_IDENTITY_UNKNOWN = IMS
			.newImsResultCode(DIAMETER_ERROR_IDENTITY_UNKNOWN_ORDINAL,
					"DIAMETER_ERROR_IDENTITY_UNKNOWN");

	/**
	 * A message was received by the BSF which the BSF can not authorize. In
	 * this case the NAF should indicate to the UE that the service is not
	 * allowed. In case of GBA push, the NAF should not contact the UE.
	 */
	public static final ResultCode DIAMETER_ERROR_NOT_AUTHORIZED = IMS
			.newImsResultCode(DIAMETER_ERROR_NOT_AUTHORIZED_ORDINAL,
					"DIAMETER_ERROR_NOT_AUTHORIZED");

	/**
	 * A message was received by the BSF for an invalid (e.g. unknown or
	 * expired) Bootstrapping Transaction Identifier (B-TID). In this case the
	 * NAF should request the UE to bootstrap again.
	 */
	public static final ResultCode DIAMETER_ERROR_TRANSACTION_IDENTIFIER_INVALID = IMS
			.newImsResultCode(
					DIAMETER_ERROR_TRANSACTION_IDENTIFIER_INVALID_ORDINAL,
					"DIAMETER_ERROR_TRANSACTION_IDENTIFIER_INVALID");

	//-------------------------------------------------------------------------
	//                               AVPs
	//-------------------------------------------------------------------------
	
	public static final int 
		GBA_USER_SEC_SETTINGS_ORDINAL = 400,
		TRANSACTION_IDENTIFIER_ORDINAL = 401, 
		NAF_ID_ORDINAL = 402,
		GAA_SERVICE_IDENTIFIER_ORDINAL = 403,
		KEY_EXPIRY_TIME_ORDINAL = 404, 
		ME_KEY_MATERIAL_ORDINAL = 405,
		UICC_KEY_MATERIAL_ORDINAL = 406,
		GBA_U_AWARENESS_INDICATOR_ORDINAL = 407,
		BOOTSTRAP_INFO_CREATION_TIME_ORDINAL = 408,
		GUSS_TIMESTAMP_ORDINAL = 409,
		GBA_TYPE_ORDINAL = 410,
		UE_ID_ORDINAL = 411,
		UE_ID_TYPE_ORDINAL = 412,
		UICC_APP_LABEL_ORDINAL = 413,
		UICC_ME_ORDINAL = 414,
		REQUESTED_KEY_LIFETIME_ORDINAL = 415,
		PRIVATE_IDENTITY_REQUEST_ORDINAL = 416,
		GBA_PUSH_INFO_ORDINAL = 417,
		NAF_SA_IDENTIFIER_ORDINAL = 418;
	
	
	

	/**
	 * The GAA-UserSecSettings AVP (AVP code 400) is of type OctetString. If
	 * transmitted on the Zh interface it contains GBA user security settings
	 * (GUSS). If transmitted on the Zn interface it contains the relevant USSs
	 * only. The content of GBA-UserSecSettings AVP is a XML document whose root
	 * element shall be the "guss" element for Zh interface and the "ussList"
	 * element for the Zn interface. The XML schema is defined in annex A.
	 */
	public static final Type<String> GBA_USER_SEC_SETTINGS = IMS.newIMSType(
			"GBA-UserSecSettings", GBA_USER_SEC_SETTINGS_ORDINAL, Common.__utf8String);
	
	/**
	 * The Transaction-Identifier AVP (AVP code 401) is of type OctetString.
	 * This AVP contains the Bootstrapping Transaction Identifier (B-TID).
	 */
	public static final Type<String> TRANSACTION_IDENTIFIER = IMS.newIMSType(
			"Transaction-Identifier", TRANSACTION_IDENTIFIER_ORDINAL, Common.__utf8String);
	
	/**
	 * The NAF-Id AVP (AVP code 402) is of type OctetString. This AVP contains
	 * the full qualified domain name (FQDN) of the NAF that the UE uses
	 * concatenated with the Ua security protocol identifier as specified in
	 * 3GPP TS 33.220 [5]. The FQDN of the NAF that is part of the NAF_Id may be
	 * a different domain name that with which the BSF knows the NAF.
	 */
	public static final Type<String> NAF_ID = IMS.newIMSType(
			"NAF-Id", NAF_ID_ORDINAL, Common.__utf8String);

	/**
	 * The GAA-Service-identifier AVP (AVP code 403) is of type OctedString.
	 * This AVP informs a BSF about the support of a GAA-service by the NAF.
	 * According this AVP the BSF can select the right service�s user security
	 * settings. For 3GPP standardized services (e.g., PKI portal), the
	 * GAA-Service-Identifier (GSID) shall be in the range 0 to 999999, and the
	 * currently standardized values for GSID shall be the GAA Service Type Code
	 * of the particular service. The GAA Service Type Codes for 3GPP
	 * standardized services are defined in Annex B.
	 * 
	 * NOTE: In the future, standardized GSID values that are different than the
	 * GAA Service Type Code may be standardised (e.g. to differentiate between
	 * the services "MBMS streaming" and "MBMS download") and then several
	 * different GSID can exist within one GAA Service Type Code.
	 * 
	 * Examples: The GSID is "1" for all PKI-portals, and "4" for all MBMS
	 * services.
	 */
	public static final Type<String> GAA_SERVICE_IDENTIFIER = IMS.newIMSType(
			"GAA-Service-identifier", GAA_SERVICE_IDENTIFIER_ORDINAL, Common.__utf8String);
	
	/**
	 * The Key-ExpiryTime AVP (AVP code 404) is of type Time. This AVP informs
	 * the NAF about the expiry time of the key.
	 */
	public static final Type<Date> KEY_EXPIRY_TIME = IMS.newIMSType(
			"Key-ExpiryTime", KEY_EXPIRY_TIME_ORDINAL, Common.__date);
	
	/**
	 * The required ME-Key-Material AVP (AVP code 405) is of type OctetString.
	 * The NAF is sharing this key material (Ks_NAF in the case of GBA_ME or
	 * Ks_ext_NAF in the case of GBA_U) with the Mobile Equipment (ME).
	 */
	public static final Type<String> ME_KEY_MATERIAL = IMS.newIMSType(
			"ME-Key-Material", ME_KEY_MATERIAL_ORDINAL, Common.__utf8String);
	
	/**
	 * The condition UICC-Key-Material AVP (AVP code 406) is of type
	 * OctetString. The NAF may share this key material (Ks_int_NAF in the case
	 * of GBA_U) with a security element (e.g. USIM, ISIM, etc..) in the UICC.
	 * Only some GAA applications use this conditional AVP.
	 */
	public static final Type<String> UICC_KEY_MATERIAL = IMS.newIMSType(
			"UICC-Key-Material", UICC_KEY_MATERIAL_ORDINAL, Common.__utf8String);
	
	public static enum GbaUAwarenessIndicator
	{
		/**
		 * NO (0)	The sending node is not GBA_U aware
		 */
		NO,
		/**
		 * YES(1)	The sending node is GBA_U aware
		 */
		YES;
	}
	
	/**
	 * The conditional GBA_U-Awareness-Indicator AVP (AVP code 407) is of type
	 * Enumerated. The following values are defined. 
	 * 
	 * NO (0)
	 * YES(1)
	 * 
	 * The default value is 0 i.e. absence of this AVP indicates that the sending node is not
	 * GBA_U aware.
	 */
	public static final Type<GbaUAwarenessIndicator> GBA_U_AWARENESS_INDICATOR = IMS.newIMSType(
			"GBA_U-Awareness-Indicator", GBA_U_AWARENESS_INDICATOR_ORDINAL, new EnumDataFormat<GbaUAwarenessIndicator>(GbaUAwarenessIndicator.class));
	
	/**
	 * The BootstrapInfoCreationTime AVP (AVP code 408) is of type Time. This
	 * AVP informs the NAF about the bootstrapinfo creation time of the key.
	 */
	public static final Type<Date> BOOTSTRAP_INFO_CREATION_TIME = IMS.newIMSType(
			"BootstrapInfoCreationTime", BOOTSTRAP_INFO_CREATION_TIME_ORDINAL, Common.__date);
	
	/**
	 * The GUSS-Timestamp AVP (AVP code 409) is of type Time. If transmitted
	 * this AVP informs the HSS about the timestamp of the GUSS stored in the
	 * BSF.
	 */
	public static final Type<Date> GUSS_TIMESTAMP = IMS.newIMSType(
			"GUSS-Timestamp", GUSS_TIMESTAMP_ORDINAL, Common.__date);
	
	
	public enum GbaType
	{
		/**
		 * 3G GBA (0) The 3G GBA has been performed as defined in TS 33.220 [5].
		 */
		GBA_3G,
		/**
		 * 2G GBA (1) The 2G GBA has been performed as defined in TS 33.220 [5].
		 */
		GBA_2G;
	}
	
	/**
	 * The GBA-Type AVP (AVP code 410) is of type Enumerated. The AVP informs
	 * the NAF about the authentication type that was used during bootstrapping
	 * procedure. The following values are defined:
	 * 
	 * 3G GBA (0) The 3G GBA has been performed as defined in TS 33.220 [5].
	 * 2G GBA (1) The 2G GBA has been performed as defined in TS 33.220 [5].
	 * 
	 * The default value is 0 i.e. the absence of this AVP indicates 3G GBA
	 */
	public static final Type<GbaType> GBA_TYPE = IMS.newIMSType(
			"GBA-Typer", GBA_TYPE_ORDINAL, new EnumDataFormat<GbaType>(GbaType.class));
	
	/**
	 * The UE-Id AVP (AVP code 411) is of type OctedString. The AVP informs the BSF the identity of the user. 
	 */
	public static final Type<String> UE_ID = IMS.newIMSType(
			"UE-Id", UE_ID_ORDINAL, Common.__utf8String);
	
	public enum UeIdType
	{
		PRIVATE_USER_IDENTITY,
		PUBLIC_USER_IDENTITY
	}
	
	/**
	 * The UE-Id-Type AVP (AVP code 412) is of type Enumerated. The AVP informs
	 * the BSF the type of the identity of the user. The following values are
	 * defined:
	 * 
	 * � (0) Private user identity.
	 * � (1) Public user identity.
	 */
	public static final Type<UeIdType> UE_ID_TYPE = IMS.newIMSType(
			"UE-Id-Type", UE_ID_TYPE_ORDINAL, new EnumDataFormat<UeIdType>(UeIdType.class));
	
	/**
	 * The UICC-App-Label AVP (AVP code 413) is of type OctedString. The AVP
	 * informs the BSF the UICC application to be used for GBA push.
	 */
	public static final Type<String> UICC_APP_LABEL = IMS.newIMSType(
			"UICC-App-Label", UICC_APP_LABEL_ORDINAL, Common.__utf8String);
	
	public enum UiccMe
	{
		/**
		 * GBA_ME (0) GBA_ME shall be run.
		 */
		GBA_ME,
		/**
		 * GBA_U (1) GBA_U shall be run.
		 */
		GBA_U;
	}
	
	/**
	 * The UICC-ME AVP (AVP code 414) is of type Enumerated. The AVP informs the
	 * BSF the whether GBA_ME or GBA_U is to be used for GBA push. The following
	 * values are defined:
	 * 
	 * � GBA_ME (0) GBA_ME shall be run.
	 * � GBA_U (1) GBA_U shall be run.
	 */
	public static final Type<UiccMe> UICC_ME = IMS.newIMSType(
			"UICC-ME", UICC_ME_ORDINAL, new EnumDataFormat<UiccMe>(UiccMe.class));
	
	/**
	 * The Requested-Key-Lifetime AVP (AVP code 415) is of type Time. The AVP
	 * informs the BSF about the requested lifetime for the NAF keys.
	 */
	public static final Type<Date> REQUESTED_KEY_LIFETIME = IMS.newIMSType(
			"Requested-Key-Lifetime", REQUESTED_KEY_LIFETIME_ORDINAL, Common.__date);
	
	public enum PrivateIdentityRequest
	{
		PRIVATE_IDENTITY_REQUESTED,
		PRIVATE_IDENTITY_NOT_REQUESTED
	}
	
	/**
	 * The Private-Identity-Request AVP (AVP code 416) is of type Enumerated.
	 * The AVP informs the BSF if the NAF requests the private identity of the
	 * user. The following values are defined:
	 * 
	 * � Private identity requested (0) .
	 * � Private identity not requested (1)
	 */
	public static final Type<PrivateIdentityRequest> PRIVATE_IDENTITY_REQUEST = IMS.newIMSType(
			"Private-Identity-Request", PRIVATE_IDENTITY_REQUEST_ORDINAL, new EnumDataFormat<PrivateIdentityRequest>(PrivateIdentityRequest.class));
	
	/**
	 * The GBA-Push-Info AVP (AVP code 417) is of type OctetString. The AVP
	 * includes the GBA-Push-Info as defined in 3GPP TS 33.223 [23].
	 */
	public static final Type<String> GBA_PUSH_INFO = IMS.newIMSType(
			"GBA-Push-Info", GBA_PUSH_INFO_ORDINAL, Common.__utf8String);
	
	/**
	 * The NAF-SA-Identifier AVP (AVP code 418) is of type OctetString. The AVP
	 * contains the NAF-SA-Identifier (P-TID). See 3GPP TS 33.223 [23].
	 */
	public static final Type<String> NAF_SA_IDENTIFIER = IMS.newIMSType(
			"NAF-SA-Identifier", NAF_SA_IDENTIFIER_ORDINAL, Common.__utf8String);
	
}
