package com.adobe.dp.css;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

public class CSSParser {

	String encoding;

	Reader reader;

	int lineCount;

	boolean afterCR;

	int savedLineCount;

	boolean savedAfterCR;

	Vector errors;

	String defaultNamespace;

	Hashtable namespaces = new Hashtable();

	CSSURLFactory cssurlFactory;

	private static byte[] utf8_mark = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

	private static byte[] utf16be_mark = { (byte) 0xFE, (byte) 0xFF };

	private static byte[] utf16le_mark = { (byte) 0xFF, (byte) 0xFE };

	private static byte[] at_charset = { 40, 63, 68, 61, 72, 73, 65, 74, 20, 22 };

	private void reportError(String err) {
		reportError(lineCount, err);
	}

	private void reportError(int line, String err) {
		if (errors == null)
			errors = new Vector();
		errors.add(new CSSParsingError(line, err));
	}

	public void setCSSURLFactory(CSSURLFactory factory) {
		cssurlFactory = factory;
	}

	private boolean matches(byte[] sniff, byte[] sig) {
		for (int i = 0; i < sig.length; i++)
			if (sig[i] != sniff[i])
				return false;
		return true;
	}

	private void setReader(Reader in) throws IOException {
		this.lineCount = 1;
		this.encoding = null;
		if (in.markSupported())
			this.reader = in;
		else
			this.reader = new BufferedReader(in);
	}

	private void setReader(InputStream in) throws IOException {
		this.lineCount = 1;
		if (encoding == null) {
			encoding = "UTF-8";
			if (!in.markSupported())
				in = new BufferedInputStream(in);
			byte[] sniff = new byte[64];
			in.mark(sniff.length);
			in.read(sniff);
			in.reset();
			if (matches(sniff, utf8_mark)) {
				// indeed, UTF-8
			} else if (matches(sniff, utf16be_mark) || matches(sniff, utf16le_mark)) {
				encoding = "UTF-16";
			} else if (sniff[0] == 0 && sniff[1] != 0) {
				encoding = "UnicodeBigUnmarked";
			} else if (sniff[0] != 0 && sniff[1] == 0) {
				encoding = "UnicodeLittleUnmarked";
			} else if (matches(sniff, at_charset)) {
				StringBuffer sb = new StringBuffer();
				for (int i = at_charset.length;; i++) {
					if (i >= sniff.length) {
						reportError("AT_CHARSET_SYNTAX");
						break;
					}
					char c = (char) (sniff[i] & 0xFF);
					if (c == '"') {
						encoding = sb.toString();
						break;
					}
					if (c >= 0x80) {
						reportError("AT_CHARSET_SYNTAX");
						break;
					}
					sb.append(c);
				}
			}
		}
		this.reader = new BufferedReader(new InputStreamReader(in, encoding));
	}

	private void mark(int readAheadLimit) throws IOException {
		reader.mark(readAheadLimit);
		savedLineCount = lineCount;
		savedAfterCR = afterCR;
	}

	private int read() throws IOException {
		int ch = reader.read();
		if (ch == '\r') {
			lineCount++;
			afterCR = true;
		} else {
			if (ch == '\n') {
				if (!afterCR)
					lineCount++;
			}
			afterCR = false;
		}
		return ch;
	}

	private void reset() throws IOException {
		reader.reset();
		lineCount = savedLineCount;
		afterCR = savedAfterCR;
	}

	private void skipToEndOfComment() throws IOException {
		while (true) {
			int ch = read();
			if (ch < 0)
				return;
			while (ch == '*') {
				ch = read();
				if (ch == '/' || ch < 0)
					return;
			}
		}
	}

	private int skipWhitespace() throws IOException {
		while (true) {
			mark(1);
			int ch = read();
			if (ch < 0)
				return ch;
			if (ch == '/') {
				reset();
				mark(2);
				read();
				ch = read();
				if (ch == '*') {
					// comment
					skipToEndOfComment();
					continue;
				}
				reset();
				return '/';
			}
			if (ch != ' ' && ch != '\n' && ch != '\r' && ch != '\t' && ch != '\f') {
				reset();
				return ch;
			}
		}
	}

