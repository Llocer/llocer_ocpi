package com.llocer.ev.ocpi.modules;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.llocer.common.Log;
import com.llocer.common.SimpleMap;
import com.llocer.ev.ocpi.msgs22.OcpiCredentials;
import com.llocer.ev.ocpi.msgs22.OcpiCredentialsRole;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoints;
import com.llocer.ev.ocpi.msgs22.OcpiVersions;
import com.llocer.ev.ocpi.server.OcpiAgentId;
import com.llocer.ev.ocpi.server.OcpiConfig;
import com.llocer.ev.ocpi.server.OcpiLink;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiRequestData.HttpMethod;
import com.llocer.ev.ocpi.server.OcpiResult;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public class OcpiCredentialsModule {
	private final SimpleMap< String /*own token*/, OcpiLink > linksByToken;
	private final Map< OcpiAgentId, OcpiLink > linksByDstAgent = new HashMap<OcpiAgentId, OcpiLink>();

	public OcpiCredentialsModule( SimpleMap< String /*own token*/, OcpiLink > linksByToken ) {
		this.linksByToken = linksByToken;
		for( OcpiLink link : linksByToken ) {
			putAgentIds( link );
		}
	}
	
	private void putAgentIds( OcpiLink link ) {
		if( link.peerCredentials != null && link.peerCredentials.getRoles() != null ) {
			for( OcpiCredentialsRole credentialsRole: link.peerCredentials.getRoles() ) {
				OcpiAgentId agentId = new OcpiAgentId( credentialsRole.getCountryCode(), credentialsRole.getPartyId() );
				linksByDstAgent.put( agentId, link );
			}
		}
	}
	
	public OcpiLink getLinkByAgentId( OcpiAgentId agentId ) {
		return linksByDstAgent.get( agentId );
	}

	public Collection<OcpiLink> getLinks() {
		return linksByDstAgent.values();
	}

	public void allowLink( OcpiLink link ) {
		linksByToken.put( link.ownCredentials.getToken(), link );
		putAgentIds( link );
	}
	
	public OcpiLink authorizePeer( String authorizationToken ) {
		return linksByToken.get( authorizationToken );
	}

	public OcpiResult<?> commonInterface( OcpiRequestData oreq ) throws Exception {
		switch( oreq.method ) {
		case POST: // login
			if( oreq.link.peerCredentials != null ) return OcpiResultEnum.METHOD_NOT_ALLOWED;
			break;
			
		case PUT: // update
			if( oreq.link.peerCredentials == null ) return OcpiResultEnum.METHOD_NOT_ALLOWED;
			break;
			
		case DELETE:
			if( oreq.link.peerCredentials == null ) return OcpiResultEnum.METHOD_NOT_ALLOWED;

			// logout
			if( oreq.link.peerCredentials.getRoles() != null ) {
				for( OcpiCredentialsRole credentialsRole: oreq.link.peerCredentials.getRoles() ) {
					OcpiAgentId agentId = new OcpiAgentId( credentialsRole.getCountryCode(), credentialsRole.getPartyId() );
					linksByDstAgent.remove( agentId );
				}
			}

			linksByToken.remove( oreq.link.ownCredentials.getToken() );

			oreq.link.peerCredentials = null;
			return OcpiResultEnum.OK;
			
		case GET:
			return OcpiResult.success(oreq.link.ownCredentials);
			
		default:
			return OcpiResultEnum.METHOD_NOT_ALLOWED;
		}
		
		OcpiCredentials newCredentials = OcpiRequestData.getJsonBody( oreq.request, OcpiCredentials.class );
		
		oreq.link.peerCredentials = newCredentials;
		
		// query peer versions
		OcpiVersions[] versions = oreq.link.makeBuilder()
				.uri( newCredentials.getUrl() )
				.method​( HttpMethod.GET, null )
				.send( OcpiVersions[].class );
		if( versions == null ) return OcpiResultEnum.FAILED_GET_VERSION;
		Log.debug("Ocpi221.credentials.post: version=%s--%s", versions[0].getVersion(), versions[0].getUrl() );

		// look for a supported version
		URI endpoints221Url = null;
		for( OcpiVersions v : versions ) {
			if( v.getVersion() == OcpiEndpoints.Version._2_2_1 ) {
				endpoints221Url  = v.getUrl();
				break;
			}
		}
		if( endpoints221Url == null ) return OcpiResultEnum.UNSUPPORTED_VERSION;

		// query peer endpoints
		oreq.link.peerEndpoints = oreq.link.makeBuilder()
				.uri( endpoints221Url )
				.method​( HttpMethod.GET, null )
				.send( OcpiEndpoints.class );
		if( oreq.link.peerEndpoints == null ) return OcpiResultEnum.FAILED_GET_VERSION;

		synchronized( linksByToken ) {
			String newToken;
			if( OcpiConfig.config.testing ) {
				newToken = oreq.link.ownCredentials.getToken();
			} else {
				// generate a new own token
				do {
					newToken = makeRandomToken();
				} while( linksByToken.get( newToken ) != null );
			}
			
			oreq.link.ownCredentials.setToken( newToken );

			linksByToken.remove( oreq.link.ownCredentials.getToken() );
			linksByToken.put( oreq.link.ownCredentials.getToken(), oreq.link );
			
		}
		
		putAgentIds( oreq.link );
		
		return OcpiResult.success( oreq.link.ownCredentials );
	}
	
	private static String initABC() {
		StringBuffer res = new StringBuffer();
		
		for( int i = 'A'; i<='Z'; i++ ) {
			res.append( Character.toString( i ) );
		}
		for( int i = 'a'; i<='z'; i++ ) {
			res.append( Character.toString( i ) );
		}
		for( int i = '0'; i<='9'; i++ ) {
			res.append( Character.toString( i ) );
		}
		
		return res.toString();
	}
	
	private static String abc = initABC();
	private static Random rnd = new SecureRandom();

	private static String makeRandomToken() {
		StringBuffer res = new StringBuffer();
		res.append( "Token G"); // all generated tokens start by G
		
		int v = 1+rnd.nextInt( abc.length()-1 ); // generated codes never starts by A
		res.append( abc.charAt(v) );
		
		for( int i = 1; i<62; i++ ) {
			v = rnd.nextInt( abc.length() );
			res.append( abc.charAt(v) );
		}
		
		return res.toString();
	}
}
