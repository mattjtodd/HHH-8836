package test.hibernate;

/**
 * ALL tests pass when using an H2 database.
 * 
 * @author daiman patel
 * @version $Id$
 */
public class H2Test extends ConcatTest
{
	public H2Test()
	{
		super("h2PersistenceUnit");
	}
}
