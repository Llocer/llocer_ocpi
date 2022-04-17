package com.llocer.ev.ocpi.modules;

import java.util.LinkedList;
import java.util.List;

import com.llocer.common.Log;
import com.llocer.common.Tuple2;
import com.llocer.ev.ocpi.msgs22.OcpiConnector;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint;
import com.llocer.ev.ocpi.msgs22.OcpiEvse;
import com.llocer.ev.ocpi.msgs22.OcpiLocation;
import com.llocer.ev.ocpi.server.OcpiLink;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiRequestData.HttpMethod;
import com.llocer.ev.ocpi.server.OcpiResult;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public class OcpiLocationsSenderModule {
	public static interface OcpiLocationsSender extends OcpiSender {
		OcpiLocation getOcpiLocation( String locationId );
	}

	private final OcpiLocationsSender core;
	private final List<OcpiLink> receivers;

	public OcpiLocationsSenderModule( OcpiLocationsSender core ) {
		this.core = core;
		this.receivers = new LinkedList<OcpiLink>();
	}

	public void addReceiver( OcpiLink link ) {
		this.receivers.add( link );
	}

	public OcpiResult<?> senderInterface( OcpiRequestData oreq ) throws Exception {
		if( oreq.method != HttpMethod.GET ) return OcpiResultEnum.METHOD_NOT_ALLOWED;

		if( oreq.args.length == 0 ) {
			// list
			return OcpiSender.paginationServer( this.core, oreq ); 
		}

		OcpiLocation location = this.core.getOcpiLocation( oreq.args[0] );
		if( location == null ) return OcpiResultEnum.UNKNOWN_LOCATION;

		if( oreq.args.length == 1 ) {
			return OcpiResult.success( location );
		}

		OcpiEvse evse = OcpiLocationsReceiverModule.getEvseByOcpiId( location, oreq.args[1] ); 
		if( evse == null ) return OcpiResultEnum.UNKNOWN_ITEM;

		if( oreq.args.length == 2 ) {
			return OcpiResult.success( evse );
		}

		Tuple2<OcpiConnector, Integer> tConnector = OcpiLocationsReceiverModule.getConnectorByOcpiId( evse, oreq.args[2] );
		if( tConnector == null ) return OcpiResultEnum.UNKNOWN_ITEM;

		return OcpiResult.success( tConnector.f1 );
	}

	
	public void reportLocationChange( OcpiLocation locationData ) throws Exception {
		// report to eMSP
		for( OcpiLink link : this.receivers ) {
			try {
				link.makeBuilder()
				.uri( OcpiEndpoint.Identifier.LOCATIONS )
				.parameter( link.ownId.countryCode )
				.parameter( link.ownId.partyId )
				.parameter( locationData.getId() )
				.method​( HttpMethod.PUT, locationData )
				.send();
			} catch ( Exception e ) {
				Log.warning( "error sending to eMSP" );
				// ignore
			}
		}
	}

	public void reportEvseChange( String locationId, OcpiEvse evse ) {
		// report to eMSP
		for( OcpiLink link : this.receivers ) {
			try {
				link.makeBuilder()
				.uri( OcpiEndpoint.Identifier.LOCATIONS ) 
				.parameter( link.ownId.countryCode )
				.parameter( link.ownId.partyId )
				.parameter( locationId )
				.parameter( evse.getEvseId() )
				.method​( HttpMethod.PUT, evse )
				.send();
			} catch ( Exception e ) {
				Log.warning( "error sending to eMSP" );
				// ignore
			}
		}
	}

	public void reportConnectorChange( String locationId, String evseId, OcpiConnector connector ) throws Exception {
		// report to eMSP
		for( OcpiLink link : this.receivers ) {
			try {
				link.makeBuilder()
				.uri( OcpiEndpoint.Identifier.LOCATIONS )
				.parameter( link.ownId.countryCode )
				.parameter( link.ownId.partyId )
				.parameter( locationId )
				.parameter( evseId )
				.parameter( connector.getId() )
				.method​( HttpMethod.PUT, connector )
				.send();
			} catch ( Exception e ) {
				Log.warning( "error sending to eMSP" );
				// ignore
			}
		}
	}
}
