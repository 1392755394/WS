package controllers;

import org.apache.commons.lang.StringUtils;

import play.mvc.Before;
import utils.CaptchaUtil;

public class SubmitRepeat extends BaseController{

	@Before
	static void checkAccess() {
		
		SubmitOnly check = getActionAnnotation(SubmitOnly.class);
		SubmitCheck addCheck = getActionAnnotation(SubmitCheck.class);
		
		if(addCheck != null) {
			 String uuid = CaptchaUtil.getUUID();
			 flash.success(uuid);
		}
		
		if(check != null) {
			String uuid = params.get("uuid");
			if(StringUtils.isBlank(uuid) || CaptchaUtil.getUUID() == null) {
				String url = request.headers.get("referer").value();
				redirect(url);
			}
	    }
	}
}