	private void skipToEnd(boolean blockStarted) throws IOException {
		// CSS spec talks about matching pairs of () and [] - but it is not
		// clear what to do with them, exactly
		int counter = (blockStarted ? 1 : 0);
		char quote = '\0';
		while (!blockStarted || counter > 0) {
			int ch = read();
			if (ch < 0)
				return;
			switch (ch) {
			case ';':
				if (!blockStarted)
					return;
				break;
			case '{':
				if (quote == '\0') {
					counter++;
					blockStarted = true;
				}
				break;
			case '}':
				if (quote == '\0')
					counter--;
				break;
			case '\\':
				readEscape();
				break;
			case '\'':
			case '"':
				if (quote == ch)
					quote = '\0'; // end of string
				else if (quote == '\0')
					quote = (char) ch; // beginning of string
				break;
			case '/':
				if (quote == '\0') {
					mark(1);
					ch = read();
					if (ch == '*')
						skipToEndOfComment();
					else
						reset();
					break;
				}
			case '\n':
			case '\r':
				quote = '\0';
				break;
			}
		}
	}

	private int hexValue(int ch) {
		if ('0' <= ch && ch <= '9')
			return ch - '0';
		if ('a' <= ch && ch <= 'f')
			return ch - ('a' - 10);
		if ('A' <= ch && ch <= 'F')
			return ch - ('A' - 10);
		return -1;
	}

	private int readEscape() throws IOException {
		int ch = read();
		if (ch == '\n')
			return 0;
		if (ch == '\r') {
			mark(1);
			ch = read();
			if (ch != '\n')
				reset();
			return 0;
		}
		if (hexValue(ch) >= 0) {
			int unival = hexValue(ch);
			for (int i = 0; i < 6; i++) {
				mark(1);
				ch = read();
				if (ch < 0 || ch == ' ' || ch == '\n')
					break;
				if (ch == '\r') {
					mark(1);
					ch = read();
					if (ch != '\n')
						reset();
					break;
				}
				int hv = hexValue(ch);
				if (hv < 0) {
					reset();
					break;
				}
				unival = (unival << 4) | hv;
			}
			ch = unival;
		}
		return ch;
	}

	private String readName() throws IOException {
		StringBuffer sb = new StringBuffer();
		while (true) {
			mark(1);
			int ch = read();
			if (ch < 0) {
				if( sb.length() > 0 )
					return sb.toString();					
				return null;
			}
			if (ch == '\\') {
				ch = readEscape();
				if (ch == 0)
					continue;
				if (ch < 0)
					return null;
			} else if (('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9') || ch == '-'
					|| ch == '_' || ch >= 0x80) {
				// good
			} else {
				reset();
				if (sb.length() == 0) {
					reportError("NAME_EXPECTED");
					return null;
				}
				return sb.toString();
			}
			sb.append((char) ch);
		}
	}

	private String readQuoted() throws IOException {
		int fin = read();
		if (fin != '\'' && fin != '"')
			throw new RuntimeException("Not quoted!");
		StringBuffer sb = new StringBuffer();
		while (true) {
			int ch = read();
			if (ch < 0 || ch == fin)
				break;
			if (ch == '\\') {
				ch = readEscape();
				if (ch == 0)
					continue;
				if (ch < 0)
					return null;
			}
			if (ch == '\n' || ch == '\r') {
				reportError(lineCount - 1, "UNTERMINATED_STRING");
				break;
			}
			sb.append((char) ch);
		}
		return sb.toString();
	}

