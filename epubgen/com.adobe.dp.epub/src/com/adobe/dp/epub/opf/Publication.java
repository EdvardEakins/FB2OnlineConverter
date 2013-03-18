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

package com.adobe.dp.epub.opf;

import com.adobe.dp.epub.dtd.EPUBEntityResolver;
import com.adobe.dp.epub.io.ContainerSource;
import com.adobe.dp.epub.io.ContainerWriter;
import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.io.ZipContainerSource;
import com.adobe.dp.epub.ncx.TOCEntry;
import com.adobe.dp.epub.ops.OPSDocument;
import com.adobe.dp.epub.otf.FontEmbeddingReport;
import com.adobe.dp.epub.otf.FontSubsetter;
import com.adobe.dp.epub.util.TOCLevel;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.xml.util.SMapImpl;
import com.adobe.dp.xml.util.XMLSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

/**
 * Publication represents an EPUB document. At a minimum, Publication must have
 * OPF resource (that contains metadata, manifest and a spine), NCX resource
 * (that contains table of contents) and some content resources (also called
 * chapters).
 */
public class Publication {

	/**
	 * Dublin Core namespace
	 */
	public static final String dcns = "http://purl.org/dc/elements/1.1/";

	/**
	 * OCF namespace
	 */
	public static final String ocfns = "urn:oasis:names:tc:opendocument:xmlns:container";

	/**
	 * XML encryption namespace
	 */
	public static final String encns = "http://www.w3.org/2001/04/xmlenc#";

	/**
	 * Adobe Digital Edition encoding namespace (for font embedding)
	 */
	public static final String deencns = "http://ns.adobe.com/digitaleditions/enc";

	boolean translit;

	boolean useIDPFFontMangling = true;

	Hashtable resourcesByName = new Hashtable();

	Hashtable resourceRefsByName = new Hashtable();

	Hashtable resourcesById = new Hashtable();

	Hashtable namingIndices = new Hashtable();

	Vector spine = new Vector();

	Vector metadata = new Vector();

	Resource toc; // can be unparsed

	OPFResource opf; // has to be parsed!

	int idCount = 1;

	private String contentFolder;

	private byte[] idpfMask;

	private byte[] adobeMask;

	PageMapResource pageMap;

	ContainerSource containerSource;
    private BitmapImageResource coverImage;

    public void setCoverImage(BitmapImageResource coverImage) {
        this.coverImage = coverImage;
    }

    public BitmapImageResource getCoverImage() {
        return coverImage;
    }

    static class SimpleMetadata {

		String ns;

		String name;

		String value;

		SimpleMetadata(String ns, String name, String value) {
			this.name = name;
			this.ns = ns;
			this.value = value;
		}
	}

	/**
	 * Create a new empty EPUB document. Content folder is set to "OPS".
	 */
	public Publication() {
		this("OPS");
	}

	/**
	 * Create a new empty EPUB document with the given content folder.
	 */
	public Publication(String contentFolder) {
		this.contentFolder = contentFolder;
		toc = new NCXResource(this, contentFolder + "/toc.ncx");
		opf = new OPFResource(this, contentFolder + "/content.opf");
		resourcesByName.put(toc.name, toc);
		resourcesByName.put(opf.name, opf);
	}

	/**
	 * Create a new EPUB document by reading it from a file.
	 */
	public Publication(File file) throws Exception {
		this(new ZipContainerSource(file));
	}

	public Publication(ContainerSource containerSource) throws Exception {
		this.containerSource = containerSource;
		DataSource cont = containerSource.getDataSource("META-INF/container.xml");
		if (cont == null)
			throw new IOException("Not an EPUB file: META-INF/container.xml missing");
		String opfName = processOCF(cont.getInputStream());
		opf = new OPFResource(this, opfName);
		resourcesByName.put(opfName, opf);
		opf.load(containerSource, opfName);
		Iterator entries = containerSource.getResourceList();
		while (entries.hasNext()) {
			String name = (String) entries.next();
			if (name.startsWith("META-INF/") || name.equals("mimetype") || name.equals(opfName))
				continue;
			if (resourcesByName.get(name) == null) {
				loadMissingResource(name);
			}
		}
	}

