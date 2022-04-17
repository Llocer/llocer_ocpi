package com.llocer.ev.ocpi.server;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OcpiAnswer<T> implements OcpiResult<T> {
	private final int httpStatusCode ;
	public T data = null;
	public Integer status_code = null; 
	public String status_message = null;
	public Instant timestamp = null;
	
	public OcpiAnswer() {
		httpStatusCode = 0;
	}
	
	OcpiAnswer( int httpStatusCode ) {
		this.httpStatusCode = httpStatusCode;
	}
	
	@JsonIgnore
	@Override
	public int getHttpStatusCode() {
		return httpStatusCode;
	}
	
	@JsonIgnore
	@Override
	public OcpiAnswer<?> getAnswer() {
		return this;
	}
 }
