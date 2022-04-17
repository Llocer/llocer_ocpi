package com.llocer.ev.ocpi.server;

public interface OcpiResult<T> {
	public static class OcpiException extends RuntimeException {

		private static final long serialVersionUID = -1699319879554330765L;

		public final OcpiResult<?> result;

		public OcpiException( OcpiResult<?> result ) {
			this.result = result;

		}
	}

	public enum OcpiResultEnum implements OcpiResult<Void> {
		/*
		 *  success
		 */

		OK(200,1000), 
		CREATED(201,1000),

		/*
		 * errors
		 */

		INVALID_SYNTAX(400,null),
		INVALID_JSON(400,null),
		INVALID_SEMANTICS(422,null),
		//			MISSING_INFORMATION(422,null),
		METHOD_NOT_ALLOWED(405,null),

		UNKNOWN_ITEM(404,2000),
		UNKNOWN_LOCATION(404,2003),
		MISSING_AUTHORIZATION(401,2004),
		UNKNOWN_AUTHORIZATION(401,2004),

		// credentials errors
		FAILED_GET_VERSION(200,3001),
		UNSUPPORTED_VERSION(200,3002),
		NOT_SUPPORTED_ENDPOINT(200,3003),
		UNKNOWN_TO_ADDRESS(200,4001);

		private final int http;
		private final Integer ocpi;

		OcpiResultEnum( int http, Integer ocpi ) {
			this.http = http;
			this.ocpi = ocpi;
		}

		@Override
		public int getHttpStatusCode() {
			return http;
		}

		@Override
		public OcpiAnswer<Void> getAnswer() {
			OcpiAnswer<Void> res = new OcpiAnswer<Void>();
			res.status_code = this.ocpi;
			res.status_message = this.toString();
			return res;
		}
	}

	public int getHttpStatusCode();
	public OcpiAnswer<?> getAnswer();
	
	public static <T> OcpiResult<T> success( T data ) {
		OcpiAnswer<T> res = new OcpiAnswer<T>( 200 );
		res.data = data;
		res.status_code = 1000;
		res.status_message="SUCCESS";
		return res;
		
	}
 }
