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

package com.adobe.dp.fb2;

public class FB2DocumentInfo {

	private FB2AuthorInfo[] authors;

	private String programUsed;

	private FB2DateInfo date;

	private String[] srcUrls;

	private String srcOcr;

	private String id;

	private String version;

	private FB2Section history;

	public FB2AuthorInfo[] getAuthors() {
		return authors;
	}

	public void setAuthors(FB2AuthorInfo[] authors) {
		this.authors = authors;
	}

	public FB2DateInfo getDate() {
		return date;
	}

	public void setDate(FB2DateInfo date) {
		this.date = date;
	}

	public FB2Section getHistory() {
		return history;
	}

	public void setHistory(FB2Section history) {
		this.history = history;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getProgramUsed() {
		return programUsed;
	}

	public void setProgramUsed(String programUsed) {
		this.programUsed = programUsed;
	}

	public String getSrcOcr() {
		return srcOcr;
	}

	public void setSrcOcr(String srcOcr) {
		this.srcOcr = srcOcr;
	}

	public String[] getSrcUrls() {
		return srcUrls;
	}

	public void setSrcUrls(String[] srcUrls) {
		this.srcUrls = srcUrls;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
