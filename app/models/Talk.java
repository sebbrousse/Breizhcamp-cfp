package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.data.format.Formats;
import play.data.validation.Constraints;

import play.db.ebean.Model;

@Entity
public class Talk extends Model {
	
	@Id
	public Long id;
	
	@Constraints.Required
	@Constraints.MaxLength(50)	
	@Formats.NonEmpty
	@Column(unique = true)
	public String title;
	
	@Constraints.Required
	@Constraints.MaxLength(2000)
	@Formats.NonEmpty
	@Column
	public String description;
	
	@ManyToOne
	public User speaker;

	public static Finder<Long, Talk> find = new Finder<Long, Talk>(Long.class, Talk.class);
}
