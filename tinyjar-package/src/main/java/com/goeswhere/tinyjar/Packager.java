package com.goeswhere.tinyjar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;
import java.util.zip.ZipEntry;

import lzma.streams.LzmaOutputStream;

public class Packager {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length != 1 || (args.length != 0 && (args[0].equals("--help") || args[0].equals("-h")))) {
			System.err.println("usage: jarfile");
			System.exit(1);
			return;
		}

		final File inFile = new File(args[0]);
		final Manifest mf = new Manifest();
		{
			final Attributes mainAttributes = mf.getMainAttributes();
			mainAttributes.putValue("Manifest-Version", "1.0");
			mainAttributes.putValue("Main-Class", "com.goeswhere.tinyjar.Boot");
		}
		final JarOutputStream jos = new JarOutputStream(new FileOutputStream(newNameFor(inFile).getAbsolutePath()), mf);
		try {
			final InputStream boot = Packager.class.getResourceAsStream("/tinyjar-boot.jar");
			final JarInputStream jis = new JarInputStream(boot);
			ZipEntry en;
			while (null != (en = jis.getNextEntry())) {
				jos.putNextEntry(en);
				copy(jis, jos);
			}

			jos.putNextEntry(new ZipEntry("data"));

			final FileInputStream fis = new FileInputStream(inFile);
			try {
				final JarInputStream jarIn = new JarInputStream(fis);
				try {
					final LzmaOutputStream lzma = new LzmaOutputStream.Builder(jos).build();
					try {
						final Packer packer = Pack200.newPacker();
						packer.addPropertyChangeListener(new PropertyChangeListener() {
							public void propertyChange(PropertyChangeEvent evt) {
								if (!Packer.PROGRESS.equals(evt.getPropertyName()))
									return;
								final String val = String.valueOf(evt.getNewValue());
								System.err.printf("jar200: %3s%% complete..\n", val);

								if ("100".equals(val))
									System.err.println("Awaiting lzma completion...");
							}
						});
						packer.pack(jarIn, lzma);
					} finally {
						lzma.flush();
						lzma.close();
					}
				} finally {
					jarIn.close();
				}
			} finally {
				fis.close();
			}
		} finally {
			jos.flush();
			jos.close();
		}

		System.out.println("Done.");
	}

	static File newNameFor(File inFile) {
		final String name = inFile.getName();
		final int dot = name.lastIndexOf('.');
		final String parent = inFile.getParent();
		if (-1 == dot)
			return new File(parent, name + "-tiny.jar");

		return new File(parent, name.substring(0, dot) + "-tiny" + name.substring(dot));
	}

	public static long copy(InputStream from, OutputStream to) throws IOException {
		byte[] buf = new byte[4096];
		long total = 0;
		while (true) {
			int r = from.read(buf);
			if (r == -1) {
				break;
			}
			to.write(buf, 0, r);
			total += r;
		}
		return total;
	}
}
