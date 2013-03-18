package com.adobe.dp.epub.web.font;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Iterator;

import com.adobe.dp.epub.web.util.Initializer;
import com.adobe.dp.otf.FileFontInputStream;
import com.adobe.dp.otf.FontInputStream;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.otf.FontProperties;
import com.adobe.dp.otf.FontPropertyConstants;
import com.adobe.dp.otf.OpenTypeFont;

public class SharedFontSet {

	static private SharedFontSet instance = new SharedFontSet();

	private File fontFolder;
	private Hashtable fontNameToKeyMap = new Hashtable();

	class SharedFontLocator extends FontLocator {

		Hashtable keyToName;
		FontLocator base;

		SharedFontLocator(Hashtable keyToName, FontLocator base) {
			this.keyToName = keyToName;
			this.base = base;
		}

		FontProperties substitute(FontProperties key) {
			if (key.getStyle() == FontPropertyConstants.STYLE_ITALIC && key.getFamilyName().equals("Tahoma")) {
				// workaround: Tahoma does not have italic, replace with Verdana
				key = new FontProperties("Verdana", key.getWeight(), key.getStyle());
			}
			return key;
		}

		String getFontSource(FontProperties key) {
			key = substitute(key);
			String name = (String) keyToName.get(key);
			if (name == null) {
				// try a bit bolder...
				FontProperties key1 = new FontProperties(key.getFamilyName(), key.getWeight() + 100, key.getStyle());
				name = (String) keyToName.get(key1);
				if (name == null) {
					// ...and a bit lighter
					key1 = new FontProperties(key.getFamilyName(), key.getWeight() - 100, key.getStyle());
					name = (String) keyToName.get(key);
					if (name == null)
						return null;
				}
			}
			return name;
		}

		public FontInputStream locateFont(FontProperties key) throws IOException {
			String src = getFontSource(key);
			if (src == null) {
				if (base == null)
					return null;
				return base.locateFont(key);
			}
			File file = new File(fontFolder, src);
			return new FileFontInputStream(file);
		}

		public boolean hasFont(FontProperties key) {
			return getFontSource(key) != null || (base != null && base.hasFont(key));
		}

	}

	private SharedFontSet() {
		File home = Initializer.getEPubGenHome();
		fontFolder = new File(home, "uploadedFonts");
		fontFolder.mkdirs();
		String[] list = fontFolder.list();
		if (list != null) {
			for (int i = 0; i < list.length; i++)
				if (list[i].endsWith("=")) {
					loadFont(list[i]);
				}
		}
	}

	private boolean loadFont(String name) {
		File fontFile = new File(fontFolder, name);
		FontProperties key = loadFont(fontFile);
		if (key == null)
			return false;
		fontNameToKeyMap.put(name, key);
		return true;
	}

	private FontProperties loadFont(File fontFile) {
		try {
			if (!fontFile.exists())
				return null;
			FontInputStream fin = new FileFontInputStream(fontFile);
			OpenTypeFont font = new OpenTypeFont(fin, true);
			fin.close();
			if (!font.canEmbedForReading())
				return null;
			return new FontProperties(font.getFamilyName(), font.getWeight(), font.getStyle());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public FontProperties getFontProperties(String sha1str) {
		FontProperties prop = (FontProperties) fontNameToKeyMap.get(sha1str);
		if (prop == null) {
			loadFont(sha1str);
			prop = (FontProperties) fontNameToKeyMap.get(sha1str);
		}
		return prop;
	}

	public boolean addFont(String sha1str, InputStream in) {
		try {
			if (getFontProperties(sha1str) != null) {
				in.close();
				return false;
			}
			File fontFile = File.createTempFile("fnt", ".tmp", fontFolder);
			FileOutputStream out = new FileOutputStream(fontFile);
			byte[] buffer = new byte[4096];
			int len;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
			out.close();
			in.close();
			FontProperties prop = loadFont(fontFile);
			if (prop == null) {
				fontFile.delete();
				return false;
			}
			fontNameToKeyMap.put(sha1str, prop);
			File dest = new File(fontFolder, sha1str);
			if (!fontFile.renameTo(dest)) {
				fontFile.delete();
				return false;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public FontLocator getFontLocator(FontCookieSet cookies, FontLocator base) {
		Iterator it = cookies.hashes();
		Hashtable fontSet = new Hashtable();
		while (it.hasNext()) {
			String hash = (String) it.next();
			FontProperties key = (FontProperties) fontNameToKeyMap.get(hash);
			if (key != null) {
				fontSet.put(key, hash);
			}
		}
		return new SharedFontLocator(fontSet, base);
	}

	public static SharedFontSet getInstance() {
		return instance;
	}

}
