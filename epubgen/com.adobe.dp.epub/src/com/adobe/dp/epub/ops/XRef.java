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

import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Resource;
import com.adobe.dp.epub.opf.ResourceRef;

public class XRef {

	ResourceRef targetResource;

	Element targetElement;
	
	String targetId; // only matters if targetElement is null

	int playOrder;

	int usage;

	public static final int USAGE_TOC = 1;

	public static final int USAGE_PAGE = 2;

	public static final int USAGE_REF = 4;

	public XRef(OPSResource resource, Element element) {
		targetElement = element;
		targetResource = resource.getResourceRef();
	}

	public XRef(Resource resource, String id) {
		targetId = id;
		targetResource = resource.getResourceRef();
	}

	public Element getTagetElement() {
		return targetElement;
	}

	public String getTargetId() {
		if (targetElement == null)
			return targetId;
		if (targetElement.id == null)
			targetElement.document.assignId(targetElement);
		return targetElement.id;
	}

	public OPSResource getTargetResource() {
		return (OPSResource)targetResource.getResource();
	}

	public String makeReference(Resource fromResource) {
		return fromResource.makeReference(targetResource.getResourceName(), getTargetId());
	}

	public int getPlayOrder() {
		return playOrder;
	}

	public void setPlayOrder(int playOrder) {
		this.playOrder = playOrder;
	}

	public void requestPlayOrder() {
		this.playOrder = -1;
	}

	public boolean playOrderNeeded() {
		return this.playOrder != 0;
	}

	public void addUsage(int usage) {
		this.usage |= usage;
	}

	public int getUsage() {
		return usage;
	}
}
