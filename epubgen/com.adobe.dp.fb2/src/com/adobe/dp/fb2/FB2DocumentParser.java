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

package com.adobe.dp.fb2;

import com.adobe.dp.css.*;
import com.adobe.dp.epub.util.Base64;
import com.adobe.dp.xml.util.SMapAttributesAdapter;
import com.adobe.dp.xml.util.SMapImpl;
import com.sun.org.apache.xml.internal.utils.XMLChar;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.ZipInputStream;

public class FB2DocumentParser {

	private static final String xlinkNS = "http://www.w3.org/1999/xlink";

	FB2Document doc;

	Vector stylesheets = new Vector();

	Vector genres = new Vector();

	class FB2CSSURLFactory implements CSSURLFactory {
		public CSSURL createCSSURL(String url) {
			return new FB2CSSURL(doc, url);
		}
	}

	public FB2DocumentParser(FB2Document doc) {
		this.doc = doc;
	}

	public void parse(InputStream in) throws IOException, FB2FormatException {
		if (!in.markSupported())
			in = new BufferedInputStream(in);
		// sniff
		in.mark(4);
		byte[] sniff = new byte[4];
		in.read(sniff);
		in.reset();
		if (sniff[0] == 'P' && sniff[1] == 'K' && sniff[2] == 3 && sniff[3] == 4) {
			try {
				// zipped file
				ZipInputStream zip = new ZipInputStream(in);
				zip.getNextEntry();
				in = new BufferedInputStream(zip);
				in.mark(4);
				in.read(sniff);
				in.reset();
			} catch (Exception e) {
				throw new FB2FormatException("Zip file structure error");
			}
		}

        String encoding = null;
		if (sniff[0] == (byte) 0xef && sniff[1] == (byte) 0xbb && sniff[2] == (byte) 0xbf) {
			// UTF-8 marker. Not all XML parsers correctly ignore that
			in.skip(3);
            encoding = "UTF-8";
        }
		if (sniff[0] == (byte) 0xfe && sniff[1] == (byte) 0xff) {
			// UTF-16be marker. Not all XML parsers correctly ignore that
			in.skip(2);
            encoding = "UTF-16BE";
        }
		if (sniff[0] == (byte) 0x00 && sniff[1] == (byte) 0x00 && sniff[2] == (byte) 0xfe && sniff[3] == (byte) 0xff) {
			// UTF-32be marker. Not all XML parsers correctly ignore that
			in.skip(4);
            encoding = "UTF-32BE";
        }
		if (sniff[0] == (byte) 0xff && sniff[1] == (byte) 0xfe) {
			// UTF-16le marker. Not all XML parsers correctly ignore that
			in.skip(2);
            encoding = "UTF-16LE";
        }
		if (sniff[0] == (byte) 0xff && sniff[1] == (byte) 0xfe && sniff[2] == (byte) 0x00 && sniff[3] == (byte) 0x00) {
			// UTF-16le marker. Not all XML parsers correctly ignore that
			in.skip(4);
            encoding = "UTF-32LE";
        }
        if (encoding == null) { // no BOM found - read encoding from prolog
            in.mark(128);
            sniff = new byte[128];
            in.read(sniff);
            in.reset();
            String head = new String(sniff);
            int encodingBeginIndex = head.indexOf("encoding=\"");
            if (encodingBeginIndex > 0) {
                encodingBeginIndex += "encoding=\"".length();
                int encodingEndIndex = head.indexOf('"', encodingBeginIndex + 1);
                encoding = head.substring(encodingBeginIndex, encodingEndIndex);
            }
        }
        SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			XMLHandler handler = new XMLHandler();
			reader.setContentHandler(handler);
			reader.setEntityResolver(handler);
			InputSource source = new InputSource(new UtfFilterInputStream(in, encoding));
			reader.parse(source);
			int count = handler.bodyElements.size();
			if (count == 0)
				throw new FB2FormatException("No body sections found");
			doc.bodySections = new FB2Section[count];
			handler.bodyElements.copyInto(doc.bodySections);
			doc.stylesheets = new CSSStylesheet[stylesheets.size()];
			stylesheets.copyInto(doc.stylesheets);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		} catch (SAXException e) {
			throw new FB2FormatException("XML Syntax error: " + e.getMessage());
		}
	}

	FB2Element createElement(String ns, String localName, Attributes attr) {
		String styleStr = attr.getValue("style");
		InlineRule style = null;
		if (styleStr != null) {
			CSSParser parser = new CSSParser();
			parser.setCSSURLFactory(new FB2CSSURLFactory());
			style = parser.readInlineStyle(styleStr);
		}
		if (ns.equals(FB2Document.fb2NS)) {
			if (localName.equals("section") || localName.equals("epigraph") || localName.equals("cite")
					|| localName.equals("poem") || localName.equals("stanza"))
				return new FB2Section(localName);
			if (localName.equals("p"))
				return new FB2Paragraph(style);
			if (localName.equals("v"))
				return new FB2Line(style);
			if (localName.equals("date"))
				return new FB2Date();
			if (localName.equals("text-author"))
				return new FB2TextAuthor(style);
			if (localName.equals("title"))
				return new FB2Title();
			if (localName.equals("annotation"))
				return new FB2Section("annotation");
			if (localName.equals("subtitle"))
				return new FB2Subtitle(style);
			if (localName.equals("image") || localName.equals("a")) {
				String link = attr.getValue(xlinkNS, "href");
				if (link != null && link.startsWith("#"))
					link = link.substring(1);
				else
					link = null;
				if (localName.equals("a"))
					return new FB2Hyperlink(link);
				else {
					String alt = attr.getValue("alt");
					String title = attr.getValue("title");
					return new FB2Image(link, alt, title);
				}
			}
			if (localName.equals("empty-line"))
				return new FB2EmptyLine();
			if (localName.equals("style"))
				return new FB2StyledText(attr.getValue("name"));
			if (localName.equals("strong") || localName.equals("emphasis") || localName.equals("sub")
					|| localName.equals("sup") || localName.equals("strikethrough") || localName.equals("code"))
				return new FB2Text(localName);
			if (localName.equals("th") || localName.equals("td")) {
				String val = attr.getValue("colspan");
				int colSpan = 1;
				if (val != null) {
					try {
						colSpan = Integer.parseInt(val);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				val = attr.getValue("rowspan");
				int rowSpan = 1;
				if (val != null) {
					try {
						rowSpan = Integer.parseInt(val);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				String align = attr.getValue("align");
				return new FB2TableCell(localName, style, align, colSpan, rowSpan);
			}
			if (localName.equals("table") || localName.equals("tr"))
				return new FB2OtherElement(localName, style);
		}
		return new FB2UnknownElement(ns, localName);
	}

	class Context {
		Context(FB2Element e) {
			curr = e;
		}

		FB2Element curr;

		Vector children = new Vector();
	}

    private static class UtfFilterInputStream extends FilterReader {
        public int read() throws IOException {
            int read;
            do read = super.read(); while (read > -1 && !XMLChar.isValid(read));
            return read;
        }

        public int read(char cbuf[], int off, int len) throws IOException {
            char buff[] = new char[len];
            int read = super.read(buff, 0, len);
            if (read > -1) {
                int validChars = 0;
                for (int i = 0; i < read; i++) if (XMLChar.isValid(buff[i])) cbuf[off + validChars++] = buff[i];
                return validChars;
            } else {
                return read;
            }
        }

        private UtfFilterInputStream(InputStream in, String encoding) throws UnsupportedEncodingException {
            super(encoding == null ? new InputStreamReader(in, "UTF-8") : new InputStreamReader(in, encoding));
        }
    }

	class XMLHandler extends DefaultHandler {

		Vector bodyElements = new Vector();

		Vector emails = new Vector();

		Vector homePages = new Vector();

		Vector srcUrls = new Vector();

		Vector sequences = new Vector();

		Vector translators = new Vector();

		FB2TitleInfo currTitleInfo;

		FB2AuthorInfo currAuthorInfo;

		FB2DocumentInfo currDocumentInfo;

		FB2PublishInfo currPublishInfo;

		FB2DateInfo currDateInfo;

		FB2Binary currBinary;

		FB2GenreInfo currGenreInfo;

		Vector authors = new Vector();

		Stack contexts = new Stack();

		StringBuffer acc = new StringBuffer();

		String coverpageImage;

		private Context getContext() {
			return (Context) contexts.peek();
		}

		public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
			throw new SAXException("External entities not allowed");
		}

		private void emptyAcc() {
			if (acc.length() > 0)
				acc.replace(0, acc.length(), "");
		}

		private String flushAcc() {
			String res = acc.toString();
			emptyAcc();
			return res;
		}

		private boolean inContent() {
			return !contexts.isEmpty();
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			if (inContent()) {
				String text = new String(ch, start, length);
				if (getContext().curr.acceptsText())
					getContext().children.add(text);
			} else {
				acc.append(ch, start, length);
			}
		}

		private void addSequence(Attributes attributes) {
			try {
				String name = attributes.getValue("name");
				String number = attributes.getValue("number");
				if (name != null && number != null) {
					FB2SequenceInfo sequence = new FB2SequenceInfo();
					sequence.setName(name);
					sequence.setNumber(Integer.parseInt(number));
					sequences.add(sequence);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void addAuthor(Vector vector) {
			currAuthorInfo = new FB2AuthorInfo();
			vector.add(currAuthorInfo);
			emails.clear();
			homePages.clear();
		}

		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (inContent()) {
				Context cx = getContext();
				cx.curr.children = new Object[cx.children.size()];
				cx.children.copyInto(cx.curr.children);
				contexts.pop();
				if (cx.curr instanceof FB2Title && getContext().curr instanceof FB2Section) {
					((FB2Section) getContext().curr).title = (FB2Title) cx.curr;
				}
			} else if (currDateInfo != null) {
				if (localName.equals("date")) {
					currDateInfo.setHumanReadable(flushAcc());
					currDateInfo = null;
				}
			} else if (currAuthorInfo != null) {
				if (localName.equals("author") || localName.equals("translator")) {
					if (homePages.size() > 0) {
						String[] arr = new String[homePages.size()];
						homePages.copyInto(arr);
						currAuthorInfo.setHomePages(arr);
						homePages.clear();
					}
					if (emails.size() > 0) {
						String[] arr = new String[emails.size()];
						emails.copyInto(arr);
						currAuthorInfo.setEmails(arr);
						emails.clear();
					}
					currAuthorInfo = null;
				} else if (localName.equals("first-name")) {
					currAuthorInfo.setFirstName(flushAcc());
				} else if (localName.equals("last-name")) {
					currAuthorInfo.setLastName(flushAcc());
				} else if (localName.equals("middle-name")) {
					currAuthorInfo.setMiddleName(flushAcc());
				} else if (localName.equals("nickname")) {
					currAuthorInfo.setNickname(flushAcc());
				} else if (localName.equals("home-page")) {
					homePages.add(flushAcc());
				} else if (localName.equals("email")) {
					emails.add(flushAcc());
				}
			} else if (currTitleInfo != null) {
				if (localName.equals("title-info") || localName.equals("src-title-info")) {
					if (genres.size() > 0) {
						FB2GenreInfo[] arr = new FB2GenreInfo[genres.size()];
						genres.copyInto(arr);
						currTitleInfo.setGenres(arr);
						genres.clear();
					}
					if (authors.size() > 0) {
						FB2AuthorInfo[] arr = new FB2AuthorInfo[authors.size()];
						authors.copyInto(arr);
						currTitleInfo.setAuthors(arr);
						authors.clear();
					}
					if (translators.size() > 0) {
						FB2AuthorInfo[] arr = new FB2AuthorInfo[translators.size()];
						translators.copyInto(arr);
						currTitleInfo.setTranslators(arr);
						translators.clear();
					}
					if (sequences.size() > 0) {
						FB2SequenceInfo[] arr = new FB2SequenceInfo[sequences.size()];
						sequences.copyInto(arr);
						currTitleInfo.setSequences(arr);
						sequences.clear();
					}
					currTitleInfo = null;
				} else if (localName.equals("book-title")) {
					currTitleInfo.setBookTitle(flushAcc());
				} else if (localName.equals("keywords")) {
					currTitleInfo.setKeywords(flushAcc());
				} else if (localName.equals("lang")) {
					currTitleInfo.setLanguage(flushAcc());
				} else if (localName.equals("src-lang")) {
					currTitleInfo.setSrcLanguage(flushAcc());
				} else if (localName.equals("coverpage")) {
					currTitleInfo.setCoverpageImage(coverpageImage);
					coverpageImage = null;
				}
			} else if (currDocumentInfo != null) {
				if (localName.equals("document-info")) {
					if (authors.size() > 0) {
						FB2AuthorInfo[] arr = new FB2AuthorInfo[authors.size()];
						authors.copyInto(arr);
						currDocumentInfo.setAuthors(arr);
						authors.clear();
					}
					if (srcUrls.size() > 0) {
						String[] arr = new String[srcUrls.size()];
						srcUrls.copyInto(arr);
						currDocumentInfo.setSrcUrls(arr);
						srcUrls.clear();
					}
					currDocumentInfo = null;
				} else if (localName.equals("program-used")) {
					currDocumentInfo.setProgramUsed(flushAcc());
				} else if (localName.equals("src-url")) {
					srcUrls.add(flushAcc());
				} else if (localName.equals("src-ocr")) {
					currDocumentInfo.setSrcOcr(flushAcc());
				} else if (localName.equals("version")) {
					currDocumentInfo.setVersion(flushAcc());
				} else if (localName.equals("id")) {
					currDocumentInfo.setId(flushAcc());
				}
			} else if (currPublishInfo != null) {
				if (localName.equals("publish-info")) {
					if (sequences.size() > 0) {
						FB2SequenceInfo[] arr = new FB2SequenceInfo[sequences.size()];
						sequences.copyInto(arr);
						currPublishInfo.setSequences(arr);
						sequences.clear();
					}
					currPublishInfo = null;
				} else if (localName.equals("publisher")) {
					currPublishInfo.setPublisher(flushAcc());
				} else if (localName.equals("city")) {
					currPublishInfo.setCity(flushAcc());
				} else if (localName.equals("year")) {
					currPublishInfo.setYear(flushAcc());
				} else if (localName.equals("book-name")) {
					currPublishInfo.setBookName(flushAcc());
				} else if (localName.equals("isbn")) {
					currPublishInfo.setISBN(flushAcc());
				}
			} else if (currBinary != null) {
				currBinary.setData(Base64.decode(flushAcc()));
				currBinary = null;
			} else if (localName.equals("stylesheet")) {
				CSSParser parser = new CSSParser();
				parser.setCSSURLFactory(new FB2CSSURLFactory());
				try {
					CSSStylesheet stylesheet = parser.readStylesheet(new StringReader(flushAcc()));
					if (stylesheet != null)
						stylesheets.add(stylesheet);
				} catch (Exception e) {
					// unexpected
					e.printStackTrace();
				}
			}
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (inContent()) {
				FB2Element element = createElement(uri, localName, attributes);
				if (attributes.getLength() > 0) {
					element.attrs = new SMapImpl(new SMapAttributesAdapter(attributes));
					String id = attributes.getValue("id");
					if (id != null) {
						element.id = id;
						doc.idMap.put(id, element);
					}
				}
				getContext().children.add(element);
				contexts.push(new Context(element));
			} else if (currAuthorInfo != null) {
				emptyAcc();
			} else if (currDateInfo != null) {
				// ignore
			} else if (currTitleInfo != null) {
				if (localName.equals("author")) {
					addAuthor(authors);
				} else if (localName.equals("translator")) {
					addAuthor(translators);
				} else if (localName.equals("genre")) {
					currGenreInfo = new FB2GenreInfo();
					String match = attributes.getValue("match");
					if (match != null) {
						try {
							currGenreInfo.setMatch(Integer.parseInt(match));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} else if (localName.equals("sequence")) {
					addSequence(attributes);
				} else if (localName.equals("date")) {
					currDateInfo = new FB2DateInfo();
					String date = attributes.getValue("value");
					currDateInfo.setMachineReadable(date);
					currTitleInfo.setDate(currDateInfo);
				} else if (localName.equals("annotation")) {
					FB2Section annot = new FB2Section("annotation");
					contexts.push(new Context(annot));
					currTitleInfo.setAnnotation(annot);
				} else if (localName.equals("coverpage")) {
					coverpageImage = null;
				} else if (localName.equals("image")) {
					String ref = attributes.getValue(xlinkNS, "href");
					if (ref.startsWith("#")) {
						coverpageImage = ref.substring(1);
					}
				}
				emptyAcc();
			} else if (currDocumentInfo != null) {
				if (localName.equals("autor")) {
					addAuthor(authors);
				} else if (localName.equals("date")) {
					currDateInfo = new FB2DateInfo();
					String date = attributes.getValue("value");
					currDateInfo.setMachineReadable(date);
					currDocumentInfo.setDate(currDateInfo);
				} else if (localName.equals("history")) {
					FB2Section annot = new FB2Section("history");
					contexts.push(new Context(annot));
					currDocumentInfo.setHistory(annot);
				}
				emptyAcc();
			} else if (currPublishInfo != null) {
				if (localName.equals("sequence")) {
					addSequence(attributes);
				}
				emptyAcc();
			} else if (currBinary != null) {
				// ignore
			} else if (localName.equals("body")) {
				FB2Section body = new FB2Section("body");
				body.name = attributes.getValue("name");
				bodyElements.add(body);
				contexts.push(new Context(body));
			} else if (localName.equals("document-info")) {
				currDocumentInfo = new FB2DocumentInfo();
				authors.clear();
				srcUrls.clear();
				doc.documentInfo = currDocumentInfo;
			} else if (localName.equals("title-info")) {
				currTitleInfo = new FB2TitleInfo();
				authors.clear();
				sequences.clear();
				translators.clear();
				genres.clear();
				doc.titleInfo = currTitleInfo;
			} else if (localName.equals("src-title-info")) {
				currTitleInfo = new FB2TitleInfo();
				authors.clear();
				doc.srcTitleInfo = currTitleInfo;
			} else if (localName.equals("publish-info")) {
				currPublishInfo = new FB2PublishInfo();
				doc.publishInfo = currPublishInfo;
			} else if (localName.equals("binary")) {
				currBinary = new FB2Binary();
				String id = attributes.getValue("id");
				String contentType = attributes.getValue("content-type");
				doc.binaryResources.put(id, currBinary);
				currBinary.setMediaType(contentType);
				emptyAcc();
			} else if (localName.equals("stylesheet")) {
				emptyAcc();
			}
		}
	}
}
