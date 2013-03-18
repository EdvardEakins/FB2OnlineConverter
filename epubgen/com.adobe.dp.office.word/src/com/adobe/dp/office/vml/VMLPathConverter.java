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
package com.adobe.dp.office.vml;

import java.util.Iterator;
import java.util.Vector;

import com.adobe.dp.office.word.Element;

public class VMLPathConverter implements VMLEnv {

	float xscale;

	float yscale;

	int[] adj;

	VMLCoordPair origin;

	VMLCoordPair size;

	VMLCoordPair limo;

	VMLShapeElement shape;

	VMLShapeTypeElement shapeType;

	Vector callouts = new Vector();

	float outerWidth = 100;

	float outerHeight = 100;

	float pathScaleX = 1;

	float pathScaleY = 1;

	float pathScale = 1;

	public VMLPathConverter(VMLShapeElement shape) {
		this.shape = shape;
		this.shapeType = shape.type;
		if (this.shapeType == null)
			return;
		this.adj = (shape.adj == null ? shapeType.adj : shape.adj);
		this.origin = (shape.origin == null ? shapeType.origin : shape.origin);
		this.size = (shape.size == null ? shapeType.size : shape.size);
		this.limo = (shape.limo == null ? shapeType.limo : shape.limo);
	}

	public void setScale(float xscale, float yscale) {
		this.xscale = xscale;
		this.yscale = yscale;
	}

	public int resolveCallout(VMLCallout callout) {
		if (callout.code == '#')
			return adj[callout.index];
		return ((Integer) callouts.get(callout.index)).intValue();
	}

	public int resolveEnv(String env) {
		env = env.toLowerCase();
		if (env.equals("width"))
			return size.x;
		if (env.equals("height"))
			return size.y;
		if (env.equals("xcenter"))
			return origin.x + size.x / 2;
		if (env.equals("ycenter"))
			return origin.y + size.y / 2;
		if (env.equals("xlimo"))
			return (limo == null ? 0 : limo.x);
		if (env.equals("ylimo"))
			return (limo == null ? 0 : limo.y);
		if (env.equals("linedrawn"))
			return shape.stroke == null ? 0 : 1;
		if (env.equals("pixellinewidth")) {
			String weight = shape.strokeWeight;
			if (weight != null) {
				float w = readCSSLength(weight, 0);
				if (w > 0)
					return (int) Math.round(w * xscale * pathScale);
			}
		}
		if (env.equals("pixelwidth")) {
			return (int) Math.round(size.x * xscale * pathScale);
		}
		if (env.equals("pixelheight")) {
			return (int) Math.round(size.x * xscale * pathScale);
		}
		throw new RuntimeException("unknown env: " + env);
	}

	public void readFormulas() {
		Element formulas = null;
		Iterator it = shapeType.content();
		while (it.hasNext()) {
			Object n = it.next();
			if (n instanceof VMLFormulasElement) {
				formulas = (VMLFormulasElement) n;
				break;
			}
		}
		if (formulas == null)
			return;
		it = formulas.content();
		while (it.hasNext()) {
			Object n = it.next();
			if (n instanceof VMLFElement) {
				VMLFElement f = (VMLFElement) n;
				int val = f.eval(this);
				callouts.add(new Integer(val));
			}
		}
	}

	public VMLCoordPair getSize() {
		return size;
	}

	public VMLCoordPair getOrigin() {
		return origin;
	}

	public void setOuterSize(float width, float height) {
		outerWidth = width;
		outerHeight = height;
		pathScaleX = width / size.x;
		pathScaleY = height / size.y;
		pathScale = (pathScaleX > pathScaleY ? pathScaleY : pathScaleX);
	}

	private float transformX(int x) {
		float r;
		if (limo != null) {
			if (x < limo.x)
				r = -outerWidth / 2 + pathScale * (x - origin.x);
			else
				r = outerWidth / 2 - pathScale * (origin.x + size.x - x);
		} else {
			r = pathScaleX * (x - origin.x) - outerWidth / 2;
		}
		return Math.round(r * 100) / 100.0f;
	}

	private float transformY(int y) {
		float r;
		if (limo != null) {
			if (y < limo.y)
				r = -outerHeight / 2 + pathScale * (y - origin.y);
			else
				r = outerHeight / 2 - pathScale * (origin.y + size.y - y);
		} else {
			r = pathScaleY * (y - origin.y) - outerHeight / 2;
		}
		return Math.round(r * 100) / 100.0f;
	}

