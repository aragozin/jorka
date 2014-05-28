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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

public class Jorka extends Object {

    private static Pattern META_CHARACTERS = Pattern.compile("\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\]\\}\\)\\?\\*\\+\\.\\>\\s:");
	private static Pattern TEMPLATE_PATTERN = Pattern.compile("(\\%[{][^}]*[}])|(\\s+)|([" + META_CHARACTERS + "])|([^" + META_CHARACTERS + "\\%]+)");
	
	/**
	 * <p>
	 * Simple template format is less error prone in case if you want to
	 * make pattern from text to be matched.
	 * </p>
	 * <p>
	 * Template is converted to Java regular expression with following transformations:
	 * <li>Special characters (dots, parenthesis, asterisks, etc) are replaced by their literals</li>
	 * <li>Spaces are replaced with "\s+"</li>
	 * <li>%{...} constructs are retained</li>
	 * </p>
	 */
	public static String simpleTemplateToRegEx(String template) {
		StringBuilder sb = new StringBuilder();
		Matcher m = TEMPLATE_PATTERN.matcher(template);
		int n = 0;
		while(m.find(n)) {
			if (m.start() != n) {
				throw new IllegalArgumentException("Cannot tokenize [" + template + "]");
			}
			String match = m.group();
			if (m.group(1) != null) {
				sb.append(match);
			}
			else if (m.group(2) != null) {
				sb.append("\\s+");
			}
			else if (m.group(3) != null) {
				sb.append('\\').append(match);
			}
			else {
				sb.append(match);
			}
			n = m.end();
		}
		return sb.toString();
	}
	
	private static Pattern PATTERN_RE = Pattern.compile("%\\{(?<name>(?<pattern>[A-z0-9]+)(?::(?<subname>[A-z0-9_:]+))?)(?:=(?<definition>(?:(?:[^{}]+|\\.+)+)+))?\\}");

	private Map<String, String> patterns;

	private Map<String, String> capturedMap;

	private String expandedPattern;
	private Pattern regexp;

	public Jorka() {
		expandedPattern = null;
		regexp = null;
		patterns = new TreeMap<String, String>();
		capturedMap = new TreeMap<String, String>();
	}
	
	public Jorka copyPatterns() {
		Jorka j = new Jorka();
		j.patterns.putAll(this.patterns);
		return j;
	}
	
	public void addPattern(String name, String pattern) {
	    String ep = expand(pattern);
	    try {
	        Pattern.compile(ep);
	    }
	    catch(Exception e) {
	        throw new IllegalArgumentException("Cannot compile: " + pattern, e);
	    }
		patterns.put(name, pattern);
	}

	public void addPatterns(Map<String, String> cpy) {
		for (Map.Entry<String, String> entry : cpy.entrySet()) {
			patterns.put(entry.getKey().toString(), entry.getValue().toString());
		}
	}

	/**
	 * @return currently added patterns
	 */
	public Map<String, String> getPatterns() {
		return this.patterns;
	}

	/**
	 * @return the compiled regex of <tt>expanded_pattern</tt>
	 * @see compile
	 */
	public Pattern getRegEx() {
		return regexp;
	}

	/**
	 * 
	 * @return the string pattern
	 * @see compile
	 */
	public String getExpandedPattern() {
		return expandedPattern;
	}

	/**
	 * Add patterns from a file 
	 */
	public void addPatternFromFile(String file) throws IOException {

		File f = new File(file);
		addPatternFromReader(new FileReader(f));
	}

	/**
	 * Add patterns  from a reader
	 */
	public void addPatternFromReader(Reader r) throws IOException {
		BufferedReader br = new BufferedReader(r);
		String line;
		// We dont want \n and commented line
		Pattern MY_PATTERN = Pattern.compile("^([A-z0-9_]+)([~]?)\\s+(.*)$");
		while ((line = br.readLine()) != null) {
			Matcher m = MY_PATTERN.matcher(line);
			if (m.matches()) {
			    if (m.group(2).length() > 0) {
			        // process line as simple template
			        this.addPattern(m.group(1), simpleTemplateToRegEx(m.group(3)));
			    }
			    else {
			        this.addPattern(m.group(1), m.group(3));
			    }
			}
		}
		br.close();
	}

	/**
	 * Match the <tt>text</tt> with the pattern
	 * 
	 * @param text to match
	 * @see Match
	 */
	public Match find(String text) {

		if (regexp == null)
			return null;

		Matcher m = regexp.matcher(text);
		if (m.find()) {
			Match match = new Match();
			match.setText(text);
			match.match = m;
			match.start = m.start(0);
			match.end = m.end(0);
			match.line = text;
			return match;
		}
		else {
			return null;
		}
	}

	/**
	 * Match the <tt>text</tt> with the pattern
	 * 
	 * @param text to match
	 * @see Match
	 */
	public Match match(String text) {
	    
	    if (regexp == null)
	        return null;
	    
	    Matcher m = regexp.matcher(text);
	    if (m.matches()) {
	        Match match = new Match();
	        match.setText(text);
	        match.match = m;
	        match.start = m.start(0);
	        match.end = m.end(0);
	        match.line = text;
	        return match;
	    }
	    else {
	        return null;
	    }
	}

	/**
	 * Transform Jorka regex into a compiled regex
	 */
	public void compile(String pattern) {
		this.expandedPattern = expand(pattern);
		
		// Compile the regex
		if (!expandedPattern.isEmpty()) {
			regexp = Pattern.compile(expandedPattern);
		} else {
			throw new IllegalArgumentException("Pattern is not found '"
					+ pattern + "'");
		}
	}

