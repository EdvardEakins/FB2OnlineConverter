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

package com.adobe.dp.office.rtf;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class RTFStyle {

	String name;

	RTFStyle parent;

	private Hashtable storage;

	private boolean clone;

	private boolean locked;

	public RTFStyle cloneStyle() {
		RTFStyle s = new RTFStyle();
		s.parent = parent;
		s.storage = storage;
		s.name = name;
		s.clone = storage != null;
		return s;
	}

	public Object get(String prop) {
		if (storage == null)
			return null;
		return storage.get(prop);
	}

	void put(String prop, Object val) {
		if (locked)
			throw new IllegalStateException("locked");
		if (storage == null)
			storage = new Hashtable();
		else if (clone) {
			clone = false;
			name = null;
			storage = (Hashtable) storage.clone();
		}
		storage.put(prop, val);
	}

	void remove(String prop) {
		if (locked)
			throw new IllegalStateException("locked");
		if (storage == null || storage.get(prop) == null)
			return;
		if (clone) {
			clone = false;
			name = null;
			storage = (Hashtable) storage.clone();
		}
		storage.remove(prop);
	}

	public boolean isLocked() {
		return locked;
	}

	public void lock() {
		locked = true;
	}

	public boolean isEmpty() {
		return storage == null || storage.isEmpty();
	}

	public Iterator properties() {
		if (parent != null)
			throw new IllegalStateException("needs to be collapsed to iterate");
		return storage.keySet().iterator();
	}

	public static RTFStyle collapse(RTFStyle[] styles, Set propSet) {
		RTFStyle rs = new RTFStyle();
		for (int i = 0; i < styles.length; i++) {
			for (RTFStyle s = styles[i]; s != null; s = s.parent) {
				if (s.storage == null)
					continue;
				Iterator keys = s.storage.keySet().iterator();
				while (keys.hasNext()) {
					String k = (String) keys.next();
					if (!propSet.contains(k))
						continue;
					if (rs.get(k) == null) {
						Object v = s.storage.get(k);
						rs.put(k, v);
					}
				}
			}
		}
		return rs;
	}

	public RTFStyle collapse(Set propSet) {
		RTFStyle[] s = { this };
		return collapse(s, propSet);
	}

	public int hashCode() {
		return (isEmpty() ? 0 : storage.hashCode()) + (parent == null ? 0 : parent.hashCode());
	}

	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other.getClass() == getClass()) {
			RTFStyle prop = (RTFStyle) other;
			if (parent == null) {
				if (prop.parent != null)
					return false;
			} else if (!parent.equals(prop.parent))
				return false;
			if (isEmpty())
				return prop.isEmpty();
			else
				return storage.equals(prop.storage);
		}
		return false;
	}
}
