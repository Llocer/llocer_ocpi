package com.llocer.ev.ocpi.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.llocer.common.Log;
import com.llocer.ev.ocpi.msgs22.OcpiEndpoint.Identifier;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiException;
import com.llocer.ev.ocpi.server.OcpiResult.OcpiResultEnum;

public class OcpiRequestData {
	private static ObjectMapper initMapper() {
		ObjectMapper mapper = new ObjectMapper();

		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		JavaTimeModule javaTimeModule = new JavaTimeModule();
		mapper.registerModule( javaTimeModule );

		return mapper;
	}
	
	public static final ObjectMapper mapper = initMapper();
	
	public static <T> T getJsonBody( HttpServletRequest request, Class<T> cl ) {
		try {
			return mapper.readValue( request.getInputStream(), cl );
		} catch (Exception e) {
			Log.debug(e, "invalid json");
			throw new OcpiException( OcpiResultEnum.INVALID_JSON );
		}
	}

	public enum HttpMethod {
		GET,
		POST,
		PATCH,
		PUT,
		DELETE
	};
	
	public OcpiServlet servlet;
	public HttpServletRequest request;
	public HttpServletResponse response;
	public OcpiLink link;
	public HttpMethod method;
	public Identifier module;
	public OcpiAgentId from;
	public OcpiAgentId to;
	public String[] args;
}
