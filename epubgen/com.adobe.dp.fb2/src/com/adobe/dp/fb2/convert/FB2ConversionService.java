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
package com.adobe.dp.fb2.convert;

import com.adobe.dp.epub.conv.ConversionClient;
import com.adobe.dp.epub.conv.ConversionService;
import com.adobe.dp.epub.io.OCFContainerWriter;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.epub.util.ConversionTemplate;
import com.adobe.dp.epub.util.Translit;
import com.adobe.dp.fb2.FB2Document;
import com.adobe.dp.fb2.FB2TitleInfo;
import com.adobe.dp.otf.ChainedFontLocator;
import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Properties;

public class FB2ConversionService extends ConversionService {

	BufferedImage fb2icon;
	boolean embedFonts = true;
	boolean adobeMangling = true;
	boolean translit = true;
    private FontLocator fontLocator;

    boolean getBooleanProperty(Properties prop, String name, boolean def) {
		String s = prop.getProperty(name);
		if (s == null)
			return def;
		return s.toLowerCase().startsWith("t");
	}

	public void setProperties(Properties prop) {
		embedFonts = getBooleanProperty(prop, "embedFonts", embedFonts);
		adobeMangling = getBooleanProperty(prop, "adobeMangling", adobeMangling);
		translit = getBooleanProperty(prop, "translit", translit);
        String  dirs = prop.getProperty("fontDirs");
        if (dirs != null) {
            fontLocator = DefaultFontLocator.getInstance(dirs);
        }
	}

	public FB2ConversionService() {
		InputStream png = FB2ConversionService.class
				.getResourceAsStream("fb2.png");
		try {
			fb2icon = ImageIO.read(png);
		} catch (IOException e) {
			e.printStackTrace();
		}
        fontLocator = DefaultFontLocator.getInstance(DefaultFontLocator.BUILT_IN_DIRS);
	}

	public boolean canConvert(File src) {
		String name = src.getName().toLowerCase();
		return name.endsWith(".fb2") || name.endsWith("fb2.zip");
	}

	public boolean canUse(File src) {
		return false;
	}

	public File convert(File src, File[] aux, ConversionClient client, PrintWriter log) {
		try {
			InputStream fb2in = new FileInputStream(src);
			FB2Document doc = new FB2Document(fb2in);
			Publication epub = new Publication();
			epub.setTranslit(translit);
			if (adobeMangling)
				epub.useAdobeFontMangling();
			else
				epub.useIDPFFontMangling();
			fb2in.close();
			FB2TitleInfo bookInfo = doc.getTitleInfo();
			String title = (bookInfo == null ? null : bookInfo.getBookTitle());
			String fname;
			if (title == null)
				fname = "book";
			else
				fname = Translit.translit(title).replace(' ', '_').replace(
						'\t', '_').replace('\n', '_').replace('\r', '_')
						.replace('/', '_').replace('\\', '_').replace('\"', '_');
			if( fname.length() == 0 )
				fname = "book";
			File outFile = client.makeFile(fname + ".epub");
			OutputStream out = new FileOutputStream(outFile);
			OCFContainerWriter container = new OCFContainerWriter(out);
			FB2Converter conv = new FB2Converter();
            conv.setFontLocator(fontLocator);
			conv.setLog(log);
			if( aux != null && aux.length > 0 ) {
				ConversionTemplate template = new ConversionTemplate(aux);
				FontLocator customLocator = template.getFontLocator();
				fontLocator = new ChainedFontLocator(customLocator, fontLocator);
				Stylesheet stylesheet = template.getStylesheet();
				if( stylesheet != null )
					conv.setTemplate(stylesheet.getCSS());
			}
			conv.convert(doc, epub);
			if( embedFonts )
				conv.embedFonts();
			epub.serialize(container);
			return outFile;
		} catch (Exception e) {
			e.printStackTrace(log);
//			e.printStackTrace();
            throw new RuntimeException(e);
		}
	}

	public Image getIcon(File src) {
		return fb2icon;
	}

	public static void main(String[] args) {
		//com.adobe.dp.epub.conv.GUIDriver.main(args);
		com.adobe.dp.epub.conv.CLDriver.main(args);
	}
}
