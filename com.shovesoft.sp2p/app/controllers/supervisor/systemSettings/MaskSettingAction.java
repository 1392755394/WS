package controllers.supervisor.systemSettings;

import models.t_view_index;
import utils.ErrorInfo;
import business.BackstageSet;
import controllers.supervisor.SupervisorController;

public class MaskSettingAction extends SupervisorController {

	public static void maskSettingJump() {
		ErrorInfo error = new ErrorInfo();

		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();

		render(backstageSet, error);
	}

	public static void maskAdd(int mask_percent, int mask_multiple,
			int mask_small_multiple, int mask_give_money) {
		t_view_index tvi = new t_view_index();
		// tvi.id = (long) 1;
		tvi.mask_give_money = mask_give_money;
		tvi.mask_multiple = mask_multiple;
		tvi.mask_small_multiple = mask_small_multiple;
		tvi.mask_percent = mask_percent;
        tvi.save();
		MaskSettingAction.maskSettingJump();
	}

}
