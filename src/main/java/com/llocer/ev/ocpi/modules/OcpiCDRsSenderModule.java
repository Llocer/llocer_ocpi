package com.llocer.ev.ocpi.modules;

import com.llocer.common.Log;
import com.llocer.ev.ocpi.msgs22.OcpiCdr;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint;
import com.llocer.ev.ocpi.server.OcpiLink;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiRequestData.HttpMethod;
import com.llocer.ev.ocpi.server.OcpiResult;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public class OcpiCDRsSenderModule {
	private final OcpiSender core; 

	public OcpiCDRsSenderModule( OcpiSender core ) {
		this.core = core;
	}

	public OcpiResult<?> senderInterface( OcpiRequestData oreq ) throws Exception {
		if( oreq.method != HttpMethod.GET ) return OcpiResultEnum.METHOD_NOT_ALLOWED;
		if( oreq.args.length != 0 ) return OcpiResultEnum.INVALID_SYNTAX;

		// list
		return OcpiSender.paginationServer( this.core, oreq ); 
	}
	
	public void reportChange( OcpiLink link, OcpiCdr cdr ) {
		try {
			link.makeBuilder()
			.uri( OcpiEndpoint.Identifier.CDRS )
			.method​( HttpMethod.POST, cdr )
			.send();
		} catch ( Exception e ) {
			Log.warning( "error sending to eMSP" );
			// ignore
		}
	}
}
