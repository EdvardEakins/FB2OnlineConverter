package com.adobe.dp.office.conv;

import com.adobe.dp.epub.conv.CLDriver;
import com.adobe.dp.epub.conv.ConversionClient;
import com.adobe.dp.epub.conv.ConversionService;
import com.adobe.dp.epub.conv.GUIDriver;
import com.adobe.dp.epub.io.OCFContainerWriter;
import com.adobe.dp.epub.io.ZipContainerSource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.util.ConversionTemplate;
import com.adobe.dp.epub.util.Translit;
import com.adobe.dp.office.word.WordDocument;
import com.adobe.dp.otf.ChainedFontLocator;
import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Properties;

public class DOCXConversionService extends ConversionService {

	BufferedImage docxicon;

	boolean embedFonts = true;

	boolean adobeMangling = true;

	boolean translit = true;

	boolean pageBreaks = false;

    private FontLocator fontLocator;

	boolean getBooleanProperty(Properties prop, String name, boolean def) {
		String s = prop.getProperty(name);
		if (s == null)
			return def;
		return s.toLowerCase().startsWith("t");
	}

	public DOCXConversionService() {
		InputStream png = DOCXConversionService.class.getResourceAsStream("docx.png");
		try {
			docxicon = ImageIO.read(png);
		} catch (IOException e) {
			e.printStackTrace();
		}
        fontLocator = DefaultFontLocator.getInstance(DefaultFontLocator.BUILT_IN_DIRS);
	}

	public boolean canConvert(File src) {
		String name = src.getName().toLowerCase();
		return name.endsWith(".docx");
	}

	public boolean canUse(File src) {
		return false;
	}

	public File convert(File src, File[] aux, ConversionClient client, PrintWriter log) {
		ZipContainerSource resources = null;
		try {
			WordDocument doc = new WordDocument(src);
			Publication epub = new Publication();
			epub.setTranslit(translit);
			epub.useAdobeFontMangling();
			DOCXConverter conv = new DOCXConverter(doc, epub);
			conv.setLog(log);
			resources = new ZipContainerSource(src);
			conv.setWordResources(resources);
			epub.setTranslit(translit);
			if (adobeMangling)
				epub.useAdobeFontMangling();
			else
				epub.useIDPFFontMangling();
			if (aux != null && aux.length > 0) {
				ConversionTemplate template = new ConversionTemplate(aux);
				FontLocator customLocator = template.getFontLocator();
				fontLocator = new ChainedFontLocator(customLocator, fontLocator);
				// Stylesheet stylesheet = template.getStylesheet();
				// if (stylesheet != null)
				// conv.setStylesheet(stylesheet);
			}
			conv.setFontLocator(fontLocator);
			if (pageBreaks) {
				conv.useWordPageBreaks();
				epub.usePageMap();
			}
			conv.convert();
			if (embedFonts)
				conv.embedFonts();

			String title = epub.getDCMetadata("title");
			String fname;
			if (title == null) {
				fname = src.getName();
				epub.addDCMetadata("title", fname);
				if (fname.endsWith(".docx"))
					fname = fname.substring(0, fname.length() - 5);
			} else {
				fname = Translit.makeFileName(title);
			}

			File outFile = client.makeFile(fname + ".epub");
			OutputStream out = new FileOutputStream(outFile);
			OCFContainerWriter container = new OCFContainerWriter(out);
			epub.serialize(container);
			return outFile;
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(log);
		} finally {
			if (resources != null) {
				try {
					resources.close();
				} catch (Exception e) {

				}
			}
		}
		return null;
	}

	public Image getIcon(File src) {
		return docxicon;
	}

	public void setProperties(Properties prop) {
		embedFonts = getBooleanProperty(prop, "embedFonts", embedFonts);
		adobeMangling = getBooleanProperty(prop, "adobeMangling", adobeMangling);
		translit = getBooleanProperty(prop, "translit", translit);
		pageBreaks = getBooleanProperty(prop, "pageBreaks", pageBreaks);
        String  dirs = prop.getProperty("fontDirs");
        if (dirs != null) {
            fontLocator = DefaultFontLocator.getInstance(dirs);
        }
	}

	public static void main(String[] args) {
		if (args.length > 0)
			CLDriver.main(args);
		else
			GUIDriver.main(args);
	}

}
