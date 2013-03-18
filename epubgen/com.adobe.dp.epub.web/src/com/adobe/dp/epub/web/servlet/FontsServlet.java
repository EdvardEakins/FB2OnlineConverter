package com.adobe.dp.epub.web.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.adobe.dp.epub.otf.FontEmbeddingReport;
import com.adobe.dp.epub.util.Base64;
import com.adobe.dp.epub.util.ConversionTemplate;
import com.adobe.dp.epub.web.font.FontCookieSet;
import com.adobe.dp.epub.web.font.SharedFontSet;
import com.adobe.dp.otf.FontProperties;

public class FontsServlet extends HttpServlet {

	public static final long serialVersionUID = 0;

	static Logger logger;

	static File home;

	static HashSet activeStreams = new HashSet();

	static ConversionTemplate sharedTemplate;

	static String css = "";

	final static int S_NO_FONTS = 0;
	final static int S_INSTALLED = 1;
	final static int S_ADD = 2;
	final static int S_TITLE = 3;
	final static int S_BACK = 4;
	final static int S_REPORT_TITLE = 5;
	final static int S_REPORT_NO_EMBEDDED = 6;
	final static int S_REPORT_EMBEDDED = 7;
	final static int S_REPORT_PROHIBITED = 8;
	final static int S_REPORT_MISSING = 9;

