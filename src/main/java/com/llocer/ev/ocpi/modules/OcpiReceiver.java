package com.llocer.ev.ocpi.modules;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.llocer.common.Log;
import com.llocer.common.Tuple2;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint;
import com.llocer.ev.ocpi.server.OcpiAnswer;
import com.llocer.ev.ocpi.server.OcpiLink;
import com.llocer.ev.ocpi.server.OcpiRequestBuilder;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiRequestData.HttpMethod;

public class OcpiReceiver {
	/*
	 * pagination
	 */
	
	private static String toUTC( Instant instant ) {
		return instant.toString();
	}
	
	public static <T> void paginationClient( 
			Collection<OcpiLink> links, Instant reloadStart, 
			Class<T> cl, Consumer<T> consumer ) {
		
		if( reloadStart == null || links == null ) return;
		
		for( OcpiLink link : links ) {

			OcpiRequestBuilder builder =  null;
			try {
				builder = link.makeBuilder()
					.uri( OcpiEndpoint.Identifier.TOKENS )
					.query( "data_from", OcpiReceiver.toUTC( reloadStart ) )
					.method​( HttpMethod.GET, null );
			} catch( Exception e ) {
				Log.error( e, "Unable to construct builder");
			}

			while( builder != null ) {
				try {
					HttpResponse<String> httpResponse = builder.send();

					Tuple2<List<T>, String/*link*/> t = handlePaginationResponse( httpResponse, cl );

					if( t.f1 != null ) {
						for( T item : t.f1 ) {
							consumer.accept(item);
						}
					}
					
					if( t.f2 != null ) {
						builder = link.makeBuilder()
								.uri( new URI(t.f2) )
								.method​( HttpMethod.GET, null );
					} else {
						builder = null;
					}


				} catch (Exception e) {
					Log.error( e );
				}
			}
		}
	}
	
	private static <T> Tuple2<List<T>, String/*link*/> handlePaginationResponse( HttpResponse<String> httpResponse, Class<T> cl ) throws Exception {
		        String answerBody = httpResponse.body();
		        Log.debug( "OcpiHttpBuilder.send: answer body=%s", answerBody );
		        if( answerBody == null || answerBody.isEmpty() ) throw new IOException();
		        
		        TypeFactory typeFactory = OcpiRequestData.mapper.getTypeFactory();
		        JavaType t = typeFactory.constructParametricType( 
		        		OcpiAnswer.class, 
		        		typeFactory.constructCollectionLikeType( List.class, cl) );
		        OcpiAnswer<List<T>> answer = OcpiRequestData.mapper.readValue( answerBody, t  );

		        if( answer.status_code != 1000 ) throw new IOException();

		        Optional<String> optionalLink = httpResponse.headers().firstValue( "Link");
		        String link = ( optionalLink.isPresent() ? optionalLink.get() : null );
		        
		        return new Tuple2<List<T>,String>(answer.data,link);
	}
	
	/*
	 * merge objects
	 */
	
	public static <T> void mergeObjects( T to, T from ) {
		Class<?> clTo = to.getClass();
		for( Method setter: clTo.getMethods() ) {
			try {
				if( !setter.isAnnotationPresent( JsonProperty.class ) ) continue;
				if( setter.getParameterCount() != 1 ) continue;
				// is a setter, find getter pair
				String getterName = setter.getName().replace("set", "get");
				Method getter = clTo.getMethod(getterName);
					
//				System.out.println( field.getName() );
				Object vFrom = getter.invoke(from);
				if( vFrom == null ) continue;
				Class<?> clFrom = vFrom.getClass();

				if( clFrom.isPrimitive() || clFrom.isEnum() || vFrom instanceof String ) {
					
					setter.invoke( to, vFrom );
					continue;
				}

				Object vTo = getter.invoke(to);
				if( vTo == null ) {
					setter.invoke( to, vFrom );
					continue;
				}
				
				JsonInclude jsonInclude = clFrom.getAnnotation(JsonInclude.class);
				if( jsonInclude != null ) {
					mergeObjects( vTo, vFrom );
				}

			} catch ( Exception e ) {
				Log.error(e);
				
			}
		}
	}
}