	private void loadMissingResource(String name) throws Exception {
		String s = name.toLowerCase();
		if (s.endsWith(".jpeg") || s.endsWith(".jpg"))
			loadResource(name, "image/jpeg");
		else if (s.endsWith(".png"))
			loadResource(name, "image/png");
		else if (s.endsWith(".gif"))
			loadResource(name, "image/gif");
		else if (s.endsWith(".xml"))
			loadResource(name, "text/xml");
		else if (s.endsWith(".opf"))
			; // ignore
		else if (s.endsWith(".ncx"))
			loadResource(name, "application/x-dtbncx+xml");
		else if (s.endsWith(".svg"))
			loadResource(name, "image/svg+xml");
		else if (s.endsWith(".htm") || s.endsWith(".html") || s.endsWith(".xhtml"))
			loadResource(name, "application/xhtml+xml");
		else if (s.endsWith(".ttf") || s.endsWith(".otf"))
			loadResource(name, "application/vnd.ms-opentype");
		else
			loadResource(name, "application/octet-stream");
	}

	/**
	 * Return this EPUB package content folder. OPF file is located in that
	 * folder; typically all other content is stored in this folder or its
	 * subfolders as well.
	 */
	public String getContentFolder() {
		return contentFolder;
	}

	/**
	 * Make this EPUB document use Adobe proprietary font mangling method when
	 * serializing. This method works in all versions of Digital Editions and
	 * Adobe Reader Mobile SDK-based devices
	 * 
	 * @see <a href=
	 *      "http://www.adobe.com/devnet/digitalpublishing/pdfs/content_protection.pdf"
	 *      >Adobe&#32;documentation</a>
	 */
	public void useAdobeFontMangling() {
		useIDPFFontMangling = false;
	}

	/**
	 * Make this EPUB document use standard font mangling method when
	 * serializing. This method is not supported in any reading system as of
	 * today (May 2009), but that is expected to change.
	 * 
	 * @see <a href=
	 *      "http://www.openebook.org/doc_library/informationaldocs/FontManglingSpec.html"
	 *      >IDPF&#32;documentation</a>
	 */
	public void useIDPFFontMangling() {
		useIDPFFontMangling = true;
	}

	/**
	 * Transliterate cyrillic metadata when serializing. This is useful when
	 * formatting books written in cyrillic for reading on non-localized reading
	 * devices. This setting should be avoided.
	 * 
	 * @param translit
	 *            true if cyrillic metadata should be transliterated
	 */
	public void setTranslit(boolean translit) {
		this.translit = translit;
	}

	public void splitLargeChapters(int sizeToSplit) {
		for (int i = 0; i < spine.size(); i++) {
			Resource item = (Resource) spine.elementAt(i);
			if (item instanceof OPSResource) {
				OPSResource[] split = ((OPSResource) item).splitLargeChapter(this, sizeToSplit);
				if (split != null) {
					spine.remove(i);
					for (int j = 0; j < split.length; j++) {
						spine.insertElementAt(split[j], i);
						i++;
					}
					i--;
				}
			}
		}
	}

	public void splitLargeChapters() {
		splitLargeChapters(100000);
	}

	public void generateTOCFromHeadings(int depth) {
		TOCEntry entry = getTOC().getRootTOCEntry();
		entry.removeAll();
		Iterator spine = spine();
		Stack headings = new Stack();
		headings.push(new TOCLevel(0, entry));
		while (spine.hasNext()) {
			Resource r = (Resource) spine.next();
			if (r instanceof OPSResource)
				((OPSResource) r).generateTOCFromHeadings(headings, depth);
		}
	}

