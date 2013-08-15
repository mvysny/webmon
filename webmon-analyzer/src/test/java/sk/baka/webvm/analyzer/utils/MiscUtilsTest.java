package sk.baka.webvm.analyzer.utils;

import java.io.File;
import static org.junit.Assert.*;
import org.junit.Test;
/**
 *
 * @author Martin Vysny
 */
public class MiscUtilsTest {
    @Test
    public void testToLocalFile() {
        assertEquals(MiscUtils.toLocalFile("/foo.bar"), null);
        assertEquals(MiscUtils.toLocalFile("foo.bar"), new File("foo.bar"));
        assertEquals(MiscUtils.toLocalFile("file:///foo.bar"), new File("/foo.bar"));
        assertEquals(MiscUtils.toLocalFile("file://C:/foo.bar"), new File("C:/foo.bar"));
        // this is okay as this works on Windows
        assertEquals(MiscUtils.toLocalFile("file:///C:/foo.bar"), new File("/C:/foo.bar"));
        assertEquals(MiscUtils.toLocalFile("file:C:/foo.bar"), new File("C:/foo.bar"));
        assertEquals(MiscUtils.toLocalFile("http://foo/bar/baz"), null);
        assertEquals(MiscUtils.toLocalFile("ftp://foo/bar/baz"), null);
        assertEquals(MiscUtils.toLocalFile("file:///foo/%20/baz"), new File("/foo/ /baz"));
        assertEquals(MiscUtils.toLocalFile("file:///foo/+/baz"), new File("/foo/ /baz"));
        assertEquals(MiscUtils.toLocalFile("C:/whatever"), null);
    }
}
