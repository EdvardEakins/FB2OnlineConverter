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

public class FB2TitleInfo {

	private FB2AuthorInfo[] authors;
	
	private FB2GenreInfo[] genres;
	
	private String bookTitle;
	
	private FB2Section annotation;
	
	private String keywords;
	
	private FB2DateInfo date;
	
	private String coverpageImage;
	
	private String language;
	
	private String srcLanguage;
	
	private FB2AuthorInfo[] translators;
	
	private FB2SequenceInfo[] sequences;

	public FB2Section getAnnotation() {
		return annotation;
	}

	public void setAnnotation(FB2Section annotation) {
		this.annotation = annotation;
	}

	public FB2AuthorInfo[] getAuthors() {
		return authors;
	}

	public void setAuthors(FB2AuthorInfo[] authors) {
		this.authors = authors;
	}

	public String getBookTitle() {
		return bookTitle;
	}

	public void setBookTitle(String bookTitle) {
		this.bookTitle = bookTitle;
	}

	public String getCoverpageImage() {
		return coverpageImage;
	}

	public void setCoverpageImage(String coverpageImage) {
		this.coverpageImage = coverpageImage;
	}

	public FB2DateInfo getDate() {
		return date;
	}

	public void setDate(FB2DateInfo date) {
		this.date = date;
	}

	public FB2GenreInfo[] getGenres() {
		return genres;
	}

	public void setGenres(FB2GenreInfo[] genres) {
		this.genres = genres;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public FB2SequenceInfo[] getSequences() {
		return sequences;
	}

	public void setSequences(FB2SequenceInfo[] sequences) {
		this.sequences = sequences;
	}

	public String getSrcLanguage() {
		return srcLanguage;
	}

	public void setSrcLanguage(String srcLanguage) {
		this.srcLanguage = srcLanguage;
	}

	public FB2AuthorInfo[] getTranslators() {
		return translators;
	}

	public void setTranslators(FB2AuthorInfo[] translators) {
		this.translators = translators;
	}
}
