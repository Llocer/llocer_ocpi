package com.llocer.ev.ocpi.modules;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import com.llocer.ev.ocpi.msgs.HasLastUpdated;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint.Identifier;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint.InterfaceRole;
import com.llocer.ev.ocpi.server.OcpiConfig;
import com.llocer.ev.ocpi.server.OcpiRequestData;
import com.llocer.ev.ocpi.server.OcpiResult;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public interface OcpiSender {
	URI getOcpiModuleUri(InterfaceRole role, Identifier module);
	default int getOcpiPaginationLimit( Identifier module ) { return OcpiConfig.config.pagination_limit; }
	Iterator<? extends HasLastUpdated> getOcpiItems(OcpiRequestData oreq);

	static class LastUpdatedComparator implements Comparator<HasLastUpdated> {

		@Override
		public int compare(HasLastUpdated arg0, HasLastUpdated arg1) {
			return arg1.getLastUpdated().compareTo( arg0.getLastUpdated() );
		}
		
	}

	static OcpiResult<?> paginationServer(
			OcpiSender sender,
			OcpiRequestData oreq ) throws Exception {
		
		int maxLimit = sender.getOcpiPaginationLimit( oreq.module );
		URI uri = sender.getOcpiModuleUri( OcpiEndpoint.InterfaceRole.SENDER, oreq.module ); 
		Iterator<? extends HasLastUpdated> data = sender.getOcpiItems( oreq );
		
		/*
		 * parse request parameters
		 */

		String date_from_s = oreq.request.getParameter("date_from");
		String date_to_s = oreq.request.getParameter("date_to");
		String offset_s = oreq.request.getParameter("offset");
		String limit_s = oreq.request.getParameter("limit");

		Instant date_from;
		Instant date_to;
		Integer offset;
		Integer limit;
		try {
			date_from = ( date_from_s == null ? null : Instant.parse( date_from_s ) );
			date_to = ( date_to_s == null ? null : Instant.parse( date_to_s ) );
			offset = ( offset_s == null ? 0 : Integer.valueOf(offset_s) );
			limit = ( limit_s == null ? maxLimit : Integer.valueOf(limit_s) );
			
			if( limit <= 0 ) throw new IllegalArgumentException();
		} catch( Exception e ) {
			return OcpiResultEnum.INVALID_SYNTAX;
		}

		if( limit > maxLimit ) limit = maxLimit; 
		if( limit > 1000 ) limit = 1000; // hardcoded extreme

		/*
		 * get locations
		 */
		
		int queueSize = offset+limit;

		PriorityQueue<HasLastUpdated> res = new PriorityQueue<HasLastUpdated>( queueSize, new LastUpdatedComparator() );
		int totalCount = 0;
		boolean more = false;
		
		if( data != null ) {
			while( data.hasNext() ) {
				HasLastUpdated item = data.next();
				if( date_from != null && date_from.compareTo( item.getLastUpdated() ) > 0 ) continue;
				if( date_to != null && date_to.compareTo( item.getLastUpdated() ) <= 0 ) continue;
				
				totalCount++;
				
				if( res.size() >= queueSize ) {
					// discard newer
					res.poll();
					more = true;
				}
				res.add( item );
			}
		}

//		res.sort( new Comparator<HasLastUpdated>() {
//			@Override
//			public int compare(HasLastUpdated o1, HasLastUpdated o2) {
//				return o1.getLastUpdated().compareTo( o2.getLastUpdated() );
//			} 
//		} );

		/*
		 * send answer
		 */

		oreq.response.addHeader( "X-Limit", Integer.toString( maxLimit ) );
		oreq.response.addHeader( "X-Total-Count", Integer.toString( totalCount ) );

		for( int i=0; i<offset; i++ ) {
			res.remove();
		}

		if( more ) {
			Instant next_date = res.peek().getLastUpdated();

			StringBuilder builder = new StringBuilder();
			builder.append( uri.toASCIIString() );
			builder.append( "?date_from=" ); 
			builder.append( next_date.toString() );
			builder.append( "&limit=" );
			builder.append( limit );
			oreq.response.addHeader( "Link", builder.toString() );
		}

		return OcpiResult.success( res );
	}



}
