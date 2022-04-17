package com.llocer.ev.ocpi.modules;

import com.llocer.ev.ocpi.msgs22.CommandType;
import com.llocer.ev.ocpi.msgs22.OcpiCancelReservation;
import com.llocer.ev.ocpi.msgs22.OcpiCommandResponse;
import com.llocer.ev.ocpi.msgs22.OcpiReserveNow;
import com.llocer.ev.ocpi.msgs22.OcpiStartSession;
import com.llocer.ev.ocpi.msgs22.OcpiStopSession;
import com.llocer.ev.ocpi.msgs22.OcpiUnlockConnector;
import com.llocer.ev.ocpi.server.OcpiAgentId;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiResult;
import com.llocer.ev.ocpi.server.OcpiRequestData.HttpMethod;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public class OcpiCommandsReceiverModule {
//	final SimpleMap< String /*reservationId*/, OcpiReserveNow > reservations;

	public static interface OcpiCommandsReceiver {
		OcpiCommandResponse handleReserveNowCommand(OcpiAgentId eMSP, OcpiReserveNow command);
		OcpiCommandResponse handleCancelReservationCommand(OcpiAgentId eMSP, OcpiCancelReservation command);
		OcpiCommandResponse handleStartSessionCommand(OcpiAgentId eMSP, OcpiStartSession command);
		OcpiCommandResponse handleStopSessionCommand(OcpiAgentId eMSP, OcpiStopSession command);
		OcpiCommandResponse handleUnlockConnectorCommand(OcpiAgentId eMSP, OcpiUnlockConnector command);
	}

	private final OcpiCommandsReceiver core;
	
	public OcpiCommandsReceiverModule( String id, OcpiCommandsReceiver core ) {
		this.core = core;
//		reservations = Config.getMapFactory().make( id, OcpiReserveNow.class );

	}

	public OcpiResult<?> receiverInterface( OcpiRequestData oreq ) throws Exception {

		if( oreq.method != HttpMethod.POST ) return OcpiResultEnum.METHOD_NOT_ALLOWED;

		// .../{command}

		if( oreq.args.length < 1 ) return OcpiResultEnum.METHOD_NOT_ALLOWED;

		CommandType commandType;
		try { 
			commandType = CommandType.valueOf( oreq.args[0].toUpperCase() ); 
		} catch ( Exception exc ) {
			return OcpiResult.success( notSupported() );
		}

		
		switch( commandType ) {
		case RESERVE_NOW: {
			OcpiReserveNow command = OcpiRequestData.getJsonBody( oreq.request, OcpiReserveNow.class );

			OcpiCommandResponse response = this.core.handleReserveNowCommand( oreq.from, command );
			return OcpiResult.success( response );
		}
		
		case CANCEL_RESERVATION: {
			OcpiCancelReservation command = OcpiRequestData.getJsonBody( oreq.request, OcpiCancelReservation.class );

			OcpiCommandResponse response = this.core.handleCancelReservationCommand( oreq.from, command );
			return OcpiResult.success( response );
		}
		
		case START_SESSION: {
			OcpiStartSession command = OcpiRequestData.getJsonBody( oreq.request, OcpiStartSession.class );

			OcpiCommandResponse response = this.core.handleStartSessionCommand( oreq.from, command );
			return OcpiResult.success( response );
		}
		
		case STOP_SESSION: {
			OcpiStopSession command = OcpiRequestData.getJsonBody( oreq.request, OcpiStopSession.class );

			OcpiCommandResponse response = this.core.handleStopSessionCommand( oreq.from, command );
			return OcpiResult.success( response );
		}
		
		case UNLOCK_CONNECTOR: {
			OcpiUnlockConnector command = OcpiRequestData.getJsonBody( oreq.request, OcpiUnlockConnector.class );

			OcpiCommandResponse response = this.core.handleUnlockConnectorCommand( oreq.from, command );
			return OcpiResult.success( response );
		} }
		
		return OcpiResult.success( notSupported() );
	}
	
	private OcpiCommandResponse notSupported() {
		OcpiCommandResponse response = new OcpiCommandResponse();
		response.setResult( OcpiCommandResponse.Result.NOT_SUPPORTED );
		response.setTimeout( 0 );
		return response;
	}
	
}
