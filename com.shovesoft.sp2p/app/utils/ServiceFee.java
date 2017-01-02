package utils;

import business.BackstageSet;
import utils.Arith;
import constants.Constants;
import constants.OptionKeys;

/**
 * 服务费
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-4-13 上午11:59:34
 */
public class ServiceFee {

	/**
	 * 借款管理费
	 * 
	 * @param amount 金额
	 * @param period 期限
	 * @param unit 期限单位
	 * @param error 信息值
	 * @return 借款管理费
	 */
	public static double loanServiceFee(double amount, int period, int unit, ErrorInfo error) {
		/* 换算月份 */
		if(unit == Constants.YEAR){
			period *= 12;
		}else if(unit == Constants.DAY){
			period = 0;
		}
		
		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();

		double num1 = backstageSet.borrowFee;
		double num2 = backstageSet.borrowFeeMonth;
		double num3 = backstageSet.borrowFeeRate;

		if(num1 <= 0 || (num2 > 0 && num3 <= 0)) return 0;
		
		/** 公式：按借款本金 ? % +本金*（期数 - ?个月）* ? % **/

		num1 = Arith.div(num1, 100, 20);
		double div = Arith.mul(amount, num1); // 本金 *?

		// 如果达到给定的额外收费期数
		if (period > num2) {
			num3 = Arith.div(num3, 100, 20);
			div = Arith.add(Arith.mul(Arith.mul(amount, (period - num2)), num3), div);
		}

		if (div < 0) {
			error.code = -5;
			error.msg = error.FRIEND_INFO + "借款管理费有误!" + error.PROCESS_INFO;
			
			return -5;
		}

		return div;
	}
	
	/**
	 * 理财管理费
	 * @param amount 金额
	 * @param apr 年利率
	 * @param error 信息值
	 * @return 理财管理费
	 */
	public static double investServiceFee(double amount, double apr, ErrorInfo error) { 
		/* 得到理财管理费基准值 */
		String strfee = OptionKeys.getvalue(OptionKeys.INVESTMENT_FEE, error);

		if (null == strfee)  return -1;
		
		double fee = Double.parseDouble(strfee);
		
		fee = Arith.mul(Arith.mul(amount, Arith.div(apr, 100, 20)), Arith.div(fee, 100, 20));
		
		if(fee <= 0)
			return 0;
		
		return fee;
	}
	
	/**
	 * 提现管理费
	 * @param amount
	 * @return
	 */
	public static double withdrawalFee(double amount) {
		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();
		
		double withdrawFee = amount - backstageSet.withdrawFee;
		
		if(withdrawFee <= 0) {
			return 0;
		}
		
		return Arith.round(withdrawFee*backstageSet.withdrawRate/100, 2);
	}
}
