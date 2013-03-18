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

package com.adobe.dp.epub.ops;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import com.adobe.dp.css.CSSValue;
import com.adobe.dp.css.CascadeEngine;
import com.adobe.dp.css.CascadeResult;
import com.adobe.dp.css.InlineRule;
import com.adobe.dp.epub.otf.FontSubsetter;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.xml.util.SMapImpl;
import com.adobe.dp.xml.util.XMLSerializer;

abstract public class Element {

	OPSDocument document;

	String className;

	InlineRule style;

	CascadeResult cascade;

	boolean assignStyle;

	String elementName;

	String id;

	Vector children = new Vector();

	XRef selfRef;
	
	String lang;

	private static class SizeRemains {
		int size;
	}

	Element(OPSDocument document, String name) {
		this.document = document;
		elementName = name;
	}

	abstract public String getNamespaceURI();

	abstract Element cloneElementShallow(OPSDocument newDoc);

	public Element cloneElementShallow() {
		return cloneElementShallow(document);
	}

	protected Object getBuiltInProperty(String propName) {
		return null;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public Object getCascadedProperty(String propName) {

		// style attribute: highest specificity
		InlineRule style = getStyle();
		if (style != null) {
			Object value = style.get(propName);
			if (value != null)
				return value;
		}

		// CSS cascade from stylesheets
		if (cascade != null) {
			Object value = cascade.getProperties().getPropertySet().get(propName);
			if (value != null)
				return value;
		}

		// built-in stylesheet
		return getBuiltInProperty(propName);
	}

	public String getElementName() {
		return elementName;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public void add(Object child) {
		children.add(child);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		document.setElementId(this, id);
	}

	public Iterator content() {
		return children.iterator();
	}

	public Object getLastChild() {
		if (children.isEmpty())
			return null;
		return children.lastElement();
	}

	public XRef getSelfRef() {
		if (selfRef == null) {
			document.assignId(this);
			selfRef = new XRef(document.resource, this);
		}
		return selfRef;
	}

	SMapImpl getAttributes() {
		SMapImpl attrs = new SMapImpl();
		if (className != null)
			attrs.put(null, "class", className);
		if (id != null)
			attrs.put(null, "id", id);
		if (style != null)
			attrs.put(null, "style", style);
		if (lang != null)
			attrs.put(OPSDocument.xmlns, "lang", lang);
		return attrs;
	}

	public void addFonts(FontSubsetter subsetter) {
		subsetter.push(this);
		try {
			Iterator children = content();
			if (children != null) {
				while (children.hasNext()) {
					Object child = children.next();
					if (child instanceof Element) {
						((Element) child).addFonts(subsetter);
					} else if (child instanceof String) {
						subsetter.play((String) child);
					}
				}
			}
		} finally {
			subsetter.pop(this);
		}
	}

	boolean isSection() {
		return false;
	}

	private static int getUTF8Length(String s) {
		try {
			return s.getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			throw new Error("UTF-8 unsupported???");
		}
	}

	int getElementSize() {
		int size = elementName.length() + 3;
		if (className != null) {
			size += className.length() + 9;
		}
		if (selfRef != null)
			size += 10;
		if (children.size() == 0)
			size++;
		else
			size += elementName.length() + 5;
		return size;
	}

	int getPeelingBonus() {
		return 0;
	}

	boolean forcePeel() {
		return false;
	}

	boolean canPeelChild() {
		return false;
	}

	final int getEstimatedSize() {
		int size = getElementSize();
		Iterator it = content();
		if (!it.hasNext())
			return size;
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof Element)
				size += ((Element) next).getEstimatedSize();
			else if (next instanceof String) {
				size += getUTF8Length((String) next);
			}
			size++;
		}
		return size;
	}

