package com.llocer.ev.ocpi.modules;

import java.time.Instant;
import java.util.Collection;

import com.llocer.ev.ocpi.msgs22.OcpiToken;
import com.llocer.ev.ocpi.msgs22.OcpiToken.TokenType;
import com.llocer.ev.ocpi.server.OcpiLink;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiResult;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public class OcpiTokensReceiverModule {
	public static interface OcpiTokensReceiver {
		OcpiToken getToken( TokenType tokenType, String tokenId );
		void updateToken( OcpiToken token );
		void removeToken( TokenType tokenType, String tokenId );
	}

	private final OcpiTokensReceiver core;

	public OcpiTokensReceiverModule( OcpiTokensReceiver core ) {
		this.core = core;
	}
	
	public void queryTokens( Collection<OcpiLink> links, Instant reloadStart ) {
		OcpiReceiver.paginationClient( links, reloadStart, OcpiToken.class, this::storeToken );
	}
	
	
	public OcpiResult<?> receiverInterface( OcpiRequestData oreq ) throws Exception {

		// .../{country_code}/{party_id}/{token_uid}[?type={type}]

		String tokenType_s = oreq.request.getParameter("type");
		TokenType tokenType;
		if( tokenType_s == null ) {
			tokenType = TokenType.RFID;
		} else {
			try {
				tokenType = TokenType.valueOf(tokenType_s);
			} catch( Exception e ) {
				return OcpiResultEnum.INVALID_SYNTAX;
			}
		}
				
				
		OcpiToken token = this.core.getToken( tokenType, oreq.args[2] );

		switch( oreq.method ) {
		case PUT: {
			OcpiToken tokenData = OcpiRequestData.getJsonBody( oreq.request, OcpiToken.class );
			updateToken( tokenType, token, tokenData );
			return token == null ? OcpiResultEnum.CREATED : OcpiResultEnum.OK;
		}

		case PATCH: {
			if( token == null ) return OcpiResultEnum.UNKNOWN_ITEM;

			OcpiToken tokenData = OcpiRequestData.getJsonBody( oreq.request, OcpiToken.class );
			updateToken( tokenType, token, tokenData );
			return OcpiResultEnum.OK;
		}

		case GET: {
			if( token == null ) return OcpiResultEnum.UNKNOWN_ITEM;
			return OcpiResult.success( token );
		}

		default:
			return OcpiResultEnum.METHOD_NOT_ALLOWED;

		}
	}

	private void updateToken( TokenType tokenType, OcpiToken token, OcpiToken newTokenData ) throws Exception {
		
		if( token == null ) {
			token = newTokenData;

		} else {
			Instant oldLastUpdated = token.getLastUpdated();

			OcpiReceiver.mergeObjects( token, newTokenData );

			if( oldLastUpdated.isAfter( token.getLastUpdated() )) {
				token.setLastUpdated( oldLastUpdated );
			}

		}
		
		storeToken( token );
	}
	
	private void storeToken( OcpiToken token ) {
		
		if( token.getLastUpdated() == null ) {
			token.setLastUpdated( Instant.now() );
		}

		if( token.getValid() ) {
			this.core.updateToken( token );
		} else {
			this.core.removeToken( token.getType(), token.getUid() );
		}
	}
}
