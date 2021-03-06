package test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.osgi.resource.Capability;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.OSInformation;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.lib.strings.Strings;
import junit.framework.TestCase;

public class ProcessorTest extends TestCase {

	public void testFixupMerge() throws IOException {
		Processor p = new Processor();
		p.setProperty("-fixupmessages.foo", "foo");
		p.setProperty("-fixupmessages.bar", "bar");
		p.error("foo");
		p.error("bar");
		assertTrue(p.check());
		p.close();
	}

	public void testFixupMacro() throws IOException {
		Processor p = new Processor();
		p.setProperty("skip", "foo");
		p.setProperty("-fixupmessages", "${skip},bar");
		p.error("foo");
		p.error("bar");
		assertTrue(p.check());
		p.close();
	}

	public void testNative() throws Exception {
		assertNative("osname=linux;osversion=2.3;processor=arm_le", "(osgi.native.osname~=LINUX)");
		assertNative("osname=Windows;osversion=10.0;processor=x86", "(osgi.native.osname~=Win32)");
		assertNative("osname=Windows;osversion=10.0;processor=x86", "(osgi.native.processor~=pentium)");
		assertNative("osname=Windows;osversion=10.0;processor=x86-64", "(osgi.native.processor~=amd64)");
		assertNative("osname=Linux;osversion=5.1.0;processor=arm", "(osgi.native.processor~=arm)",
				"The 'arm' processor is deprecated");

	}

	public void testNativeDefaults() throws Exception {
		Processor p = new Processor();
		p.setProperty("a", "${native_capability}");

		//
		// Mac OS
		//

		assertNativeDefault("Mac OS X", "10.8.2", "x86_64",
				"(&(osgi.native.osname~=MacOSX)(osgi.native.osname~=Mac OS X))");
		assertNativeDefault("Mac OS X", "10.8.2", "x86_64", "(osgi.native.osversion=0010.8.2)");
		assertNativeDefault("Mac OS X", "10.8.2", "x86_64",
				"(&(osgi.native.processor=x86-64)(osgi.native.processor=amd64)(osgi.native.processor=em64t)(osgi.native.processor=x86_64))");

		//
		// Linux
		//

		assertNativeDefault("Linux", "3.8.8-202.fc18.x86_64", "amd64",
				"(&(osgi.native.osname~=linux)(osgi.native.processor=*)(osgi.native.osversion=3.8.8.-202_fc18_x86_64))");

		assertNativeDefault("Linux", "3.8.8-202.fc18.x86_64", "em64t",
				"(&(osgi.native.osname~=linux)(osgi.native.processor=em64t)(osgi.native.osversion=3.8.8.-202_fc18_x86_64))");

		//
		// Windows
		//

		assertNativeDefault("Windows XP", "5.1.7601.17514", "x86",
				"(&(osgi.native.osname~=WindowsXP)(osgi.native.osname~=WinXP)(osgi.native.osname~=Windows XP)(osgi.native.osname~=Win32))");

		assertNativeDefault("Windows XP", "5.1.7601.17514", "x86",
				"(&(osgi.native.processor~=x86)(osgi.native.processor~=pentium)(osgi.native.processor~=i386)(osgi.native.processor~=i486)(osgi.native.processor~=i686)(osgi.native.processor~=i586))");

		assertNativeDefault("Windows XP", "5.1.7601.17514", "x86", "(&(osgi.native.osversion=5.1.0))");

		assertNativeDefault("Windows Vista", "6.0.7601.17514", "x86",
				"(&(osgi.native.osname~=WindowsVista)(osgi.native.osname~=WinVista)(osgi.native.osname~=Windows Vista)(osgi.native.osname~=Win32))");

		assertNativeDefault("Windows 7", "6.1.7601.17514", "x86",
				"(&(osgi.native.osname~=Windows7)(osgi.native.osname~=Windows 7)(osgi.native.osname~=Win32)(osgi.native.osversion=6.1.0))");

		assertNativeDefault("Windows 8", "6.2.7601.17514", "x86",
				"(&(osgi.native.osname~=Windows8)(osgi.native.osname~=Windows 8)(osgi.native.osname~=Win32)(osgi.native.osversion=6.2.0))");
	}

	public void testOperatingSystems() {
		
		assertIn(OSInformation.getOperatingSystemAliases("Windows XP", "5.1.x").osnames, "WindowsXP", "Windows XP",
				"WinXP", "Win32");
		assertIn(OSInformation.getOperatingSystemAliases("Windows Vista", "6.0.x").osnames, "WindowsVista",
				"Windows Vista", "WinVista", "Win32");
		assertIn(OSInformation.getOperatingSystemAliases("Solaris", "3.8").osnames, "Solaris");
		assertIn(OSInformation.getOperatingSystemAliases("AIX", "3.8").osnames, "AIX");
		assertIn(OSInformation.getOperatingSystemAliases("HP-UX", "3.8").osnames, "HPUX", "hp-ux");

	}

	private void assertIn(String osnames, String... members) {
		List<String> split = Strings.split(osnames);
		for (String member : members) {
			if (!split.contains(member))
				fail(member + " is not a member of " + split);
		}
	}