	public void generateTOCFromHeadings() {
		generateTOCFromHeadings(6);
	}

	/**
	 * Determine if cyrillic metadata should be transliterated when serializing.
	 * 
	 * @see #setTranslit
	 * @return true if cyrillic metadata should be transliterated
	 */
	public boolean isTranslit() {
		return translit;
	}

	private static void addRandomHexDigit(StringBuffer sb, Random r, int count, int mask, int value) {
		for (int i = 0; i < count; i++) {
			int v = (r.nextInt(16) & mask) | value;
			if (v < 10)
				sb.append((char) ('0' + v));
			else
				sb.append((char) (('a' - 10) + v));
		}
	}

	/**
	 * Generate random UUID and add it as a publication's identifier (in URN
	 * format, i.e. prefixed with "urn:uuid:")
	 * 
	 * @return generated identifier
	 */
	public String generateRandomIdentifier() {
		// generate v4 UUID
		StringBuffer sb = new StringBuffer("urn:uuid:");
		SecureRandom sr = new SecureRandom();
		addRandomHexDigit(sb, sr, 8, 0xF, 0);
		sb.append('-');
		addRandomHexDigit(sb, sr, 4, 0xF, 0);
		sb.append('-');
		sb.append('4');
		addRandomHexDigit(sb, sr, 3, 0xF, 0);
		sb.append('-');
		addRandomHexDigit(sb, sr, 1, 0x3, 0x8);
		addRandomHexDigit(sb, sr, 3, 0xF, 0);
		sb.append('-');
		addRandomHexDigit(sb, sr, 12, 0xF, 0);
		String id = sb.toString();
		this.addDCMetadata("identifier", id);
		return id;
	}

	/**
	 * Get table-of-content resource. Each Publication has one.
	 * 
	 * @return table-of-content resource
	 */
	public NCXResource getTOC() {
		if (!(toc instanceof NCXResource)) {
			return null;
		}
		return (NCXResource) toc;
	}

	/**
	 * Get OPF resource. Each Publication has one. OPF resource lists other
	 * resources in the Publication, defines their types and determines chapter
	 * order.
	 * 
	 * @return OPF resource
	 */
	public OPFResource getOPF() {
		return opf;
	}

	/**
	 * Get Dublin Core metadata value. Note that multiple metadata values of the
	 * same type are allowed and this method returns only the first one.
	 * 
	 * @param name
	 *            type of Dublin Core metadata, such as "creator" or "title"
	 * @return metadata element value; null if no such value exists
	 */
	public String getDCMetadata(String name) {
		return getMetadata(dcns, name, 0);
	}

	/**
	 * Add Dublin Core metadata value. Note that multiple metadata values of the
	 * same type are allowed.
	 * 
	 * @param name
	 *            type of Dublin Core metadata, such as "creator" or "title"
	 * @param value
	 *            metadata element value
	 */
	public void addDCMetadata(String name, String value) {
		if (value != null) {
			addMetadata(dcns, name, value);
		}
	}

	/**
	 * Get metadata value. Note that multiple metadata values of the same type
	 * are allowed. This method can be used to iterate over
	 * 
	 * @param ns
	 *            metadata element namespace, i.e Publication.dcns
	 * @param name
	 *            type of the metadata element
	 * @return metadata element value; null if no such value exists
	 */
	public String getMetadata(String ns, String name, int index) {
		Iterator it = metadata.iterator();
		while (it.hasNext()) {
			SimpleMetadata md = (SimpleMetadata) it.next();
			if ((ns == null ? md.ns == null : md.ns != null && md.ns.equals(ns)) && md.name.equals(name)) {
				if (index == 0)
					return md.value;
				index--;
			}
		}
		return null;
	}

