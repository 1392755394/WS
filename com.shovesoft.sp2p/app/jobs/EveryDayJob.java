package jobs;

import business.Vip;
import play.jobs.Job;
import play.jobs.On;
import reports.StatisticAuditItems;
import reports.StatisticBorrow;
import reports.StatisticInvest;
import reports.StatisticMember;
import reports.StatisticProduct;
import reports.StatisticRecharge;
import reports.StatisticSecurity;
import utils.ErrorInfo;


/**
 * 每天定时定点任务
 * @author lwh
 *
 */

//每天23:50执行


@On("0 50 23 * * ?")
public class EveryDayJob extends Job{
	
	@Override
	public void doJob() {
		ErrorInfo error = new ErrorInfo();
        
      	StatisticAuditItems.executeUpdate(error);
		StatisticProduct.executeUpdate(error);
		StatisticBorrow.executeUpdate(error);
		
		StatisticInvest.platformIncomeStatistic();//平台收入
		StatisticInvest.platformWithdrawStatistic();//系统提现
		StatisticInvest.platformFloatstatistics();//平台浮存金统计
		
		StatisticRecharge.executeUpdate(error);
		StatisticMember.executeUpdate(error);
		StatisticSecurity.executeUpdate(error);
		
		Vip.vipExpiredJob(); //vip过期处理
	}
}
