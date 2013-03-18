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

package com.adobe.dp.epub.util;

public class ImageDimensions {

	private static int readInt(byte[] buf, int offset) {
		return ((buf[offset] & 0xFF) << 24) | ((buf[offset + 1] & 0xFF) << 16) | ((buf[offset + 2] & 0xFF) << 8)
				| (buf[offset + 3] & 0xFF);
	}

	private static int readShort(byte[] buf, int offset) {
		return ((buf[offset] & 0xFF) << 8) | (buf[offset + 1] & 0xFF);
	}

	private static int readShortBE(byte[] buf, int offset) {
		return (buf[offset] & 0xFF) | ((buf[offset + 1] & 0xFF) << 8);
	}

	public static int[] getImageDimensions(byte[] buf) {
		if (buf[0] == (byte) 0xFF && buf[1] == (byte) 0xD8 && buf[2] == (byte) 0xFF && buf[3] == (byte) 0xE0
				&& buf[6] == 'J' && buf[7] == 'F' && buf[8] == 'I' && buf[9] == 'F') {
			// JPEG image
			int k = 2;
			while (k + 5 < buf.length && buf[k] == (byte) 0xFF) {
				int tag = buf[k + 1] & 0xFF;
				switch (tag) {
				case 0xC0:
				case 0xC1:
				case 0xC2:
				case 0xC3:
				case 0xC9:
				case 0xCA:
				case 0xCB: {
					int[] dim = new int[2];
					dim[0] = readShort(buf, k + 7);
					dim[1] = readShort(buf, k + 5);
					return dim;
				}
				}
				int size = 2 + ((buf[k + 2] & 0xFF) << 8) + (buf[k + 3] & 0xFF);
				k += size;
			}
		} else if (buf[0] == 'G' && buf[1] == 'I' && buf[2] == 'F') {
			// GIF image
			int[] dim = new int[2];
			dim[0] = readShortBE(buf, 6);
			dim[1] = readShortBE(buf, 8);
			return dim;
		} else if (buf[0] == (byte) 0x89 && buf[1] == 'P' && buf[2] == 'N' && buf[3] == 'G' && buf[4] == 0x0D
				&& buf[5] == 0x0A && buf[6] == 0x1A && buf[7] == 0x0A && buf[12] == 'I' && buf[13] == 'H'
				&& buf[14] == 'D' && buf[15] == 'R') {
			// PNG image
			int[] dim = new int[2];
			dim[0] = readInt(buf, 16);
			dim[1] = readInt(buf, 20);
			return dim;
		}
		return null;
	}

	/*

	bool GetImageSize(const char *fn, int *x,int *y)
	{ FILE *f=fopen(fn,"rb"); if (f==0) return false;
	  fseek(f,0,SEEK_END); long len=ftell(f); fseek(f,0,SEEK_SET); 
	  if (len<24) {fclose(f); return false;}

	  // Strategy:
	  // reading GIF dimensions requires the first 10 bytes of the file
	  // reading PNG dimensions requires the first 24 bytes of the file
	  // reading JPEG dimensions requires scanning through jpeg chunks
	  // In all formats, the file is at least 24 bytes big, so we'll read that always
	  unsigned char buf[24]; fread(buf,1,24,f);

	  // For JPEGs, we need to read the first 12 bytes of each chunk.
	  // We'll read those 12 bytes at buf+2...buf+14, i.e. overwriting the existing buf.
	  if (buf[0]==0xFF && buf[1]==0xD8 && buf[2]==0xFF && buf[3]==0xE0 && buf[6]=='J' && buf[7]=='F' && buf[8]=='I' && buf[9]=='F')
	  { long pos=2;
	    while (buf[2]==0xFF)
	    { if (buf[3]==0xC0 || buf[3]==0xC1 || buf[3]==0xC2 || buf[3]==0xC3 || buf[3]==0xC9 || buf[3]==0xCA || buf[3]==0xCB) break;
	      pos += 2+(buf[4]<<8)+buf[5];
	      if (pos+12>len) break;
	      fseek(f,pos,SEEK_SET); fread(buf+2,1,12,f);    
	    }
	  }

	  fclose(f);

	  // JPEG: (first two bytes of buf are first two bytes of the jpeg file; rest of buf is the DCT frame
	  if (buf[0]==0xFF && buf[1]==0xD8 && buf[2]==0xFF)
	  { *y = (buf[7]<<8) + buf[8];
	    *x = (buf[9]<<8) + buf[10];
	    return true;
	  }

	  // GIF: first three bytes say "GIF", next three give version number. Then dimensions
	  if (buf[0]=='G' && buf[1]=='I' && buf[2]=='F')
	  { *x = buf[6] + (buf[7]<<8);
	    *y = buf[8] + (buf[9]<<8);
	    return true;
	  }

	  // PNG: the first frame is by definition an IHDR frame, which gives dimensions
	  if ( buf[0]==0x89 && buf[1]=='P' && buf[2]=='N' && buf[3]=='G' && buf[4]==0x0D && buf[5]==0x0A && buf[6]==0x1A && buf[7]==0x0A
	    && buf[12]=='I' && buf[13]=='H' && buf[14]=='D' && buf[15]=='R')
	  { *x = (buf[16]<<24) + (buf[17]<<16) + (buf[18]<<8) + (buf[19]<<0);
	    *y = (buf[20]<<24) + (buf[21]<<16) + (buf[22]<<8) + (buf[23]<<0);
	    return true;
	  }

	  return false;
	}

	 */	
	
}