	/**
	 * Add Dublin Core metadata value. Note that multiple metadata values of the
	 * same type are allowed.
	 * 
	 * @param ns
	 *            metadata element namespace, i.e Publication.dcns
	 * @param name
	 *            type of the metadata element
	 * @param value
	 *            metadata element value
	 */
	public void addMetadata(String ns, String name, String value) {
		if (value == null)
			return;
		metadata.add(new SimpleMetadata(ns, name, value));
	}

	public void addPrimaryIdentifier(String value) {
		metadata.add(0, new SimpleMetadata(OPFResource.dcns, "identifier", value));
	}

	private String getAdobePrimaryUUID() {
		Iterator it = metadata.iterator();
		while (it.hasNext()) {
			SimpleMetadata item = (SimpleMetadata) it.next();
			if (item.ns != null && item.ns.equals(dcns) && item.name.equals("identifier")
					&& item.value.startsWith("urn:uuid:"))
				return item.value.substring(9);
		}
		String value = generateRandomIdentifier();
		return value.substring(9);
	}

	/**
	 * Return Publication unique identifier; create one (in the form
	 * "urn:uuid:UUID") if it does not exist
	 * 
	 * @return unique identifier
	 */
	public String getPrimaryIdentifier() {
		Iterator it = metadata.iterator();
		while (it.hasNext()) {
			SimpleMetadata item = (SimpleMetadata) it.next();
			if (item.ns != null && item.ns.equals(dcns) && item.name.equals("identifier"))
				return item.value;
		}
		return generateRandomIdentifier();
	}

	/**
	 * Create a unique resource name using baseName as a template. If baseName
	 * looks like "name.foo", "name.foo" name will be tried first. If it already
	 * exists, names like "name-1.foo", "name-2.foo" will be tried until unused
	 * resource name is found.
	 * 
	 * @param baseName
	 *            desired resource name
	 * @return unique resource name based on the desired one
	 */
	public String makeUniqueResourceName(String baseName) {
		if (resourcesByName.get(baseName) == null)
			return baseName;
		int index = baseName.lastIndexOf('.');
		String suffix;
		if (index < 0) {
			suffix = "";
		} else {
			suffix = baseName.substring(index);
			baseName = baseName.substring(0, index);
		}
		Integer lastIndex = (Integer) namingIndices.get(baseName);
		if (lastIndex == null)
			index = 1;
		else
			index = lastIndex.intValue() + 1;
		String name;
		while (true) {
			name = baseName + "-" + index + suffix;
			if (resourcesByName.get(name) == null)
				break;
			index++;
		}
		namingIndices.put(baseName, new Integer(index));
		return name;
	}

	/**
	 * Create a new XHTML OPS resource and insert it into this Publication (but
	 * not spine!).
	 * 
	 * @param name
	 *            OPS resource name
	 * @return new OPSResource
	 */
	public OPSResource createOPSResource(String name) {
		if (resourcesByName.get(name) != null)
			throw new RuntimeException("Resource already exists: " + name);
		return createOPSResource(name, "application/xhtml+xml");
	}

	public OPSResource createOPSResource(String name, String mediaType) {
		if (resourcesByName.get(name) != null)
			throw new RuntimeException("Resource already exists: " + name);
		OPSResource resource = new OPSResource(this, name, mediaType);
		resourcesByName.put(name, resource);
		return resource;
	}

	public void removeResource(Resource r) {
		resourcesByName.remove(r.name);
		if (r.id != null) {
			resourcesById.remove(r.id);
			r.id = null;
		}
		if (toc == r)
			toc = null;
	}

	public void renameResource(Resource r, String newName) {
		if (resourcesByName.get(newName) != null)
			throw new RuntimeException("Resource already exists: " + newName);
		resourcesByName.remove(r.name);
		ResourceRef ref = (ResourceRef) resourceRefsByName.remove(r.name);
		r.name = newName;
		resourcesByName.put(newName, r);
		if (ref != null) {
			ref.name = newName;
			resourceRefsByName.put(newName, ref);
		}
	}

