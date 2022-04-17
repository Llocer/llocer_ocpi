package com.llocer.ev.ocpi.server;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OcpiAgentId {
	public final String countryCode;
	public final String partyId;

	@JsonCreator
	public OcpiAgentId( 
			@JsonProperty("countryCode") String countryCode, 
			@JsonProperty("partyId") String partyId ) {
		this.countryCode = countryCode;
		this.partyId = partyId;
	}

	@Override
	public int hashCode() {
		return Objects.hash( countryCode, partyId );
	}

	@Override
	public boolean equals( Object that ) {
		if( that == this) return true;

		if( !(that instanceof OcpiAgentId) ) return false;
		OcpiAgentId tthat = (OcpiAgentId) that;

		return Objects.equals( countryCode, tthat.countryCode ) 
				&& Objects.equals( partyId, tthat.partyId );
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append( this.countryCode );
		res.append( "::" );
		res.append( this.partyId );
		return res.toString();
	}
}


