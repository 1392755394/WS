//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//import java.util.Set;
//
//import play.mvc.Http;
//
//import com.shove.security.Encrypt;
//
//import constants.Constants;
//
//
//public class Test {
//
//	
//	Date date = new Date();
////	Http.Request.current().date.setTime(date.getTime());
////	Http.Request request = Http.Request.current();
////	Calendar calendar = Calendar.getInstance();
////    calendar.setTime(new Date());
////    calendar.add(1, -30);
//    Http.Request request = Http.Request.current();
//    request.encoding = "utf-8";
//    response.encoding = "utf-8";
//
//    Set<String> keys = request.params.data.keySet();
//    List parameterNames = new ArrayList(keys);
//    Collections.sort(parameterNames);
//
//    StringBuffer signData = new StringBuffer();
//    for (int i = 0; i < parameters.size(); i++) {
//      signData.append((String)parameterNames.get(i) + "=" + (String)parameters.get(parameterNames.get(i)) + (i < parameters.size() - 1 ? "&" : ""));
//    }
//    String _ss = Encrypt.MD5(signData + Constants.APP_ENCRYPTION_KEY, "utf-8");
//	request.params.put("_t", String.valueOf(date.getTime()));
//	request.params.put("_s", _ss);
////    Date timestamp = Convert.strToDate(_t, getLongGoneDate());
//}

