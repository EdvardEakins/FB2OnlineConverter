package com.adobe.dp.epub.web.font;

import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FontCookieSet {
	
	private HashSet set = new HashSet();
		
	public FontCookieSet() {	
	}
	
	public FontCookieSet( HttpServletRequest req ) {
		Cookie[] cookies = req.getCookies();
		if( cookies != null ) {
			for( int i = 0 ; i < cookies.length ; i++ ) {
				Cookie cookie = cookies[i];
				if( !cookie.getName().startsWith("fontSet") )
					continue;
				String value = cookie.getValue();
				int len = value.length();
				if( len % 28 != 0 )
					continue;
				for( int k = 0 ; k < len ; k += 28 ) {
					String hash = value.substring(k, k+28);
					if( !hash.endsWith("=") )
						continue;
					set.add(hash);
				}
			}
		}
	}
	
	public void addFontHash( String sha1str ) {
		set.add(sha1str);
	}
	
	public void removeFontHash( String sha1str ) {
		set.remove(sha1str);
	}
	
	public Iterator hashes() {
		return set.iterator();
	}
	
	public void setCookies( HttpServletResponse resp, String pathURI, int expiry ) {
		// group them by 100s, not to exceed max cookie size
		final int maxFontSetSize = 100;
		int fontSetCount = 1;
		int fontCount = 0;
		Iterator it = set.iterator();
		StringBuffer sb = new StringBuffer();
		while( true ) {
			if( it.hasNext() ) {
				sb.append(it.next());
				fontCount++;
				if( fontCount < maxFontSetSize )
					continue;
			}
			Cookie cookie = new Cookie("fontSet" + fontSetCount, sb.toString());
			cookie.setPath(pathURI);
			cookie.setMaxAge(expiry);
			sb.delete(0, sb.length());
			fontSetCount++;
			resp.addCookie(cookie);
			if( !it.hasNext() )
				break;
		}
	}
}
