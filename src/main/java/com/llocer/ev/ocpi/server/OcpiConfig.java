package com.llocer.ev.ocpi.server;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.llocer.common.Log;
import com.llocer.ev.ocpi.msgs22.OcpiToken.Whitelist;



public class OcpiConfig {
	private static String configFile = "/opt/llocer/ev/etc/ocpi.conf";
	
	public String publicUrl = "http://127.0.0.1:8080/llocer_cso_war/";
	public Integer privateUriLength = 3; // /llocer_cso_war/cso/ocpi
	public Integer ocpiMaxGetLimit = 100;
	public Whitelist allowedWhitelist = Whitelist.ALWAYS;
	public Boolean keepInvitationTokens = true;
	public String dataBackend = "com.llocer.redis.RedisMapFactory";
	public boolean testing = true;

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final ObjectReader reader = mapper.readerFor(OcpiConfig.class);

	public static final OcpiConfig config = readConfig();

	private static OcpiConfig readConfig() {
		try {
			return reader.readValue( new File( configFile )  );
		} catch (Exception e2) {
			Log.warning( "unable to read config file "+ configFile );
			return new OcpiConfig();
		}
	}

	public static URI getPublicURI() {
		try {
			return new URI( config.publicUrl );
		} catch (URISyntaxException e) {
			return null;
		}
	}

}
