package com.adobe.dp.office.conv;

import com.adobe.dp.epub.conv.ConversionClient;
import com.adobe.dp.epub.conv.ConversionService;
import com.adobe.dp.epub.io.OCFContainerWriter;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.util.ConversionTemplate;
import com.adobe.dp.epub.util.Translit;
import com.adobe.dp.office.rtf.RTFDocument;
import com.adobe.dp.otf.ChainedFontLocator;
import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Properties;

public class RTFConversionService extends ConversionService {

	BufferedImage rtficon;

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

	public RTFConversionService() {
		InputStream png = DOCXConversionService.class.getResourceAsStream("docx.png");
		try {
			rtficon = ImageIO.read(png);
		} catch (IOException e) {
			e.printStackTrace();
		}
        fontLocator = DefaultFontLocator.getInstance(DefaultFontLocator.BUILT_IN_DIRS);
	}

	public boolean canConvert(File src) {
		String name = src.getName().toLowerCase();
		return name.endsWith(".rtf");
	}

	public boolean canUse(File src) {
		return false;
	}

	public File convert(File src, File[] aux, ConversionClient client, PrintWriter log) {
		try {
			RTFDocument doc = new RTFDocument(src);
			Publication epub = new Publication();
			epub.setTranslit(translit);
			epub.useAdobeFontMangling();
			RTFConverter conv = new RTFConverter(doc, epub);
			conv.setLog(log);
			epub.setTranslit(translit);
			if (adobeMangling)
				epub.useAdobeFontMangling();
			else
				epub.useIDPFFontMangling();
			if (aux != null && aux.length > 0) {
				ConversionTemplate template = new ConversionTemplate(aux);
				FontLocator customLocator = template.getFontLocator();
				fontLocator = new ChainedFontLocator(customLocator, fontLocator);
			}
			conv.convert();
			if (embedFonts)
				conv.embedFonts(fontLocator);

			String title = epub.getDCMetadata("title");
			String fname;
			if (title == null) {
				fname = src.getName();
				epub.addDCMetadata("title", fname);
				if (fname.endsWith(".rtf"))
					fname = fname.substring(0, fname.length() - 4);
			} else {
				fname = Translit.translit(title).replace(' ', '_').replace('\t', '_').replace('\n', '_').replace('\r',
						'_').replace('/', '_').replace('\\', '_').replace('\"', '_');
			}

			File outFile = client.makeFile(fname + ".epub");
			OutputStream out = new FileOutputStream(outFile);
			OCFContainerWriter container = new OCFContainerWriter(out);
			epub.serialize(container);
			return outFile;
		} catch (Exception e) {
			e.printStackTrace(log);
		}
		return null;
	}

	public Image getIcon(File src) {
		return rtficon;
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
}