	/**
	 * Create new bitmap image resource and insert it into this Publication.
	 * 
	 * @param name
	 *            resource name
	 * @param mediaType
	 *            resource MIME type, i.g. "image/jpeg"
	 * @param data
	 *            resource data
	 * @return new BitmapImageResource
	 */
	public BitmapImageResource createBitmapImageResource(String name, String mediaType, DataSource data) {
		if (resourcesByName.get(name) != null)
			throw new RuntimeException("Resource already exists: " + name);
		BitmapImageResource resource = new BitmapImageResource(this, name, mediaType, data);
		resourcesByName.put(name, resource);
		return resource;
	}

	/**
	 * Create new CSS resource and insert it into this Publication
	 * 
	 * @param name
	 *            resource name
	 * @return new StyleResource
	 */
	public StyleResource createStyleResource(String name) {
		if (resourcesByName.get(name) != null)
			throw new RuntimeException("Resource already exists: " + name);
		StyleResource resource = new StyleResource(this, name);
		resourcesByName.put(name, resource);
		return resource;
	}

	/**
	 * Create new generic resource and insert it into this Publication.
	 * 
	 * @param name
	 *            resource name
	 * @param mediaType
	 *            resource MIME type
	 * @param data
	 *            resource data
	 * @return new Resource
	 */
	public Resource createGenericResource(String name, String mediaType, DataSource data) {
		if (resourcesByName.get(name) != null)
			throw new RuntimeException("Resource already exists: " + name);
		Resource resource = new Resource(this, name, mediaType, data);
		resourcesByName.put(name, resource);
		return resource;
	}

	/**
	 * Create new embedded font resource and insert it into this Publication.
	 * Font resources differ from genertic binary resources in that they get
	 * "mangled" during serialization.
	 * 
	 * @param name
	 *            resource name
	 * @param data
	 *            resource data
	 * @return new FontResource
	 */
	public FontResource createFontResource(String name, DataSource data) {
		if (resourcesByName.get(name) != null)
			throw new RuntimeException("Resource already exists: " + name);
		FontResource resource = new FontResource(this, name, data);
		resourcesByName.put(name, resource);
		return resource;
	}

	/**
	 * Add another resource to the spine (chapter list)
	 * 
	 * @param resource
	 *            resource to add; must be one of the existing OPS resources in
	 *            this Publication
	 */
	public void addToSpine(Resource resource) {
		spine.add(resource);
	}

	/**
	 * Iterate all resources in this Publication.
	 * 
	 * @return resource iterator
	 */
	public Iterator resources() {
		return resourcesByName.values().iterator();
	}

	/**
	 * Iterate resources in the spine (chapter list).
	 * 
	 * @return spine iterator
	 */
	public Iterator spine() {
		return spine.iterator();
	}

	public Resource getResourceByName(String name) {
		return (Resource) resourcesByName.get(name);
	}

	String assignId(Resource res) {
		if (res.id == null) {
			res.id = makeId();
			resourcesById.put(res.id, res);
		}
		return res.id;
	}

	String makeId() {
		while (true) {
			String id = "id" + (idCount++);
			if (resourcesById.get(id) == null)
				return id;
		}
	}

	public void usePageMap() {
		if (pageMap == null) {
			String name = makeUniqueResourceName("OPS/pageMap.xml");
			pageMap = new PageMapResource(this, name);
			resourcesByName.put(name, pageMap);
		}
	}

    /**
	 * Add embedded font resources to this Publication and add corresponding
	 * &#064;font-face rules for the embedded fonts. Fonts are taken from the
	 * supplied font locator.
	 * 
	 * @param styleResource
	 *            style resource where &#064;font-face rules will be added
	 * @param locator
	 *            FontLocator object to lookup font files
	 */
	public FontEmbeddingReport addFonts(StyleResource styleResource, FontLocator locator) {
		return addFonts(styleResource, locator, false);
	}

