package com.llocer.ev.ocpi.modules;

import java.util.LinkedList;
import java.util.List;

import com.llocer.ev.ocpi.server.OcpiAgentId;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiRequestData.HttpMethod;
import com.llocer.ev.ocpi.server.OcpiResult;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public class OcpiTariffsSenderModule {
	private final  OcpiSender core;
	private final List<OcpiAgentId> receivers = new LinkedList<OcpiAgentId>();

	public OcpiTariffsSenderModule( OcpiSender core ) {
		this.core = core;
	}

	public void addReceiver( OcpiAgentId eMSPAddress ) {
		this.receivers.add( eMSPAddress );
	}

	public OcpiResult<?> senderInterface( OcpiRequestData oreq ) throws Exception {
		if( oreq.method != HttpMethod.GET ) return OcpiResultEnum.METHOD_NOT_ALLOWED;
		if( oreq.args.length != 0 ) return OcpiResultEnum.INVALID_SYNTAX;

		// list
		return OcpiSender.paginationServer( this.core, oreq ); 
	}
}
