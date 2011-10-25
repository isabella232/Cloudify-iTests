package test.jpa.objects;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.gigaspaces.annotation.pojo.SpaceId;

/**
 * A basic JPA entity containing an Integer Id property.
 * 
 * @author idan
 * @since 8.0.1
 *
 */
@Entity
public class JpaObject {
	private Integer id;

	public JpaObject() {
	}
	
	public JpaObject(Integer id) {
		this.id = id;
	}

	@Id @SpaceId
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	
}