	static {
		logger = Logger.getLogger(FontsServlet.class);
		logger.setLevel(Level.ALL);
		logger.trace("servlet loaded");
		try {
			InputStream in = FontsServlet.class.getResourceAsStream("fonts.css");
			InputStreamReader r = new InputStreamReader(in, "UTF-8");
			StringBuffer sb = new StringBuffer();
			char[] buf = new char[128];
			int len;
			while ((len = r.read(buf)) > 0) {
				sb.append(buf, 0, len);
			}
			css = sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class FontInfo implements Comparable {
		FontInfo(String sha1str, FontProperties prop) {
			this.sha1str = sha1str;
			this.prop = prop;
		}

		public int compareTo(Object o) {
			FontInfo f = (FontInfo) o;
			int c = prop.getFamilyName().compareTo(f.prop.getFamilyName());
			if (c != 0)
				return c;
			c = prop.getWeight() - f.prop.getWeight();
			if (c != 0)
				return c;
			return prop.getStyle() - f.prop.getStyle();
		}

		String sha1str;
		FontProperties prop;
	}

	private static String getString(int code, String lang) {
		if (lang != null && lang.equals("ru")) {
			switch (code) {
			case S_NO_FONTS:
				return "Пользовательские шрифты не установлены";
			case S_INSTALLED:
				return "Шрифты установленные в системе:";
			case S_ADD:
				return "Установить дополнительные шрифты:";
			case S_TITLE:
				return "Управление пользовательскими шрифтами";
			case S_BACK:
				return "Назад";
			case S_REPORT_TITLE :
				return "Информация об использованных шрифтах";
			case S_REPORT_NO_EMBEDDED :
				return "Внедрённых шрифтов нет";
			case S_REPORT_EMBEDDED :
				return "Шрифты, которые будут внедрены в документ";
			case S_REPORT_PROHIBITED :
				return "Шрифты, которые запрещены к внедрению";
			case S_REPORT_MISSING :
				return "Шрифты, которые не были найдены";
			}
		}
		switch (code) {
		case S_NO_FONTS:
			return "No custom fonts installed";
		case S_INSTALLED:
			return "Currently installed fonts:";
		case S_ADD:
			return "Install custom fonts:";
		case S_TITLE:
			return "Custom Font Management";
		case S_BACK:
			return "Back";
		case S_REPORT_TITLE :
			return "Font Usage Report";
		case S_REPORT_NO_EMBEDDED :
			return "No fonts embedded";
		case S_REPORT_EMBEDDED :
			return "Fonts that will be embedded";
		case S_REPORT_PROHIBITED :
			return "Fonts which cannot be embedded";
		case S_REPORT_MISSING :
			return "Missing fonts (not embedded)";
		}
		return "";
	}

	private String addFont(FileItem item) throws Exception {
		InputStream in = item.getInputStream();
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] buffer = new byte[4096];
		int len;
		int size = 0;
		while ((len = in.read(buffer)) > 0) {
			sha1.update(buffer, 0, len);
			size += len;
		}
		in.close();
		if (size <= 100) {
			// hard to imagine font smaller than 100 bytes
			return null;
		}
		byte[] hash = sha1.digest();
		String sha1str = Base64.encodeBytes(hash).replace('/', '-');
		SharedFontSet fontSet = SharedFontSet.getInstance();
		if (fontSet.getFontProperties(sha1str) == null) {
			if (!fontSet.addFont(sha1str, item.getInputStream()))
				return null;
		}
		return sha1str;
	}

	private static String replace(String text, char c, String rep) {
		if (text == null)
			return "";
		int i = text.indexOf(c);
		if (i < 0)
			return text;
		StringBuffer sb = new StringBuffer(text.substring(0, i));
		int k;
		i++;
		while ((k = text.indexOf(c, i)) >= 0) {
			sb.append(rep);
			sb.append(text.substring(i, k));
			i = k + 1;
		}
		sb.append(rep);
		sb.append(text.substring(i));
		return sb.toString();
	}

	private static String escape(String text) {
		text = replace(text, '&', "&amp;");
		text = replace(text, '<', "&lt;");
		text = replace(text, '>', "&gt;");
		text = replace(text, '"', "&quot;");
		return text;
	}

	private void writeFontList(HttpServletResponse resp, FontCookieSet cookieSet, String myPath, String ref, String lang)
			throws Exception {
		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), "UTF-8"));
		Iterator hashes = cookieSet.hashes();
		Vector props = new Vector();
		SharedFontSet fontSet = SharedFontSet.getInstance();
		while (hashes.hasNext()) {
			String sha1str = hashes.next().toString();
			FontProperties prop = (FontProperties) fontSet.getFontProperties(sha1str);
			if (prop != null)
				props.add(new FontInfo(sha1str, prop));
		}
		Collections.sort(props);
		out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"");
		out.println("http://www.w3.org/TR/html4/loose.dtd\">");
		out.println("<html>");
		out.println("<head>");
		out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
		out.println("<title>" + getString(S_TITLE, lang) + "</title>");
		out.println("<style type=\"text/css\">");
		out.println(css);
		out.println("</style>");
		out.println("</head>");
		out.println("<body>");
		out.println("<div class=\"tool\">");
		if (ref != null && ref.length() > 0) {
			out.println("<p class=\"back\">");
			out.println("<a href=\"" + escape(ref) + "\"><span>" + getString(S_BACK, lang) + "</a>");
			out.println("</p>");
		}
		out.println("<h1>" + getString(S_ADD, lang) + "</h1>");
		out.println("<form action=\"" + myPath + "\" method=\"post\" enctype=\"multipart/form-data\">");
		out.println("<input type=\"hidden\" name=\"ref\" value=\"" + escape(ref) + "\"/>");
		out.println("<input type=\"hidden\" name=\"lang\" value=\"" + escape(lang) + "\"/>");
		out.println("<input class=\"file\" type=\"file\" name=\"font01\"/><br/>");
		out.println("<input class=\"file\" type=\"file\" name=\"font02\"/><br/>");
		out.println("<input class=\"file\" type=\"file\" name=\"font03\"/><br/>");
		out.println("<input class=\"file\" type=\"file\" name=\"font04\"/><br/>");
		out.println("<input class=\"file\" type=\"file\" name=\"font05\"/><br/>");
		out.println("<input class=\"install\" type=\"submit\" value=\"Install\"/>");
		out.println("</form>");
		if (props.isEmpty()) {
			out.println("<h1 class=\"sep\">" + getString(S_NO_FONTS, lang) + "</h1>");
		} else {
			out.println("<h1 class=\"sep\">" + getString(S_INSTALLED, lang) + "</h1>");
			Iterator fp = props.iterator();
			while (fp.hasNext()) {
				FontInfo fi = (FontInfo) fp.next();
				FontProperties f = fi.prop;
				out.println("<form action=\"" + myPath + "\" method=\"post\">");
				out.println("<input type=\"hidden\" name=\"remove\" value=\"" + fi.sha1str + "\"/>");
				out.println("<input type=\"hidden\" name=\"ref\" value=\"" + escape(ref) + "\"/>");
				out.println("<input type=\"hidden\" name=\"lang\" value=\"" + escape(lang) + "\"/>");
				out.println("<input class=\"delete\" type=\"image\" alt=\"Remove Font\" src=\"/images/del.png\"/>");
				out.println(escape(f.getFamilyName()) + " - " + f.getWeightString() + " " + f.getStyleString());
				out.println("</form>");
			}
		}
		out.println("</div>");
		out.println("</body>");
		out.println("</html>");
		out.flush();
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			String ref = req.getParameter("ref");
			String lang = req.getParameter("lang");
			String pathURI = req.getRequestURI();
			FontCookieSet cookieSet = new FontCookieSet(req);
			writeFontList(resp, cookieSet, pathURI, ref, lang);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			String ref = "";
			String lang = "";
			String pathURI = req.getRequestURI();
			FontCookieSet cookieSet = new FontCookieSet(req);
			if (ServletFileUpload.isMultipartContent(req)) {
				// add some fonts
				DiskFileItemFactory itemFac = new DiskFileItemFactory();
				File repositoryPath = new File(home, "upload");
				repositoryPath.mkdir();
				itemFac.setRepository(repositoryPath);
				ServletFileUpload servletFileUpload = new ServletFileUpload(itemFac);
				List fileItemList = servletFileUpload.parseRequest(req);
				Iterator list = fileItemList.iterator();
				while (list.hasNext()) {
					FileItem item = (FileItem) list.next();
					String paramName = item.getFieldName();
					if (paramName.startsWith("font")) {
						String sha1str = addFont(item);
						if (sha1str != null)
							cookieSet.addFontHash(sha1str);
					} else if (paramName.startsWith("remove")) {
						String sha1str = item.getString();
						cookieSet.removeFontHash(sha1str);
					} else if (paramName.equals("ref")) {
						ref = item.getString();
					} else if (paramName.equals("lang")) {
						lang = item.getString();
					}
					item.delete();
				}
			} else {
				// remove some fonts or just generate the list
				Enumeration names = req.getParameterNames();
				while (names.hasMoreElements()) {
					String paramName = (String) names.nextElement();
					if (paramName.startsWith("remove")) {
						String sha1str = req.getParameter(paramName);
						cookieSet.removeFontHash(sha1str);
					}
				}
				ref = req.getParameter("ref");
				lang = req.getParameter("lang");
			}
			String cookiePath;
			int index = pathURI.lastIndexOf('/');
			cookiePath = pathURI.substring(0, index + 1);
			cookieSet.setCookies(resp, cookiePath, 10 * 365 * 24 * 60 * 60);
			writeFontList(resp, cookieSet, pathURI, ref, lang);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void reportFonts(HttpServletResponse resp, FontEmbeddingReport report, String ref, String lang)
			throws IOException {
		PrintWriter out = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), "UTF-8"));
		Iterator used = report.usedFonts();
		Iterator missing = report.missingFonts();
		Iterator prohibited = report.prohibitedFonts();
		out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"");
		out.println("http://www.w3.org/TR/html4/loose.dtd\">");
		out.println("<html>");
		out.println("<head>");
		out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
		out.println("<title>" + getString(S_REPORT_TITLE, lang) + "</title>");
		out.println("<style type=\"text/css\">");
		out.println(css);
		out.println("</style>");
		out.println("</head>");
		out.println("<body>");
		out.println("<div class=\"tool\">");
		if (ref != null && ref.length() > 0) {
			out.println("<p class=\"back\">");
			out.println("<a href=\"" + escape(ref) + "\"><span>" + getString(S_BACK, lang) + "</a>");
			out.println("</p>");
		}
		if (!used.hasNext()) {
			out.println("<h1>" + getString(S_REPORT_NO_EMBEDDED, lang) + "</h1>");
		} else {
			out.println("<h1>" + getString(S_REPORT_EMBEDDED, lang) + "</h1>");
			while (used.hasNext()) {
				FontProperties f = (FontProperties) used.next();
				out.println("<p class=\"rfont\"><img src=\"/images/used.png\"/ alt=\"used font\"> "
						+ escape(f.getFamilyName()) + " - " + f.getWeightString() + " " + f.getStyleString() + "</p>");
			}
		}
		if (prohibited.hasNext()) {
			out.println("<h1 class=\"sep\">" + getString(S_REPORT_PROHIBITED, lang) + "</h1>");
			while (prohibited.hasNext()) {
				FontProperties f = (FontProperties) prohibited.next();
				out.println("<p class=\"rfont\"><img src=\"/images/prohibit.png\"/ alt=\"prohibited font\"> "
						+ escape(f.getFamilyName()) + " - " + f.getWeightString() + " " + f.getStyleString() + "</p>");
			}
		}
		if (missing.hasNext()) {
			out.println("<h1 class=\"sep\">" + getString(S_REPORT_MISSING, lang) + "</h1>");
			while (missing.hasNext()) {
				FontProperties f = (FontProperties) missing.next();
				out.println("<p class=\"rfont\"><img src=\"/images/missing.png\"/ alt=\"missing font\"> "
						+ escape(f.getFamilyName()) + " - " + f.getWeightString() + " " + f.getStyleString() + "</p>");
			}
		}
		out.println("</div>");
		out.println("</body>");
		out.println("</html>");
		out.flush();
	}
}
