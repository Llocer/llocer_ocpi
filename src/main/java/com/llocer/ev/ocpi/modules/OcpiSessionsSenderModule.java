package com.llocer.ev.ocpi.modules;

import com.llocer.common.Log;
import com.llocer.ev.ocpi.msgs22.OcpiChargingPreferences;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint;
import com.llocer.ev.ocpi.msgs22.OcpiSession;
import com.llocer.ev.ocpi.server.OcpiLink;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiRequestData.HttpMethod;
import com.llocer.ev.ocpi.server.OcpiResult;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;


//session.basic fields
//this.session.setCountryCode( cpo.getId().countryCode );
//this.session.setPartyId( cpo.getId().partyId );
//this.session.setId( UUID.randomUUID().toString() );

////session.location fields:
//this.session.setLocationId(null);
//this.session.setEvseUid(null);
//this.session.setConnectorId(null);
//this.session.setMeterId(null); // optional
//		
////session.charging fields:
//this.session.setCdrToken( new OcpiCdrToken() );
//this.session.setKwh(null);
//this.session.setCurrency(null);
//this.session.setTotalCost(null); // optional
//this.session.setChargingPeriods(null); // 0 or more
//
////session.state fields:
//this.session.setAuthMethod(null);
//this.session.setAuthReference(null); // optional
//this.session.setStartDatetime( Instant.now() );
//this.session.setEndDatetime(null); // optional
//this.session.setStatus(null);
//this.session.setLastUpdated(null);

public class OcpiSessionsSenderModule {
	public static interface OcpiSessionsSender extends OcpiSender {
		OcpiResult<?> setChargingPreferences( String sessionId, OcpiChargingPreferences chargingPreferences );
	}
	
	private final OcpiSessionsSender core;

	public OcpiSessionsSenderModule( OcpiSessionsSender core ) {
		this.core = core;
	}
	
	public OcpiResult<?> senderInterface( OcpiRequestData oreq ) throws Exception {
		if( oreq.args.length == 0 ) {
			// list
			if( oreq.method != HttpMethod.GET ) return OcpiResultEnum.METHOD_NOT_ALLOWED;
			
			return OcpiSender.paginationServer( this.core, oreq ); 
		}

		// PUT .../{session_id}/charging_preferences
		if( oreq.method != HttpMethod.PUT ) return OcpiResultEnum.METHOD_NOT_ALLOWED;
		if( oreq.args.length != 2 ) return OcpiResultEnum.INVALID_SYNTAX;
		if( !"charging_preferences".equalsIgnoreCase( oreq.args[1] ) ) return OcpiResultEnum.INVALID_SYNTAX;

		OcpiChargingPreferences chargingPreferences = OcpiRequestData.getJsonBody( oreq.request, OcpiChargingPreferences.class );
		
		return this.core.setChargingPreferences( oreq.args[0], chargingPreferences );
	}

	public void reportSessionChange( OcpiLink link, OcpiSession sessionData ) {
		try {
			link.makeBuilder()
			.uri( OcpiEndpoint.Identifier.SESSIONS )
			.parameter( sessionData.getCountryCode() )
			.parameter( sessionData.getPartyId() )
			.parameter( sessionData.getId() )
			.method​( HttpMethod.PUT, sessionData )
			.send();
			
		} catch ( Exception e ) {
			Log.warning( "error sending to eMSP" );
			// ignore
			
		}
	}
}
