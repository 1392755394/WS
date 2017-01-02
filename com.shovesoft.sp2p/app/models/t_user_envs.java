package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class t_user_envs extends Model {

	public long user_id;
	public Date time;
	public double money;
	public String user_name;
}