	/**
	 * Add embedded font resources to this Publication and add corresponding
	 * &#064;font-face rules for the embedded fonts. Fonts are taken from the
	 * supplied font locator.
	 * 
	 * @param styleResource
	 *            style resource where &#064;font-face rules will be added
	 * @param locator
	 *            FontLocator object to lookup font files
	 * @param addStyle
	 *            If a reference to the style resource should be automatically
	 *            added to each OPS resource
	 */
	public FontEmbeddingReport addFonts(StyleResource styleResource, FontLocator locator, boolean addStyle) {
		FontSubsetter subsetter = new FontSubsetter(this, styleResource, locator);
		Iterator res = resources();
		while (res.hasNext()) {
			Object next = res.next();
			if (next instanceof OPSResource) {
				OPSResource ops = (OPSResource) next;
				OPSDocument doc = ops.getDocument();
				doc.addStyleResource(styleResource.getResourceRef());
				doc.addFonts(subsetter, styleResource);
			}
		}
		subsetter.addFonts(this);
		return subsetter;
	}

	static String strip(String source) {
		return source.replaceAll("\\s+", "");
	}

	/*
	 * This starts with the "unique-identifier", strips the whitespace, and
	 * applies SHA1 hash giving a 20 byte key that we can apply to the font
	 * file.
	 * 
	 * See:
	 * http://www.openebook.org/doc_library/informationaldocs/FontManglingSpec
	 * .html
	 */
	byte[] makeIDPFXORMask() {
		if (idpfMask == null) {
			try {
				MessageDigest sha = MessageDigest.getInstance("SHA-1");
				String temp = strip(getPrimaryIdentifier());
				sha.update(temp.getBytes("UTF-8"), 0, temp.length());
				idpfMask = sha.digest();
			} catch (NoSuchAlgorithmException e) {
				System.err.println("No such Algorithm (really, did I misspell SHA-1?");
				System.err.println(e.toString());
				return null;
			} catch (IOException e) {
				System.err.println("IO Exception. check out mask.write...");
				System.err.println(e.toString());
				return null;
			}
		}
		return idpfMask;
	}

	byte[] makeAdobeXORMask() {
		if (adobeMask != null)
			return adobeMask;
		ByteArrayOutputStream mask = new ByteArrayOutputStream();
		String opfUID = getAdobePrimaryUUID();
		int acc = 0;
		int len = opfUID.length();
		for (int i = 0; i < len; i++) {
			char c = opfUID.charAt(i);
			int n;
			if ('0' <= c && c <= '9')
				n = c - '0';
			else if ('a' <= c && c <= 'f')
				n = c - ('a' - 10);
			else if ('A' <= c && c <= 'F')
				n = c - ('A' - 10);
			else
				continue;
			if (acc == 0) {
				acc = 0x100 | (n << 4);
			} else {
				mask.write(acc | n);
				acc = 0;
			}
		}
		if (mask.size() != 16)
			return null;
		adobeMask = mask.toByteArray();
		return adobeMask;
	}

