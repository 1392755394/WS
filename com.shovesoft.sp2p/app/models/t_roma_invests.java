package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 
 * 流转标model
 * 
 * ***/
@Entity
public class t_roma_invests extends Model{
	public long user_id;
	public Date roma_time;
	public long roma_bid_id;
	public double roma_amount;
    public double roma_loan_amount;
    public int roma_invests_state;
    public String user_name;
}