	public void testUnknownProcessor() throws Exception {
		try (Processor p = new Processor();) {
			assertNative("osname=linux;osversion=2.3;processor=FOO;processor=BLA",
					"(&(osgi.native.processor~=FOO)(osgi.native.processor~=BLA))");
		}

	}

	public void testUnknownOsname() throws Exception {
		try (Processor p = new Processor();) {
			assertNative("osname=Beos;osversion=2.3;processor=FOO;processor=BLA", "(&(osgi.native.osname~=beos))");
		}
	}

	public void testNoOsVersion() throws Exception {
		try (Processor p = new Processor();) {
			String cap = p._native_capability("native_capability", "processor=x86", "osname=Linux");
			System.out.println(cap);
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		}

	}


	private void assertNativeDefault(String osname, String osversion, String processor, String filter)
			throws Exception {
		String origOsName = System.getProperty("os.name");
		String origOsVersion = System.getProperty("os.version");
		String origOsArch = System.getProperty("os.arch");
		String processed;
		try {
			System.setProperty("os.name", osname);
			System.setProperty("os.version", osversion);
			System.setProperty("os.arch", processor);
			assertNative(null, filter);

		} finally {
			System.setProperty("os.name", origOsName);
			System.setProperty("os.version", origOsVersion);
			System.setProperty("os.arch", origOsArch);
		}
	}

	private void assertNative(String in, String filter, String... fixup) throws Exception {
		List<String> split = in == null ? new ArrayList<String>() : Strings.split("\\s*;\\s*", in);
		split.add(0, "native_capability");
		try (Processor p = new Processor();) {
			String s = p._native_capability(split.toArray(new String[0]));
			assertNotNull(s);
			System.out.println(s);

			ResourceBuilder rb = new ResourceBuilder();
			List<Capability> capabilities = rb.addProvideCapabilities(s);

			RequirementBuilder qb = new RequirementBuilder("osgi.native");
			qb.addDirective("filter", filter);
			List<Capability> found = ResourceUtils.findProviders(qb.synthetic(), capabilities);
			assertTrue(!found.isEmpty());

			assertTrue(p.check(fixup));
		}
	}

	public static void testPlugins() {

	}

	public void testFixupMessages() throws IOException {
		Processor p = new Processor();
		p.setTrace(true);

		p.error("abc");
		assertFalse(p.isOk());

		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;restrict:=warning");
		assertEquals(1, p.getErrors().size());
		assertEquals(0, p.getWarnings().size());

		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc");
		assertEquals(0, p.getErrors().size());
		assertEquals(0, p.getWarnings().size());

		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;is:=error");
		assertEquals(1, p.getErrors().size());
		assertEquals(0, p.getWarnings().size());

		p.clear();
		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;is:=warning");
		assertEquals(0, p.getErrors().size());
		assertEquals(1, p.getWarnings().size());

		p.clear();
		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;replace:=def");
		assertEquals("def", p.getErrors().get(0));
		assertEquals(0, p.getWarnings().size());

		p.clear();
		p.setProperty(Constants.FIXUPMESSAGES, "'abc def\\s*ghi';is:=warning");
		p.error("abc def  \t\t   ghi");
		assertEquals(0, p.getErrors().size());
		assertEquals(1, p.getWarnings().size());

		p.error("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;replace:=def;is:=warning");
		assertEquals("def", p.getWarnings().get(0));
		assertEquals(0, p.getErrors().size());

		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;restrict:=error");
		assertEquals(0, p.getErrors().size());
		assertEquals(1, p.getWarnings().size());

		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc");
		assertEquals(0, p.getErrors().size());
		assertEquals(0, p.getWarnings().size());

		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;is:=warning");
		assertEquals(0, p.getErrors().size());
		assertEquals(1, p.getWarnings().size());

		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;is:=error");
		assertEquals(1, p.getErrors().size());
		assertEquals(0, p.getWarnings().size());

		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;replace:=def");
		assertEquals("def", p.getWarnings().get(0));
		assertEquals(0, p.getErrors().size());

		p.clear();
		p.warning("abc");
		p.setProperty(Constants.FIXUPMESSAGES, "abc;replace:=def;is:=error");
		assertEquals("def", p.getErrors().get(0));
		assertEquals(0, p.getWarnings().size());
		p.close();
	}

	public static void testDuplicates() {
		assertEquals("", Processor.removeDuplicateMarker("~"));

		assertTrue(Processor.isDuplicate("abc~"));
		assertTrue(Processor.isDuplicate("abc~~~~~~~~~"));
		assertTrue(Processor.isDuplicate("~"));
		assertFalse(Processor.isDuplicate(""));
		assertFalse(Processor.isDuplicate("abc"));
		assertFalse(Processor.isDuplicate("ab~c"));
		assertFalse(Processor.isDuplicate("~abc"));

		assertEquals("abc", Processor.removeDuplicateMarker("abc~"));
		assertEquals("abc", Processor.removeDuplicateMarker("abc~~~~~~~"));
		assertEquals("abc", Processor.removeDuplicateMarker("abc"));
		assertEquals("ab~c", Processor.removeDuplicateMarker("ab~c"));
		assertEquals("~abc", Processor.removeDuplicateMarker("~abc"));
		assertEquals("", Processor.removeDuplicateMarker(""));
		assertEquals("", Processor.removeDuplicateMarker("~~~~~~~~~~~~~~"));
	}

