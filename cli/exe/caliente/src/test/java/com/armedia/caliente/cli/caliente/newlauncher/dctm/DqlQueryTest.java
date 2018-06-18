package com.armedia.caliente.cli.caliente.newlauncher.dctm;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.caliente.cli.caliente.newlauncher.dctm.DqlQuery;
import com.armedia.caliente.cli.caliente.newlauncher.dctm.DqlQuery.Clause;
import com.armedia.caliente.cli.caliente.newlauncher.dctm.DqlQuery.ClauseGenerator;

public class DqlQueryTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDqlQuery() throws Exception {
		DqlQuery q = new DqlQuery(
			" leading manure                    select      main    \n\n\n\t       crap        from   crapola       in     document a     in assembly bblblb  search gravity group by     stench having          weight       union select other           from more crap   union    yet more crap to be select on manure      order by something ENABLE     the rest of the manure");
		String str = q.toString();
		String STR = "leading manure SELECT main    \n\n\n\t       crap FROM crapola IN DOCUMENT a IN ASSEMBLY bblblb SEARCH gravity GROUP BY stench HAVING weight UNION SELECT other FROM more crap UNION yet more crap to be SELECT on manure ORDER BY something ENABLE the rest of the manure";
		Assert.assertEquals(STR, str);
		q = new DqlQuery(
			" leading manure                    select      main    \n\n\n\t       crap        from   crapola       in     document a     in assembly bblblb  search gravity where grouped condition group by     stench having          weight       union select other           from more crap   union    yet more crap to be select on manure      order by something ENABLE     the rest of the manure");
		STR = "leading manure SELECT main    \n\n\n\t       crap FROM crapola IN DOCUMENT a IN ASSEMBLY bblblb SEARCH gravity WHERE [grouped condition] and extra GROUP BY stench HAVING weight UNION SELECT other FROM more crap UNION yet more crap to be SELECT on manure ORDER BY something ENABLE the rest of the manure";
		str = q.toString(new ClauseGenerator() {

			@Override
			public String generate(int nestLevel, Clause clause, String data) {
				if (clause == Clause.WHERE) { return String.format("%s [%s] and extra", clause, data); }
				return super.generate(nestLevel, clause, data);
			}

		});
		Assert.assertEquals(STR, str);
	}

}
