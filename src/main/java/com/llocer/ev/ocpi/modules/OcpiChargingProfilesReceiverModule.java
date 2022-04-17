package com.llocer.ev.ocpi.modules;

import java.net.URI;
import java.net.URISyntaxException;

import com.llocer.ev.ocpi.msgs22.OcpiChargingProfile;
import com.llocer.ev.ocpi.msgs22.OcpiSetChargingProfile;
import com.llocer.ev.ocpi.server.OcpiAgentId;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiResult;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public class OcpiChargingProfilesReceiverModule {
	public static interface OcpiChargingProfilesReceiver {
		
		OcpiResult<?> handleSetChargingProfile(
				OcpiAgentId SCSP, URI responseUrl, 
				String ocpiSessionId,
				OcpiChargingProfile chargingProfile );

		OcpiResult<?> handleDeleteChargingProfile(
				OcpiAgentId SCSP, URI responseUrl, 
				String ocpiSessionId);

		OcpiResult<?> handleGetChargingProfile(
				OcpiAgentId SCSP, URI responseUrl, 
				String ocpiSessionId, Integer duration);
	}
	
	private final OcpiChargingProfilesReceiver core;

	public OcpiChargingProfilesReceiverModule( OcpiChargingProfilesReceiver core ) {
		this.core = core;
	}

	public OcpiResult<?> receiverInterface( OcpiRequestData oreq ) throws Exception {

		if( oreq.args.length < 1 ) return OcpiResultEnum.INVALID_SYNTAX;
		
		switch( oreq.method ) {
		case GET: {
			// .../{session_id}?duration={duration}&response_url={url}
			
			String response_url_s = oreq.request.getParameter("response_url");
			String duration_s = oreq.request.getParameter("duration");
			
			URI response_url;
			try {
				response_url = new URI(response_url_s);
			} catch (URISyntaxException e) {
				return OcpiResultEnum.INVALID_SYNTAX;
			}
			Integer duration = null;
			try {
				duration = Integer.valueOf( duration_s );
			} catch ( NumberFormatException e) {
				return OcpiResultEnum.INVALID_SYNTAX;
			}
			
			return core.handleGetChargingProfile( oreq.from, response_url, oreq.args[0], duration );
		}
		
		case PUT: {
			// .../{session_id}
			OcpiSetChargingProfile setChargingProfile = OcpiRequestData.getJsonBody( oreq.request, OcpiSetChargingProfile.class );
			return core.handleSetChargingProfile( oreq.from, setChargingProfile.getResponseUrl(), oreq.args[0], setChargingProfile.getChargingProfile() );
		}

		case DELETE: {
			// .../{session_id}?response_url={url}
			String response_url_s = oreq.request.getParameter("response_url");
			
			URI response_url;
			try {
				response_url = new URI(response_url_s);
			} catch (URISyntaxException e) {
				return OcpiResultEnum.INVALID_SYNTAX;
			}
			
			return core.handleDeleteChargingProfile( oreq.from, response_url, oreq.args[0] );
		}


		default:
			return OcpiResultEnum.METHOD_NOT_ALLOWED;

		}
	}
}