	public static void appendPathTest() throws Exception {
		assertEquals("a/b/c", Processor.appendPath("", "a/b/c/"));
		assertEquals("a/b/c", Processor.appendPath("", "/a/b/c"));
		assertEquals("a/b/c", Processor.appendPath("/", "/a/b/c/"));
		assertEquals("a/b/c", Processor.appendPath("a", "b/c/"));
		assertEquals("a/b/c", Processor.appendPath("a", "b", "c"));
		assertEquals("a/b/c", Processor.appendPath("a", "b", "/c/"));
		assertEquals("a/b/c", Processor.appendPath("/", "a", "b", "/c/"));
		assertEquals("a/b/c", Processor.appendPath("////////", "////a////b///c//"));

	}

	public void testUriMacro() throws Exception {
		try (Processor p = new Processor()) {
			String baseURI = p.getBaseURI().toString();
			String otherURI = new URI("file:/some/dir/").toString();
			p.setProperty("uri1", "${uri;dist/bundles}");
			p.setProperty("uri2", "${uri;/dist/bundles}");
			p.setProperty("uri3", "${uri;file:dist/bundles}");
			p.setProperty("uri4", "${uri;file:/dist/bundles}");
			p.setProperty("uri5", "${uri;dist/bundles;" + otherURI + "}");
			p.setProperty("uri6", "${uri;/dist/bundles;" + otherURI + "}");
			p.setProperty("uri7", "${uri;file:dist/bundles;" + otherURI + "}");
			p.setProperty("uri8", "${uri;file:/dist/bundles;" + otherURI + "}");
			p.setProperty("uri9", "${uri;http://foo.com/dist/bundles}");
			p.setProperty("uri10", "${uri;http://foo.com/dist/bundles;" + otherURI + "}");
			p.setProperty("uri11", "${uri;.}");
			String uri1 = p.getProperty("uri1");
			String uri2 = p.getProperty("uri2");
			String uri3 = p.getProperty("uri3");
			String uri4 = p.getProperty("uri4");
			String uri5 = p.getProperty("uri5");
			String uri6 = p.getProperty("uri6");
			String uri7 = p.getProperty("uri7");
			String uri8 = p.getProperty("uri8");
			String uri9 = p.getProperty("uri9");
			String uri10 = p.getProperty("uri10");
			String uri11 = p.getProperty("uri11");
			assertEquals(baseURI + "dist/bundles", uri1);
			assertEquals("file:/dist/bundles", uri2);
			assertEquals(baseURI + "dist/bundles", uri3);
			assertEquals("file:/dist/bundles", uri4);
			assertEquals(otherURI + "dist/bundles", uri5);
			assertEquals("file:/dist/bundles", uri6);
			assertEquals(otherURI + "dist/bundles", uri7);
			assertEquals("file:/dist/bundles", uri8);
			assertEquals("http://foo.com/dist/bundles", uri9);
			assertEquals("http://foo.com/dist/bundles", uri10);
			assertEquals(baseURI, uri11);
			assertTrue(p.check());
		}
	}

	public void testUriMacroTooFew() throws IOException {
		try (Processor p = new Processor()) {
			p.setProperty("urix", "${uri}");
			String uri = p.getProperty("urix");
			assertTrue(p.check("too few arguments", "No translation found for macro: uri"));
		}
	}

	public void testUriMacroTooMany() throws IOException {
		try (Processor p = new Processor()) {
			p.setProperty("urix", "${uri;file:/dist/bundles;file:/some/dir/;another}");
			String uri = p.getProperty("urix");
			assertTrue(p.check("too many arguments", "No translation found for macro: uri"));
		}
	}

	public void testUriMacroNoBase() throws IOException {
		try (Processor p = new Processor()) {
			p.setBase(null);
			p.setProperty("urix", "${uri;dist/bundles}");
			String uri = p.getProperty("urix");
			assertTrue(p.check("No base dir set", "No translation found for macro: uri"));
		}
	}

	public void testFileUriMacro() throws Exception {
		try (Processor p = new Processor()) {
			String baseURI = p.getBaseURI().toString();
			File some = new File("generated");
			p.setProperty("uri1", "${fileuri;dist/bundles}");
			p.setProperty("uri2", "${fileuri;" + some.getCanonicalPath() + "/dist/bundles}");
			p.setProperty("uri3", "${fileuri;.}");
			String uri1 = p.getProperty("uri1");
			String uri2 = p.getProperty("uri2");
			String uri3 = p.getProperty("uri3");
			assertEquals(baseURI + "dist/bundles", uri1);
			assertEquals(some.toURI() + "dist/bundles", uri2);
			assertEquals(baseURI, uri3);
			assertTrue(p.check());
		}
	}

}