	/**
	 * Serialize Publication into a container such as OCF container or folder.
	 * 
	 * @param container
	 *            container writing interface
	 * @throws IOException
	 *             if I/O error occurs while writing
	 */
	public void serialize(ContainerWriter container) throws IOException {
		getPrimaryIdentifier(); // if no unique id, make one
		NCXResource ncx = getTOC();
		if (ncx != null)
			ncx.prepareTOC();
		Iterator spine = spine();
		int playOrder = 0;
		while (spine.hasNext()) {
			Object sp = spine.next();
			if (sp instanceof OPSResource) {
				playOrder = ((OPSResource) sp).getDocument().assignPlayOrder(playOrder);
			}
		}
		Enumeration names = resourcesByName.keys();
		boolean needEnc = false;
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Resource res = (Resource) resourcesByName.get(name);
//			if (res instanceof FontResource) {
//				needEnc = true;
//			}
			OutputStream out = container.getOutputStream(name, res.canCompress());
			res.serialize(out);
		}
		if (needEnc) {
			XMLSerializer ser = new XMLSerializer(container.getOutputStream("META-INF/encryption.xml"));
			ser.startDocument("1.0", "UTF-8");
			ser.startElement(ocfns, "encryption", null, true);
			ser.newLine();
			names = resourcesByName.keys();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();
				Resource res = (Resource) resourcesByName.get(name);
				if (res instanceof FontResource) {
					SMapImpl attrs = new SMapImpl();
					ser.startElement(encns, "EncryptedData", null, true);
					ser.newLine();
					if (useIDPFFontMangling)
						attrs.put(null, "Algorithm", "http://www.idpf.org/2008/embedding");
					else
						attrs.put(null, "Algorithm", "http://ns.adobe.com/pdf/enc#RC");
					ser.startElement(encns, "EncryptionMethod", attrs, false);
					ser.endElement(encns, "EncryptionMethod");
					ser.newLine();
					ser.startElement(encns, "CipherData", null, false);
					ser.newLine();
					attrs = new SMapImpl();
					attrs.put(null, "URI", name);
					ser.startElement(encns, "CipherReference", attrs, false);
					ser.endElement(encns, "CipherReference");
					ser.newLine();
					ser.endElement(encns, "CipherData");
					ser.newLine();
					ser.endElement(encns, "EncryptedData");
					ser.newLine();
				}
			}
			ser.endElement(ocfns, "encryption");
			ser.newLine();
			ser.endDocument();
		}
		XMLSerializer ser = new XMLSerializer(container.getOutputStream("META-INF/container.xml"));
		ser.startDocument("1.0", "UTF-8");
		SMapImpl attrs = new SMapImpl();
		attrs.put(null, "version", "1.0");
		ser.startElement(ocfns, "container", attrs, true);
		ser.newLine();
		ser.startElement(ocfns, "rootfiles", null, false);
		ser.newLine();
		attrs = new SMapImpl();
		attrs.put(null, "full-path", opf.name);
		attrs.put(null, "media-type", opf.mediaType);
		ser.startElement(ocfns, "rootfile", attrs, true);
		ser.endElement(ocfns, "rootfile");
		ser.newLine();
		ser.endElement(ocfns, "rootfiles");
		ser.newLine();
		ser.endElement(ocfns, "container");
		ser.newLine();
		ser.endDocument();
		container.close();
	}

	public void cascadeStyles() {
		Enumeration names = resourcesByName.keys();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Resource res = (Resource) resourcesByName.get(name);
			if (res instanceof OPSResource) {
				((OPSResource) res).getDocument().cascadeStyles();
			}
		}
	}

	public void generateStyles(StyleResource styleResource) {
		Enumeration names = resourcesByName.keys();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Resource res = (Resource) resourcesByName.get(name);
			if (res instanceof OPSResource) {
				((OPSResource) res).getDocument().generateStyles(styleResource.getStylesheet());
			}
		}
	}

	static class OCFHandler extends DefaultHandler {

		String opf;

		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if (opf != null)
				return;
			if (uri.equals(ocfns) && localName.equals("rootfile")) {
				String path = attributes.getValue("full-path");
				String type = attributes.getValue("media-type");
				if (type != null && type.equals(OPFResource.opfmedia))
					opf = path;
			}
		}
	}

	private static String processOCF(InputStream ocfStream) throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		SAXParser parser = factory.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		OCFHandler handler = new OCFHandler();
		reader.setContentHandler(handler);
		reader.setEntityResolver(EPUBEntityResolver.instance);
		InputSource source = new InputSource(ocfStream);
		reader.parse(source);
		if (handler.opf == null)
			throw new RuntimeException("No OPF file found");
		return handler.opf;
	}

	Resource loadResource(String href, String mediaType) throws Exception {
		DataSource data = containerSource.getDataSource(href);
		if (data == null)
			return null;
		return createGenericResource(href, mediaType, data);
	}

	public Resource parseResource(Resource res) {
		Resource r = parseResourceRaw(res);
		if (r != null) {
			resourcesByName.put(res.getName(), r);
			int spineIndex = spine.indexOf(res);
			if (spineIndex >= 0)
				spine.set(spineIndex, r);
			if (res == toc)
				toc = r;
			if (res.id != null) {
				r.id = res.id;
				resourcesById.put(res.id, r);
			}
		}
		return r;
	}

	private Resource parseResourceRaw(Resource res) {
		if (res.getClass() != Resource.class)
			return null;
		String mediaType = res.getMediaType();
		DataSource data = res.source;
		String href = res.getName();
		try {
			if (mediaType.equals("application/xhtml+xml") || mediaType.equals("image/svg+xml")
					|| mediaType.equals("text/html")) {
				if (mediaType.equals("text/html"))
					mediaType = "application/xhtml+xml";
				OPSResource resource = new OPSResource(this, href, mediaType);
				resource.load(data);
				return resource;
			}
			if (mediaType.equals("application/x-dtbncx+xml")) {
				NCXResource toc = new NCXResource(this, href);
				resourcesByName.put(href, toc);
				toc.load(data);
				return toc;
			}
			if (mediaType.equals("text/css")) {
				StyleResource resource = new StyleResource(this, href);
				resource.load(data);
				return resource;
			}
			if (mediaType.startsWith("image/"))
				return new BitmapImageResource(this, href, mediaType, data);
			if (mediaType.equals("application/vnd.ms-opentype"))
				return new FontResource(this, href, data);
			if (mediaType.equals("application/octet-stream")) {
				String s = href.toLowerCase();
				if (s.endsWith(".ttf") || s.endsWith(".otf"))
					return new FontResource(this, href, data);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public ResourceRef getResourceRef(String name) {
		ResourceRef ref = (ResourceRef) resourceRefsByName.get(name);
		if (ref == null) {
			ref = new ResourceRef(this, name);
			resourceRefsByName.put(name, ref);
		}
		return ref;
	}

	public void parseSpine() {
		for (int i = 0; i < spine.size(); i++) {
			Resource r = (Resource) spine.get(i);
			try {
				parseResource(r);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void parseAll() {
		Vector resources = new Vector();
		resources.addAll(resourcesByName.values());
		Iterator list = resources.iterator();
		while (list.hasNext()) {
			Resource r = (Resource) list.next();
			try {
				parseResource(r);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Apply existing stylesheets, refactor all properties into new CSS classes,
	 * remove all existing stylesheets and create a single one in place of them.
	 */
	public StyleResource refactorStyles() {
		cascadeStyles();
		HashSet resources = new HashSet();
		Iterator list = resourcesByName.values().iterator();
		while (list.hasNext()) {
			Resource r = (Resource) list.next();
			if (r instanceof StyleResource) {
				resources.add(r);
			}
		}
		String name = makeUniqueResourceName((contentFolder == null ? "" : contentFolder + "/") + "common.css");
		StyleResource styleResource = createStyleResource(name);
		list = resources.iterator();
		while (list.hasNext()) {
			removeResource((Resource) list.next());
		}
		list = resourcesByName.values().iterator();
		while (list.hasNext()) {
			Object next = list.next();
			if (next instanceof OPSResource) {
				OPSResource ops = (OPSResource) next;
				OPSDocument doc = ops.getDocument();
				doc.removeAllStyleResources();
				doc.setAssignStylesFlag();
				doc.addStyleResource(styleResource.getResourceRef());
				doc.generateStyles(styleResource.getStylesheet());
			}
		}
		return styleResource;
	}

}
