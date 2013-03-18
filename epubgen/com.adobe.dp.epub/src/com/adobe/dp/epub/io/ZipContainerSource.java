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

package com.adobe.dp.epub.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ZipContainerSource extends ContainerSource {

	ZipFile zip;
	Vector entryList;

	public ZipContainerSource(File zip) throws ZipException, IOException {
		this.zip = new ZipFile(zip);
	}

	class DataSourceImpl extends DataSource {

		ZipEntry entry;
		
		DataSourceImpl(ZipEntry entry) {
			this.entry = entry;
		}

		public InputStream getInputStream() throws IOException {
			return zip.getInputStream(entry);
		}

	}
	
	public void close() throws IOException {
		zip.close();
	}

	public DataSource getDataSource(String name) {
		ZipEntry entry = zip.getEntry(name);
		if (entry == null)
			return null;
		return new DataSourceImpl(entry);
	}

	public Iterator getResourceList() {
		if( entryList == null ) {
			entryList = new Vector();
			Enumeration entries = zip.entries();
			while( entries.hasMoreElements() )
			{
				ZipEntry entry = (ZipEntry)entries.nextElement();
				String name = entry.getName();
				entryList.add(name);
			}
		}
		return entryList.iterator();
	}
	
}