	void transferToDocument(OPSDocument newDoc) {
		if (id != null)
			document.idMap.remove(id);
		document = newDoc;
		if (id != null)
			document.idMap.put(id, this);
		if (selfRef != null) {
			selfRef.targetResource = newDoc.resource.getResourceRef();
		}
		Iterator it = content();
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof Element)
				((Element) next).transferToDocument(newDoc);
		}
	}

	final Element peelElements(OPSDocument newDoc, int targetSize, boolean first) {
		SizeRemains sr = new SizeRemains();
		sr.size = targetSize + 1000;
		return peelElements(newDoc, sr, first);
	}

	final Element peelElements(OPSDocument newDoc, SizeRemains remains, boolean first) {
		int size = getElementSize();
		int bonus = getPeelingBonus();
		remains.size -= size;
		if (!first && (forcePeel() || (bonus >= 0 && bonus > remains.size))) {
			// System.out.println("Break for bonus " + bonus);
			transferToDocument(newDoc);
			return this;
		}
		Element result = null;
		boolean canPeelChild = canPeelChild();
		int i = 0;
		while (i < children.size()) {
			Object next = children.elementAt(i);
			if (next instanceof Element) {
				Element child = (Element) next;
				if (result == null) {
					if (canPeelChild) {
						Element p = child.peelElements(newDoc, remains, i == 0);
						if (p != null) {
							result = cloneElementShallow(newDoc);
							result.add(p);
							if (p == child) {
								children.remove(i);
								continue;
							}
						}
					} else {
						remains.size -= child.getEstimatedSize();
					}
				} else {
					children.remove(i);
					child.transferToDocument(newDoc);
					result.add(child);
					continue;
				}
			} else if (next instanceof String) {
				remains.size -= getUTF8Length((String) next);
			}
			remains.size--;
			i++;
		}
		return result;
	}

	public void generateTOCFromHeadings(Stack headings, int depth) {
		Iterator it = content();
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof Element)
				((Element) next).generateTOCFromHeadings(headings, depth);
		}
	}

	void serializeChildren(XMLSerializer ser) {
		boolean section = isSection();
		Iterator it = content();
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof Element)
				((Element) next).serialize(ser);
			else if (next instanceof String) {
				char[] arr = ((String) next).toCharArray();
				ser.text(arr, 0, arr.length);
			}
			if (section)
				ser.newLine();
		}
	}

	boolean makeNSDefault() {
		return false;
	}

	void serialize(XMLSerializer ser) {
		boolean section = isSection();
		String ns = getNamespaceURI();
		ser.startElement(ns, elementName, getAttributes(), makeNSDefault());
		if (section)
			ser.newLine();
		serializeChildren(ser);
		ser.endElement(ns, elementName);
	}

	public String getText() {
		StringBuffer sb = new StringBuffer();
		Iterator it = content();
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof Element)
				sb.append(((Element) next).getText());
			else if (next instanceof String) {
				sb.append((String) next);
			}
		}
		return sb.toString();
	}

	public CascadeResult getCascadeResult() {
		if (cascade == null)
			cascade = new CascadeResult();
		return cascade;
	}

	public void setDesiredCascadeResult(InlineRule s) {
		CascadeResult cr = new CascadeResult();
		InlineRule t = cr.getProperties().getPropertySet();
		if (s != null) {
			Iterator it = s.properties();
			if (it != null) {
				while (it.hasNext()) {
					String p = (String) it.next();
					CSSValue value = s.get(p);
					t.set(p, value);
				}
			}
		}
		setDesiredCascadeResult(cr);
	}

	public void setDesiredCascadeResult(CascadeResult cascade) {
		this.cascade = cascade;
		this.assignStyle = true;
	}

	public InlineRule getStyle() {
		return style;
	}

	public void setStyle(InlineRule style) {
		this.style = style;
	}

	public int assignPlayOrder(int playOrder) {
		if (selfRef != null && selfRef.playOrderNeeded()) {
			selfRef.setPlayOrder(++playOrder);
		}
		Iterator it = content();
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof Element)
				playOrder = ((Element) next).assignPlayOrder(playOrder);
		}
		return playOrder;
	}

	public void cascade(CascadeEngine engine) {
		engine.pushElement(getNamespaceURI(), getElementName(), getAttributes());
		cascade = engine.getCascadeResult();
		Iterator it = content();
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof Element)
				((Element) next).cascade(engine);
		}
		engine.popElement();
	}

	public void generateStyles(Stylesheet stylesheet) {
		if (assignStyle) {
			if (cascade != null && !cascade.isEmpty())
				className = stylesheet.makeClass(className, cascade);
			else
				className = null;
			assignStyle = false;
		}
		Iterator it = content();
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof Element)
				((Element) next).generateStyles(stylesheet);
		}
	}
	
	public void setAssignStylesFlag() {
		assignStyle = true;
		Iterator it = content();
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof Element)
				((Element) next).setAssignStylesFlag();
		}
	}

	

}
