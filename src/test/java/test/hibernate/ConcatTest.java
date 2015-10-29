package test.hibernate;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class ConcatTest
{
	private final String persistenceUnit;
	
	private static EntityManager entityManager;
	
	private EntityTransaction entityTransaction;

	ConcatTest(String persistenceUnit)
	{
		this.persistenceUnit = persistenceUnit;
	}
	
	@Before
	public void setUp()
	{
		EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnit);
		entityManager = entityManagerFactory.createEntityManager();
		
		// Enable transactions
		entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();

		// Setup domain
		TestBean testBean = new TestBean("value", 1L);

		AnotherBean anotherBean = new AnotherBean("string", 1L);
		testBean.setAnotherBean(anotherBean);
		
		entityManager.persist(testBean);
	}
	
	@After
	public void after()
	{
		if (entityTransaction != null)
		{
			entityTransaction.rollback();
		}
		
		entityTransaction = null;
		
		if (entityManager != null)
		{
			EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
			
			entityManager.close();
			
			entityManagerFactory.close();
		}
	}
	
	/*  This test passes because there are no literal strings in the concatenate function */
	
	@Test
	public void testSimpleGroupByWithConcat()
	{
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		
		CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createQuery(Tuple.class);
		Root<TestBean> root = criteriaQuery.from(TestBean.class);
		
		Path<String> pathA = root.get("value");
		Path<String> pathB = root.get("value");
		
		// No literals included in the concatenation
		Expression<String> expression = criteriaBuilder.concat(pathA, pathB);
		
		Expression<Long> count = criteriaBuilder.count(root);
		
		criteriaQuery.multiselect(expression, count);
		
		criteriaQuery.groupBy(expression);
		
		TypedQuery<Tuple> typedQuery = entityManager.createQuery(criteriaQuery);
		
		List<Tuple> testBeans = typedQuery.getResultList();
		
		List<?> expectedResults = asList(asList("valuevalue", 1L));
		
		assertResultsEqual(expectedResults, testBeans);
	}
	
	/* 
	 * The following tests will fail because the concatenate function comprises literal strings.
	 * However, by adding the "correct" path to the 'group by' method these tests can also pass.
	 * 
	 * In all examples written using JPA Criteria, the concatenation
	 * function is included in both the SELECT and GROUP BY clauses
	 */
	
	@Test
	public void testSimpleGroupByWithConcatNotWorking()
	{
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		
		CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createQuery(Tuple.class);
		Root<TestBean> root = criteriaQuery.from(TestBean.class);
		
		Path<String> pathA = root.get("value");
		Path<String> pathB = root.get("value");
		
		// Literal included in the concatenation - space character to separate values
		Expression<String> expression = criteriaBuilder.concat(pathA, " ");
		expression = criteriaBuilder.concat(expression, pathB);
		
		Expression<Long> count = criteriaBuilder.count(root);
		
		criteriaQuery.multiselect(expression, count);
		
		// Only the concat function is included in the 'group by' method
		criteriaQuery.groupBy(expression);
		
		TypedQuery<Tuple> typedQuery = entityManager.createQuery(criteriaQuery);
		
		List<Tuple> testBeans = typedQuery.getResultList();
		
		List<?> expectedResults = asList(asList("value value", 1L));
		
		assertResultsEqual(expectedResults, testBeans);
	}

	/**
	 * Writing the above query in raw HQL is valid - this test passes 
	 */
	@Test
	public void testSimpleGroupByWithConcatUsingHQL()
	{
		// ' ' used to add a space between the two concatenated values
		Query query = entityManager.createQuery(
			"select "
				+ "concat(value, ' ', value), "
				+ "count(id)"
			+ "from "
				+ "TestBean testBean "
			+ "group by concat(value, ' ', value)");


		Object[] result = (Object[]) query.getSingleResult();

		assertArrayEquals(new Object[]{"value value", 1L}, result);
	}

	/**
	 * Writing the above query in raw HQL is invalid - why are we unable to use an alias here?
	 */
	@Test
	public void testSimpleGroupByWithAliasConcatUsingHQL()
	{
		// ' ' used to add a space between the two concatenated values
		Query query = entityManager.createQuery(
			"select "
				+ "concat(value, ' ', value) as mygrouping, "
				+ "count(id)"
			+ "from "
				+ "TestBean testBean "
			+ "group by mygrouping");


		Object[] result = (Object[]) query.getSingleResult();

		assertArrayEquals(new Object[]{"value value", 1L}, result);
	}

	@Test
	public void testSimpleGroupByWithConcatWorking()
	{
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		
		CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createQuery(Tuple.class);
		Root<TestBean> root = criteriaQuery.from(TestBean.class);
		
		Path<String> pathA = root.get("value");
		Path<String> pathB = root.get("value");
		
		// Literal included in the concatenation - space character to separate values
		Expression<String> expression = criteriaBuilder.concat(pathA, " ");
		expression = criteriaBuilder.concat(expression, pathB);
		
		Expression<Long> count = criteriaBuilder.count(root);
		
		criteriaQuery.multiselect(expression, count);
		
		// Grouping by the root entity (id) has been added to make query valid
		criteriaQuery.groupBy(root.get("id"), expression);
		
		TypedQuery<Tuple> typedQuery = entityManager.createQuery(criteriaQuery);
		
		List<Tuple> testBeans = typedQuery.getResultList();
		
		List<?> expectedResults = asList(asList("value value", 1L));
		
		assertResultsEqual(expectedResults, testBeans);
	}
	
	@Test
	public void testComplexGroupByWithConcatNotWorking()
	{
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		
		CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createQuery(Tuple.class);
		Root<TestBean> root = criteriaQuery.from(TestBean.class);
		
		Path<String> pathA = root.get("anotherBean").get("value");
		Path<String> pathB = root.get("anotherBean").get("value");
		
		// Literal included in the concatenation - space character to separate values
		Expression<String> expression = criteriaBuilder.concat(pathA, " ");
		expression = criteriaBuilder.concat(expression, pathB);
		
		Expression<Long> count = criteriaBuilder.count(root);
		
		criteriaQuery.multiselect(expression, count);
		
		// Only the concat function is included in the 'group by' method
		criteriaQuery.groupBy(expression);
		
		TypedQuery<Tuple> typedQuery = entityManager.createQuery(criteriaQuery);
		
		List<Tuple> testBeans = typedQuery.getResultList();
		
		List<?> expectedResults = asList(asList("string string", 1L));
		
		assertResultsEqual(expectedResults, testBeans);
	}
	
	@Test
	public void testComplexGroupByWithConcatNotWorkingWithGroupBy()
	{
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		
		CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createQuery(Tuple.class);
		Root<TestBean> root = criteriaQuery.from(TestBean.class);
		
		Path<String> pathA = root.get("anotherBean").get("value");
		Path<String> pathB = root.get("anotherBean").get("value");
		
		// Literal included in the concatenation - space character to separate values
		Expression<String> expression = criteriaBuilder.concat(pathA, " ");
		expression = criteriaBuilder.concat(expression, pathB);
		
		Expression<Long> count = criteriaBuilder.count(root);
		
		criteriaQuery.multiselect(expression, count);
		
		// Grouping by the root entity (id) has been added here, but the test still fails
		// The query is still said to be invalid because part of the concat function does not appear in the 'group by'
		criteriaQuery.groupBy(root.get("id"), expression);
		
		TypedQuery<Tuple> typedQuery = entityManager.createQuery(criteriaQuery);
		
		List<Tuple> testBeans = typedQuery.getResultList();
		
		List<?> expectedResults = asList(asList("string string", 1L));
		
		assertResultsEqual(expectedResults, testBeans);
	}
	
	@Test
	public void testComplexGroupByWithConcatWorking()
	{
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		
		CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createQuery(Tuple.class);
		Root<TestBean> root = criteriaQuery.from(TestBean.class);
		
		Path<Object> anotherBeanPath = root.join("anotherBean");
		
		Path<String> pathA = anotherBeanPath.get("value");
		Path<String> pathB = anotherBeanPath.get("value");
		
		// Literal included in the concatenation - space character to separate values
		Expression<String> expression = criteriaBuilder.concat(pathA, " ");
		expression = criteriaBuilder.concat(expression, pathB);
		
		Expression<Long> count = criteriaBuilder.count(root);
		
		criteriaQuery.multiselect(expression, count);
		
		// Grouping by the 'anotherBean' entity (which contains the properties being concatenated) has been added here)
		// The query is now valid, and the test passes
		criteriaQuery.groupBy(anotherBeanPath, expression);
		
		TypedQuery<Tuple> typedQuery = entityManager.createQuery(criteriaQuery);
		
		List<Tuple> testBeans = typedQuery.getResultList();
		
		List<?> expectedResults = asList(asList("string string", 1L));
		
		assertResultsEqual(expectedResults, testBeans);
	}
	
	/* Using criteriaBuilder.literal, to create an expression for a literal, results in the same outcome */
	
	@Test
	public void testSimpleGroupByWithConcatNotWorkingUsingLiteralForString()
	{
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		
		CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createQuery(Tuple.class);
		Root<TestBean> root = criteriaQuery.from(TestBean.class);
		
		Path<String> pathA = root.get("value");
		Path<String> pathB = root.get("value");
		
		Expression<String> string = criteriaBuilder.literal(" ");
		
		Expression<String> expression = criteriaBuilder.concat(pathA, string);
		expression = criteriaBuilder.concat(expression, pathB);
		
		Expression<Long> count = criteriaBuilder.count(root);
		
		criteriaQuery.multiselect(expression, count);
		
		// Only the concat function is included in the 'group by' method
		criteriaQuery.groupBy(expression);
		
		TypedQuery<Tuple> typedQuery = entityManager.createQuery(criteriaQuery);
		
		List<Tuple> testBeans = typedQuery.getResultList();
		
		List<?> expectedResults = asList(asList("value value", 1L));
		
		assertResultsEqual(expectedResults, testBeans);
	}
	
	@Test
	public void testSimpleGroupByWithConcatWorkingUsingLiteralForString()
	{
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		
		CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createQuery(Tuple.class);
		Root<TestBean> root = criteriaQuery.from(TestBean.class);
		
		Path<String> pathA = root.get("value");
		Path<String> pathB = root.get("value");
		
		Expression<String> string = criteriaBuilder.literal(" ");
		
		Expression<String> expression = criteriaBuilder.concat(pathA, string);
		expression = criteriaBuilder.concat(expression, pathB);
		
		Expression<Long> count = criteriaBuilder.count(root);
		
		criteriaQuery.multiselect(expression, count);
		
		// Grouping by the root entity (id) has been added to make query valid
		criteriaQuery.groupBy(root.get("id"), expression);
		
		TypedQuery<Tuple> typedQuery = entityManager.createQuery(criteriaQuery);
		
		List<Tuple> testBeans = typedQuery.getResultList();
		
		List<?> expectedResults = asList(asList("value value", 1L));
		
		assertResultsEqual(expectedResults, testBeans);
	}
	
	private void assertResultsEqual(List<?> expected, List<Tuple> actual)
	{
		assertEquals(expected.size(), actual.size());
		
		List<?> row;
		
		for (int i = 0; i < actual.size(); i++)
		{
			row = (List<?>) expected.get(i);
			
			assertArrayEquals(row.toArray(), actual.get(i).toArray());
		}
	}
	/*

    select
        (anotherbea1_.value||?||anotherbea1_.value) as col_0_0_,
        count(testbean0_.id) as col_1_0_
    from
        TestBean testbean0_,
        AnotherBean anotherbea1_
    where
        testbean0_.anotherBean_id=anotherbea1_.id
    group by
        testbean0_.id ,
        (anotherbea1_.value||?||anotherbea1_.value)

        -----

    select
        (anotherbea1_.value||?||anotherbea1_.value) as col_0_0_,
        count(testbean0_.id) as col_1_0_
    from
        TestBean testbean0_,
        AnotherBean anotherbea1_
    where
        testbean0_.anotherBean_id=anotherbea1_.id
    group by
        testbean0_.id ,
        (anotherbea1_.value||?||anotherbea1_.value)

	 */
}
