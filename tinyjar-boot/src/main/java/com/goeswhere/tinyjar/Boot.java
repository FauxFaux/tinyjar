package com.goeswhere.tinyjar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;

public class Boot {
	public static void main(String[] args) throws Exception {
		final File tmp = File.createTempFile("data", ".jar");
		System.out.println("Unpacking to: " + tmp.getAbsolutePath() + "...");

		final FileOutputStream fos = new FileOutputStream(tmp);
		try {
			final JarOutputStream jos = new JarOutputStream(fos);
			try {
				final InputStream data = Boot.class.getResourceAsStream("/data");
				try {
					final LzmaInputStream in = new LzmaInputStream(data, new Decoder());
					try {
						Pack200.newUnpacker().unpack(in, jos);
					} finally {
						in.close();
					}
				} finally {
					data.close();
				}
			} finally {
				jos.flush();
				jos.close();
			}
		} finally {
			fos.flush();
			fos.close();
		}

		System.out.println("  done, loading...");

		final String mainClass;
		final JarFile jf = new JarFile(tmp);
		try {
			mainClass = String.valueOf(jf.getManifest().getMainAttributes().getValue("Main-Class"));
		} finally {
			jf.close();
		}

		callMain(args, tmp, mainClass);
	}

	static void callMain(String[] args, final File url, final String mainClass) throws Exception {
		final URLClassLoader cl = new URLClassLoader(new URL[] { url.toURI().toURL() }, Boot.class.getClassLoader()) {
			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				Thread.currentThread().setContextClassLoader(this);
				return super.loadClass(name, resolve);
			}
		};

		cl.loadClass(mainClass).getMethod("main", new Class[] { String[].class }).invoke(null, new Object[] { args });
	}
}
