package controllers.supervisor.weixin;

import com.shove.gateway.weixin.gongzhong.GongZhongService;

public class BaseInfo extends GongZhongService {

	public GongZhongService gongZhongService;

	public GongZhongService getGongZhongService() {
		return gongZhongService;
	}

	public void setGongZhongService(GongZhongService gongZhongService) {
		this.gongZhongService = gongZhongService;
	}
	

	
}
