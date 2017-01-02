package jobs;

import business.Bid;
import play.jobs.Every;
import play.jobs.Job;

/**
 * 每5分钟检查一次是否流标
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-8-1 上午10:32:52
 */
@Every("15s")
public class CheckBidIsFlow extends Job {

	@Override
	public void doJob() throws Exception {
		Bid.checkBidIsFlow();
	}
}
