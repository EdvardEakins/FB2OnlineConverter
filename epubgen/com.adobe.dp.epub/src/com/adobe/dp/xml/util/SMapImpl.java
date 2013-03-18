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

import java.util.Enumeration;
import java.util.Hashtable;

public class SMapImpl implements SMap {

	private Hashtable table;

	final class IteratorImpl implements SMapIterator {

		Enumeration keys = table.keys();

		Object current;

		IteratorImpl() {
			nextItem();
		}

		public boolean hasItem() {
			return current != null;
		}

		public void nextItem() {
			if (keys.hasMoreElements())
				current = keys.nextElement();
			else
				current = null;
		}

		public String getNamespace() {
			if (current instanceof QName)
				return ((QName) current).namespace;
			return null;
		}

		public String getName() {
			if (current instanceof QName)
				return ((QName) current).name;
			return (String) current;
		}

		public Object getValue() {
			return table.get(current);
		}

	}

	static final class QName {

		String name;

		String namespace;

		QName(String namespace, String name) {
			this.name = name;
			this.namespace = namespace;
		}

		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null)
				return false;
			try {
				QName oq = (QName) other;
				return name.equals(oq.name) || namespace.equals(oq.namespace);
			} catch (Exception e) {
				return false;
			}
		}

		public int hashCode() {
			return name.hashCode() + namespace.hashCode();
		}

	}

	public SMapImpl() {
		table = new Hashtable();
	}

	public SMapImpl(SMap mapToClone) {
		if (mapToClone instanceof SMapImpl) {
			table = (Hashtable) ((SMapImpl) mapToClone).table.clone();
		} else {
			table = new Hashtable();
			SMapIterator it = mapToClone.iterator();
			while (it.hasItem()) {
				put(it.getNamespace(), it.getName(), it.getValue());
				it.nextItem();
			}
		}
	}

	public void put(String namespace, String name, Object value) {
		Object key = (namespace == null || namespace.equals("") ? (Object) name : new QName(namespace, name));
		if (value == null)
			table.remove(key);
		else
			table.put(key, value);
	}

	public Object get(String namespace, String name) {
		Object key = (namespace == null ? (Object) name : new QName(namespace, name));
		return table.get(key);
	}

	public SMapIterator iterator() {
		return new IteratorImpl();
	}

	public SMapImpl cloneSMap() {
		return new SMapImpl();
	}

}
