/**
 * Copyright 2013 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jorka;

import org.gridkit.jorka.Jorka.Match;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class JorkaTest {

	@Test
	public void test_simple_pattern() throws Throwable {
		Jorka g = new Jorka();

		g.addPatternFromFile("src/test/resources/patterns/base");
		g.compile("%{WORD:NAME}: %{NUMBER:TIME}ms");
		Match gm = g.match("A: 1.0ms");
		gm.parse();
		// See the result
		System.out.println(gm.toJSON());
	}

	@Test
	public void test_apache_pattern() throws Throwable {
		Jorka g = new Jorka();
		
		g.addPatternFromFile("src/test/resources/patterns/base");
		g.compile("%{APACHE:XXX}");
		Match gm = g.match("10.192.1.47 - - [23/May/2013:10:47:40 0] \"GET /flower1_store/category1.screen?category_id1=FLOWERS HTTP/1.1\" 200 10577 \"http://mystore.abc.com/flower1_store/main.screen&JSESSIONID=SD1SL10FF3ADFF3\" \"Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.8.0.10) Gecko/20070223 CentOS/1.5.0.10-0.1.el4.centos Firefox/1.5.0.10\" 3823 404");
		gm.parse();
		// See the result
		System.out.println(gm.toJSON());
	}
}