    private String expand(String pattern) {
        String expandedPattern = new String(pattern);
		int index = 0;
		Boolean Continue = true;

		// Replace %{foo} with the regex (mostly groupname regex)
		// and then compile the regex
		while (Continue) {
			Continue = false;

			Matcher m = PATTERN_RE.matcher(expandedPattern);
			// Match %{Foo:bar} -> pattern name and subname
			// Match %{Foo=regex} -> add new regex definition
			if (m.find()) {
				Continue = true;
				Map<String, String> group = m.namedGroups();

				if (group.get("definition") != null) {
					addPattern(group.get("pattern"), group.get("definition"));
					group.put("name",
							group.get("name") + "=" + group.get("definition"));
				}
				capturedMap.put("name" + index,
						(group.get("subname") != null ? group.get("subname")
								: group.get("name")));
				expandedPattern = expandedPattern.replace((CharSequence) "%{"
						+ group.get("name") + "}", "(?<name" + index + ">"
						+ this.patterns.get(group.get("pattern")) + ")");
				index++;
			}
		}
        return expandedPattern;
    }

	public Map<String, String> getCaptured() {
		return capturedMap;
	}

	public int isPattern() {
		if (patterns == null)
			return 0;
		if (patterns.isEmpty())
			return 0;
		return 1;
	}
	
	public class Match {

		private Matcher match;
		@SuppressWarnings("unused")
		private int start;
		@SuppressWarnings("unused")
		private int end;
		@SuppressWarnings("unused")
		private String line;
		private Garbage garbage;

		private String text;
		private Map<String, Object> capture;

		public Match() {
			text = "Nothing";
			match = null;
			capture = new TreeMap<String, Object>();
			garbage = new Garbage();
			start = 0;
			end = 0;
		}

		void setText(String text) {
			if (text == null || text.isEmpty()) {
				throw new IllegalArgumentException("subject should not be empty");
			}
			this.text = text;
		}

		public String getText() {
			return text;
		}

		/**
		 * Match to the <tt>subject</tt> the <tt>regex</tt> and save the matched
		 * element into a map
		 * 
		 * @see toMap
		 * @see toJson
		 */
		public void parse() {
			if (this.match == null) {
				throw new IllegalStateException("Not matched yet");
			}

			Map<String, String> mappedw = this.match.namedGroups();
			Iterator<Entry<String, String>> it = mappedw.entrySet().iterator();
			while (it.hasNext()) {

				Entry<String, String> pairs = it.next();
				String key = null;
				Object value = null;
				if (!getCaptured().get(pairs.getKey()).isEmpty()) {
					key = getCaptured().get(pairs.getKey());
				}
				if (pairs.getValue() != null) {
					value = pairs.getValue();
					if (this.isInteger((String)value))
						value = Integer.parseInt((String)value);
					else
						value = cleanString(pairs.getValue().toString());
				}
				if (value != null || !capture.containsKey(key)) {
				    capture.put(key, (Object) value);
				}
				it.remove();
			}
		}

		private String cleanString(String value) {
			if (value == null || value.isEmpty()) {
				return value;
			}
			char[] tmp = value.toCharArray();
			if ((tmp[0] == '"' && tmp[value.length() - 1] == '"')
					|| (tmp[0] == '\'' && tmp[value.length() - 1] == '\'')) {
				value = value.substring(1, value.length() - 1);
			}
			return value;
		}

		/**
		 * @return java map object from the matched element in the text
		 */
		public Map<String, Object> toMap() {
			this.clean();
			return capture;
		}

		/**
		 * @return java map object from the matched element in the text
		 */
		public String toJSON() {
			StringWriter writer = new StringWriter();
			try {
				new JsonWriter(writer).writeMap(capture);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return writer.toString();
		}

		private void clean() {
		    if (garbage != null) {
    			garbage.rename(capture);
    			garbage.remove(capture);
		    }
		    garbage = null;
		}

		public Boolean isNull() {
			if (this.match == null)
				return true;
			return false;
		}

		// TODO use pattern
		private boolean isInteger(String s) {
			try {
				Integer.parseInt(s);
			} catch (NumberFormatException e) {
				return false;
			}
			return true;
		}
	}
	
	static class Garbage {

		private final List<String> remove;
		private final Map<String, Object> rename;

		Garbage() {

			remove = new ArrayList<String>();
			rename = new TreeMap<String, Object>();
			remove.add("UNWANTED");

		}

		void addToRename(String key, Object value) {
			if (key == null) {
				throw new NullPointerException("key is null");
			}
			if (value == null) {
				throw new NullPointerException("value is null");
			}
			if (!key.isEmpty() && !value.toString().isEmpty()) {
				rename.put(key, value);
			}
		}

		void addToRemove(String item) {
			if (item == null) {
				throw new NullPointerException("item is null");
			}
			if (!item.isEmpty()) {
				remove.add(item);
			}
		}

		void addFromListRemove(List<String> list) {
			if (list == null) {
				throw new NullPointerException("list is null");
			}
			if (!list.isEmpty()) {
				remove.addAll(list);
			}
		}

		void remove(Map<String, Object> map) {

			if (map != null) {
				for (Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext();) {
					Map.Entry<String, Object> entry = it.next();
					for (int i = 0; i < remove.size(); i++)
						if (entry.getKey().equals(remove.get(i))) {
							it.remove();
						}
				}
			}
		}

		void rename(Map<String, Object> map) {

			if (map != null) {
				for (Iterator<Map.Entry<String, Object>> it = rename.entrySet().iterator(); it.hasNext();) {
					Map.Entry<String, Object> entry = it.next();
					if (map.containsKey(entry.getKey())) {
						Object obj = map.remove(entry.getKey());
						map.put(entry.getValue().toString(), obj);
					}
				}
			}
		}
	}	
}
