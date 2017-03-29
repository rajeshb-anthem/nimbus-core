/**
 * 
 */
package com.anthem.oss.nimbus.core.entity.person;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.Id;

import com.anthem.oss.nimbus.core.domain.definition.Model;
import com.anthem.oss.nimbus.core.entity.AbstractEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Soham Chakravarti
 *
 */
@Getter @Setter @ToString(callSuper=true)
public abstract class Address<ID extends Serializable> extends AbstractEntity<ID> {

	private static final long serialVersionUID = 1L;

	
	public enum Type {
		MAILING,
		BILLING;
	}
	

	
	public static class IdLong extends Address<Long> {
		
		private static final long serialVersionUID = 1L;
		
		@Id @Getter @Setter(value=AccessLevel.PROTECTED) 
		private Long id;
	}
	

	
	public static class IdString extends Address<String> {
		
		private static final long serialVersionUID = 1L;
		
		@Id @Getter @Setter(value=AccessLevel.PROTECTED) 
		private String id;
	}
	
	

	@NotNull
	@Model.Param.Values(url="staticCodeValue-/addressType")
	private Type type;

	@NotNull
	private String street1;

	private String street2;

	@NotNull
	private String city;

	@NotNull
	private String zip;

	private String zipExtn;

	@NotNull
	private String stateCd;

	@NotNull
	@Model.Param.Values(url="staticCodeValue-/country")
	private String countryCd;
	
}