	private CSSValue readSingleValue() throws IOException {
		int ch = skipWhitespace();
		switch (ch) {
		case '\'':
		case '"': {
			return new CSSQuotedString(readQuoted());
		}
		case '#': {
			read();
			int count = 0;
			int rgb = 0;
			while (count < 6) {
				mark(1);
				ch = read();
				int hv = hexValue(ch);
				if (hv < 0) {
					reset();
					break;
				}
				rgb = (rgb << 4) | hv;
				count++;
			}
			if (count == 3) {
				// normal case
				int b1 = rgb & 0xF;
				int g1 = (rgb >> 4) & 0xF;
				int r1 = (rgb >> 8) & 0xF;
				rgb = (r1 << 20) | (r1 << 16) | (g1 << 12) | (g1 << 8) | (b1 << 4) | b1;
			} else if (count == 6) {
				// normal case
			} else {
				// error
				reportError("HASH_COLOR");
				return null;
			}
			return new CSSRGBColor(rgb);
		}
		default: {
			mark(2);
			read();
			int c1 = read();
			reset();
			if (('0' <= ch && ch <= '9') || ch == '.' || (ch == '-' && (('0' <= c1 && c1 <= '9') || c1 == '.'))) {
				// number or CSS length
				StringBuffer sb = new StringBuffer();
				sb.append((char)ch);
				read();
				boolean fp = ch == '.';
				while (true) {
					mark(1);
					ch = read();
					if (ch == '.') {
						if (fp) {
							reportError("NUMBER_SYNTAX");
							return null;
						}
						fp = true;
					} else if ('0' > ch || ch > '9'){
						break;
					}
					sb.append((char) ch);
				}
				Number result;
				try {
					if (fp)
						result = new Double(sb.toString());
					else
						result = new Integer(sb.toString());
				} catch (Exception e) {
					reportError("NUMBER_SYNTAX");
					return null;
				}
				if (ch == '%')
					return new CSSLength(result.doubleValue(), "%");
				if (('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z')) {
					reset();
					String unit = readName();
					return new CSSLength(result.doubleValue(), unit.toLowerCase());
				}
				reset();
				return new CSSNumber(result);
			} else if (ch == '-' || ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ch >= 0x80 || ch == '\\'
					|| ch == '_') {
				String ident = readName();
				mark(1);
				ch = read();
				if (ch != '(') {
					reset();
					return new CSSName(ident);
				}
				ident = ident.toLowerCase();
				if (ident.equals("url")) {
					String url;
					ch = skipWhitespace();
					if (ch == '\'' || ch == '"') {
						url = readQuoted();
						ch = skipWhitespace();
						if (ch != ')') {
							reportError("URL_SYNTAX");
							return null;
						}
						read();
					} else {
						StringBuffer sb = new StringBuffer();
						int size = -1;
						int line = lineCount;
						while (true) {
							ch = read();
							if (ch < 0) {
								reportError(line, "URL_SYNTAX");
								break;
							}
							if (ch == ')')
								break;
							if (ch == '\\') {
								ch = readEscape();
								if (ch == 0)
									continue;
								if (ch < 0)
									return null;
							} else if (ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r' && ch != '\f') {
								size = sb.length();
							}
							sb.append((char) ch);
						}
						sb.setLength(size + 1); // remove trailing whitespace
						url = sb.toString();
					}
					if (cssurlFactory != null)
						return cssurlFactory.createCSSURL(url);
					return new CSSURL(url);
				} else {
					CSSValue[] params = readParams();
					if (params == null)
						return null;
					return new CSSFunction(ident, params);
				}
			} else {
				reportError("UNEXPECTED_CHAR '" + (char) ch + "'");
				return null;
			}
		}
		}
	}

	private CSSValue[] readParams() throws IOException {
		Vector list = new Vector();
		int line = lineCount;
		while (true) {
			CSSValue value = readValue(' ');
			if (value == null)
				break;
			list.add(value);
			int ch = skipWhitespace();
			if (ch == ')')
				break;
			if (ch != ',') {
				if (ch < 0)
					reportError(line, "FUNCTION_SYNTAX");
				else
					reportError("PARAM_SYNTAX '" + (char) ch + "'");
				return null;
			}
			read();
		}
		CSSValue[] arr = new CSSValue[list.size()];
		list.copyInto(arr);
		return arr;
	}

