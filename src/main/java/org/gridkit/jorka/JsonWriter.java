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

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

class JsonWriter {

	private static final int CONTROL_CHARACTERS_START = 0x0000;
	private static final int CONTROL_CHARACTERS_END = 0x001f;

	private static final char[] QUOT_CHARS = { '\\', '"' };
	private static final char[] BS_CHARS = { '\\', '\\' };
	private static final char[] LF_CHARS = { '\\', 'n' };
	private static final char[] CR_CHARS = { '\\', 'r' };
	private static final char[] TAB_CHARS = { '\\', 't' };
	// In JavaScript, U+2028 and U+2029 characters count as line endings and
	// must be encoded.
	// http://stackoverflow.com/questions/2965293/javascript-parse-error-on-u2028-unicode-character
	private static final char[] UNICODE_2028_CHARS = { '\\', 'u', '2', '0',
			'2', '8' };
	private static final char[] UNICODE_2029_CHARS = { '\\', 'u', '2', '0',
			'2', '9' };
	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	protected final Writer writer;

	public JsonWriter(Writer writer) {
		this.writer = writer;
	}

	@SuppressWarnings("unchecked")
	public void writeMap(Map<String, ?> value) throws IOException {
		writeObject((Map<String, Object>) value);
	}
	
	void write(String string) throws IOException {
		writer.write(string);
	}

	void writeString(String string) throws IOException {
		writer.write('"');
		int length = string.length();
		int start = 0;
		char[] chars = new char[length];
		string.getChars(0, length, chars, 0);
		for (int index = 0; index < length; index++) {
			char[] replacement = getReplacementChars(chars[index]);
			if (replacement != null) {
				writer.write(chars, start, index - start);
				writer.write(replacement);
				start = index + 1;
			}
		}
		writer.write(chars, start, length - start);
		writer.write('"');
	}

	private static char[] getReplacementChars(char ch) {
		char[] replacement = null;
		if (ch == '"') {
			replacement = QUOT_CHARS;
		} else if (ch == '\\') {
			replacement = BS_CHARS;
		} else if (ch == '\n') {
			replacement = LF_CHARS;
		} else if (ch == '\r') {
			replacement = CR_CHARS;
		} else if (ch == '\t') {
			replacement = TAB_CHARS;
		} else if (ch == '\u2028') {
			replacement = UNICODE_2028_CHARS;
		} else if (ch == '\u2029') {
			replacement = UNICODE_2029_CHARS;
		} else if (ch >= CONTROL_CHARACTERS_START
				&& ch <= CONTROL_CHARACTERS_END) {
			replacement = new char[] { '\\', 'u', '0', '0', '0', '0' };
			replacement[4] = HEX_DIGITS[ch >> 4 & 0x000f];
			replacement[5] = HEX_DIGITS[ch & 0x000f];
		}
		return replacement;
	}

	protected void writeObject(Map<String, Object> object) throws IOException {
		writeBeginObject();
		boolean first = true;
		for (Map.Entry<String, Object> entry: object.entrySet()) {
			if (!first) {
				writeObjectValueSeparator();
			}
			writeString(entry.getKey());
			writeNameValueSeparator();
			writeValue(entry.getValue());
			first = false;
		}
		writeEndObject();
	}

	@SuppressWarnings("unchecked")
	private void writeValue(Object value)
			throws IOException {
		if (value instanceof String) {
			writeString((String)value);
		}
		else if (value instanceof Number) {
			writeString(String.valueOf(value));
		}
		else if (value instanceof Collection) {
			writeArray((Collection<Object>)value);
		}
		else if (value instanceof Object[]) {
			writeArray(Arrays.asList((Object[])value));
		}
		else if (value instanceof Map) {
			writeObject((Map<String, Object>)value);
		}
		else if (value == null) {
			write("null");
		}
		else if (value instanceof Boolean) {
			write(String.valueOf(((Boolean)value).booleanValue()));
		}
		else {
			throw new IllegalArgumentException("Unsuported type: " + value);
		}
	}

	protected void writeBeginObject() throws IOException {
		writer.write('{');
	}

	protected void writeEndObject() throws IOException {
		writer.write('}');
	}

	protected void writeNameValueSeparator() throws IOException {
		writer.write(':');
	}

	protected void writeObjectValueSeparator() throws IOException {
		writer.write(',');
	}

	protected void writeArray(Collection<Object> array) throws IOException {
		writeBeginArray();
		boolean first = true;
		for (Object value : array) {
			if (!first) {
				writeArrayValueSeparator();
			}
			writeValue(value);
			first = false;
		}
		writeEndArray();
	}

	protected void writeBeginArray() throws IOException {
		writer.write('[');
	}

	protected void writeEndArray() throws IOException {
		writer.write(']');
	}

	protected void writeArrayValueSeparator() throws IOException {
		writer.write(',');
	}

}