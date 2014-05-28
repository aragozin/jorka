package org.gridkit.jorka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@FixMethodOrder(MethodSorters.JVM)
@RunWith(Parameterized.class)
public class SimpleTemplateTest {

	@Parameters(name = "{0}")
	public static Collection<Object[]> getCases() {
		List<Object[]> cases = new ArrayList<Object[]>();
		cases.add(new Object[]{"Match this", "Match\\s+this"});
		cases.add(new Object[]{"Match this. Match that!", "Match\\s+this\\.\\s+Match\\s+that\\!"});
		cases.add(new Object[]{"Match %{THIS}. Match that!", "Match\\s+%{THIS}\\.\\s+Match\\s+that\\!"});
		cases.add(new Object[]{"Match (me) too.", "Match\\s+\\(me\\)\\s+too\\."});
		cases.add(new Object[]{"Even :%{THIS} %{THIS:this} and %{THAT}: should be matched", "Even\\s+\\:%{THIS}\\s+%{THIS:this}\\s+and\\s+%{THAT}\\:\\s+should\\s+be\\s+matched"});
		cases.add(new Object[]{"and @%{THIS} %{TOO}@", "and\\s+@%{THIS}\\s+%{TOO}@"});
		cases.add(new Object[]{"and THIS%{TOO}", "and\\s+THIS%{TOO}"});
		return cases;
	}
	
	String template;
	String regEx;

	public SimpleTemplateTest(String template, String regEx) {
		this.template = template;
		this.regEx = regEx;
	}

	@Test
	public void verify_regex() {
		Assert.assertEquals(regEx, Jorka.simpleTemplateToRegEx(template));
	}
}
