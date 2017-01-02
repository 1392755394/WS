package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class t_reds extends Model {

	public int Red_Amount;
	public Date RedTime;
	public Date Red_StartTime;

	public long Red_Range_Time;
	public String Red_Details;
	public int Red_State;

}
