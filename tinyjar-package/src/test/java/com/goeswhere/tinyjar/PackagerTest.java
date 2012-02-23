package com.goeswhere.tinyjar;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class PackagerTest {
	@Test
	public void testNames() {
		assertEquals(new File("foo-tiny.jar"), Packager.newNameFor(new File("foo.jar")));
		assertEquals(new File("bar/foo-tiny.jar"), Packager.newNameFor(new File("bar/foo.jar")));
		assertEquals(new File("bar/foo-tiny.jar"), Packager.newNameFor(new File("bar/foo")));
	}
}
