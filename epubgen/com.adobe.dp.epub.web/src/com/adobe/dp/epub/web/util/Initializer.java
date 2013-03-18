package com.adobe.dp.epub.web.util;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;

public class Initializer {

	static File home;
	static File epubgenHome;
	static File uploadDir;
	static File workDir;
	static File logDir;
	
	static DefaultFontLocator defaultFontLocator;

	static {
		try {
			home = new File("/home/soroto2");
			if (!home.isDirectory())
				home = new File(System.getProperty("user.home"));
			epubgenHome = new File(home, "epubgen");
			epubgenHome.mkdirs();
			File fontDir = new File(epubgenHome, "defaultFonts");
			uploadDir = new File( epubgenHome, "upload" );
			uploadDir.mkdir();
			workDir = new File( epubgenHome, "work" );
			workDir.mkdir();
			logDir = new File( epubgenHome, "log" );
			logDir.mkdir();
			RollingFileAppender appender = new RollingFileAppender();
			appender.setFile((new File(logDir, "epubconv.log")).getAbsolutePath());
			appender.setBufferedIO(false);
			String pattern = "%d{DATE} %-5p [%c@%t]: %m%n";
			appender.setLayout(new PatternLayout(pattern));
			appender.setMaxFileSize("1Mb");
			appender.setMaxBackupIndex(3);
			appender.activateOptions();
			BasicConfigurator.configure(appender);
			String[] dirs = { fontDir.getAbsolutePath() };
			defaultFontLocator = new DefaultFontLocator(dirs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void init() {
		// a call to make sure class is loaded
	}
	
	public static File getEPubGenHome() {
		return epubgenHome;
	}

	public static File getWorkDir() {
		return workDir;
	}

	public static File getUploadDir() {
		return uploadDir;
	}

	public static FontLocator getDefaultFontLocator() {
		return defaultFontLocator;
	}
}
