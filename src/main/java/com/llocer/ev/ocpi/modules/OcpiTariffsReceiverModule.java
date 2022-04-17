package com.llocer.ev.ocpi.modules;

import java.time.Instant;

import com.llocer.ev.ocpi.msgs22.OcpiTariff;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiResult;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public class OcpiTariffsReceiverModule {
	public static interface OcpiTariffsReceiver {
		OcpiTariff getTariff( String tariffId );
		void updateTariff( OcpiTariff location, OcpiTariff delta );
		void removeTariff( String tariffId );
	}

	private final OcpiTariffsReceiver core;

	public OcpiTariffsReceiverModule( OcpiTariffsReceiver core ) {
		this.core = core;
	}

	public OcpiResult<?> receiverInterface( OcpiRequestData oreq ) throws Exception {

		// .../{country_code}/{party_id}/{tariff_id}

		OcpiTariff tariff = this.core.getTariff( oreq.args[2] );

		switch( oreq.method ) {
		case GET: {
			if( tariff == null ) return OcpiResultEnum.UNKNOWN_ITEM;
			return OcpiResult.success( tariff );
		}

		case PUT: {
			OcpiTariff newTariffData = OcpiRequestData.getJsonBody( oreq.request, OcpiTariff.class );
			if( newTariffData.getLastUpdated() == null ) {
				newTariffData.setLastUpdated( Instant.now() );
			}

			if( tariff == null ) {
				tariff = newTariffData;
				
			} else {
				Instant oldLastUpdated = tariff.getLastUpdated();
				
				OcpiReceiver.mergeObjects( tariff, newTariffData );
				
				if( oldLastUpdated != null && oldLastUpdated.isAfter( tariff.getLastUpdated() )) {
					tariff.setLastUpdated( oldLastUpdated );
				}
				
			}

			this.core.updateTariff( tariff, newTariffData );
			
			return tariff == null ? OcpiResultEnum.CREATED : OcpiResultEnum.OK;
		}

		case DELETE: {
			if( tariff == null ) return OcpiResultEnum.UNKNOWN_ITEM;

			this.core.removeTariff( oreq.args[2] );
			return OcpiResultEnum.OK;
		}

		default:
			return OcpiResultEnum.METHOD_NOT_ALLOWED;

		}
	}
}
