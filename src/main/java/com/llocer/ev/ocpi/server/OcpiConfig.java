package com.llocer.ev.ocpi.server;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.llocer.common.Log;



public class OcpiConfig {
	private static String configFile = "/etc/ocpi.conf";
	
	public String public_url = "http://127.0.0.1:8080";
	public Integer private_url_length = 0;
	public Integer pagination_limit = 100;
	public boolean testing_no_change_credentials = false;

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final ObjectReader reader = mapper.readerFor(OcpiConfig.class);

	public static final OcpiConfig config = readConfig();

	private static OcpiConfig readConfig() {
		try {
			return reader.readValue( new File( configFile )  );
		} catch (Exception e) {
			Log.warning( e, "unable to read config file "+ configFile );
			return new OcpiConfig();
		}
	}

	public static URI getPublicURI() {
		try {
			return new URI( config.public_url );
		} catch (URISyntaxException e) {
			return null;
		}
	}

}
