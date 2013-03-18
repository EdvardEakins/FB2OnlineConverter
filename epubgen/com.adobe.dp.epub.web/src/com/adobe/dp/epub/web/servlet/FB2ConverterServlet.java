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

package com.adobe.dp.epub.web.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.adobe.dp.epub.io.OCFContainerWriter;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.util.Translit;
import com.adobe.dp.epub.web.font.FontCookieSet;
import com.adobe.dp.epub.web.font.SharedFontSet;
import com.adobe.dp.epub.web.util.Initializer;
import com.adobe.dp.fb2.FB2Document;
import com.adobe.dp.fb2.FB2FormatException;
import com.adobe.dp.fb2.FB2TitleInfo;
import com.adobe.dp.fb2.convert.FB2Converter;
import com.adobe.dp.otf.FontLocator;

public class FB2ConverterServlet extends HttpServlet {
	public static final long serialVersionUID = 0;

	static Logger logger;

	static HashSet activeStreams = new HashSet();

	static {
		Initializer.init();
		logger = Logger.getLogger(FB2ConverterServlet.class);
		logger.setLevel(Level.ALL);
		logger.trace("servlet loaded");
	}

	void reportError(HttpServletResponse resp, String err) throws IOException {
		logger.error(err);
		resp.setContentType("text/plain; charset=utf8");
		Writer out = resp.getWriter();
		out.write(err);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doRequest(false, req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doRequest(true, req, resp);
	}

	private void doRequest(boolean post, HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		String streamIP = null;
		try {
			logger.trace("start " + req.getRemoteAddr());
			InputStream fb2in = null;
			InputStream templatein = null;
			FileItem book = null;
			FileItem template = null;
			boolean translit = false;
			boolean useurl = false;
			String fb2url = null;
			if (post && ServletFileUpload.isMultipartContent(req)) {
				DiskFileItemFactory itemFac = new DiskFileItemFactory();
				File repositoryPath = Initializer.getUploadDir();
				repositoryPath.mkdir();
				itemFac.setRepository(repositoryPath);
				ServletFileUpload servletFileUpload = new ServletFileUpload(itemFac);
				List fileItemList = servletFileUpload.parseRequest(req);
				Iterator list = fileItemList.iterator();
				while (list.hasNext()) {
					FileItem item = (FileItem) list.next();
					String t = item.getString();
					String paramName = item.getFieldName();
					if (paramName.equals("file")) {
						if (t.startsWith("http://"))
							fb2url = t;
						else if (t.length() > 0)
							book = item;
					} else if (paramName.equals("template")) {
						if (t.length() > 0)
							template = item;
					} else if (paramName.equals("translit"))
						translit = t.equals("on") || t.equals("yes");
					else if (paramName.equals("useurl"))
						useurl = t.equals("on") || t.equals("yes");
					else if (paramName.equals("url")) {
						if (t.length() > 0)
							fb2url = t;
					}
				}
				if (!useurl && book != null)
					fb2in = book.getInputStream();
				if (template != null)
					templatein = template.getInputStream();
			} else {
				fb2url = req.getParameter("url");
				String t = req.getParameter("translit");
				translit = t != null && (t.equals("on") || t.equals("yes"));
			}
			if (fb2in == null) {
				if (fb2url == null) {
					reportError(resp, "Invalid request: neither fb2 file nor URL is provided");
					return;
				}
				URL url = new URL(fb2url);
				if (!url.getProtocol().equals("http")) {
					reportError(resp, "Invalid request: fb2 URL protocol is not http");
					return;
				}
				String host = url.getHost();
				InetAddress ipaddr = InetAddress.getByName(host);
				String ipstr = ipaddr.toString();
				synchronized (activeStreams) {
					if (!activeStreams.contains(ipstr)) {
						activeStreams.add(ipstr);
						streamIP = ipstr;
					}
				}
				if (streamIP == null) {
					reportError(resp, "Only a single connection to the server " + host + " is allowed");
					return;
				}
				logger.info("downloading from " + fb2url);
				fb2in = url.openStream();
			}
			FB2Document doc = new FB2Document(fb2in);
			Publication epub = new Publication();
			epub.setTranslit(translit);
			epub.useAdobeFontMangling();
			fb2in.close();
			if (book != null)
				book.delete();
			FB2TitleInfo bookInfo = doc.getTitleInfo();
			String title = (bookInfo == null ? null : bookInfo.getBookTitle());
			String fname;
			if (title == null)
				fname = "book";
			else
				fname = Translit.translit(title).replace(' ', '_').replace('\t', '_').replace('\n', '_').replace('\r',
						'_').replace('\u00AB', '_').replace('\u00BB', '_');
			resp.setContentType("application/epub+zip");
			resp.setHeader("Content-Disposition", "attachment; filename=" + fname + ".epub");
			OutputStream out = resp.getOutputStream();
			OCFContainerWriter container = new OCFContainerWriter(out);
			FB2Converter conv = new FB2Converter();
			FontLocator fontLocator = Initializer.getDefaultFontLocator();
			FontCookieSet customFontCookies = new FontCookieSet(req);
			SharedFontSet sharedFontSet = SharedFontSet.getInstance();
			fontLocator = sharedFontSet.getFontLocator(customFontCookies, fontLocator);
			conv.setFontLocator(fontLocator);
			if (templatein != null) {
				conv.setTemplate(templatein);
				if (template != null)
					template.delete();
			}
			conv.convert(doc, epub);
			conv.embedFonts();
			epub.serialize(container);
		} catch (FB2FormatException e) {
			logger.error("error", e);
			reportError(resp, e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			logger.error("error", e);
			reportError(resp, "Internal server error: " + e.toString());
			e.printStackTrace();
		} catch (Throwable e) {
			logger.fatal("error", e);
		} finally {
			if (streamIP != null) {
				synchronized (activeStreams) {
					activeStreams.remove(streamIP);
				}
			}
			logger.trace("end");
		}
	}
}
