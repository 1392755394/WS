package jobs;

import play.jobs.Job;
import play.jobs.On;
import reports.StatisticDebt;
import reports.StatisticInvest;



//每月最后一天23:50执行
@On("0 50 23 L * ?")
public class EveryMonthJob extends Job{
	
	@Override
	public void doJob() {
		    StatisticInvest.investSituationStatistic();//理财情况统计表
			
			StatisticDebt.debtSituationStatistics();//债权转让情况统计分析表
			
	}
}
