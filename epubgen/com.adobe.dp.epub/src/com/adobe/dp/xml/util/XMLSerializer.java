/*******************************************************************************
* Copyright (c) 2009, Adobe Systems Incorporated
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without 
* modification, are permitted provided that the following conditions are met:
*
* ·        Redistributions of source code must retain the above copyright 
*          notice, this list of conditions and the following disclaimer. 
*
* ·        Redistributions in binary form must reproduce the above copyright 
*		   notice, this list of conditions and the following disclaimer in the
*		   documentation and/or other materials provided with the distribution. 
*
* ·        Neither the name of Adobe Systems Incorporated nor the names of its 
*		   contributors may be used to endorse or promote products derived from
*		   this software without specific prior written permission. 
* 
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
* ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
* OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
* THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*******************************************************************************/

package com.adobe.dp.xml.util;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class XMLSerializer implements SDocumentHandler {

	private OutputStream outStream;

	private boolean forgiving;

	private PrintWriter out;

	private StringBuffer prologue;

	private boolean hasEntities;

	private Stack openElements = new Stack();

	private Map prefixMap = new HashMap();

	private Map namespaceMap = new HashMap();

	private String defaultNamespace;

	private boolean closeTag;

	private static char[] newLineChar = {'\n'};
	
	private static String xmlns = "http://www.w3.org/XML/1998/namespace";
		
	static class OpenElement {

		OpenElement(String namespace, String name) {
			this.name = name;
			this.namespace = namespace;
		}

		String name;

		String namespace;

		String prefix;

		Set localNamespaces;

		String savedDefaultNamespace;
	}

	public XMLSerializer(OutputStream out, boolean forgiving) {
		this.outStream = out;
		this.forgiving = forgiving;
	}

	public XMLSerializer(OutputStream out) {
		this.outStream = out;
	}

	private String makePrefixForNamespace(String namespace) {
		int index = namespace.lastIndexOf('/');
		if (index < 0)
			index = namespace.lastIndexOf(':');
		if (index > 0) {
			String prefix = namespace.substring(index + 1);
			if (prefix.matches("[a-zA-Z][-a-zA-Z0-9_]*"))
				return prefix;
		}
		return "p";
	}

	private String getPrefix(String namespace) {
		if( namespace.equals(xmlns) )
			return "xml"; // built-in
		String prefix = (String) namespaceMap.get(namespace);
		if (prefix != null)
			return prefix;
		String prefixBase = makePrefixForNamespace(namespace);
		prefix = prefixBase;
		int index = 1;
		while (prefixMap.containsKey(prefix)) {
			prefix = prefixBase + index;
			index++;
		}
		prefixMap.put(prefix, namespace);
		namespaceMap.put(namespace, prefix);
		OpenElement e = (OpenElement) openElements.peek();
		if (e.localNamespaces == null)
			e.localNamespaces = new HashSet();
		e.localNamespaces.add(prefix);
		return prefix;
	}

	private boolean sameNamespace(String ns1, String ns2) {
		if (ns1 == ns2)
			return true;
		if (ns1 == null || ns2 == null)
			return false;
		return ns1.equals(ns2);
	}

	private void closeTagIfNeeded() {
		if (closeTag) {
			out.print(">");
			//newLine();
			closeTag = false;
		}
	}

	private String escapeAttribute(String value) {		
		value = StringUtil.replace(value, "&", "&amp;");
		value = StringUtil.replace(value, "<", "&lt;");
		value = StringUtil.replace(value, ">", "&gt;");
		value = StringUtil.replace(value, "\"", "&quot;");
		return value;
	}

	public void startDocument(String version, String encoding) {
		if (encoding == null)
			encoding = "utf-8";
		try {
			out = new PrintWriter(new OutputStreamWriter(outStream, encoding));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("unsupported encoding: '" + encoding + "'");
		}
		out.print("<?xml version=\"");
		out.print(version);
		out.print("\" encoding=\"");
		out.print(encoding);
		out.println("\"?>");
	}

	public void processingInstruction(String name, String value) {
		out.print("<?");
		out.print(name);
		out.print(" ");
		out.print(value);
		out.println("?>");
	}

	public void endDocument() {
		if (!openElements.isEmpty()) {
			if (forgiving) {
				do {
					OpenElement e = (OpenElement) openElements.pop();
					endElement(e.namespace, e.name);
				} while (!openElements.isEmpty());
			} else {
				throw new RuntimeException("unended elements");
			}
		}
		closeTagIfNeeded();
		out.close();
	}

	public void setDoctype(String doctype, String publicId, String systemId) {
		prologue = new StringBuffer("<!DOCTYPE ");
		prologue.append(doctype);
		if (publicId != null) {
			prologue.append(" PUBLIC \"");
			prologue.append(publicId);
			prologue.append("\"");
		}
		if (systemId != null) {
			prologue.append(" \"");
			prologue.append(systemId);
			prologue.append("\"");
		}
	}

	public void setPreferredPrefixMap(SMap preferredPrefixes) {
		SMapIterator iterator = preferredPrefixes.iterator();
		while (iterator.hasItem()) {
			String prefix = iterator.getName();
			String namespace = iterator.getValue().toString();
			namespaceMap.put(namespace, prefix);
			prefixMap.put(prefix, namespace);
			iterator.nextItem();
		}
	}

	public void defineEntity(String entityName, String value) {
		if (!hasEntities) {
			prologue.append(" [\n");
			hasEntities = true;
		}
		prologue.append("<!ENTITY ");
		prologue.append(entityName);
		prologue.append(" \"");
		prologue.append(escapeAttribute(value));
		prologue.append("\">\n");
	}

	public void startElement(String namespace, String name, SMap attributes, boolean makeDefaultNamespace) {
		boolean rootElement = openElements.size() == 0;
		if (prologue != null && rootElement) {
			if (hasEntities) {
				prologue.append("]");
			}
			prologue.append(">");
			out.println(prologue);
			//newLine();
		}
		closeTagIfNeeded();
		OpenElement e = new OpenElement(namespace, name);
		openElements.push(e);
		out.print("<");
		String newDefaultNS = null;
		if (!namespace.equals(defaultNamespace)) {
			if (makeDefaultNamespace) {
				e.savedDefaultNamespace = defaultNamespace;
				defaultNamespace = namespace;
				newDefaultNS = namespace;
			} else {
				e.prefix = getPrefix(namespace);
				out.print(e.prefix);
				out.print(":");
			}
		}
		out.print(name);
		if (newDefaultNS != null) {
			out.print(" xmlns=\"");
			out.print(escapeAttribute(newDefaultNS));
			out.print("\"");
		}
		if (attributes != null) {
			SMapIterator iterator = attributes.iterator();
			while (iterator.hasItem()) {
				out.print(" ");
				String attributeNamespace = iterator.getNamespace();
				if (attributeNamespace != null) {
					String attributePrefix = getPrefix(attributeNamespace);
					out.print(attributePrefix);
					out.print(":");
				}
				String attributeName = iterator.getName();
				out.print(attributeName);
				out.print("=\"");
				String value = escapeAttribute(iterator.getValue().toString());
				out.print(value);
				out.print("\"");
				iterator.nextItem();
			}
		}
		if (e.localNamespaces != null || rootElement) {
			Iterator prefixes;
			if (rootElement)
				prefixes = prefixMap.keySet().iterator();
			else
				prefixes = e.localNamespaces.iterator();
			while (prefixes.hasNext()) {
				String nsPrefix = (String) prefixes.next();
				out.print(" xmlns:");
				out.print(nsPrefix);
				out.print("=\"");
				String ns = (String) prefixMap.get(nsPrefix);
				out.print(escapeAttribute(ns));
				out.print("\"");
			}
		}
		closeTag = true;
	}

	public void endElement(String namespace, String name) {
		if (openElements.isEmpty()) {
			if (forgiving)
				return;
			throw new RuntimeException("{" + namespace + "}" + name + ": element closed after document end");
		}
		OpenElement e = (OpenElement) openElements.peek();
		if (!sameNamespace(e.namespace, namespace) || !e.name.equals(name)) {
			if (forgiving) {
				int len = openElements.size();
				int i;
				for (i = len - 2; i >= 0; i--) {
					e = (OpenElement) openElements.elementAt(i);
					if (sameNamespace(e.namespace, namespace) && e.name.equals(name))
						break;
				}
				if (i < 0)
					return;
				while (true) {
					e = (OpenElement) openElements.peek();
					if (sameNamespace(e.namespace, namespace) && e.name.equals(name))
						break;
					endElement(e.namespace, e.name);
				}

			} else {
				throw new RuntimeException("Illegal nesting: {" + namespace + "}" + name + ", {" + e.namespace + "}"
						+ e.name + " expected");
			}
		}
		openElements.pop();
		if (closeTag) {
			out.print("/>");
			closeTag = false;
		} else {
			out.print("</");
			if (e.prefix != null) {
				out.print(e.prefix);
				out.print(":");
			}
			out.print(e.name);
			out.print(">");
		}
		//newLine();
		if (e.savedDefaultNamespace != null)
			defaultNamespace = e.savedDefaultNamespace;
		if (e.localNamespaces != null) {
			Iterator prefixes = e.localNamespaces.iterator();
			while (prefixes.hasNext()) {
				String prefix = (String) prefixes.next();
				String ns = (String) prefixMap.remove(prefix);
				namespaceMap.remove(ns);
			}
		}
	}

	public void entityReference(String entity) {
		closeTagIfNeeded();
		out.print("&");
		out.print(entity);
		out.print(";");
	}

	public void newLine() {
		text(newLineChar, 0, 1);
	}
	
	public void text(char[] text, int offset, int len) {
		closeTagIfNeeded();
		int end = offset + len;
		String ent = null;
		for (int i = offset; i < end; i++) {
			char c = text[i];
			switch (c) {
			case '&':
				ent = "&amp;";
				break;
			case '<':
				ent = "&lt;";
				break;
			case '>':
				ent = "&gt;";
				break;
			default:
				continue;
			}
			if (i > offset)
				out.write(text, offset, i - offset);
			offset = i + 1;
			out.print(ent);
		}
		if (end > offset)
			out.write(text, offset, end - offset);
	}
}
