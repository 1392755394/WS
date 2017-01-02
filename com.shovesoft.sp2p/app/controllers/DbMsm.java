package controllers;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class DbMsm {

	public DbMsm() {

	}
//亿美短信
	/**
	 * 
	 * 
	 * @param sn
	 * @param password
	 * @param message
	 * @param mdn
	 * @return

	public static String send(String sn, String password, String message,
			String mdn) {
       //sn 和passwrldd从后台取出来
		String key = "115075";
		String baseUrl = "http://hprpt2.eucp.b2m.cn:8080/sdkproxy/";
		String param = "";
		String code = "";
		long seqId = System.currentTimeMillis();
		String str = message;

		try {
			str = URLEncoder.encode(str, "utf-8");
			// 如有中文一定要加上，在接收方用相应字符转码即可
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
					
		param = "cdkey=" + sn + "&password=" + key + "&phone=" + mdn
				+ "&message=" + str + "&addserial=" + code + "&seqid="
				+ seqId;

		String msgs = sendSMS(baseUrl, param);
		System.out.println("MSG:"+ param);
		return msgs;
	}

	// 下发
	public static String sendSMS(String url, String param) {
		String ret = "";
		url = url + "?" + param;
		
		//Import controllers.HttpClientUtil 这个HttpClientUtil是你们家的我直接放进项目里的
		
		String responseString = HttpClientUtil.getInstance().doGetRequest(url);
		responseString = responseString.trim();
		if (null != responseString && !"".equals(responseString)) {
			ret = xmlMt(responseString);
		}
		return ret;
	}

	// 解析下发response
	public static String xmlMt(String response) {
		String result = "0";
		Document document = null;
		try {
			document = DocumentHelper.parseText(response);
		} catch (DocumentException e) {
			e.printStackTrace();
			result = "-250";
		}
		Element root = document.getRootElement();
		result = root.elementText("error");
		if (null == result || "".equals(result)) {
			result = "-250";
		}
		return result;
	}

	 */

	/**
	 * 
	 * @param userName
	 * @param password
	 * @param content
	 * @param toPhoneNumbers
	 * @return
	 * 
	 **/
	public static String send(String userName, String password, String content,
			String toPhoneNumbers) {
		String strSmsUrl = "http://www.stongnet.com/sdkhttp/sendsms.aspx";
     
		String str = content;

		try {
			str = URLEncoder.encode(str, "utf-8");
			// 如有中文一定要加上，在接收方用相应字符转码即可
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String company ="【东邦易贷】";
		
		 try{
			 
			 company = URLEncoder.encode(company,"utf-8");
		 } catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		

		String strSmsParam = "reg=" + userName + "&pwd=" + password
				+ "&sourceadd=" + "" + "&phone=" + toPhoneNumbers + "&content="
				+ str +  company;
	
		return postSend(strSmsUrl, strSmsParam);
	}

	public static String postSend(String strUrl, String param) {

		URL url = null;
		HttpURLConnection connection = null;		

		try {
			url = new URL(strUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("POST");		
			connection.setUseCaches(false);
			connection.connect();

			// POST方法时使用
			DataOutputStream out = new DataOutputStream(
					connection.getOutputStream());
			out.writeBytes(param);
			out.flush();
			out.close();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream(), "utf-8"));
			StringBuffer buffer = new StringBuffer();
			String line = "";
			while ((line = reader.readLine()) != null) {
			
				buffer.append(line);
			}
           
			reader.close();
            
			
			return buffer.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

	}
	

}
