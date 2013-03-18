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

package com.adobe.dp.otf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class OpenTypeFont implements FontPropertyConstants {

	FontInputStream font;

	FileHeader header;

	byte[] buffer = new byte[256];

	int currentOffset;

	boolean trueTypeGlyphs;

	GlyphData[] glyphs;

	short fsType;

	int maxGlyphSize;

	int newGlyphCount;

	int variableWidthCount;

	int newVariableWidthCount;

	String familyName;

	int weight;

	int width;

	int style;

	NameEntry[] names;

	// TODO: binary tree is probably more appropriate here
	Hashtable characters = new Hashtable();

	private static int CFF_STD_STRING_COUNT = 391;

	String fontID;

	String nameCFF;

	Hashtable dictCFF;

	Hashtable privateDictCFF;

	StringCFF[] stringsCFF;

	Object[] globalSubrsCFF;

	Object[] privateSubrsCFF;

	// seconds from Jan 1, 1904 to to Jan 1, 1970
	static final long baseTime = (66 * 365 + 16) * 24 * 60 * 60;

	// CFF dict keys with SID values
	static final int[] sidIndicesCFF = { 0, 1, 2, 3, 4, 0x100, 0x100 | 21,
			0x100 | 22, 0x100 | 30, 0x100 | 38 };

	static class FileHeader {
		int version;

		short numTables;

		short searchRange;

		short entrySelector;

		short rangeShift;

		TableDirectoryEntry[] tableDirectory;

		Hashtable tableMap = new Hashtable();
	}

	static class TableDirectoryEntry {
		String identifier;

		int checkSum;

		int offset;

		int length;

		boolean need;

		byte[] newContent;

		int newRelativeOffset;
	}

	static class GlyphData {
		boolean need;

		boolean compositeTT;

		int offset;

		int length;

		short advance;

		short lsb;

		int newIndex;

		int namesidCFF;
	}

	static class CharacterData {
		short glyphIndex;

		boolean need;
	}

	static class EncodingRecord {
		short platformID;

		short encodingID;

		int offset;

		short format;

		int length;

		short language;
	}

	static class NameEntry {
		short platformID;

		short encodingID;

		short languageID;

		short nameID;

		int offset;

		int length;

		boolean needed;

		byte[] newContent;

		int newRelativeOffset;
	}

	static class Range {

		Range(int offset, int length) {
			this.offset = offset;
			this.length = length;
		}

		int offset;

		int length;
	}

	static class EncodingSegment {
		int start;

		int end;

		short[] glyphIds;

		boolean constDelta;

		int glyphsBefore;
	}

	static class KeyCFF {

		static final int ENCODING = 16;

		static final int CHARSET = 15;

		static final int CHARSTRINGS = 17;

		static final int PRIVATE = 18;

		int keyID;

		KeyCFF(int keyID) {
			this.keyID = keyID;
		}

		public int hashCode() {
			return keyID;
		}

		public boolean equals(Object o) {
			if (o.getClass() != getClass())
				return false;
			return keyID == ((KeyCFF) o).keyID;
		}
	}

	static class StringCFF {
		String value;

		int newIndex;

		boolean needed;
	}

	static class IntPlaceholderCFF {
		int offset;
	}

	public OpenTypeFont(FontInputStream font) throws IOException {
		this(font, false);
	}

	public OpenTypeFont(FontInputStream font, boolean queryOnly)
			throws IOException {
		this.font = font;
		readFileHeader();
		readOS2();
		readNames();
		if (!queryOnly) {
			readCMap();
			readGlyphData();
			readMetrics();
		}
	}

	private void readBuffer(int size) throws IOException {
		int len = font.read(buffer, 0, size);
		if (len != size)
			throw new IOException("could not read " + size + " bytes");
		currentOffset += size;
	}

	private void readBuffer(byte[] buf, int size) throws IOException {
		int len = font.read(buf, 0, size);
		if (len != size)
			throw new IOException("could not read " + size + " bytes");
		currentOffset += size;
	}

	private short[] readShorts(int size) throws IOException {
		if (size < 0)
			System.err.println("Bug!");
		short[] arr = new short[size];
		int offset = 0;
		while (size > 0) {
			int count = size;
			if (count > buffer.length / 2)
				count = buffer.length / 2;
			readBuffer(2 * count);
			for (int i = 0; i < count; i++) {
				arr[offset++] = getShort(buffer, 2 * i);
			}
			size -= count;
		}
		return arr;
	}

	private void seek(int offset) throws IOException {
		font.seek(offset);
		currentOffset = offset;
	}

	private static int getInt(byte[] buf, int offset) {
		return (buf[offset] << 24) | ((buf[offset + 1] & 0xFF) << 16)
				| ((buf[offset + 2] & 0xFF) << 8) | (buf[offset + 3] & 0xFF);
	}

	private static short getShort(byte[] buf, int offset) {
		return (short) (((buf[offset] & 0xFF) << 8) | (buf[offset + 1] & 0xFF));
	}

	private TableDirectoryEntry findTable(String identifier) {
		return (TableDirectoryEntry) header.tableMap.get(identifier);
	}

	private int readGlyphCountTT() throws IOException {
		TableDirectoryEntry maxp = findTable("maxp");
		seek(maxp.offset + 4);
		readBuffer(2);
		return getShort(buffer, 0) & 0xFFFF;
	}

	private int readIndexToLocFormatTT() throws IOException {
		TableDirectoryEntry h = findTable("head"); // expect size of 54
		seek(h.offset + 50);
		readBuffer(2);
		return getShort(buffer, 0);
	}

	private void readGlyphDataTT() throws IOException {
		int glyphCount = readGlyphCountTT();
		int indexToLocFormat = readIndexToLocFormatTT();
		TableDirectoryEntry glyf = findTable("glyf");
		TableDirectoryEntry locations = findTable("loca");
		int entrySize = (indexToLocFormat == 0 ? 2 : 4);
		if (locations.length != (glyphCount + 1) * entrySize)
			throw new IOException("bad 'loca' table size");
		byte[] buf = new byte[locations.length];
		seek(locations.offset);
		font.read(buf, 0, buf.length);
		GlyphData[] data = new GlyphData[glyphCount];
		int offset = 0;
		for (int i = 0; i <= glyphCount; i++) {
			int glyphOffset = (indexToLocFormat == 0 ? (getShort(buf, offset) & 0xFFFF) * 2
					: getInt(buf, offset))
					+ glyf.offset;
			offset += entrySize;
			if (i > 0) {
				int len = glyphOffset - data[i - 1].offset;
				if (len < 0)
					throw new IOException("negative glyph length");
				if (maxGlyphSize < len)
					maxGlyphSize = len;
				data[i - 1].length = len;
			}
			if (i != glyphCount) {
				data[i] = new GlyphData();
				data[i].offset = glyphOffset;
			}
		}
		glyphs = data;
	}

	private boolean decodeNibbleCFF(StringBuffer sb, int nibble)
			throws IOException {
		nibble = nibble & 0xF;
		switch (nibble) {
		case 0:
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		case 8:
		case 9:
			sb.append((char) (nibble + '0'));
			break;
		case 0xA:
			sb.append(".");
			break;
		case 0xB:
			sb.append("E");
			break;
		case 0xC:
			sb.append("E-");
			break;
		case 0xE:
			sb.append("-");
			break;
		case 0xF:
			return true; // end
		default:
			throw new IOException("could not read CFF float");
		}
		return false; // not end
	}

	private Object readObjectCFF() throws IOException {
		readBuffer(1);
		int b0 = buffer[0] & 0xFF;
		if (b0 == 30) {
			StringBuffer sb = new StringBuffer();
			while (true) {
				readBuffer(1);
				int n = buffer[0];
				if (decodeNibbleCFF(sb, n >> 4))
					break;
				if (decodeNibbleCFF(sb, n))
					break;
			}
			return new Double(sb.toString());
		} else if (b0 <= 21) {
			if (b0 == 12) {
				readBuffer(1);
				int b1 = buffer[0] & 0xFF;
				return new KeyCFF(b1 | 0x100);
			}
			return new KeyCFF(b0);
		} else if (32 <= b0 && b0 <= 246) {
			return new Integer(b0 - 139);
		} else if (247 <= b0 && b0 <= 254) {
			readBuffer(1);
			int b1 = buffer[0] & 0xFF;
			if (b0 <= 250)
				return new Integer((b0 - 247) * 256 + b1 + 108);
			else
				return new Integer(-(b0 - 251) * 256 - b1 - 108);
		} else if (b0 == 28) {
			readBuffer(2);
			int b1 = buffer[0] & 0xFF;
			int b2 = buffer[1] & 0xFF;
			return new Integer((short) ((b1 << 8) | b2));
		} else if (b0 == 29) {
			readBuffer(4);
			int b1 = buffer[0] & 0xFF;
			int b2 = buffer[1] & 0xFF;
			int b3 = buffer[2] & 0xFF;
			int b4 = buffer[3] & 0xFF;
			return new Integer((b1 << 24) | (b2 << 16) | (b3 << 8) | b4);
		} else {
			throw new IOException("error reading CFF object");
		}
	}

	private final static int CFF_INDEX_NAMES = 0;

	private final static int CFF_INDEX_DICTS = 1;

	private final static int CFF_INDEX_BINARY = 2;

	private final static int CFF_INDEX_BINARY_RANGE = 3;

	String valToStr(Object val) {
		if (val instanceof Object[]) {
			Object[] arr = (Object[]) val;
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < arr.length; i++) {
				if (i != 0)
					sb.append(' ');
				sb.append(arr[i]);
			}
			return sb.toString();
		}
		return val.toString();
	}

	private Hashtable readDictCFF(int last) throws IOException {
		Hashtable table = new Hashtable();
		Vector acc = new Vector();
		while (currentOffset < last) {
			Object value;
			while (true) {
				value = readObjectCFF();
				if (value instanceof KeyCFF)
					break;
				acc.add(value);
			}
			Object key = value;
			if (acc.size() == 1)
				value = acc.elementAt(0);
			else {
				Number[] vals = new Number[acc.size()];
				acc.copyInto(vals);
				value = vals;
			}
			acc.setSize(0);
			table.put(key, value);
			// System.out.println(((KeyCFF) key).keyID + " = " +
			// valToStr(value));
		}
		return table;
	}

	private Object[] readIndexCFF(int kind) throws IOException {
		readBuffer(2);
		int count = getShort(buffer, 0) & 0xFFFF;
		if (count == 0)
			return new Object[0];
		readBuffer(1);
		int offSize = buffer[0];
		if (offSize > 4 || offSize <= 0)
			throw new IOException("invalid CFF index offSize");
		int[] offsets = new int[count + 1];
		for (int i = 0; i <= count; i++) {
			readBuffer(offSize);
			int off;
			switch (offSize) {
			case 1:
				off = buffer[0] & 0xFF;
				break;
			case 2:
				off = getShort(buffer, 0) & 0xFFFF;
				break;
			case 3:
				off = (getInt(buffer, 0) >> 8) & 0xFFFFFF;
				break;
			default: // 4
				off = getInt(buffer, 0);
				break;
			}
			offsets[i] = off;
		}
		int offset = currentOffset - 1;
		for (int i = 0; i <= count; i++) {
			offsets[i] += offset;
		}
		Object[] arr = new Object[count];
		seek(offsets[0]);
		for (int i = 0; i < count; i++) {
			switch (kind) {
			case CFF_INDEX_NAMES: {
				int len = offsets[i + 1] - offsets[i];
				byte[] b = (len > buffer.length ? new byte[len] : buffer);
				readBuffer(b, len);
				int strlen = 0;
				while (strlen < len && b[strlen] != 0)
					strlen++;
				arr[i] = new String(b, 0, strlen, "ISO-8859-1");
				break;
			}
			case CFF_INDEX_DICTS: {
				int last = offsets[i + 1];
				arr[i] = readDictCFF(last);
				seek(last);
				break;
			}
			case CFF_INDEX_BINARY: {
				int len = offsets[i + 1] - offsets[i];
				byte[] b = new byte[len];
				readBuffer(b, len);
				arr[i] = b;
				break;
			}
			case CFF_INDEX_BINARY_RANGE: {
				int len = offsets[i + 1] - offsets[i];
				arr[i] = new Range(offsets[i], len);
				break;
			}
			}
		}
		seek(offsets[count]);
		return arr;
	}

	private void readCharsetFormat2CFF() throws IOException {
		int glyphIndex = 0;
		while (glyphIndex < glyphs.length) {
			readBuffer(4);
			int sid = getShort(buffer, 0);
			int len = getShort(buffer, 2);
			for (int i = 0; i <= len && glyphIndex < glyphs.length; i++) {
				glyphs[glyphIndex++].namesidCFF = sid++;
			}
		}
	}

	private void readCFF() throws IOException {
		TableDirectoryEntry cff = findTable("CFF ");
		seek(cff.offset);
		readBuffer(4);
		int offSize = buffer[3];
		if (offSize > 4 || offSize <= 0)
			throw new IOException("invalid CFF index offSize");
		seek(cff.offset + (buffer[2] & 0xFF));
		Object[] names = readIndexCFF(CFF_INDEX_NAMES);
		if (names.length > 1)
			throw new IOException("CFF data contains multiple fonts");
		nameCFF = names[0].toString();
		Object[] dicts = readIndexCFF(CFF_INDEX_DICTS);
		if (names.length > 1)
			throw new IOException("CFF data contains multiple font dicts");
		dictCFF = (Hashtable) dicts[0];
		Object[] strings = readIndexCFF(CFF_INDEX_NAMES);
		stringsCFF = new StringCFF[strings.length];
		for (int i = 0; i < stringsCFF.length; i++) {
			StringCFF s = new StringCFF();
			stringsCFF[i] = s;
			s.value = (String) strings[i];
		}
		globalSubrsCFF = readIndexCFF(CFF_INDEX_BINARY_RANGE);

		Integer charstrings = (Integer) dictCFF.get(new KeyCFF(
				KeyCFF.CHARSTRINGS));

		if (charstrings == null)
			throw new IOException("Invalid CFF data: no charstrings");

		seek(cff.offset + charstrings.intValue());
		Object[] glyphRanges = readIndexCFF(CFF_INDEX_BINARY_RANGE);

		glyphs = new GlyphData[glyphRanges.length];
		for (int i = 0; i < glyphs.length; i++) {
			Range range = (Range) glyphRanges[i];
			GlyphData glyph = new GlyphData();
			glyph.length = range.length;
			glyph.offset = range.offset;
			glyphs[i] = glyph;
		}

		Integer charset = (Integer) dictCFF.get(new KeyCFF(KeyCFF.CHARSET));
		if (charset != null && charset.intValue() != 0) {
			seek(cff.offset + charset.intValue());
			readBuffer(1);
			int format = buffer[0] & 0xFF;
			switch (format) {
			case 2:
				readCharsetFormat2CFF();
				break;
			}
		}

		Object[] privatedict = (Object[]) dictCFF
				.get(new KeyCFF(KeyCFF.PRIVATE));
		if (privatedict != null) {
			int size = ((Integer) privatedict[0]).intValue();
			int offset = ((Integer) privatedict[1]).intValue();
			seek(cff.offset + offset);
			privateDictCFF = readDictCFF(cff.offset + offset + size);
			seek(cff.offset + offset + size);
			privateSubrsCFF = readIndexCFF(CFF_INDEX_BINARY_RANGE);
		}
	}

	private void readGlyphData() throws IOException {
		if (findTable("glyf") != null) {
			trueTypeGlyphs = true;
			readGlyphDataTT();
		} else {
			readCFF();
		}
	}

	private void readFileHeader() throws IOException {
		FileHeader h = new FileHeader();
		readBuffer(12);
		int version = getInt(buffer, 0);
		switch (version) {
		case 0x74746366: // TrueType collection file
			// we can only read the first font in the collection
			readBuffer(4);
			seek(getInt(buffer, 0));
			readBuffer(12);
			version = getInt(buffer, 0);
			break;
		case 0x00010000: // regular TrueType
			break;
		case 0x4f54544f: // CFF based
			break;
		default:
			throw new IOException("Invalid OpenType file");
		}
		h.version = version;
		h.numTables = getShort(buffer, 4);
		h.searchRange = getShort(buffer, 6);
		h.entrySelector = getShort(buffer, 8);
		h.rangeShift = getShort(buffer, 10);
		h.tableDirectory = new TableDirectoryEntry[h.numTables];
		for (int i = 0; i < h.numTables; i++) {
			TableDirectoryEntry entry = new TableDirectoryEntry();
			readBuffer(16);
			char[] arr = { (char) (buffer[0] & 0xFF),
					(char) (buffer[1] & 0xFF), (char) (buffer[2] & 0xFF),
					(char) (buffer[3] & 0xFF) };
			int len = buffer[0] == 0 ? 0 : (buffer[1] == 0 ? 1
					: (buffer[2] == 0 ? 2 : (buffer[3] == 0 ? 3 : 4)));
			entry.identifier = new String(arr, 0, len);
			entry.checkSum = getInt(buffer, 4);
			entry.offset = getInt(buffer, 8);
			entry.length = getInt(buffer, 12);
			h.tableDirectory[i] = entry;
			h.tableMap.put(entry.identifier, entry);
		}
		header = h;
	}

	private void readCMap() throws IOException {
		TableDirectoryEntry cmap = findTable("cmap");
		seek(cmap.offset);
		readBuffer(4);
		if (getShort(buffer, 0) != 0)
			throw new IOException("unknown cmap version");
		int encCount = getShort(buffer, 2);
		EncodingRecord[] encs = new EncodingRecord[encCount];
		for (int i = 0; i < encCount; i++) {
			EncodingRecord er = new EncodingRecord();
			encs[i] = er;
			readBuffer(8);
			er.platformID = getShort(buffer, 0);
			er.encodingID = getShort(buffer, 2);
			er.offset = getInt(buffer, 4) + cmap.offset;
		}
		for (int i = 0; i < encCount; i++) {
			EncodingRecord er = encs[i];
			seek(er.offset);
			readBuffer(6);
			er.format = getShort(buffer, 0);
			er.length = getShort(buffer, 2) & 0xFFFF;
			er.language = getShort(buffer, 4);

			if (er.platformID == 3 && er.encodingID == 1) {
				// Unicode
				if (er.format == 4) {
					readFormat4CMap(er);
				}
			}
		}
	}

	private void readFormat4CMap(EncodingRecord er) throws IOException {
		readBuffer(8);
		int segCount = (getShort(buffer, 0) & 0xFFFF) / 2;
		short[] endCount = readShorts(segCount);
		readBuffer(2);
		short[] startCount = readShorts(segCount);
		short[] idDelta = readShorts(segCount);
		short[] idRangeOffset = readShorts(segCount);
		int lengthRemains = er.length - 16 - 8 * segCount;
		short[] glyphIds = readShorts(lengthRemains);
		for (int i = 0; i < segCount; i++) {
			int start = startCount[i] & 0xFFFF;
			int end = endCount[i] & 0xFFFF;
			short rangeOffset = (short) (idRangeOffset[i] / 2);
			short delta = idDelta[i];
			for (int ch = start; ch <= end; ch++) {
				CharacterData data = new CharacterData();
				if (rangeOffset == 0)
					data.glyphIndex = (short) (ch + delta);
				else {
					int index = ch - start + rangeOffset - (segCount - i);
					int glyphIndex = glyphIds[index];
					if (glyphIndex != 0)
						glyphIndex += delta;
					data.glyphIndex = (short) glyphIndex;
				}
				characters.put(new Integer(ch), data);
			}
		}
	}

	private static boolean noUnicodeBigUnmarked = false;

	private static String decodeUnicode(byte[] unicode, int offset, int length) {
		try {
			if (!noUnicodeBigUnmarked)
				return new String(unicode, offset, length, "UnicodeBigUnmarked");
		} catch (UnsupportedEncodingException e) {
			// GCJ does not support UnicodeBigUnmarked
			noUnicodeBigUnmarked = true;
		}
		// just do it "by hand"
		char[] buf = new char[length / 2];
		for (int i = 0; i < buf.length; i++) {
			buf[i] = (char) (((unicode[2 * i] & 0xFF) << 8) | (unicode[2 * i + 1] & 0xFF));
		}
		return new String(buf);
	}

	private static byte[] encodeUnicode(String str) {
		try {
			if (!noUnicodeBigUnmarked)
				return str.getBytes("UnicodeBigUnmarked");
		} catch (UnsupportedEncodingException e) {
			// GCJ does not support UnicodeBigUnmarked
			noUnicodeBigUnmarked = true;
		}
		// just do it "by hand"
		int len = str.length();
		byte[] buf = new byte[len * 2];
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			buf[2 * i] = (byte) (c >> 8);
			buf[2 * i + 1] = (byte) (c);
		}
		return buf;
	}

	private void readNames() throws IOException {
		TableDirectoryEntry table = findTable("name");
		seek(table.offset);
		readBuffer(6);
		if (getShort(buffer, 0) != 0)
			throw new IOException("unknown cmap version");
		int count = getShort(buffer, 2) & 0xFFFF;
		int offset = (getShort(buffer, 4) & 0xFFFF) + table.offset;
		names = new NameEntry[count];
		for (int i = 0; i < count; i++) {
			NameEntry entry = new NameEntry();
			names[i] = entry;
			readBuffer(12);
			entry.platformID = getShort(buffer, 0);
			entry.encodingID = getShort(buffer, 2);
			entry.languageID = getShort(buffer, 4);
			entry.nameID = getShort(buffer, 6);
			entry.length = getShort(buffer, 8);
			entry.offset = (getShort(buffer, 10) & 0xFFFF) + offset;
		}

		for (int i = 0; i < count; i++) {
			NameEntry entry = names[i];
			if ((entry.nameID == 1 || entry.nameID == 16) && isEnUnicode(entry)) {
				seek(entry.offset);
				readBuffer(entry.length);
				familyName = decodeUnicode(buffer, 0, entry.length);
				if (entry.nameID == 16)
					break;
			}
		}
	}

	private void readMetrics() throws IOException {
		TableDirectoryEntry hhea = findTable("hhea");
		seek(hhea.offset + 34);
		readBuffer(2);
		variableWidthCount = getShort(buffer, 0) & 0xFFFF;
		TableDirectoryEntry hmtx = findTable("hmtx");
		if (hmtx.length != variableWidthCount * 2 + glyphs.length * 2)
			throw new IOException("bad hmtx table length");
		seek(hmtx.offset);
		short advance = 0;
		for (int i = 0; i < variableWidthCount; i++) {
			readBuffer(4);
			advance = getShort(buffer, 0);
			glyphs[i].advance = advance;
			glyphs[i].lsb = getShort(buffer, 2);
		}
		for (int i = variableWidthCount; i < glyphs.length; i++) {
			readBuffer(2);
			glyphs[i].advance = advance;
			glyphs[i].lsb = getShort(buffer, 2);
		}
	}

	public void play(char[] text, int offset, int len) {
		for (int i = 0; i < len; i++) {
			int ch = (text[i] & 0xFFFF);
			play((char) ch);
		}
	}

	public final boolean play(char ch) {
		CharacterData chData = (CharacterData) characters.get(new Integer(ch));
		if (chData != null) {
			chData.need = true;
			if (chData.glyphIndex >= 0 && chData.glyphIndex < glyphs.length) {
				GlyphData glyph = glyphs[chData.glyphIndex];
				glyph.need = true;
				return true;
			}
		}
		return false;
	}

	private void reindexGlyphs() {
		int index = 0;
		int lastAdvance = 0x10000;
		for (int i = 0; i < glyphs.length; i++) {
			if (i < 2)
				glyphs[i].need = true;
			if (glyphs[i].need) {
				glyphs[i].newIndex = index++;
				if (glyphs[i].advance != lastAdvance) {
					lastAdvance = glyphs[i].advance;
					newVariableWidthCount = index;
				}
				if (stringsCFF != null) {
					int sid = glyphs[i].namesidCFF;
					if (sid >= CFF_STD_STRING_COUNT)
						stringsCFF[sid - CFF_STD_STRING_COUNT].needed = true;
				}
			}
		}
		newGlyphCount = index;
	}

	private int getCompositeGlyphArgSize(int flags) {
		int argSize = 0;
		if ((flags & 1) != 0) // ARG_1_AND_2_ARE_WORDS is set
			argSize = 4;
		else
			argSize = 2;
		if ((flags & 8) != 0) // WE_HAVE_A_SCALE
			argSize += 2;
		else if ((flags & 0x40) != 0) // WE_HAVE_AN_X_AND_Y_SCALE
			argSize += 4;
		else if ((flags & 0x80) != 0) // WE_HAVE_A_TWO_BY_TWO
			argSize += 8;
		return argSize;
	}

	private void resolveCompositeGlyphsTT() throws IOException {
		for (int i = 0; i < glyphs.length; i++) {
			if (glyphs[i].need) {
				seek(glyphs[i].offset);
				readBuffer(10);
				short numberOfContours = getShort(buffer, 0);
				if (numberOfContours < 0) {
					// composite glyph
					glyphs[i].compositeTT = true;
					int remains = glyphs[i].length - 10;
					while (remains > 0) {
						readBuffer(4);
						remains -= 4;
						int flags = getShort(buffer, 0);
						int glyphIndex = (getShort(buffer, 2) & 0xFFFF);
						glyphs[glyphIndex].need = true;
						if ((flags & 0x20) == 0) // MORE_COMPONENTS not set
							break;
						int argSize = getCompositeGlyphArgSize(flags);
						readBuffer(argSize);
						remains -= argSize;
					}
				}
			}
		}
	}

	private String[] reindexStringsCFF() {
		
		// for now, keep them all...
		for (int i = 0; i < stringsCFF.length; i++) {
			stringsCFF[i].needed = true;
		}

		int index = 0;
		for (int i = 0; i < stringsCFF.length; i++) {
			if (stringsCFF[i].needed) {
				stringsCFF[i].newIndex = CFF_STD_STRING_COUNT + index++;
			}
		}
		String[] newArr = new String[index];
		index = 0;
		for (int i = 0; i < stringsCFF.length; i++) {
			if (stringsCFF[i].needed) {
				newArr[index++] = stringsCFF[i].value;
			}
		}
		return newArr;
	}

	private void writeShort(ByteArrayOutputStream out, int n) {
		out.write((byte) (n >> 8));
		out.write((byte) n);
	}

	private void writeInt(ByteArrayOutputStream out, int n) {
		out.write((byte) (n >> 24));
		out.write((byte) (n >> 16));
		out.write((byte) (n >> 8));
		out.write((byte) n);
	}

	private static int floorPowerOf2(int n) {
		for (int i = 1; i < 32; i++) {
			int p = 1 << i;
			if (n < p)
				return i - 1;
		}
		throw new RuntimeException("out of range");
	}

	private byte[] buildCMap() {

		// build a bit array of needed characters
		BitSet charMask = new BitSet();
		Enumeration chars = characters.keys();
		int maxChar = 0;
		while (chars.hasMoreElements()) {
			Integer code = (Integer) chars.nextElement();
			CharacterData data = (CharacterData) characters.get(code);
			if (data.need) {
				int ic = code.intValue();
				charMask.set(ic);
				if (ic > maxChar)
					maxChar = ic;
			}
		}

		// collect segments
		Vector segments = new Vector();
		EncodingSegment segment = null;
		for (int ch = 1; ch <= maxChar; ch++) {
			if (charMask.get(ch)) {
				if (segment == null) {
					segment = new EncodingSegment();
					segments.add(segment);
					segment.start = ch;
				}
			} else {
				if (segment != null) {
					segment.end = ch - 1;
					segment = null;
				}
			}
		}

		if (segment != null) {
			segment.end = maxChar;
		}

		if (maxChar < 0xFFFF) {
			segment = new EncodingSegment();
			segments.add(segment);
			segment.start = 0xFFFF;
			segment.end = 0xFFFF;
		}

		// collect glyph ids for the segments
		int segCount = segments.size();
		int sectionLength = 16 + 8 * segCount;
		int glyphsBefore = 0;
		for (int i = 0; i < segCount; i++) {
			segment = (EncodingSegment) segments.elementAt(i);
			int segLen = segment.end - segment.start + 1;
			short[] glyphIds = new short[segLen];
			segment.glyphIds = glyphIds;
			int delta = 0;
			for (int k = 0; k < segLen; k++) {
				int ch = k + segment.start;
				CharacterData data = (CharacterData) characters
						.get(new Integer(ch));
				short glyphIndex = (data == null ? 0 : data.glyphIndex);
				if (glyphIndex != 0) {
					GlyphData glyph = glyphs[glyphIndex];
					glyphIndex = (short) glyph.newIndex;
				}
				int d = glyphIndex - ch;
				if (k == 0) {
					delta = d;
					segment.constDelta = true;
				} else {
					if (delta != d)
						segment.constDelta = false;
				}
				glyphIds[k] = glyphIndex;
			}
			if (!segment.constDelta) {
				segment.glyphsBefore = glyphsBefore;
				sectionLength += 2 * segment.glyphIds.length;
				glyphsBefore += segment.glyphIds.length;
			}
		}

		if (sectionLength > 0xFFFF)
			throw new RuntimeException("cmap too long");

		// write out cmap section
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		writeShort(result, 0);
		writeShort(result, 2); // number of tables = 2
		writeShort(result, 0); // platform = Unicode
		writeShort(result, 3); // Unicode 2.0 or later semantics
		writeInt(result, 20); // table offset
		writeShort(result, 3); // platform = Microsoft
		writeShort(result, 1); // encoding = Unicode
		writeInt(result, 20); // table offset
		writeShort(result, 4); // format
		writeShort(result, (short) sectionLength); // length
		writeShort(result, 0); // language
		writeShort(result, (short) (segCount * 2));
		int log = floorPowerOf2(segCount & 0xFFFF);
		int entrySelector = log;
		int searchRange = 1 << (log + 1);
		int rangeShift = segCount * 2 - searchRange;
		writeShort(result, (short) searchRange);
		writeShort(result, (short) entrySelector);
		writeShort(result, (short) rangeShift);
		for (int i = 0; i < segCount; i++) {
			segment = (EncodingSegment) segments.elementAt(i);
			writeShort(result, segment.end);
		}
		writeShort(result, 0);
		for (int i = 0; i < segCount; i++) {
			segment = (EncodingSegment) segments.elementAt(i);
			writeShort(result, segment.start);
		}
		for (int i = 0; i < segCount; i++) {
			segment = (EncodingSegment) segments.elementAt(i);
			if (segment.constDelta) {
				writeShort(result,
						(short) (segment.glyphIds[0] - segment.start));
			} else {
				writeShort(result, 0);
			}
		}
		for (int i = 0; i < segCount; i++) {
			segment = (EncodingSegment) segments.elementAt(i);
			if (segment.constDelta) {
				writeShort(result, 0);
			} else {
				int rangeOffset = 2 * (segCount - i + segment.glyphsBefore);
				writeShort(result, (short) rangeOffset);
			}
		}
		for (int i = 0; i < segCount; i++) {
			segment = (EncodingSegment) segments.elementAt(i);
			if (!segment.constDelta) {
				for (int k = 0; k < segment.glyphIds.length; k++)
					writeShort(result, segment.glyphIds[k]);
			}
		}

		// convert to byte array
		byte[] arr = result.toByteArray();
		if (arr.length != sectionLength + 20)
			throw new RuntimeException("inconsistent cmap");

		return arr;
	}

	private boolean isEnUnicode(NameEntry en) {
		if (en.platformID == 0)
			return true;
		if (en.platformID == 1 && en.encodingID == 3) // thnx lordkiron!
			return true;
		if (en.platformID == 3 && (en.encodingID == 1 || en.encodingID == 10)
				&& ((en.languageID & 0x3FF) == 9))
			return true;
		return false;
	}

	private String getFontID() {
		if (fontID == null) {
			fontID = "Subset:" + Long.toHexString(System.currentTimeMillis());
		}
		return fontID;
	}

	private byte[] buildNames() throws IOException {

		int offset = 0;
		int count = 0;

		for (int i = 0; i < names.length; i++) {
			NameEntry entry = names[i];
			// TODO: figure out which entries are required
			// entry.needed = true;
			switch (entry.nameID) {
			case 0:
				// copyright; always keep
				entry.needed = true;
				break;
			case 1:
				// family
			case 4:
				// full name
			case 16:
				// preferred family
			case 17:
				// preferred full name
			case 6: // thnx lordkiron!
				// Postscript name for the font
				entry.needed = true;
				seek(entry.offset);
				readBuffer(entry.length);
				if (isEnUnicode(entry)) {
					String name = decodeUnicode(buffer, 0, entry.length);
					name = "Subset-" + name;
					entry.newContent = encodeUnicode(name);
				} else {
					String name = new String(buffer, 0, entry.length,
							"ISO8859_1");
					name = "Subset-" + name;
					entry.newContent = name.getBytes("ISO8859_1");
				}
				break;
			case 2:
				// subfamily (bold, italic, etc.)
				entry.needed = true;
				break;
			case 7:
				// trademark; always keep
				entry.needed = true;
				break;
			case 8:
				// manufacturer name
				entry.needed = true;
				break;
			case 3:
				entry.needed = true;
				if (isEnUnicode(entry)) {
					entry.newContent = encodeUnicode(getFontID());
				} else {
					entry.newContent = getFontID().getBytes("ISO8859_1");
				}
				break;
			}

			if (entry.needed) {
				entry.newRelativeOffset = offset;
				int len;
				if (entry.newContent != null) {
					len = entry.newContent.length;
				} else {
					len = entry.length;
				}
				offset += len;
				count++;
			}
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeShort(out, 0); // format
		writeShort(out, (short) count);
		writeShort(out, (short) (count * 12 + 6));
		for (int i = 0; i < names.length; i++) {
			NameEntry entry = names[i];
			if (entry.needed) {
				writeShort(out, entry.platformID);
				writeShort(out, entry.encodingID);
				writeShort(out, entry.languageID);
				writeShort(out, entry.nameID);
				int len;
				if (entry.newContent != null) {
					len = entry.newContent.length;
				} else {
					len = entry.length;
				}
				writeShort(out, (short) len);
				writeShort(out, (short) entry.newRelativeOffset);
			}
		}

		for (int i = 0; i < names.length; i++) {
			NameEntry entry = names[i];
			if (entry.needed) {
				if (entry.newContent != null) {
					out.write(entry.newContent);
				} else {
					copyBytes(out, entry.offset, entry.length);
				}
			}
		}

		return out.toByteArray();
	}

	private byte[] buildGlyphsTT() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (int i = 0; i < glyphs.length; i++) {
			GlyphData glyph = glyphs[i];
			if (glyph.need) {
				if (glyph.compositeTT) {
					byte[] arr = glyph.length > buffer.length ? new byte[glyph.length]
							: buffer;
					seek(glyph.offset);
					readBuffer(arr, glyph.length);
					int index = 10;
					while (index <= glyph.length - 4) {
						int flags = getShort(arr, index);
						int glyphID = getShort(arr, index + 2) & 0xFFFF;
						stuffShort(arr, index + 2,
								(short) glyphs[glyphID].newIndex);
						if ((flags & 0x20) == 0)
							break;
						index += 4 + getCompositeGlyphArgSize(flags); // MORE_COMPONENTS
					}
					out.write(arr, 0, glyph.length);
				} else {
					copyBytes(out, glyph.offset, glyph.length);
				}
				int padCount = (4 - glyph.length) & 3;
				while (padCount > 0) {
					padCount--;
					out.write(0);
				}
			}
		}
		return out.toByteArray();
	}

	private byte[] buildGlyphLocations(boolean asShorts) throws IOException {
		int offset = 0;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (asShorts)
			writeShort(out, 0);
		else
			writeInt(out, 0);
		for (int i = 0; i < glyphs.length; i++) {
			GlyphData glyph = glyphs[i];
			if (glyph.need) {
				int paddedLength = (glyph.length + 3) & ~3;
				offset += paddedLength;
				if (asShorts)
					writeShort(out, offset / 2);
				else
					writeInt(out, offset);
			}
		}
		return out.toByteArray();
	}

	private void stuffLong(byte[] arr, int index, long n) {
		arr[index] = (byte) (n >> 56);
		arr[index + 1] = (byte) (n >> 48);
		arr[index + 2] = (byte) (n >> 40);
		arr[index + 3] = (byte) (n >> 32);
		arr[index + 4] = (byte) (n >> 24);
		arr[index + 5] = (byte) (n >> 16);
		arr[index + 6] = (byte) (n >> 8);
		arr[index + 7] = (byte) n;
	}

	private void stuffShort(byte[] arr, int index, short n) {
		arr[index] = (byte) (n >> 8);
		arr[index + 1] = (byte) n;
	}

	private byte[] buildHead(TableDirectoryEntry head, boolean shortOffsets)
			throws IOException {
		byte[] buf = new byte[head.length]; // expect 54 bytes long
		seek(head.offset);
		readBuffer(buf, head.length);
		buf[8] = 0; // zero out checkSumAdjustment
		buf[9] = 0;
		buf[10] = 0;
		buf[11] = 0;
		stuffLong(buf, 28, (new Date()).getTime() / 1000 + baseTime);
		buf[50] = 0;
		buf[51] = (byte) (shortOffsets ? 0 : 1);
		return buf;
	}

	private byte[] buildHHea(TableDirectoryEntry hhea) throws IOException {
		byte[] buf = new byte[hhea.length]; // expect 36 bytes long
		seek(hhea.offset);
		readBuffer(buf, hhea.length);
		buf[34] = (byte) (newVariableWidthCount >> 8);
		buf[35] = (byte) (newVariableWidthCount);
		return buf;
	}

	private byte[] buildHMtx() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (int i = 0; i < glyphs.length; i++) {
			GlyphData glyph = glyphs[i];
			if (glyph.need) {
				if (glyph.newIndex < newVariableWidthCount)
					writeShort(out, glyph.advance);
				writeShort(out, glyph.lsb);
			}
		}
		return out.toByteArray();
	}

	private byte[] buildMaxP(TableDirectoryEntry maxp) throws IOException {
		byte[] buf = new byte[maxp.length]; // expect at least 6 byte long
		seek(maxp.offset);
		readBuffer(buf, maxp.length);
		buf[4] = (byte) (newGlyphCount >> 8);
		buf[5] = (byte) (newGlyphCount);
		return buf;
	}

	private void writeOffsetCFF(ByteArrayOutputStream out, int offSize,
			int offset) throws IOException {
		switch (offSize) {
		case 1:
			out.write(offset);
			break;
		case 2:
			writeShort(out, offset);
			break;
		case 3:
			out.write(offset >> 16);
			writeShort(out, offset);
			break;
		case 4:
			writeInt(out, offset);
			break;
		}
	}

	private void writeIntCFF(ByteArrayOutputStream out, int val) {
		if (-107 <= val && val <= 107)
			out.write(val + 139);
		else if (108 <= val && val <= 1131) {
			val -= 108;
			out.write((val >> 8) + 247);
			out.write(val);
		} else if (-1131 <= val && val <= -108) {
			val = -val - 108;
			out.write((val >> 8) + 251);
			out.write(val);
		} else if (-0x8000 <= val && val <= 0x7FFF) {
			out.write(28);
			writeShort(out, val);
		} else {
			out.write(29);
			writeInt(out, val);
		}
	}

	private void writeObjectCFF(ByteArrayOutputStream out, Object obj)
			throws IOException {
		if (obj instanceof KeyCFF) {
			int key = ((KeyCFF) obj).keyID;
			if (key < 0xFF)
				out.write(key);
			else {
				out.write(12);
				out.write(key);
			}
		} else if (obj instanceof Integer) {
			int val = ((Integer) obj).intValue();
			writeIntCFF(out, val);
		} else if (obj instanceof StringCFF) {
			int val = ((StringCFF) obj).newIndex;
			writeIntCFF(out, val);
		} else if (obj instanceof Double) {
			String s = obj.toString();
			out.write(30);
			int b = 0;
			int i = 0;
			int len = s.length();
			boolean first = true;
			while (true) {
				int n;
				if (i >= len)
					n = 0xF;
				else {
					char c = s.charAt(i);
					if ('0' <= c && c <= '9')
						n = c - '0';
					else if (c == '-')
						n = 0xE;
					else if (c == '.')
						n = 0xA;
					else if (c == 'E' || c == 'e') {
						c = s.charAt(i + 1);
						if (c == '-') {
							i++;
							n = 0xB;
						} else {
							n = 0xC;
						}
					} else
						throw new IOException("Bad number: " + s);
				}
				if (first) {
					b = n;
				} else {
					b = (b << 4) | n;
					out.write(b);
					if (i >= len)
						break;
				}
				first = !first;
				i++;
			}
		} else if (obj instanceof IntPlaceholderCFF) {
			out.write(29);
			((IntPlaceholderCFF) obj).offset = out.size();
			writeInt(out, 0);
		} else if (obj instanceof Object[]) {
			Object[] arr = (Object[]) obj;
			for (int i = 0; i < arr.length; i++)
				writeObjectCFF(out, arr[i]);
		} else {
			throw new IOException("unknown object");
		}
	}

	private void writeDictCFF(ByteArrayOutputStream out, Hashtable dict)
			throws IOException {
		Enumeration keys = dict.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object val = dict.get(key);
			writeObjectCFF(out, val);
			writeObjectCFF(out, key);
		}
	}

	private void adjustOffsetObjectCFF(Object obj, int offsetAdj) {
		if (obj instanceof IntPlaceholderCFF) {
			((IntPlaceholderCFF) obj).offset += offsetAdj;
		} else if (obj instanceof Object[]) {
			Object[] arr = (Object[]) obj;
			for (int i = 0; i < arr.length; i++)
				adjustOffsetObjectCFF(arr[i], offsetAdj);
		}
	}

	private void adjustOffsetDictCFF(Hashtable dict, int offsetAdj) {
		Enumeration elements = dict.elements();
		while (elements.hasMoreElements()) {
			Object val = elements.nextElement();
			adjustOffsetObjectCFF(val, offsetAdj);
		}
	}

	private void writeIndexCFF(ByteArrayOutputStream out, Object[] index)
			throws IOException {
		writeShort(out, index.length);
		if (index.length == 0)
			return;

		ByteArrayOutputStream data = new ByteArrayOutputStream();
		int[] offsets = new int[index.length + 1];
		offsets[0] = 1;
		boolean adjustOffsets = false;
		for (int i = 0; i < index.length; i++) {
			Object item = index[i];
			if (item instanceof String) {
				byte[] sb = ((String) item).getBytes("ISO-8859-1");
				data.write(sb);
			} else if (item instanceof Hashtable) {
				writeDictCFF(data, (Hashtable) item);
				adjustOffsets = true;
			} else if (item instanceof Range) {
				Range r = (Range) item;
				copyBytes(data, r.offset, r.length);
			} else if (item instanceof GlyphData) {
				GlyphData r = (GlyphData) item;
				copyBytes(data, r.offset, r.length);
			} else {
				throw new IOException("unknown index type");
			}
			offsets[i + 1] = data.size() + 1;
		}

		int offSize;
		int offset = offsets[index.length];
		if (offset <= 0xFF)
			offSize = 1;
		else if (offset <= 0xFFFF)
			offSize = 2;
		else if (offset <= 0xFFFFFF)
			offSize = 3;
		else
			offSize = 4;
		out.write(offSize);
		for (int i = 0; i <= index.length; i++) {
			writeOffsetCFF(out, offSize, offsets[i]);
		}

		if (adjustOffsets) {
			int offsetAdj = out.size();
			for (int i = 0; i < index.length; i++) {
				Object item = index[i];
				if (item instanceof Hashtable) {
					adjustOffsetDictCFF((Hashtable) item, offsetAdj);
				}
			}
		}

		data.writeTo(out);
	}

	private void writeCharsetCFF(ByteArrayOutputStream out) {
		int i = 0;
		int count = 0;
		int prevsid = -1;
		out.write(2); // use format 2
		while (true) {
			GlyphData glyph = glyphs[i++];
			if (glyph.need) {
				int sid = glyph.namesidCFF;
				if (sid >= CFF_STD_STRING_COUNT) {
					sid = stringsCFF[sid - CFF_STD_STRING_COUNT].newIndex;
				}
				if (prevsid == -1) {
					writeShort(out, sid);
					count = 0;
				} else if (prevsid != sid - 1) {
					writeShort(out, count);
					writeShort(out, sid);
					count = 0;
				} else {
					count++;
				}
				prevsid = sid;
			}
			if (i >= glyphs.length) {
				writeShort(out, count);
				break;
			}
		}
	}

	private Object[] makeGlyphArrayCFF() {
		Object[] subset = new Object[newGlyphCount];
		int index = 0;
		for (int i = 0; i < glyphs.length; i++) {
			if (glyphs[i].need) {
				subset[index++] = glyphs[i];
			}
		}
		return subset;
	}

	private void sweepDictCFF(Hashtable dict) throws IOException {
		for (int i = 0; i < sidIndicesCFF.length; i++) {
			KeyCFF key = new KeyCFF(sidIndicesCFF[i]);
			Object val = dict.get(key);
			if (val == null)
				continue;
			if (val instanceof Integer) {
				int sid = ((Integer) val).intValue();
				if (sid >= CFF_STD_STRING_COUNT) {
					StringCFF string = stringsCFF[sid - CFF_STD_STRING_COUNT];
					string.needed = true;
					dict.put(key, string);
				}
			} else {
				throw new IOException("unsupported value for SID key");
			}
		}
	}

	private byte[] buildCFF() throws IOException {
		Hashtable dict = (Hashtable) dictCFF.clone();
		sweepDictCFF(dict);
		String[] newStrings = reindexStringsCFF();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(1); // version major
		out.write(0); // version minor
		out.write(4); // header size
		out.write(3); // abs offset size
		String[] names = { "Subset-" + nameCFF };
		writeIndexCFF(out, names);
		Object[] origprivatedict = (Object[]) dict.get(new KeyCFF(
				KeyCFF.PRIVATE));
		int origPrivateDictLen = ((Number) origprivatedict[0]).intValue();
		// int origPrivateDictOffset = ((Number) origprivatedict[1]).intValue();
		IntPlaceholderCFF charset = new IntPlaceholderCFF();
		IntPlaceholderCFF charstrings = new IntPlaceholderCFF();
		IntPlaceholderCFF privatedict = new IntPlaceholderCFF();
		Object[] pd = (Object[]) dict.get(new KeyCFF(KeyCFF.PRIVATE));
		Object[] privateval = { pd[0], privatedict };
		dict.put(new KeyCFF(KeyCFF.CHARSET), charset);
		dict.put(new KeyCFF(KeyCFF.CHARSTRINGS), charstrings);
		dict.put(new KeyCFF(KeyCFF.PRIVATE), privateval);
		dict.remove(new KeyCFF(KeyCFF.ENCODING));
		Hashtable[] dicts = { dict };
		writeIndexCFF(out, dicts);
		writeIndexCFF(out, newStrings);
		writeIndexCFF(out, globalSubrsCFF);
		int charsetOffset = out.size();
		writeCharsetCFF(out);
		int charstringsOffset = out.size();
		writeIndexCFF(out, makeGlyphArrayCFF());
		int privatedictOffset = out.size();
		writeDictCFF(out, privateDictCFF);
		int privatesubrOffset = out.size();
		if (privatesubrOffset - privatedictOffset != origPrivateDictLen)
			throw new IOException("private dict writing error");
		writeIndexCFF(out, privateSubrsCFF);
		byte[] result = out.toByteArray();
		setIntAtIndex(result, charset.offset, charsetOffset);
		setIntAtIndex(result, charstrings.offset, charstringsOffset);
		setIntAtIndex(result, privatedict.offset, privatedictOffset);
		return result;
	}

	private void sweepTables() throws IOException {

		byte[] glyfSection;
		byte[] cffSection;

		if (trueTypeGlyphs) {
			glyfSection = buildGlyphsTT();
			cffSection = null;
		} else {
			glyfSection = null;
			cffSection = buildCFF();
		}
		int offset = 0;
		for (int i = 0; i < header.numTables; i++) {
			TableDirectoryEntry entry = header.tableDirectory[i];
			if (entry.identifier.equals("cmap")) {
				// replace
				entry.newContent = buildCMap();
				entry.need = true;
			} else if (entry.identifier.equals("head")) {
				entry.need = true;
				entry.newContent = buildHead(entry, glyfSection != null
						&& glyfSection.length <= 0x1FFFF);
			} else if (entry.identifier.equals("hhea")) {
				entry.need = true;
				entry.newContent = buildHHea(entry);
			} else if (entry.identifier.equals("hmtx")) {
				entry.need = true;
				entry.newContent = buildHMtx();
			} else if (entry.identifier.equals("maxp")) {
				entry.need = true;
				entry.newContent = buildMaxP(entry);
			} else if (entry.identifier.equals("name")) {
				// replace
				entry.need = true;
				entry.newContent = buildNames();
			} else if (entry.identifier.equals("OS/2")) {
				// good as is
				entry.need = true;
			} else if (entry.identifier.equals("post")) {
				// good as is
				entry.need = true;
			} else if (entry.identifier.equals("cvt ")) {
				// good as is
				entry.need = true;
			} else if (entry.identifier.equals("fpgm")) {
				// good as is
				entry.need = true;
			} else if (entry.identifier.equals("glyf")) {
				// replace
				entry.need = true;
				entry.newContent = glyfSection;
			} else if (entry.identifier.equals("loca")) {
				// replace
				entry.need = true;
				entry.newContent = buildGlyphLocations(glyfSection.length <= 0x1FFFF);
			} else if (entry.identifier.equals("CFF ")) {
				// replace
				entry.need = true;
				entry.newContent = cffSection;
			} else if (entry.identifier.equals("prep")) {
				// good as is
				entry.need = true;
			} else if (entry.identifier.equals("fpgm")) {
				// good as is
				entry.need = true;
			} else if (entry.identifier.equals("gasp")) {
				// good as is
				entry.need = true;
			}
			if (entry.need) {
				entry.newRelativeOffset = offset;
				int len = (entry.newContent != null ? entry.newContent.length
						: entry.length);
				offset += ((len + 3) & ~3);
			}
		}
	}

	private int calculateTableCheckSum(byte[] content) {
		int result = 0;
		int wordCount = content.length / 4;
		for (int i = 0; i < wordCount; i++) {
			result += getInt(content, i * 4);
		}
		int offset = 24;
		for (int i = wordCount * 4; i < content.length; i++) {
			result += ((content[i] & 0xFF) << offset);
			offset -= 8;
		}
		return result;
	}

	private void copyBytes(OutputStream out, int offset, int len)
			throws IOException {
		seek(offset);
		while (len > 0) {
			int r = len;
			if (r > buffer.length)
				r = buffer.length;
			readBuffer(r);
			out.write(buffer, 0, r);
			len -= r;
		}
	}

	private void setIntAtIndex(byte[] arr, int index, int value) {
		arr[index++] = (byte) (value >> 24);
		arr[index++] = (byte) (value >> 16);
		arr[index++] = (byte) (value >> 8);
		arr[index++] = (byte) value;

	}

	private byte[] writeTables() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int numTables = 0;
		for (int i = 0; i < header.numTables; i++) {
			TableDirectoryEntry entry = header.tableDirectory[i];
			if (entry.need)
				numTables++;
		}

		// file header
		if (trueTypeGlyphs)
			writeInt(out, 0x10000);
		else {
			out.write('O');
			out.write('T');
			out.write('T');
			out.write('O');
		}
		writeShort(out, (short) numTables);
		int log = floorPowerOf2(numTables);
		int entrySelector = log;
		int searchRange = 1 << (log + 4);
		int rangeShift = numTables * 16 - searchRange;
		writeShort(out, (short) searchRange);
		writeShort(out, (short) entrySelector);
		writeShort(out, (short) rangeShift);

		// table entries
		int baseOffset = numTables * 16 + 12;
		for (int i = 0; i < header.numTables; i++) {
			TableDirectoryEntry entry = header.tableDirectory[i];
			if (entry.need) {
				byte[] tag = entry.identifier.getBytes();
				out.write(tag);
				for (int k = tag.length; k < 4; k++)
					out.write(0);
				if (entry.newContent == null) {
					writeInt(out, entry.checkSum);
					writeInt(out, entry.newRelativeOffset + baseOffset);
					writeInt(out, entry.length);
				} else {
					int checkSum = calculateTableCheckSum(entry.newContent);
					writeInt(out, checkSum);
					writeInt(out, entry.newRelativeOffset + baseOffset);
					writeInt(out, entry.newContent.length);
				}
			}
		}

		// table content
		for (int i = 0; i < header.numTables; i++) {
			TableDirectoryEntry entry = header.tableDirectory[i];
			if (entry.need) {
				if (entry.newContent != null) {
					out.write(entry.newContent);
					int len = entry.newContent.length;
					int padCount = (4 - len) & 3;
					while (padCount > 0) {
						out.write(0);
						padCount--;
					}
				} else {
					copyBytes(out, entry.offset, (entry.length + 3) & ~3);
				}
			}
		}

		// adjust checkSumAdjustment
		TableDirectoryEntry head = findTable("head");
		byte[] result = out.toByteArray();
		int checkSum = calculateTableCheckSum(result);
		int checkSumAdjustment = 0xB1B0AFBA - checkSum;
		int index = head.newRelativeOffset + baseOffset + 8;
		setIntAtIndex(result, index, checkSumAdjustment);
		return result;
	}

	private void readOS2() throws IOException {
		TableDirectoryEntry os2 = findTable("OS/2");
		if (os2 == null)
			throw new IOException("No OS/2 table found");
		seek(os2.offset);
		readBuffer(10);
		weight = getShort(buffer, 4);
		width = getShort(buffer, 6);
		fsType = getShort(buffer, 8);
		seek(os2.offset + 62);
		readBuffer(2);
		short fsSelection = getShort(buffer, 0);
		if ((fsSelection & 1) != 0)
			style = STYLE_ITALIC;
	}

	public boolean canSubset() throws IOException {
		if ((fsType & 0xF) == 2)
			return false; // explicitly disallowed embedding and subsetting
		if ((fsType & 0x0100) != 0)
			return false; // explicitly disallowed subsetting
		return true;
	}

	public boolean canEmbedForReading() throws IOException {
		if ((fsType & 0xF) == 2)
			return false; // explicitly disallowed embedding and subsetting
		if ((fsType & 0x0200) != 0)
			return false; // explicitly disallowed outline embedding
		return true;
	}

	public boolean canEmbedForEditing() throws IOException {
		switch (fsType & 0xF) {
		case 2: // explicitly disallowed embedding and subsetting
		case 4: // read-only embedding
			return false;
		}
		if ((fsType & 0x0200) != 0)
			return false; // explicitly disallowed outline embedding
		return true;
	}

	public byte[] getSubsettedFont() throws IOException {
		if (!canSubset())
			throw new IOException("subsetting not allowed for this font");
		if (trueTypeGlyphs)
			resolveCompositeGlyphsTT();
		reindexGlyphs();
		sweepTables();
		return writeTables();
	}

	public String getFamilyName() {
		return familyName;
	}

	public int getStyle() {
		return style;
	}

	public int getWeight() {
		return weight;
	}

	public int getWidth() {
		return width;
	}

	public static void main(String[] args) {
		try {
			FileFontInputStream font = new FileFontInputStream(
					new File(args[0]));
			OpenTypeFont s = new OpenTypeFont(font);
			char[] text = "Hello!".toCharArray();
			s.play(text, 0, text.length);
			byte[] result = s.getSubsettedFont();
			OutputStream out = new FileOutputStream(new File(args[1]));
			out.write(result);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