	private CSSValue readValue() throws IOException {
		return readValue(',');
	}

	private CSSValue readTerm(char expectedSeparator) throws IOException {
		switch (expectedSeparator) {
		case ',':
			return readValue(' ');
		case ' ':
			return readValue('/');
		default:
			return readSingleValue();
		}
	}

	private CSSValue readValue(char expectedSeparator) throws IOException {
		CSSValue val = readTerm(expectedSeparator);
		if (val == null)
			return null;
		int ch = skipWhitespace();
		if (ch == ';' || ch < 0 || ch == '}' || ch == '!')
			return val;
		if (expectedSeparator == ' ') {
			if (ch == ',' || ch == '/') {
				return val;
			}
		} else {
			if (ch != expectedSeparator) {
				return val;
			}
			read();
		}
		Vector list = new Vector();
		list.add(val);
		while (true) {
			CSSValue value = readTerm(expectedSeparator);
			if (value == null)
				return null;
			list.add(value);
			ch = skipWhitespace();
			if (ch == ';' || ch < 0 || ch == '}' || ch == '!')
				break;
			if (expectedSeparator != ' ') {
				if (ch != expectedSeparator) {
					break;
				}
				read();
			}
		}
		CSSValue[] arr = new CSSValue[list.size()];
		list.copyInto(arr);
		return new CSSValueList(expectedSeparator, arr);
	}

	private boolean validatePropertyValue(String name, Object value) {
		return true;
	}

	private void readProperties(BaseRule rule) throws IOException {
		while (true) {
			int ch = skipWhitespace();
			if (('a' < ch && ch < 'z') || ('A' <= ch && ch <= 'Z') || ch == '-' || ch == '_' || ch == '\\'
					|| ch >= 0x80) {
				String prop = readName().toLowerCase();
				ch = skipWhitespace();
				if (ch != ':') {
					reportError("DECL_SYNTAX");
					break;
				}
				read();
				CSSValue value = readValue();
				if (value == null)
					break;
				ch = skipWhitespace();
				if (ch == '!') {
					read();
					skipWhitespace();
					String name = readName();
					if (name == null)
						break;
					if (!name.toLowerCase().equals("important")) {
						reportError("IMPORTANT_EXPECTED");
						break;
					}
					value = new CSSImportant(value);
					ch = skipWhitespace();
				}
				if (ch == ';' || ch == ';' || ch < 0) {
					if (ch < 0)
						reportError("UNEXPECTED_EOF");
					if (validatePropertyValue(prop, value))
						rule.set(prop, value);
				} else {
					reportError("SEMICOLON_EXPECTED");
				}
				if (ch != ';') {
					break;
				}
				read();
			} else {
				if (ch != '}')
					reportError("DECL_EXPECTED");
				break;
			}
		}
	}

