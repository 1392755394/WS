package business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import constants.Constants;
import constants.UserEvent;
import play.Logger;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.ErrorInfo;
import utils.PageBean;
import models.t_bid_questions;
import models.t_bids;

/**
 * 提问
* @author lwh
* @version 6.0
* @created 2014年5月6日 下午5:28:27
 */
public class BidQuestions implements Serializable{
	
	public long id;
	private long _id;
	public Date time; // 提问时间
	public String content; // 提问内容
	public boolean isAnswer; // 是否已经回答过
	
	public long bidId;
	public long userId;
	public long questionedUserId;
	public String name;//提问者名字
	
	public List<BidAnswers> bidAnswerList; // 对应问题回答集合
	
	public long getId(){
		return _id;
	}
	
	/**
	 * 查询未回答的用户提问总数
	 */
	public static int queryQuestionCount(long userId, ErrorInfo error){
		try {
			return (int)GenericModel.count("questioned_user_id = ? and is_answer = 0", userId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("标提问记录->查询未回答的用户提问总数:" + e.getMessage());
			error.msg = "查询未回答的用户提问总数失败!";

			return 0;
		}
	}
	
	/**
	 * 用户所有的标提问
	 * @param currPage 当前页
	 * @param pageSize 总页数
	 * @param keyword 关键词
	 * @param isAnswer 是否回答
	 * @param userId 提问用户ID
	 * @param questionedUserId 当前用户ID
	 * @param error 信息值
	 * @return PageBean<BidQuestions>
	 */
	public static PageBean<BidQuestions> queryQuestion(int currPage,
			int pageSize, long bidId, int isAnswer, long questionedUserId,
			ErrorInfo error) {
		PageBean<BidQuestions> pageBean = new PageBean<BidQuestions>();
		Map<String, Object> conditionmap = new HashMap<String, Object>();
		List<BidQuestions> questions = new ArrayList<BidQuestions>();
		StringBuffer conditions = new StringBuffer(" 1 = 1 ");
		List<Object> values = new ArrayList<Object>();

		if (questionedUserId > 0) {
			conditions.append(" and questioned_user_id = ?");
			values.add(questionedUserId);
		}

		if (bidId > 0) {
			conditions.append(" and bid_id = ?");
			values.add(bidId);
			conditionmap.put("keyword", bidId);
		}

		if (isAnswer != Constants.SEARCH_ALL) {
			conditions.append(" and is_answer = ?");
			values.add(isAnswer == 1 ? true : false);
			conditionmap.put("isAnswer", isAnswer);
		}
		
		conditions.append(" order by time desc ");
		pageBean.conditions = conditionmap;
		int count = -1;

		try {
			count = (int) GenericModel.count(conditions.toString(), values.toArray());
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("标提问记录->根据标ID查询对应的提问记录,查询总记录:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载用户提问记录失败!";

			return null;
		}

		if (count == 0) {
			return pageBean;
		}
		
		pageBean.totalCount = count;
		List<t_bid_questions> tquestions = null;

		try {
			tquestions = GenericModel.find(conditions.toString(), values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("标提问记录->根据标ID查询对应的提问记录:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载用户提问记录失败!";

			return null;
		}
		
		if(null == tquestions) {
			return pageBean;
		}
		
		BidQuestions question = null;
		
		/* 填充,ID、名称、时间、内容、答案集合 */
		for (t_bid_questions tquestion : tquestions) {
			question = new BidQuestions();
			
			question._id = tquestion.id;
			question.time = tquestion.time;
			question.content = tquestion.content;
			question.bidId = tquestion.bid_id;
			question.isAnswer = tquestion.is_answer;
			question.name = User.queryUserNameById(tquestion.user_id, error);
			question.bidAnswerList = BidAnswers.queryAnswers(tquestion.id, error);
			updateQuestionReadcount(tquestion.id, error);
			questions.add(question);
		}
		
		 pageBean.currPage = currPage;
		 pageBean.pageSize = pageSize;
		 pageBean.page = questions;
		 
		 error.code = 0;
		 
		 return pageBean;
	}
	
	/**
	 * 提问详情
	 * @param userId 提问用户ID 
	 * @param questionedUserId 当前用户ID
	 * @param bidId 标ID
	 * @param error 信息值
	 * @return BidQuestions
	 */
	public static BidQuestions queryBidQuestionDetail(long id, ErrorInfo error) {
		error.clear();
		
		t_bid_questions tquestion = null;

		try {
			tquestion = GenericModel.find(" id = ? ", id).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("标提问记录->根据标ID查询对应的提问记录:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载用户提问记录失败!";

			return null;
		}
		
		if(null == tquestion) return null;
		
		BidQuestions question = new BidQuestions();
		question._id = tquestion.id;
		question.time = tquestion.time;
		question.content = tquestion.content;
		question.bidId = tquestion.bid_id;
		question.isAnswer = tquestion.is_answer;
		question.name = User.queryUserNameById(tquestion.user_id, error);
		question.bidAnswerList = BidAnswers.queryAnswers(tquestion.id, error);
		
		error.code = 0;
		
		return question;
			
	}
	
	/**
	 * 向借款者提问
	 * @param bidId 标ID
	 * @param userId 用户ID
	 * @param content 提问内容
	 * @param error 信息值
	 * @return ? > 0 : success; ? < 0 : fail
	 */
	public int addQuestion(long userId, ErrorInfo error){
		error.clear();
		
		Long uId = null;
		
		String sql = "select user_id from t_bids where id = ?" ;
		
		try {
			uId = GenericModel.find(sql, this.bidId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("标提问->根据自己的ID查询标中的用户ID:" + e.getMessage());
			
			return -1;
		}
		
		if(uId == null) {
			error.msg = "提问失败!";
			
			return -2;
		}
		
		if(uId == userId){
			error.msg = "对不起!您不能对自己的借款标提问!";
			
			return -3;
		}
		
		t_bid_questions bidQuestions = new t_bid_questions();
		
		bidQuestions.bid_id = this.bidId;
		bidQuestions.user_id = this.userId;
		bidQuestions.content = this.content;
		bidQuestions.questioned_user_id = this.questionedUserId;
		bidQuestions.time = new Date();
		bidQuestions.is_answer = Constants.NOT_ENABLE;//默认没有回答
		
		try {
			bidQuestions.save();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("标提问->向借款者提问:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "提问失败!";
			
			return -4;
		}
		
		DealDetail.userEvent(userId, UserEvent.QUESTION_TO_BORROWER, "向借款人提问", error);
		
		if(error.code < 0){
			JPA.setRollbackOnly();
			
			return error.code;
			
		}
		
		error.code = 0;
		error.msg = bidQuestions.id > 0 ? "提问成功!" : "提问失败!";
		
		return 0;
	}
	
	/**
	 * 删除提问 
	 * @param id ID
	 * @param error
	 */
	public static void delete(long id, ErrorInfo error){
		error.clear();

		int rows = 0;
		
		try {
			rows = BidAnswers.delete(id);
		} catch (Exception e) {
			error.code = -1;
			Logger.error("标提问回答->删除提问对应的回答 :" + e.getMessage());
			error.msg = "删除失败!";
			
			return;
		}
		
		/* 可能会出现0,没有针对问题回答 */
		if(rows < 0) {
			error.code = -2;
			error.msg = "删除失败!";
			JPA.setRollbackOnly();
			
			return;
		}
		
		String hql = "delete from t_bid_questions where id = ?";
		Query query = JPA.em().createQuery(hql);
		query.setParameter(1, id);
		
		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			Logger.error("标提问回答->删除提问:" + e.getMessage());
			error.code = -3;
			error.msg = "删除失败!";
			JPA.setRollbackOnly();
			
			return;
		}
		
		if(rows < 1) {
			error.code = -4;
			error.msg = "删除失败!";
			JPA.setRollbackOnly();
			
			return;
		}
		
		/* 添加事件 */
		DealDetail.supervisorEvent(Supervisor.currSupervisor().id, UserEvent.DELETE_ANSWERS_TO_QUESTION, "删除借款提问/答案", error);
		
		if(error.code < 0){
			error.msg = "删除失败!";
			JPA.setRollbackOnly();
			
			return;
		}
	}
	
	/**
	 * 更新借款标提问更新次数
	 * @param questionId
	 * @param error
	 */
	public static void updateQuestionReadcount(long questionId,ErrorInfo error){
		
		EntityManager em = JPA.em();
		String sql = "update t_bid_answers set read_count = read_count + 1 where bid_question_id = ?";
		
		int rows = 0;
		
		try {
			rows = em.createQuery(sql).setParameter(1, questionId).executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return;
		}
		
		if(rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";
			
			return;
		}
		
		error.code = 1;
		return;
	}
}
