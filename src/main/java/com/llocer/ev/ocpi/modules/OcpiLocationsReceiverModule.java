package com.llocer.ev.ocpi.modules;

import java.time.Instant;
import java.util.List;
import java.util.Vector;

import com.llocer.common.Tuple2;
import com.llocer.ev.ocpi.msgs22.OcpiConnector;
import com.llocer.ev.ocpi.msgs22.OcpiEvse;
import com.llocer.ev.ocpi.msgs22.OcpiLocation;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiResult;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public class OcpiLocationsReceiverModule {
	public static interface OcpiLocationsReceiver {
		OcpiLocation getOcpiLocation( String id );
		void updateLocation( OcpiLocation location, OcpiLocation delta );
		void updateEvse( OcpiLocation location, OcpiEvse evse, OcpiEvse delta );
		void updateConnector( OcpiLocation location, OcpiEvse evse, OcpiConnector connector, OcpiConnector delta );
	}

	private final OcpiLocationsReceiver core;
	
	public OcpiLocationsReceiverModule( OcpiLocationsReceiver core ) {
		this.core = core;
	}

	public OcpiResult<?> receiverInterface( OcpiRequestData oreq ) throws Exception {

		OcpiLocation location = this.core.getOcpiLocation( oreq.args[2] );
		
		switch( oreq.args.length ) {
		case 3: { // .../{country_code}/{party_id}/{location_id}
			switch( oreq.method ) {
			case PUT: {
				OcpiLocation locationData = OcpiRequestData.getJsonBody( oreq.request, OcpiLocation.class );
				updateLocation( location, locationData );
				return location == null ? OcpiResultEnum.CREATED : OcpiResultEnum.OK;
			}

			case PATCH: {
				if( location == null ) return OcpiResultEnum.UNKNOWN_LOCATION;

				OcpiLocation locationData = OcpiRequestData.getJsonBody( oreq.request, OcpiLocation.class );
				updateLocation( location, locationData );
				return OcpiResultEnum.OK;
			}

			case GET: {
				if( location == null ) return OcpiResultEnum.UNKNOWN_LOCATION;
				return OcpiResult.success( location );
			}

			default:
				return OcpiResultEnum.METHOD_NOT_ALLOWED;

			}
		} 

		case 4:  { // .../{country_code}/{party_id}/{location_id}/{evse_uid}
			if( location == null ) return OcpiResultEnum.UNKNOWN_LOCATION;
			OcpiEvse evse = getEvseByOcpiId( location, oreq.args[3] );
			
			switch( oreq.method ) {
			case PUT: {
				OcpiEvse evseData = OcpiRequestData.getJsonBody( oreq.request, OcpiEvse.class );
				updateEvse( location, evse, evseData );
				return ( evse == null ? OcpiResultEnum.CREATED : OcpiResultEnum.OK );
			}

			case PATCH: {
				if( evse == null ) return OcpiResultEnum.UNKNOWN_ITEM;

				OcpiEvse evseData = OcpiRequestData.getJsonBody( oreq.request, OcpiEvse.class );
				updateEvse( location, evse, evseData );
				return OcpiResultEnum.OK;
			}

			case GET: {
				if( evse == null ) return OcpiResultEnum.UNKNOWN_ITEM;
				return OcpiResult.success( evse );
			}

			default:
				return OcpiResultEnum.METHOD_NOT_ALLOWED;

			}
		}

		case 5: { // /{country_code}/{party_id}/{location_id}/{evse_uid}/{connector_id}
			if( location == null ) return OcpiResultEnum.UNKNOWN_LOCATION;

			OcpiEvse evse = getEvseByOcpiId( location, oreq.args[3] );
			if( evse == null ) return OcpiResultEnum.UNKNOWN_ITEM;
			Tuple2<OcpiConnector, Integer> tConnector = getConnectorByOcpiId( evse, oreq.args[4] );
			
			switch( oreq.method ) {
			case PUT: {
				OcpiConnector connectorData = OcpiRequestData.getJsonBody( oreq.request, OcpiConnector.class );
				updateConnector( location, evse, tConnector.f1, connectorData );
				return tConnector == null ? OcpiResultEnum.CREATED : OcpiResultEnum.OK;
			}

			case PATCH: {
				if( tConnector == null ) return OcpiResultEnum.UNKNOWN_ITEM;
				OcpiConnector connectorData = OcpiRequestData.getJsonBody( oreq.request, OcpiConnector.class );
				updateConnector( location, evse, tConnector.f1, connectorData );
				return OcpiResultEnum.OK;
			}

			case GET: {
				if( tConnector == null ) return OcpiResultEnum.UNKNOWN_ITEM;
				return OcpiResult.success( tConnector.f1 );
			}

			default:
				return OcpiResultEnum.METHOD_NOT_ALLOWED;

			}
		}

		default:
			return OcpiResultEnum.INVALID_SYNTAX;
		}
	}


	/************************************************************************************
	 * location
	 * @return 
	 */

	public void updateLocation( OcpiLocation location, OcpiLocation newLocationData ) throws Exception {
		if( newLocationData.getLastUpdated() == null ) {
			newLocationData.setLastUpdated( Instant.now() );
		}
		
		if( location == null ) {
			location = newLocationData;
			
		} else {
			Instant oldLastUpdated = location.getLastUpdated();
			
			OcpiReceiver.mergeObjects( location, newLocationData );
			
			if( oldLastUpdated != null && oldLastUpdated.isAfter( location.getLastUpdated() )) {
				location.setLastUpdated( oldLastUpdated );
			}
			
		}
		
		this.core.updateLocation( location, newLocationData );
	}
	
	/************************************************************************************
	 * evse
	 */

	public void updateEvse( OcpiLocation location, OcpiEvse evse, OcpiEvse newEvseData ) {
		
		if( newEvseData.getLastUpdated() == null ) {
			newEvseData.setLastUpdated( Instant.now() );
		}
		
		if( evse == null ) {
			evse = newEvseData;
			if( location.getEvses() == null ) {
				location.setEvses( new Vector<OcpiEvse>() );
			}
			location.getEvses().add( evse );
			
		} else {
			Instant oldLastUpdated = evse.getLastUpdated();
			
			OcpiReceiver.mergeObjects( evse, newEvseData );
			
			if( oldLastUpdated.isAfter( evse.getLastUpdated() )) {
				evse.setLastUpdated( oldLastUpdated );
			}
			
		}
		
		if( evse.getLastUpdated().isAfter( location.getLastUpdated()) ) {
			location.setLastUpdated( evse.getLastUpdated() );
		}
		
		this.core.updateEvse(location, evse, newEvseData);
	}

	public static OcpiEvse getEvseByOcpiId( OcpiLocation location, String evseId ) {
		if( location.getEvses() == null ) return null;

		for( OcpiEvse evse : location.getEvses() ) {;
			if( evse.getEvseId().equalsIgnoreCase(evseId) ) return evse;
		}
		return null;
	}


	/************************************************************************************
	 * connector
	 */

	public static Tuple2<OcpiConnector,Integer> getConnectorByOcpiId( OcpiEvse evse, String connectorId ) {
		if( evse.getConnectors() == null ) return null;

		List<OcpiConnector> connectors = evse.getConnectors();
		for( int idx = 0; idx<connectors.size(); idx++ ) {
			OcpiConnector connector = connectors.get(idx);
			if( connector.getId().equalsIgnoreCase(connectorId) ) {
				return new Tuple2<OcpiConnector,Integer>( connector, idx ); 
			}
		}
		return null;
	}

	public static OcpiConnector getConnectorByOcppIdx( OcpiEvse evse, int idx1 ) {
		// in OCPP, index starts by 1
		if( evse.getConnectors() == null ) return null;

		return evse.getConnectors().get(idx1-1);
	}
	
	public void updateConnector( OcpiLocation location, OcpiEvse evse, OcpiConnector connector, OcpiConnector newConnectorData ) throws Exception {
		if( newConnectorData.getLastUpdated() == null ) {
			newConnectorData.setLastUpdated( Instant.now() );
		}
		
		if( connector == null ) {
			connector = newConnectorData;
			if( evse.getConnectors() == null ) {
				evse.setConnectors( new Vector<OcpiConnector>() );
			}
			evse.getConnectors().add( connector );
			
		} else {
			Instant oldLastUpdated = connector.getLastUpdated();
			
			OcpiReceiver.mergeObjects( connector, newConnectorData );
			
			if( oldLastUpdated.isAfter( connector.getLastUpdated() )) {
				connector.setLastUpdated( oldLastUpdated );
			}
		}
		
		if( connector.getLastUpdated().isAfter( evse.getLastUpdated() ) ) {
			evse.setLastUpdated( connector.getLastUpdated() );

			if( evse.getLastUpdated().isAfter( location.getLastUpdated()) ) {
				location.setLastUpdated( evse.getLastUpdated() );
			}
		}

		this.core.updateConnector(location, evse, connector, newConnectorData);
	}
}
