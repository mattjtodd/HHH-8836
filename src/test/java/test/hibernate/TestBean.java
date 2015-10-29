package test.hibernate;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class TestBean
{
	@Id
	private Long id;
	
	@ManyToOne(cascade = CascadeType.ALL)
	private AnotherBean anotherBean;
	
	private String value;

	public AnotherBean getAnotherBean()
	{
		return anotherBean;
	}

	public void setAnotherBean(AnotherBean embeddedBean)
	{
		this.anotherBean = embeddedBean;
	}
	
	// for hibernate
	TestBean()
	{
	}
	
	public TestBean(String value, Long id)
	{
		setValue(value);
		
		this.id = id;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}
