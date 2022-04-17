package com.llocer.ev.ocpi.server;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.llocer.common.Log;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint.Identifier;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoints;
import com.llocer.ev.ocpi.msgs22.OcpiVersions;
import com.llocer.ev.ocpi.server.OcpiRequestData.HttpMethod;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiException;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public abstract class OcpiServlet extends HttpServlet {
	abstract protected OcpiVersions[] getVersions();
	abstract protected OcpiEndpoints getEndpoints( String version );
	abstract protected OcpiLink authorizePeer( String authorization );
	abstract protected OcpiResult<?> executeCredentials(OcpiRequestData oreq) throws Exception;
	abstract protected OcpiResult<?> execute( OcpiRequestData oreq ) throws Exception;

	
	private static final long serialVersionUID = 452014230033312875L;

	public static final String HeaderRequestId = "X-Request-ID";
	public static final String HeaderCorrelationId = "X-Correlation-ID";
	public static final String HeaderToCountryCode = "OCPI-to-country-code";
	public static final String HeaderToPartyId = "OCPI-to-party-id";
	public static final String HeaderFromCountryCode = "OCPI-from-country-code";
	public static final String HeaderFromPartyId = "OCPI-from-party-id";

	@Override
	protected void service( HttpServletRequest request, HttpServletResponse response )
	        throws ServletException, IOException {
	    try {
			Log.debug( "OcpiServlet.service: %s %s (auth=%s)", request.getMethod(), request.getRequestURL(), request.getHeader("Authorization") );
			executeRequest( request, response );
			
		} catch( Exception exc ) {
			Log.error(exc);
			
			try {
				response.sendError( 500 );
				
			} catch( IOException exc2 ) {
				Log.error(exc2);
				
			}
		}
	}

	private void executeRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {

		OcpiRequestData oreq = new OcpiRequestData();
		oreq.servlet = this;
		oreq.request = request;
		oreq.response = response;

		OcpiResult<?> result;
		try {
			result = executeRequest( oreq );
			
		} catch( OcpiException e ) {
			result = e.result;

		}
		
		if( result == null ) {
			response.sendError( 500 );
			return;
		}
		
		response.setStatus( result.getHttpStatusCode() );
		
		response.setContentType( "application/json" );
		
		OcpiAnswer<?> answer = result.getAnswer();
		
		if( answer.status_code != null && answer.timestamp == null ) {
			answer.timestamp = Instant.now();
		}
		
		String responseBody = OcpiRequestData.mapper.writeValueAsString(answer);
		Log.debug( "OcpiServlet.handleRequest: response.body=%s", responseBody );
		response.getWriter().append( responseBody );			
	}

	protected OcpiResult<?> executeRequest( OcpiRequestData oreq ) throws Exception {
		try { 
			oreq.method = HttpMethod.valueOf( oreq.request.getMethod() ); 
		} catch ( Exception exc ) {
			return OcpiResultEnum.METHOD_NOT_ALLOWED;
		}

		// specification:
		// "If the header is missing or the credentials token doesn’t match any known party 
		// then the server SHALL respond with an HTTP 401- Unauthorized status code."

		String authorization = oreq.request.getHeader("Authorization");
		if( authorization == null ) return OcpiResultEnum.MISSING_AUTHORIZATION;

		oreq.link = authorizePeer( authorization );
		if( oreq.link == null ) return OcpiResultEnum.UNKNOWN_AUTHORIZATION;

		/*
		 *  Credentials & Versions
		 *  
		 *  specification:
		 *  Credentials, Versions and Hub Client Info [...] route headers SHALL NOT be used with these modules.
		 */
		
		String uri_s = oreq.request.getRequestURI();
		String uri[] = uri_s.split("/"); 
		
		int uriIdx = ( uri.length > 0 && uri[0].isEmpty() ? 1 : 0 ); // /... : first empty element
		uriIdx += OcpiConfig.config.privateUriLength;

		if( uriIdx == uri.length ) {
			// .../ : get versions
			return( oreq.method == HttpMethod.GET ? OcpiResult.success(getVersions()) : OcpiResultEnum.METHOD_NOT_ALLOWED );
		} 
		
		if ( uriIdx+1 == uri.length ) {
			// .../221/
			if( oreq.method != HttpMethod.GET ) return OcpiResultEnum.METHOD_NOT_ALLOWED;
			return OcpiResult.success( getEndpoints( uri[uriIdx] ));
		} 
		
		uriIdx++;
		if( uriIdx >= uri.length ) return OcpiResultEnum.INVALID_SYNTAX;
		
		// .../221/{module}/...
		
		try { 
			oreq.module = OcpiEndpoint.Identifier.valueOf( uri[uriIdx].toUpperCase() ); 
			uriIdx++;

		} catch ( Exception exc ) {
			return OcpiResultEnum.NOT_SUPPORTED_ENDPOINT;

		}

		oreq.args = new String[ uri.length-uriIdx ];
		for( int i = 0; i<oreq.args.length; i++ ) {
			oreq.args[i] = URLDecoder.decode( uri[i+uriIdx], StandardCharsets.UTF_8 );
		}
		
		if( oreq.module == Identifier.CREDENTIALS ) {
			return executeCredentials( oreq );
		}
		
		return executeModule( oreq );
		
	}
		
	protected OcpiResult<?> executeModule( OcpiRequestData oreq ) throws Exception {
		
		String tmp = oreq.request.getHeader( HeaderRequestId );
		if( tmp != null ) oreq.response.addHeader( HeaderRequestId, tmp );
		
		tmp = oreq.request.getHeader( HeaderCorrelationId );
		if( tmp != null ) oreq.response.addHeader( HeaderCorrelationId, tmp );

		String fromCountryCode = oreq.request.getHeader( HeaderFromCountryCode );
		if( fromCountryCode != null ) oreq.response.addHeader( HeaderToCountryCode, fromCountryCode );
		
		String fromPartyId = oreq.request.getHeader( HeaderFromPartyId );
		if( fromPartyId != null ) oreq.response.addHeader( HeaderToPartyId, fromPartyId );
		
		String toCountryCode = oreq.request.getHeader( HeaderToCountryCode );
		if( toCountryCode != null ) oreq.response.addHeader( HeaderFromCountryCode, toCountryCode );
		
		String toPartyId = oreq.request.getHeader( HeaderToPartyId );
		if( toPartyId != null ) oreq.response.addHeader( HeaderFromPartyId, toPartyId );

		
		oreq.from = ( fromCountryCode == null || fromPartyId == null ? 
							null : 
							new OcpiAgentId( fromCountryCode, fromPartyId ) );

		oreq.to = ( toCountryCode == null || toPartyId == null ? 
				null : 
				new OcpiAgentId( toCountryCode, toPartyId ) );
		
		return execute( oreq );
	}
}