	private Selector readSingleSelector() throws IOException {
		int ch = skipWhitespace();
		Selector selector = null;
		while (true) {
			Selector term = null;
			switch (ch) {
			case '.': {
				// class
				read();
				String ident = readName();
				term = new ClassSelector(ident);
				break;
			}
			case '#': {
				// id
				read();
				String ident = readName();
				term = new IdSelector(ident);
				break;
			}
			case '[': {
				// attr
				read();
				ch = skipWhitespace();
				String ident;
				if (ch == '|')
					ident = null;
				else {
					ident = readName();
					if (ident == null)
						return null;
					ch = skipWhitespace();
				}
				String prefix = null;
				String ns = null;
				if (ch == '|') {
					read();
					mark(1);
					int ch1 = read();
					reset();
					if (ch1 != '=') {
						prefix = ident;
						ident = readName();
						if (prefix == null)
							ns = "";
						else {
							ns = (String) namespaces.get(prefix);
							if (ns == null)
								reportError("UNDECLARED_PREFIX '" + prefix + "'");
						}
					}
				}
				if (ch == ']') {
					read();
					term = new AttributeSelector(prefix, ns, ident, "", null);
				} else {
					String op;
					if (ch == '=') {
						read();
						op = "=";
					} else if (ch == '~' || ch == '|' || ch == '^' || ch == '$' || ch == '*') {
						read();
						mark(1);
						int ch1 = read();
						if (ch1 != '=') {
							reset();
							return null;
						}
						op = (char) ch + "=";
					} else {
						return null;
					}
					CSSValue value = readSingleValue();
					if (value == null)
						return null;
					ch = skipWhitespace();
					if (ch != ']') {
						return null;
					}
					read();
					term = new AttributeSelector(prefix, ns, ident, op, value);
				}
				break;
			}
			case ':': {
				read();
				ch = skipWhitespace();
				boolean pseudoElement = ch == ':';
				if (pseudoElement) {
					read();
					skipWhitespace();
				}
				String name = readName();
				if (name == null)
					return null;
				name = name.toLowerCase();
				if (pseudoElement || name.equals("first-line") || name.equals("first-letter") || name.equals("before")
						|| name.equals("after"))
					term = new PseudoElementSelector(name);
				else
					term = new PseudoClassSelector(name);
				break;
			}
			case '*':
				read();
				term = new AnyElementSelector();
				break;
			default:
				if (('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ch == '_' || ch == '-' || ch >= 0x80
						|| ch == '\\' || ch == '|') {
					String ident;
					if (ch == '|')
						ident = null;
					else
						ident = readName();
					mark(1);
					ch = read();
					String ns = null;
					String prefix = null;
					if (ch == '|') {
						prefix = ident;
						ident = readName();
						if (prefix == null)
							ns = "";
						else {
							ns = (String) namespaces.get(prefix);
							if (ns == null)
								reportError("UNDECLARED_PREFIX '" + prefix + "'");
						}
					} else {
						ns = defaultNamespace;
						reset();
					}
					term = new NamedElementSelector(prefix, ns, ident);
				}
			}
			if (term == null)
				return selector;
			if (selector == null)
				selector = term;
			else
				selector = new AndSelector(selector, term);
			mark(1);
			ch = read();
			reset();
			if (ch < 0 || ch == '{' || ch == '>' || ch == '+' || ch == ',' || ch == ' ' || ch == '\t' || ch == '\r'
					|| ch == '\n' || ch == '\f')
				return selector;
		}
	}

	private Selector readSelector() throws IOException {
		Selector selector = readSingleSelector();
		while (true) {
			int ch = skipWhitespace();
			switch (ch) {
			case '>': {
				read();
				Selector term = readSingleSelector();
				if (term == null)
					return null;
				selector = new ChildSelector(selector, term);
				break;
			}
			case '+': {
				read();
				Selector term = readSingleSelector();
				if (term == null)
					return null;
				selector = new SiblingSelector(selector, term);
				break;
			}
			case '{': {
				return selector;
			}
			default: {
				Selector term = readSingleSelector();
				if (term == null)
					return null;
				selector = new DescendantSelector(selector, term);
			}
			}
		}
	}

	private Selector[] readSelectors() throws IOException {
		Vector selectors = new Vector();
		while (true) {
			Selector selector = readSelector();
			if (selector == null)
				return null;
			selectors.add(selector);
			int ch = skipWhitespace();
			if (ch < 0)
				return null;
			if (ch != ',')
				break;
		}
		if (selectors.size() == 0)
			return null;
		Selector[] arr = new Selector[selectors.size()];
		selectors.copyInto(arr);
		return arr;
	}

	private Set readMediaList() throws IOException {
		TreeSet mediaList = new TreeSet();
		boolean expectIdent = true;
		while (true) {
			int ch = skipWhitespace();
			if (ch < 0)
				return null;
			if (('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ch == '_' || ch == '-' || ch == '\\'
					|| ch >= 0x80) {
				if (!expectIdent)
					break;
				expectIdent = false;
				String ident = readName();
				if (ident == null)
					return null;
				mediaList.add(ident.toLowerCase());
			} else if (ch == ',') {
				if (expectIdent)
					break;
				expectIdent = true;
				read();
			} else {
				break;
			}
		}

		if (mediaList.size() == 0)
			return null;

		return mediaList;
	}

	private void readFontFaceRule(CSSStylesheet stylesheet) throws IOException {
		FontFaceRule fontFaceRule = new FontFaceRule();
		if (readRuleBody(fontFaceRule))
			stylesheet.add(fontFaceRule);
	}

	private void readNamespaceRule(CSSStylesheet stylesheet) throws IOException {
		Object value1 = readSingleValue();
		if (value1 == null) {
			reportError("NAMESPACE_RULE_SYNTAX");
			skipToEnd(false);
			return;
		}
		int ch = skipWhitespace();
		Object value2;
		if (ch == ';') {
			value2 = value1;
			value1 = null;
		} else {
			value2 = readSingleValue();
			if (value2 == null || !(value1 instanceof String)) {
				reportError("NAMESPACE_RULE_SYNTAX");
				skipToEnd(false);
				return;
			}
			ch = skipWhitespace();
			if (ch != ';') {
				reportError("SEMICOLON_EXPECTED");
				skipToEnd(false);
				return;
			}
		}
		read(); // read final ';'
		String ns;
		if (value2 instanceof CSSQuotedString) {
			ns = value2.toString();
		} else if (value2 instanceof CSSURL) {
			ns = ((CSSURL) value2).getURI();
		} else {
			reportError("NAMESPACE_RULE_SYNTAX");
			return;
		}
		String prefix;
		if (value1 == null) {
			defaultNamespace = ns;
			prefix = null;
		} else {
			prefix = value1.toString();
			namespaces.put(value1, ns); // key is CSSName
		}
		stylesheet.add(new NamespaceRule(prefix, ns));
	}

	private void readImportRule(CSSStylesheet stylesheet) throws IOException {
		Object value = readSingleValue();
		String url;
		if (value instanceof CSSQuotedString) {
			url = value.toString();
		} else if (value instanceof CSSURL) {
			url = ((CSSURL) value).getURI();
		} else {
			skipToEnd(false);
			return;
		}
		Set media = readMediaList();
		int ch = skipWhitespace();
		if (ch == ';') {
			read();
		} else if (ch >= 0) {
			skipToEnd(false);
		}
		ImportRule importRule = new ImportRule(url, media);
		stylesheet.add(importRule);
	}

	private void readMediaRule(CSSStylesheet stylesheet) throws IOException {

		// read media list
		Set media = readMediaList();
		if (media == null) {
			skipToEnd(false);
			return;
		}

		MediaRule mediaRule = new MediaRule(media);

		// read rules
		while (true) {
			int ch = skipWhitespace();
			if (ch == '}') {
				read();
				break;
			}
			Selector[] selectors = readSelectors();
			if (selectors == null) {
				ch = read();
				if (ch < 0)
					break;
				skipToEnd(false);
			} else {
				SelectorRule rule = new SelectorRule(selectors);
				if (readRuleBody(rule))
					mediaRule.add(rule);
			}
		}

		stylesheet.add(mediaRule);
	}

	private void readPageRule(CSSStylesheet stylesheet) throws IOException {
		String pseudo = null;
		int ch = skipWhitespace();
		if (ch < 0)
			return;
		if (ch == ':') {
			read();
			mark(1);
			ch = read();
			reset();
			if (ch < 0)
				return;
			if (('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ch == '_' || ch == '-' || ch == '\\'
					|| ch >= 0x80) {
				String ident = readName();
				if (ident == null)
					return;
				pseudo = ident.toLowerCase();
			}
		}
		PageRule pageRule = new PageRule(pseudo);
		if (readRuleBody(pageRule))
			stylesheet.add(pageRule);
	}

	private boolean readRuleBody(BaseRule rule) throws IOException {
		int ch = skipWhitespace();
		if (ch != '{') {
			skipToEnd(false);
			return false;
		}
		read();
		readProperties(rule);
		ch = skipWhitespace();
		if (ch >= 0) {
			if (ch == '}') {
				read();
			} else {
				reportError("CLOSING_BRACE_EXPECTED");
				skipToEnd(true);
			}
		}
		return true;
	}

	public InlineRule readInlineStyle(String style) {
		InlineRule result = new InlineRule();
		try {
			this.lineCount = 1;
			this.reader = new StringReader(style);
			readProperties(result);
		} catch (IOException e) {
			// should not really happen
			e.printStackTrace();
		}
		return result;
	}

	public CSSStylesheet readStylesheet(Reader in) throws IOException {
		CSSStylesheet stylesheet = new CSSStylesheet();
		readStylesheet(in, stylesheet);
		return stylesheet;
	}

	public CSSStylesheet readStylesheet(InputStream in) throws IOException {
		CSSStylesheet stylesheet = new CSSStylesheet();
		readStylesheet(in, stylesheet);
		return stylesheet;
	}

	public void readStylesheet(Reader in, CSSStylesheet stylesheet) throws IOException {
		setReader(in);
		readStylesheet(stylesheet);
	}

	public void readStylesheet(InputStream in, CSSStylesheet stylesheet) throws IOException {
		setReader(in);
		readStylesheet(stylesheet);
	}

	private void readStylesheet(CSSStylesheet stylesheet) throws IOException {
		boolean canImport = true;
		while (true) {
			int ch = skipWhitespace();
			int line = lineCount;
			if (ch < 0) {
				break;
			} else if (ch == '@') {
				read();
				mark(1);
				ch = read();
				reset();
				if (ch < 0)
					break;
				if (('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ch == '_' || ch == '-' || ch == '\\'
						|| ch >= 0x80) {
					String ident = readName();
					if (ident == null)
						break;
					ident = ident.toLowerCase();
					if (ident.equals("import")) {
						if (canImport)
							readImportRule(stylesheet);
						else
							reportError(line, "IMPORT_NOT_ALLOWED_HERE");
					} else if (ident.equals("media"))
						readMediaRule(stylesheet);
					else if (ident.equals("page"))
						readPageRule(stylesheet);
					else if (ident.equals("namespace"))
						readNamespaceRule(stylesheet);
					else if (ident.equals("font-face"))
						readFontFaceRule(stylesheet);
					else if (ident.equals("charset")) {
						// need to check that it matches encoding variable
						skipToEnd(false);
					} else {
						reportError(line, "UNKNOWN_AT_RULE '" + ident + "'");
						skipToEnd(false);
					}
					if (canImport && !ident.equals("import") && !ident.equals("charset")) {
						canImport = false;
					}
				} else {
					skipToEnd(false);
				}
			} else if (ch == ',' || ch == ';') {
				// stray chars
				reportError("UNEXPECTED_CHAR '" + (char) ch + "'");
				read();
			} else if (ch == '<') {
				read();
				mark(3);
				if (read() == '!' && read() == '-' && read() == '-') {
					// CDO, skip it
					continue;
				}
				reportError("UNEXPECTED_CHAR '<'");
				reset();
			} else {
				if (ch == '-') {
					mark(3);
					read();
					if (read() == '-' && read() == '>') {
						// CDC, skip it
						continue;
					}
					reset();
				}
				canImport = false;
				Selector[] selectors = readSelectors();
				if (selectors == null) {
					ch = read();
					if (ch < 0)
						break;
					skipToEnd(false);
				} else {
					SelectorRule rule = new SelectorRule(selectors);
					if (readRuleBody(rule))
						stylesheet.add(rule);
				}
			}
		}
	}

	public Iterator errors() {
		if (errors == null)
			return null;
		return errors.iterator();
	}

}
