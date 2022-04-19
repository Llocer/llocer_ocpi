package com.llocer.ev.ocpi.server;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JavaType;
import com.llocer.common.Log;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint.Identifier;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoints;
import com.llocer.ev.ocpi.server.OcpiRequestData.HttpMethod;

public class OcpiRequestBuilder {
	private final OcpiLink link;
	private Builder builder;
	private URI uri = null;
	
	OcpiRequestBuilder( OcpiLink link ) {
		this.link = link; 
		
		builder = HttpRequest.newBuilder()	            
					.version( HttpClient.Version.HTTP_2 )
					.setHeader("Authorization", link.peerCredentials.getToken() );			

		if( link.ownId != null ) {
			builder = builder
				.setHeader( OcpiServlet.HeaderFromCountryCode, link.ownId.countryCode )
				.setHeader( OcpiServlet.HeaderFromPartyId, link.ownId.partyId );
		}
		
		if( link.peerId != null ) {
			builder = builder
				.setHeader( OcpiServlet.HeaderToCountryCode, link.peerId.countryCode )
				.setHeader( OcpiServlet.HeaderToPartyId, link.peerId.partyId );
		}
		
	}

	public static URI getModuleUrl( OcpiEndpoints endpoints, Identifier module ) {
		if( endpoints == null || endpoints.getEndpoints() == null ) return null;

		URI res = null;
		for( OcpiEndpoint v : endpoints.getEndpoints() ) {
			if( v.getIdentifier() == module ) {
				res = v.getUrl();
				break;
			}
		}
		
		return res;
	}
	

	public OcpiRequestBuilder uri( URI uri ) {
		this.uri = uri;
		return this;
	}
	
	public OcpiRequestBuilder uri( Identifier module ) throws Exception {
		this.uri = getModuleUrl( link.peerEndpoints, module );
		if( this.uri == null ) throw new IOException();
		return this;
	}
	
	public OcpiRequestBuilder parameter( String path ) {
		uri = uri.resolve( URLEncoder.encode( path, StandardCharsets.UTF_8 )+"/" );
		return this;
	}

	public OcpiRequestBuilder query( String k, String v ) throws Exception {
		// URI(String scheme, String authority, String path, String query, String fragment)
		String query = uri.getQuery();
		if( query == null || query.isEmpty() ) {
			query = query+"?"+k+"="+v;
		} else {
			query = query+","+k+"="+v;
		}
		uri = new URI( uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment() );
		return this;
	}

	public OcpiRequestBuilder method​( HttpMethod method, Object body ) throws Exception {
		BodyPublisher bodyPublisher;
		if( body == null ) {
			bodyPublisher = BodyPublishers.noBody();

		} else {
	        String body_s = OcpiRequestData.mapper.writeValueAsString( body  );
	        Log.debug( "OcpiHttpBuilder.method: request body=%s", body_s );
	        bodyPublisher = BodyPublishers.ofString( body_s );
	        
		}

		builder = builder.method( method.name(), bodyPublisher );
		return this;
	}
		
	public HttpResponse<String> send() throws Exception {
		HttpClient httpClient = HttpClient.newBuilder().build();
		HttpRequest httpRequest = builder.uri(uri).build();
		
        Log.debug( "OcpiHttpBuilder.send: uri=%s", uri );
		HttpResponse<String> httpResponse = httpClient.send( httpRequest, HttpResponse.BodyHandlers.ofString() );

        int status = httpResponse.statusCode();
        Log.debug( "OcpiHttpBuilder.send: status code=%d", status );
        if( status != 200 && status != 201 ) throw new IOException();
        
        return httpResponse;
	}
	
	public <T> T send( Class<T> answerClass ) throws Exception {
		HttpResponse<String> httpResponse = this.send();
		
        String answerBody = httpResponse.body();
        Log.debug( "OcpiHttpBuilder.send: answer body=%s", answerBody );
        if( answerBody == null || answerBody.isEmpty() ) return null;
        
        JavaType t = OcpiRequestData.mapper.getTypeFactory().constructParametricType( OcpiAnswer.class, answerClass );
        OcpiAnswer<T> answer = OcpiRequestData.mapper.readValue( answerBody, t  );
        if( answer.status_code != 1000 ) throw new IOException();
        
        return answer.data;
	}

}
