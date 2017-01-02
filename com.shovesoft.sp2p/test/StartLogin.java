import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import play.mvc.Http;

import com.shove.gateway.GeneralRestGateway;

import constants.Constants;


public class StartLogin {

	@Test
	public  void mains() {
		
		Http.Request request = Http.Request.current();
		Set<String> keys = request.params.data.keySet();
		Map parameters = new HashMap();
	    for (String t_key : keys) {
	        try {
				parameters.put(t_key, URLDecoder.decode(request.params.get(t_key), "utf-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
		
		String finalURL;
		try {
			finalURL = GeneralRestGateway.buildUrl("http://localhost:9000/app/services?OPT=1&name=18626850291&pwd=wangsong123", Constants.APP_ENCRYPTION_KEY, parameters);
			URL url;
			try {
				url = new URL(finalURL);
				URLConnection rulConnection = url.openConnection();
			} catch (IOException  e) {
				e.printStackTrace();
			}   
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}
}
