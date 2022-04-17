package com.llocer.ev.ocpi.server;

import com.llocer.ev.ocpi.msgs22.OcpiCredentials;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoints;

public class OcpiLink {
	public OcpiAgentId ownId;
	public OcpiAgentId peerId;
	public OcpiCredentials ownCredentials = null;
	public OcpiEndpoints peerEndpoints = null;
	public OcpiCredentials peerCredentials = null;
	
	public OcpiRequestBuilder makeBuilder() {
		return new OcpiRequestBuilder( this );
	}
}
