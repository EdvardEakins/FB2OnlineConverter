package com.adobe.dp.xml.util;

import org.xml.sax.Attributes;

public class SMapAttributesAdapter implements SMap {

	private Attributes attributes;

	public SMapAttributesAdapter(Attributes attributes) {
		this.attributes = attributes;
	}

	public Object get(String namespace, String name) {
		return attributes.getValue(namespace, name);
	}

	public SMapIterator iterator() {
		return new SMapIterator() {
			int index;

			public String getName() {
				return attributes.getLocalName(index);
			}

			public String getNamespace() {
				return attributes.getURI(index);
			}

			public Object getValue() {
				return attributes.getValue(index);
			}

			public boolean hasItem() {
				return index < attributes.getLength();
			}

			public void nextItem() {
				index++;
			}
		};
	}

}