	private int resolve(Object arg) {
		if (arg instanceof VMLCallout) {
			return resolveCallout((VMLCallout) arg);
		} else if (arg instanceof Integer) {
			return ((Integer) arg).intValue();
		}
		throw new RuntimeException("bad arg: " + arg);
	}

	private void drawArrow(StringBuffer sb, String type, int sx, int sy, int tx, int ty) {
		double x0 = transformX(sx);
		double y0 = transformY(sy);
		double x1 = transformX(tx);
		double y1 = transformY(ty);
		double dx = x0 - x1;
		double dy = y0 - y1;
		double len = Math.sqrt(dx * dx + dy * dy);
		if (len < 0.001)
			return;
		double nx = dx / len;
		double ny = dy / len;
		double targetLen = 100;

		if (type.equals("diamond") || type.equals("oval")) {
			double targetHalfWidth = targetLen / 3;
			double ax1 = x1 + targetHalfWidth * nx;
			double ay1 = y1 + targetHalfWidth * ny;
			double ax2 = x1 - targetHalfWidth * ny;
			double ay2 = y1 + targetHalfWidth * nx;
			double ax3 = x1 - targetHalfWidth * nx;
			double ay3 = y1 - targetHalfWidth * ny;
			double ax4 = x1 + targetHalfWidth * ny;
			double ay4 = y1 - targetHalfWidth * nx;
			sb.append('M');
			sb.append(Math.round(ax1 * 100) / 100.0);
			sb.append(' ');
			sb.append(Math.round(ay1 * 100) / 100.0);
			sb.append('L');
			sb.append(Math.round(ax2 * 100) / 100.0);
			sb.append(' ');
			sb.append(Math.round(ay2 * 100) / 100.0);
			sb.append('L');
			sb.append(Math.round(ax3 * 100) / 100.0);
			sb.append(' ');
			sb.append(Math.round(ay3 * 100) / 100.0);
			sb.append('L');
			sb.append(Math.round(ax4 * 100) / 100.0);
			sb.append(' ');
			sb.append(Math.round(ay4 * 100) / 100.0);
			sb.append('z');
		} else {
			double targetHalfWidth = targetLen / 4;
			double bx = x1 + targetLen * nx;
			double by = y1 + targetLen * ny;
			double ax1 = bx + targetHalfWidth * ny;
			double ay1 = by - targetHalfWidth * nx;
			double ax2 = bx - targetHalfWidth * ny;
			double ay2 = by + targetHalfWidth * nx;
			sb.append('M');
			sb.append(Math.round(ax1 * 100) / 100.0);
			sb.append(' ');
			sb.append(Math.round(ay1 * 100) / 100.0);
			sb.append('L');
			sb.append(x1);
			sb.append(' ');
			sb.append(y1);
			sb.append('L');
			sb.append(Math.round(ax2 * 100) / 100.0);
			sb.append(' ');
			sb.append(Math.round(ay2 * 100) / 100.0);
			sb.append('z');
		}
	}

	private void startLine(StringBuffer sb, int x0, int y0, int x1, int y1) {
		if (shape.startArrow != null && !shape.startArrow.equals("none")) {
			drawArrow(sb, shape.startArrow, x1, y1, x0, y0);
		}
		sb.append('M');
		sb.append(transformX(x0));
		sb.append(' ');
		sb.append(transformY(y0));
	}

	private void endLine(StringBuffer sb, int x0, int y0, int x1, int y1) {
		if (shape.endArrow != null && !shape.endArrow.equals("none")) {
			drawArrow(sb, shape.endArrow, x0, y0, x1, y1);
		}
	}

