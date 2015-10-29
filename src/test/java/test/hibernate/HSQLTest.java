package test.hibernate;

/**
 * When using an HSQL database, issues occur when trying to construct the query; same also applies when using PostgreSQL.
 * Comments have been added to the ConcatTest class to explain these issues, and workarounds.
 */
public class HSQLTest extends ConcatTest
{
	public HSQLTest()
	{
		super("hsqlPersistenceUnit");
	}
}
