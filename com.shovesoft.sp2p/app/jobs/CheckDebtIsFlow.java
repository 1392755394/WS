package jobs;

import business.Debt;
import play.jobs.Every;
import play.jobs.Job;

//h 小时, mn 分钟, s 秒


/**
 * 每五分钟判断正在转让的债权是否到达流拍时间
 */

@Every("5mn")
public class CheckDebtIsFlow extends Job{
	
	@Override
	public void doJob() {
	  Debt.judgeDebtFlow();
	}
}