	public String getSVGPath() {
		VMLPathSegment[] segments = shapeType.path;
		if (segments == null)
			return null;
		StringBuffer sb = new StringBuffer();
		int lastX = 0;
		int lastY = 0;
		int prevX = 0;
		int prevY = 0;
		boolean movedTo = false;
		for (int i = 0; i < segments.length; i++) {
			VMLPathSegment s = segments[i];
			if (s.command.equals("m")) {
				lastX = resolve(s.args[0]);
				lastY = resolve(s.args[1]);
				movedTo = false;
			} else if (s.command.equals("l")) {
				// lineto
				for (int k = 0; k + 1 < s.args.length; k += 2) {
					int x = resolve(s.args[k]);
					int y = resolve(s.args[k + 1]);
					if (!movedTo) {
						startLine(sb, lastX, lastY, x, y);
						movedTo = true;
					}
					prevX = lastX;
					prevY = lastY;
					lastX = x;
					lastY = y;
					sb.append('L');
					sb.append(transformX(lastX));
					sb.append(' ');
					sb.append(transformY(lastY));
				}
			} else if (s.command.equals("t")) {
				// rmoveto
				lastX += resolve(s.args[0]);
				lastY += resolve(s.args[1]);
				movedTo = false;
			} else if (s.command.equals("r")) {
				// rlineto
				int x0 = lastX;
				int y0 = lastY;
				for (int k = 0; k + 1 < s.args.length; k += 2) {
					if (k > 0)
						sb.append(' ');
					int x = resolve(s.args[k]) + x0;
					int y = resolve(s.args[k + 1]) + y0;
					if (!movedTo) {
						startLine(sb, lastX, lastY, x, y);
						movedTo = true;
					}
					prevX = lastX;
					prevY = lastY;
					lastX = x;
					lastY = y;
					sb.append('L');
					sb.append(transformX(lastX));
					sb.append(' ');
					sb.append(transformY(lastY));
				}
			} else if (s.command.equals("x")) {
				if (movedTo) {
					sb.append('z');
					movedTo = false;
				}
			} else if (s.command.equals("e")) {
				if (movedTo) {
					endLine(sb, prevX, prevY, lastX, lastY);
					// no such SVG thing
					movedTo = false;
				}
			} else if (s.command.equals("qy") || s.command.equals("qx")) {
				boolean tanX = s.command.equals("qx");
				for (int k = 0; k + 1 < s.args.length; k += 2) {
					int tx = resolve(s.args[k]);
					int ty = resolve(s.args[k + 1]);
					if (!movedTo) {
						if (tanX)
							startLine(sb, lastX, lastY, tx, lastY);
						else
							startLine(sb, lastX, lastY, lastX, ty);
						movedTo = true;
					}
					sb.append('C');
					if (tanX) {
						sb.append(transformX(lastX + 2 * (tx - lastX) / 3));
						sb.append(' ');
						sb.append(transformY(lastY));
						sb.append(' ');
						sb.append(transformX(tx));
						sb.append(' ');
						sb.append(transformY(lastY + (ty - lastY) / 3));
						prevX = tx;
						prevY = lastY;
					} else {
						sb.append(transformX(lastX));
						sb.append(' ');
						sb.append(transformY(lastY + 2 * (ty - lastY) / 3));
						sb.append(' ');
						sb.append(transformX(lastX + (tx - lastX) / 3));
						sb.append(' ');
						sb.append(transformY(ty));
						prevX = lastX;
						prevY = ty;
					}
					sb.append(' ');
					sb.append(transformX(tx));
					sb.append(' ');
					sb.append(transformY(ty));
					lastX = tx;
					lastY = ty;
					tanX = !tanX;
				}
			} else if (s.command.equals("c")) {
				for (int k = 0; k + 5 < s.args.length; k += 6) {
					int cx1 = resolve(s.args[k]);
					int cy1 = resolve(s.args[k + 1]);
					int cx2 = resolve(s.args[k + 2]);
					int cy2 = resolve(s.args[k + 3]);
					int cx3 = resolve(s.args[k + 4]);
					int cy3 = resolve(s.args[k + 5]);
					if (!movedTo) {
						startLine(sb, lastX, lastY, cx1, cy1);
						movedTo = true;
					}
					sb.append('C');
					sb.append(transformX(cx1));
					sb.append(' ');
					sb.append(transformY(cy1));
					sb.append(' ');
					sb.append(transformX(cx2));
					sb.append(' ');
					sb.append(transformY(cy2));
					sb.append(' ');
					sb.append(transformX(cx3));
					sb.append(' ');
					sb.append(transformY(cy3));
					prevX = cx2;
					prevY = cy2;
					lastX = cx3;
					lastY = cy3;
				}
			} else {
				System.err.println("path command " + s.command + " not supported");
			}
		}
		return sb.toString();
	}

	static public float readCSSLength(String len, float def) {
		int ulen = 2;
		float conv = 0;
		if (len.endsWith("pt") || len.endsWith("px")) {
			conv = 1;
		} else if (len.endsWith("in")) {
			conv = 1 / 72.0f;
		}
		if (conv > 0) {
			try {
				return Float.parseFloat(len.substring(0, len.length() - ulen)) * conv;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return def;
	}

	public float[] getTextBox() {
		Object[] textbox = shapeType.textbox;
		if (textbox == null)
			return null;
		float[] tb = new float[textbox.length];
		for (int i = 0; i < textbox.length; i += 2) {
			tb[i] = transformX(resolve(textbox[i]));
			tb[i + 1] = transformY(resolve(textbox[i + 1]));
		}
		return tb;
	}

}
