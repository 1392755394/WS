package reports;

import play.db.jpa.GenericModel;
import models.t_statistic_withdraw;


/**
 * 平台提现数据统计分析表
 * @author liuwenhui
 *
 */
public class StatisticPlatformWithdraw {
	
	
	public static Long queryPaymentNumber(){
		Long payment_number = 0l;//付款笔数
		String sql = "select count(*) from t_user_withdrawals where status = 2 and  DATE_FORMAT(time, '%Y%m') = DATE_FORMAT(CURDATE(),'%Y%m')";

		try {
			payment_number = GenericModel.find(sql).first();// 付款笔数
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (null == payment_number || payment_number == 0) {
			payment_number = 0l;
		}
		return  payment_number ;//付款笔数
	}
	
	
	
	public static Double queryPaymentSum(){
		
		Double payment_sum = 0.0;//付款总额
		String sql = "select sum(amount) from t_user_withdrawals where status = 2 and  DATE_FORMAT(time, '%Y%m') = DATE_FORMAT(CURDATE(),'%Y%m')";
		
		try {
			 payment_sum = GenericModel.find(sql).first();//付款总额
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(null == payment_sum || payment_sum == 0){
			payment_sum = 0.0;
		}
		return payment_sum;
	}
	
	
	public static Long queryApplyWithdraw(){
		Long apply_withdraw_account = 0l;//申请提现笔数（含付款中）
		String sql = "select count(*) from t_user_withdrawals where status in (0,1,-1) and  DATE_FORMAT(time, '%Y%m') = DATE_FORMAT(CURDATE(),'%Y%m')";
		
		try {
			 apply_withdraw_account = GenericModel.find(sql).first();//申请提现笔数（含付款中）
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(null == apply_withdraw_account || apply_withdraw_account == 0){
			apply_withdraw_account = 0l;
		}
		return apply_withdraw_account;
	}
	
	
	public static Double queryApplyWithdrawSum(){
		Double apply_withdraw_sum = 0.0;//申请提现总额
		
		String sql = "select sum(amount) from t_user_withdrawals where status in (0,1,-1) and  DATE_FORMAT(time, '%Y%m') = DATE_FORMAT(CURDATE(),'%Y%m')";
		try {
			apply_withdraw_sum = GenericModel.find(sql).first();//申请提现总额
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(null == apply_withdraw_sum || apply_withdraw_sum == 0){
			apply_withdraw_sum = 0.0;
		}
		return apply_withdraw_sum;
	}
	
	
	
	
	public static Double queryMaxAmount(){
		Double max_withdraw_amount = 0.0;//最高申请提现金额
		
		String sql = "select max(amount) from t_user_withdrawals where   DATE_FORMAT(time, '%Y%m') = DATE_FORMAT(CURDATE(),'%Y%m')";
		try {
			 max_withdraw_amount = GenericModel.find(sql).first();//最高申请提现金额
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(null == max_withdraw_amount || max_withdraw_amount == 0){
			max_withdraw_amount = 0.0;
		}
		return max_withdraw_amount;
	}
	
	
	public static Double queryMinAmount(){
		Double min_withdraw_amount = 0.0;//最低申请提现金额
		
		String sql = "select min(amount) from t_user_withdrawals where   DATE_FORMAT(time, '%Y%m') = DATE_FORMAT(CURDATE(),'%Y%m')";
		try {
			min_withdraw_amount = GenericModel.find(sql).first();//最高申请提现金额
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(null == min_withdraw_amount || min_withdraw_amount == 0){
			min_withdraw_amount = 0.0;
		}
		return min_withdraw_amount;
	}
	
	
	//判断记录是否存在
	public static boolean judgeIsNew(int year,int month,int day){
		t_statistic_withdraw statistic = null;
		
		try {
			statistic = GenericModel.find(" year = ? and month = ? and day = ?", year,month,day).first();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(null == statistic){
			return true;
		}
		return false;
	}
	
	
	//获取对象
	public static t_statistic_withdraw getTarget(int year,int month,int day){
		
		t_statistic_withdraw statistic = null;

		try {
			statistic = GenericModel.find(
					"  year = ? and month = ? and day = ?",year,month,day).first();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return statistic;
	}
}
