package com.goeswhere.tinyjar;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;

public class Boot {
	public static void main(String[] args) throws Exception {
		final File tmp = File.createTempFile("data", "jar");
		Pack200.newUnpacker().unpack(new LzmaInputStream(Boot.class.getResourceAsStream("data"), new Decoder()), new JarOutputStream(new FileOutputStream(tmp)));
		final URLClassLoader cl = new URLClassLoader(new URL[] { tmp.toURI().toURL() });
		final Class<?> cls = cl.loadClass(String.valueOf(new JarFile(tmp).getManifest().getMainAttributes().get("Main-Class")));
        cls.getMethod("main", new Class[]{String[].class}).invoke(null, new Object[]{args});
	}
}
