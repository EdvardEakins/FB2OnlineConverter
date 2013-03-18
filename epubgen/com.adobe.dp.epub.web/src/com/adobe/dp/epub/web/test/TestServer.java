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

package com.adobe.dp.epub.web.test;

import java.io.File;

import org.mortbay.http.HttpServer;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.jetty.servlet.ServletHttpContext;
import org.mortbay.util.InetAddrPort;

import com.adobe.dp.epub.web.servlet.DOCXConverterServlet;
import com.adobe.dp.epub.web.servlet.FB2ConverterServlet;
import com.adobe.dp.epub.web.servlet.FontsServlet;

public class TestServer {
	public static void main(String[] args) {
		// Sample HTTP server using jetty
		try {
			HttpServer svr = new HttpServer();
			svr.addListener(new InetAddrPort(80));
			ResourceHandler resourceHandler = new ResourceHandler();
			File root = new File("http_root");
			ServletHttpContext cx = new ServletHttpContext();
			cx.setContextPath("");
			cx.addHandler(resourceHandler);
			cx.setResourceBase(root.getAbsolutePath());

			cx.addServlet("DOCX", "/epubgen/docx2epub", DOCXConverterServlet.class
					.getName());
			cx.addServlet("FB2", "/epubgen/fb2epub", FB2ConverterServlet.class
					.getName());
			cx.addServlet("Fonts", "/epubgen/fonts", FontsServlet.class
					.getName());

			svr.addContext(cx);
			svr.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
