package models;

import java.util.Date;

import javax.persistence.Entity;


import play.db.jpa.Model;

@Entity
public class t_reds_use extends Model{
	
	public long Red_Use_RedId;
	
	public long Red_Use_UserId;
	
	
	
	public Date Red_UseTime;
	
	public Date Red_UseGetTime;
	
	public int Red_Use_State;
	

	

}
