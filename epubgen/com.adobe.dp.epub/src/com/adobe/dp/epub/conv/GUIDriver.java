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
package com.adobe.dp.epub.conv;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GUIDriver extends JFrame {

	public static final long serialVersionUID = 0;

	BufferedImage epubIcon;

	BufferedImage cssIcon;

	BufferedImage otfIcon;

	BufferedImage ttfIcon;

	BufferedImage errIcon;

	DragSource dragSource = DragSource.getDefaultDragSource();

	boolean dragActive;

	Point dragStart = new Point(0, 0);

	Vector conversionQueue = new Vector();

	FileIcon currentlyConverting;

	FileIcon[] localDrag;

	JTabbedPane tabbedPane;

	FilePanel docPane;

	FilePanel resourcePane;

	SettingsPanel settingsPane;

	JEditorPane helpPane;

	File docFolder;

	File resourceFolder;

	File settingsFile;

	File workFolder;

	Properties settings = new Properties();

	static DataFlavor urilist;

	static {
		try {
			urilist = new DataFlavor("text/uri-list;class=java.lang.String");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class HighlightedFilter extends RGBImageFilter {
		public HighlightedFilter() {
			canFilterIndexColorModel = true;
		}

		public int filterRGB(int x, int y, int rgb) {
			return (((rgb & 0xFEFEFE) >> 1) + 0x80) | (rgb & 0xFF000000);
		}
	}

	class AlphaFilter extends RGBImageFilter {
		private int alpha;

		public AlphaFilter() {
			canFilterIndexColorModel = true;
		}

		public void setLevel(float f) {
			alpha = (int) Math.round(f * 255);
		}

		public int filterRGB(int x, int y, int rgb) {
			int alpha = ((this.alpha * (rgb >>> 24)) / 255) << 24;
			return (rgb & 0x00FFFFFF) | alpha;
		}
	}

	class Updater extends AbstractAction {

		public static final long serialVersionUID = 0;

		Updater() {
			new Timer(50, this).start();
		}

		public void actionPerformed(ActionEvent evt) {
			if (currentlyConverting != null)
				currentlyConverting.repaint();
		}
	}

	class FileCheck extends AbstractAction {

		public static final long serialVersionUID = 0;

		Timer timer;

		FileCheck() {
			timer = new Timer(500, this);
			timer.start();
		}

		public void actionPerformed(ActionEvent evt) {
			timer.setDelay(3000);
			docPane.checkFiles();
			resourcePane.checkFiles();
		}
	}

	class SettingsPanel extends JPanel implements ChangeListener {

		public static final long serialVersionUID = 0;

		JCheckBox translit = new JCheckBox("Transliterate cyrillic metadata");

		JCheckBox embedFonts = new JCheckBox("Embed fonts");

		JCheckBox adobeMangling = new JCheckBox("Use Adobe font mangling");

		JCheckBox pageBreaks = new JCheckBox("Add page map using page breaks (DOCX only)");

		SettingsPanel() {
			Box box = Box.createVerticalBox();
			add(box);
			box.add(translit);
			box.add(embedFonts);
			box.add(adobeMangling);
			// does not work all that well
			//box.add(pageBreaks); 
			translit.setSelected(getBooleanProperty("translit", true));
			embedFonts.setSelected(getBooleanProperty("embedFonts", true));
			adobeMangling.setSelected(getBooleanProperty("adobeMangling", true));
			pageBreaks.setSelected(getBooleanProperty("pageBreaks", false));
			translit.addChangeListener(this);
			embedFonts.addChangeListener(this);
			adobeMangling.addChangeListener(this);
			pageBreaks.addChangeListener(this);
		}

		boolean getBooleanProperty(String name, boolean def) {
			String s = settings.getProperty(name);
			if (s == null)
				return def;
			return s.toLowerCase().startsWith("t");
		}

		void setBooleanProperty(String name, boolean val) {
			settings.setProperty(name, val ? "true" : "false");
		}

		public void stateChanged(ChangeEvent arg) {
			setBooleanProperty("translit", translit.isSelected());
			setBooleanProperty("embedFonts", embedFonts.isSelected());
			setBooleanProperty("adobeMangling", adobeMangling.isSelected());
			setBooleanProperty("pageBreaks", pageBreaks.isSelected());
			try {
				FileOutputStream out = new FileOutputStream(settingsFile);
				settings.store(out, "EPUBGen");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	class FilePanel extends JPanel implements DropTargetListener, DragSourceListener {

		public static final long serialVersionUID = 0;

		File folder;

		HashSet blackList = new HashSet();

		public FilePanel(File folder) {
			this.folder = folder;
			setBackground(new Color(0xFFFFFFFF));
			setLayout(null);
			new DropTarget(this, this);
			addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent evt) {
					resetSelection();
				}
			});
		}

		void resetSelection() {
			Component[] components = getComponents();
			for (int i = 0; i < components.length; i++) {
				if (components[i] instanceof FileIcon) {
					FileIcon fi = (FileIcon) components[i];
					fi.clearHighlight();
				}
			}
		}

		public void paint(Graphics g) {
			super.paint(g);
			if (getComponentCount() == 0) {
				String label;
				if (this == docPane)
					label = "Drop documents here";
				else
					label = "Drop resources here";

				Dimension d = getSize();
				int size = 20;
				Font f = new Font("Serif", Font.PLAIN, size);
				g.setFont(f);
				FontMetrics fm = g.getFontMetrics();
				char[] arr = label.toCharArray();
				int width = fm.charsWidth(arr, 0, arr.length);
				int fs = (2 * size * d.width) / (3 * width);
				f = new Font("Serif", Font.PLAIN, fs);
				g.setFont(f);
				int x = (d.width - (fs * width) / size) / 2;
				int y = (d.height + (2 * fs) / 3) / 2;
				g.setColor(new Color(0xCCCCCC));
				g.drawString(label, x, y);
			}
		}

		public void add(FileIcon component) {
			if (getComponentCount() == 0)
				repaint();
			super.add(component);
		}

		private boolean isEPub(File f) {
			return f.getName().toLowerCase().endsWith(".epub");
		}

		private boolean isErrorLog(File f) {
			String nm = f.getName();
			return nm.startsWith("error") && nm.endsWith(".txt");
		}

		private boolean canUse(File f) {
			String nm = f.getName().toLowerCase();
			return nm.endsWith(".css") || nm.endsWith(".otf") || nm.endsWith(".ttf") || nm.endsWith(".ttc");
		}

		private BufferedImage getResIcon(String nm) {
			if (nm.endsWith(".ttf") || nm.endsWith(".ttc"))
				return ttfIcon;
			if (nm.endsWith(".otf"))
				return otfIcon;
			if (nm.endsWith(".css"))
				return cssIcon;
			return null;
		}

		public void dragEnter(DropTargetDragEvent dtde) {
			if (!dragActive) {
				dragActive = true;
				if (localDrag != null) {
					for (int i = 0; i < localDrag.length; i++) {
						localDrag[i].repaint();
					}
				}
			}
			dragOver(dtde);
		}

		public void dragExit(DropTargetEvent dte) {
			if (dragActive) {
				dragActive = false;
				if (localDrag != null) {
					for (int i = 0; i < localDrag.length; i++) {
						localDrag[i].repaint();
					}
				}
			}
		}

		private List getFilesFromTransferable(Transferable t) throws Exception {
			if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
				return (List) t.getTransferData(DataFlavor.javaFileListFlavor);
			String uris = (String) t.getTransferData(urilist);
			StringTokenizer tok = new StringTokenizer(uris);
			Vector files = new Vector();
			while (tok.hasMoreTokens()) {
				String uri = tok.nextToken();
				try {
					URL url = new URL(uri);
					if (url.getProtocol().equals("file")) {
						files.add(new File(url.getFile()));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return files;
		}

		public void dragOver(DropTargetDragEvent dtde) {
			if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor) && !dtde.isDataFlavorSupported(urilist)) {
				// DataFlavor[] list = dtde.getCurrentDataFlavors();
				// for( int i = 0 ; i < list.length ; i++ )
				// System.out.println("" + list[i]);
				return;
			}
			if (localDrag != null) {
				for (int i = 0; i < localDrag.length; i++) {
					localDrag[i].setDragLocation(dtde.getLocation());
				}
				dtde.acceptDrag(DnDConstants.ACTION_MOVE);
				return;
			}
			Transferable t = dtde.getTransferable();
			try {
				List files = getFilesFromTransferable(t);
				if (files == null) {
					// well, we cannot really make much of it, just accept
					dtde.acceptDrag(DnDConstants.ACTION_COPY);
					return;
				}
				Iterator f = files.iterator();
				boolean canUse = false;
				while (f.hasNext()) {
					File file = ((File) f.next());
					Iterator it = ConversionService.registeredSerivces();
					while (it.hasNext()) {
						ConversionService service = (ConversionService) it.next();
						if (service.canConvert(file)) {
							dtde.acceptDrag(DnDConstants.ACTION_COPY);
							tabbedPane.setSelectedIndex(0); // doc pane
							return;
						}
						if (canUse(file) || service.canUse(file)) {
							canUse = true;
						}
					}
				}
				if (canUse) {
					dtde.acceptDrag(DnDConstants.ACTION_COPY);
					tabbedPane.setSelectedIndex(1); // resource pane
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			dtde.rejectDrag();
		}

		void nextLocation(FilePanel panel, Point loc, FileIcon icon) {
			loc.x += icon.getWidth() + 5;
			if (loc.x + icon.getWidth() > panel.getWidth()) {
				loc.x = 5;
				loc.y += icon.getHeight() + 5;
			}
		}

		private void copyResourceFile(File file, Image fileIcon, Point loc) {
			byte[] buffer = new byte[4096];
			try {
				// for now, copy it synchronously
				if (!resourceFolder.equals(file.getParentFile())) {
					InputStream in = new FileInputStream(file);
					File target = GUIDriver.makeFile(resourceFolder, file.getName());
					OutputStream out = new FileOutputStream(target);
					int len;
					while ((len = in.read(buffer)) > 0) {
						out.write(buffer, 0, len);
					}
					out.close();
					in.close();
					file = target;
				}
				FileIcon icon = new FileIcon(file, fileIcon, null, file.getName());
				resourcePane.add(icon);
				icon.setLocation(loc);
				nextLocation(resourcePane, loc, icon);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		boolean addFile(File file, Point loc, boolean anyType) {
			boolean canAddResources = anyType || this == resourcePane;
			boolean canAddDocs = anyType || this == docPane;
			if (canAddResources && canUse(file)) {
				String nm = file.getName().toLowerCase();
				BufferedImage fileIcon = getResIcon(nm);
				if (fileIcon != null) {
					copyResourceFile(file, fileIcon, loc);
					return true;
				}
			}
			Iterator it = ConversionService.registeredSerivces();
			while (it.hasNext()) {
				ConversionService service = (ConversionService) it.next();
				if (canAddDocs && service.canConvert(file)) {
					Image image = service.getIcon(file);
					FileIcon icon = new FileIcon(file, image, service, file.getName());
					docPane.add(icon);
					icon.setLocation(loc);
					nextLocation(docPane, loc, icon);
					scheduleConversion(icon);
					return true;
				}
				if (canAddResources && service.canUse(file)) {
					Image image = service.getIcon(file);
					if (image != null) {
						copyResourceFile(file, image, loc);
						return true;
					}
				}
			}
			return false;
		}

		public void drop(DropTargetDropEvent dtde) {
			dragActive = false;
			if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor) && !dtde.isDataFlavorSupported(urilist)) {
				dtde.rejectDrop();
				return;
			}
			dtde.acceptDrop(DnDConstants.ACTION_LINK);
			Transferable t = dtde.getTransferable();
			if (dtde.isLocalTransfer()) {
				localDrop(dtde);
				dtde.dropComplete(true);
				return;
			}
			try {
				Point loc = dtde.getLocation();
				List files = getFilesFromTransferable(t);
				Iterator f = files.iterator();
				boolean success = false;
				while (f.hasNext()) {
					File file = (File) f.next();
					if (addFile(file, loc, true))
						success = true;
				}
				dtde.dropComplete(success);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void startDrag(FileIcon src, DragGestureEvent dge) {
			InputEvent trigger = dge.getTriggerEvent();
			Point dragStart = null;
			if (trigger instanceof MouseEvent) {
				dragStart = ((MouseEvent) trigger).getPoint();
				Point pos = src.getLocation();
				GUIDriver.this.dragStart = new Point(dragStart.x + pos.x, dragStart.y + pos.y);
			}
			Vector files = new Vector();
			Vector dragIcons = new Vector();
			Component[] components = getComponents();
			for (int i = 0; i < components.length; i++) {
				if (components[i] instanceof FileIcon) {
					FileIcon fi = (FileIcon) components[i];
					if (fi.highlighted) {
						files.add(fi.file);
						FileIcon di = fi.makeDragIcon();
						dragIcons.add(di);
						add(di);
						setComponentZOrder(di, 0);
					}
				}
			}
			Transferable transferable = new FileTransferable(files);
			if (DragSource.isDragImageSupported()) {
				BufferedImage dragImage = new BufferedImage(src.getWidth(), src.getHeight(),
						BufferedImage.TYPE_INT_ARGB);
				Graphics g = dragImage.getGraphics();
				src.paint(g);
				g.dispose();
				Point imageOffset = new Point(-dragStart.x, -dragStart.y);
				dge.startDrag(null, dragImage, imageOffset, transferable, this);
			} else {
				dge.startDrag(null, transferable, this);
			}
			localDrag = new FileIcon[dragIcons.size()];
			dragIcons.copyInto(localDrag);
		}

		public void dropActionChanged(DropTargetDragEvent dtde) {
			// int action = dtde.getDropAction();
			// System.err.println("Drop action: " + action);
		}

		public void localDrop(DropTargetDropEvent dtde) {
			Point dragEnd = dtde.getLocation();
			int dx = dragEnd.x - dragStart.x;
			int dy = dragEnd.y - dragStart.y;
			Component[] components = getComponents();
			for (int i = 0; i < components.length; i++) {
				if (components[i] instanceof FileIcon) {
					FileIcon fi = (FileIcon) components[i];
					if (fi.highlighted) {
						Point loc = fi.getLocation();
						loc.x += dx;
						loc.y += dy;
						fi.setLocation(loc);
					}
				}
			}

		}

		public void dragDropEnd(DragSourceDropEvent dsde) {
			for (int i = 0; i < localDrag.length; i++) {
				remove(localDrag[i]);
			}
			localDrag = null;
			// int action = dsde.getDropAction();
			// System.err.println("Drop end: " + action);

			// we cannot really trust dsde.getDropSuccess() and action
			// check if any of our files got removed
			checkFiles();
		}

		public void checkFiles() {
			Component[] components = getComponents();
			boolean repaint = false;
			HashSet names = new HashSet();
			int maxy = 0;
			for (int i = 0; i < components.length; i++) {
				if (components[i] instanceof FileIcon) {
					FileIcon fi = (FileIcon) components[i];
					if (fi.file == null)
						return;
					if (!fi.file.exists()) {
						// file was moved
						remove(fi);
						repaint = true;
					} else if (fi.file.getParentFile().equals(folder)) {
						names.add(fi.file.getName());
						int y = fi.getY() + fi.getHeight();
						if (y > maxy)
							maxy = y;
					}
				}
			}
			Point location = new Point(5, maxy + 5);
			String[] list = folder.list();
			if (list != null) {
				for (int i = 0; i < list.length; i++) {
					String name = list[i];
					if (names.contains(name))
						continue;
					File newFile = new File(folder, name);
					if (blackList.contains(newFile)) {
						if (newFile.delete())
							blackList.remove(newFile);
					} else if (this == docPane && (isErrorLog(newFile) || isEPub(newFile))) {
						Image img = isErrorLog(newFile) ? errIcon : epubIcon;
						FileIcon icon = new FileIcon(newFile, img, null, name);
						add(icon);
						repaint = true;
						icon.setLocation(location);
						nextLocation(this, location, icon);
					} else {
						addFile(newFile, location, false);
					}
				}
			}
			if (repaint) {
				validate();
				repaint();
			}
		}

		public void dragEnter(DragSourceDragEvent dsde) {
			// System.err.println("Drag entered");
		}

		public void dragExit(DragSourceEvent dse) {
			// System.err.println("Drag exited");
		}

		public void dragOver(DragSourceDragEvent dsde) {
			// System.err.println("Drag over");
		}

		public void dropActionChanged(DragSourceDragEvent dsde) {
			// int action = dsde.getDropAction();
			// System.err.println("Drop source action: " + action);
		}

		public Dimension getPreferredSize() {
			Component[] components = getComponents();
			int width = 100;
			int height = 100;
			if (components != null)
				for (int i = 0; i < components.length; i++) {
					Component c = components[i];
					int w = c.getX() + c.getWidth();
					int h = c.getY() + c.getHeight();
					if (w > width)
						width = w;
					if (h > height)
						height = h;
				}
			return new Dimension(width, height);
		}

	}

	class FileIcon extends JComponent implements DragGestureListener {

		public static final long serialVersionUID = 0;

		File file;

		Image icon;

		Image highlightedIcon;

		ConversionService service;

		boolean highlighted;

		boolean dragIcon;

		String name;

		Point initial;

		String log;

		FileIcon(File file, Image icon, ConversionService service, String name) {
			this.file = file;
			this.icon = icon;
			this.service = service;
			this.name = name;
			this.dragIcon = file == null;
			setOpaque(false);
			setSize(64, 88);
			if (!FileIcon.this.dragIcon) {
				addMouseListener(new MouseAdapter() {
					public void mousePressed(MouseEvent evt) {
						if (!highlighted && !evt.isControlDown())
							((FilePanel) getParent()).resetSelection();
						highlighted = true;
						repaint();
					}

					public void mouseReleased(MouseEvent evt) {
						if (highlighted && !evt.isControlDown()) {
							((FilePanel) getParent()).resetSelection();
							highlighted = true;
						}
					}
				});
				int actions = DnDConstants.ACTION_COPY_OR_MOVE;
				dragSource.createDefaultDragGestureRecognizer(this, actions, this);
			}
		}

		void setLog(String log) {
			this.log = log;
		}

		void clearHighlight() {
			highlighted = false;
			repaint();
		}

		FileIcon makeDragIcon() {
			FileIcon fi = new FileIcon(null, icon, null, name);
			fi.highlighted = true;
			fi.initial = getLocation();
			fi.setLocation(fi.initial);
			return fi;
		}

		void setDragLocation(Point dragNow) {
			if (dragStart != null) {
				int newX = initial.x + dragNow.x - dragStart.x;
				int newY = initial.y + dragNow.y - dragStart.y;
				setLocation(newX, newY);
			}
		}

		public void paint(Graphics g) {
			Graphics baseG = g;
			BufferedImage group = null;
			if (dragIcon) {
				if (!dragActive)
					return;
				group = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
				g = group.getGraphics();
			}
			Graphics2D g2d = ((Graphics2D) g);
			int width = getWidth();
			int height = 50;
			if (this == currentlyConverting) {
				// show being converted
				Random r = new Random();
				int[] pix = new int[50 * 50];
				for (int i = 0; i < 50; i++) {
					int xd;
					if (i < 11)
						xd = 11 - i;
					else if (i > 38)
						xd = i - 38;
					else
						xd = 0;
					for (int j = 0; j < 50; j++) {
						int yd;
						if (j < 11)
							yd = 11 - j;
						else if (j > 38)
							yd = j - 38;
						else
							yd = 0;
						float d;
						if (xd == 0 && yd == 0)
							d = 11;
						else
							d = 11 - (float) Math.sqrt(xd * xd + yd * yd);
						float p = 20 * r.nextFloat();
						if (d > p) {
							p = 11 * r.nextFloat();
							if (p > d)
								pix[i * 50 + j] = 0xFFFFFF00;
							else
								pix[i * 50 + j] = 0xFFFF0000;
						} else
							pix[i * 50 + j] = 0;
					}
				}
				Image img = createImage(new MemoryImageSource(50, 50, pix, 0, 50));
				g.drawImage(img, 7, 0, null);
			}
			if (icon != null) {
				int iwidth = icon.getWidth(this);
				int iheight = icon.getHeight(this);
				int x = (width - iwidth) / 2;
				int y = (height - iheight) / 2;
				if (highlighted) {
					if (highlightedIcon == null) {
						ImageFilter filter = new HighlightedFilter();
						ImageProducer producer = new FilteredImageSource(icon.getSource(), filter);
						highlightedIcon = createImage(producer);
					}
					g.drawImage(highlightedIcon, x, y, this);
				} else
					g.drawImage(icon, x, y, this);
			}
			int[] lineBreaks = new int[3];
			int[] offx = new int[lineBreaks.length];
			int lbi = 0;
			Font font = new Font("SansSerif", Font.PLAIN, 11);
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics();
			int em = fm.charWidth('m');
			int emlen = width / em;
			char[] nameChars = name.toCharArray();
			int chi = 0;
			while (chi < nameChars.length && lbi < lineBreaks.length) {
				int len = emlen;
				if (chi + len > nameChars.length)
					len = nameChars.length - chi;
				int chw = fm.charsWidth(nameChars, chi, len);
				if (chw > width) {
					while (len > 1) {
						len--;
						chw = fm.charsWidth(nameChars, chi, len);
						if (chw <= width)
							break;
					}
				} else {
					while (chi + len < nameChars.length) {
						len++;
						int chw1 = fm.charsWidth(nameChars, chi, len);
						if (chw1 > width) {
							len--;
							break;
						}
						chw = chw1;
					}
				}
				chi += len;
				offx[lbi] = (width - chw) / 2;
				lineBreaks[lbi++] = chi;
			}
			chi = 0;
			if (highlighted) {
				// g.setColor(new Color(0x4070C0));
				g2d.setPaint(new Color(0, 0.2f, 0.8f, 0.75f));
				Rectangle rect = new Rectangle(0, getHeight() - lineBreaks.length * fm.getHeight(), width, lbi
						* fm.getHeight());
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
				g2d.fill(rect);
				g.setColor(Color.white);
			}
			for (int line = 0; line < lbi; line++) {
				int chi1 = lineBreaks[line];
				int ty = getHeight() - fm.getDescent() - (lineBreaks.length - 1 - line) * fm.getHeight();
				g.drawString(name.substring(chi, chi1), offx[line], ty);
				chi = chi1;
			}
			if (group != null) {
				g.dispose();
				AlphaFilter filter = new AlphaFilter();
				filter.setLevel(0.5f);
				ImageProducer producer = new FilteredImageSource(group.getSource(), filter);
				Image filtered = createImage(producer);
				baseG.drawImage(filtered, 0, 0, null);
			}
		}

		public void dragGestureRecognized(DragGestureEvent dge) {
			((FilePanel) getParent()).startDrag(this, dge);
		}

		void changeFile(File file, Image icon) {
			FilePanel fp = ((FilePanel) getParent());
			File folder = fp.folder;
			if (folder.equals(this.file.getParentFile())) {
				if (!this.file.delete()) {
					fp.blackList.add(this.file);
				}
			}
			service = null;
			this.file = file;
			this.icon = icon;
			this.name = file.getName();
			highlightedIcon = null;
			repaint();
		}
	}

	class FileTransferable implements Transferable {

		Vector list;

		FileTransferable(Vector list) {
			this.list = list;
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (flavor.equals(DataFlavor.javaFileListFlavor))
				return list;
			if (flavor.equals(urilist)) {
				StringBuffer sb = new StringBuffer();
				Iterator it = list.iterator();
				while (it.hasNext()) {
					File f = (File) it.next();
					if (sb.length() > 0)
						sb.append(' ');
					sb.append(f.toURI());
				}
				return sb.toString();
			}
			throw new UnsupportedFlavorException(flavor);
		}

		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] flavors = { DataFlavor.javaFileListFlavor, urilist };
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor.equals(DataFlavor.javaFileListFlavor) || flavor.equals(urilist);
		}

	}

	class Converter extends Thread implements ConversionClient {
		Converter() {
			super("Converter");
			setDaemon(true);
		}

		public void run() {
			while (true) {
				FileIcon item;
				synchronized (conversionQueue) {
					while (conversionQueue.size() == 0) {
						try {
							conversionQueue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					item = (FileIcon) conversionQueue.get(0);
					conversionQueue.removeElementAt(0);
					currentlyConverting = item;
				}
				final Vector resources = new Vector();
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							Component[] components = resourcePane.getComponents();
							for (int i = 0; i < components.length; i++) {
								if (components[i] instanceof FileIcon) {
									FileIcon fi = (FileIcon) components[i];
									if (fi.file != null) {
										resources.add(fi.file);
									}
								}
							}
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				File[] reslist = new File[resources.size()];
				resources.copyInto(reslist);
				reportProgress(0);
				item.service.setProperties(settings);
				final FileIcon srcItem = item;
				StringWriter log = new StringWriter();
				PrintWriter plog = new PrintWriter(log);
				plog.println("Conversion log for " + item.file.getAbsolutePath());
				plog.println("Start: " + new Date());
				final File res = item.service.convert(item.file, reslist, this, plog);
				plog.println("End: " + new Date());
				final String logTxt = log.toString();
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							if (res == null) {
								File dest = GUIDriver.makeFile(docFolder, "error.txt");
								try {
									Writer out = new OutputStreamWriter(new FileOutputStream(dest), "UTF-8");
									out.write(logTxt);
									out.close();
								} catch (Exception e) {
									e.printStackTrace();
								}
								srcItem.changeFile(dest, errIcon);
							} else {
								File dest = GUIDriver.makeFile(docFolder, res.getName());
								if (res.renameTo(dest))
									srcItem.changeFile(dest, epubIcon);
								else
									srcItem.changeFile(res, epubIcon);
							}
							srcItem.setLog(logTxt);
							currentlyConverting = null;
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				reportProgress(1);
			}
		}

		public void reportIssue(String errorCode) {
		}

		public void reportProgress(float progress) {
		}

		public File makeFile(String baseName) {
			return GUIDriver.makeFile(workFolder, baseName);
		}

	}

	public GUIDriver(File epubgenHome) {
		super("EPUBGen - Buttonless Converter");

		if (epubgenHome == null || !epubgenHome.isDirectory()) {
			File home = new File(System.getProperty("user.home"));
			epubgenHome = new File(home, "EPUBGen");
		}

		docFolder = new File(epubgenHome, "Documents");
		docFolder.mkdirs();
		resourceFolder = new File(epubgenHome, "Resources");
		resourceFolder.mkdirs();
		File settingsFolder = new File(epubgenHome, "Settings");
		settingsFolder.mkdirs();
		settingsFile = new File(settingsFolder, "settings.prop");
		workFolder = new File(epubgenHome, "Work");
		workFolder.mkdirs();

		if (settingsFile.exists()) {
			try {
				settings.load(new FileInputStream(settingsFile));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		docPane = new FilePanel(docFolder);
		resourcePane = new FilePanel(resourceFolder);
		settingsPane = new SettingsPanel();
		tabbedPane = new JTabbedPane();
		try {
			helpPane = new JEditorPane(GUIDriver.class.getResource("help.html"));
			helpPane.setEditable(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(tabbedPane);
		tabbedPane.add("Documents", new JScrollPane(docPane));
		tabbedPane.add("Resources", new JScrollPane(resourcePane));
		tabbedPane.add("Settings", new JScrollPane(settingsPane));
		if (helpPane != null)
			tabbedPane.add("Help", new JScrollPane(helpPane));
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		setSize(350, 250);

		try {
			InputStream png = GUIDriver.class.getResourceAsStream("epub.png");
			epubIcon = ImageIO.read(png);
			png = GUIDriver.class.getResourceAsStream("css.png");
			cssIcon = ImageIO.read(png);
			png = GUIDriver.class.getResourceAsStream("otf.png");
			otfIcon = ImageIO.read(png);
			png = GUIDriver.class.getResourceAsStream("ttf.png");
			ttfIcon = ImageIO.read(png);
			png = GUIDriver.class.getResourceAsStream("err.png");
			errIcon = ImageIO.read(png);
		} catch (IOException e) {
			e.printStackTrace();
		}

		(new Converter()).start();
		new Updater();
		new FileCheck();
	}

	public static File makeFile(File folder, String baseName) {
		File file = new File(folder, baseName);
		if (file.exists()) {
			String baseStr;
			String extStr;
			int ext = baseName.indexOf('.');
			if (ext < 0) {
				baseStr = baseName;
				extStr = "";
			} else {
				baseStr = baseName.substring(0, ext);
				extStr = baseName.substring(ext);
			}
			int count = 1;
			while (true) {
				file = new File(folder, baseStr + "-" + count + extStr);
				if (!file.exists())
					break;
				count++;
			}
		}
		return file;
	}

	void scheduleConversion(FileIcon file) {
		synchronized (conversionQueue) {
			conversionQueue.add(file);
			conversionQueue.notify();
		}
	}

	public static void main(String[] args) {
		File home = null;
		if (args.length == 1) {
			home = new File(args[0]);
			if (!home.isDirectory())
				System.err.println(args[0] + ": not a folder");
		}
		GUIDriver conv = new GUIDriver(home);
		conv.setVisible(true);
	}

}
