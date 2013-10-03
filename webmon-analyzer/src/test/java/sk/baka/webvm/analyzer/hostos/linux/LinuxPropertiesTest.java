package sk.baka.webvm.analyzer.hostos.linux;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Martin Vysny
 */
public class LinuxPropertiesTest {

    @Test(expected = IllegalArgumentException.class)
    public void testGetValueInBytesFailsWhenPropertyDoesNotExist() {
        Proc.LinuxProperties.EMPTY.getValueInBytes("blabla");
    }

    @Test
    public void testGetValueInBytesZeroReturnsZeroWhenPropertyDoesNotExist() {
        assertEquals(0L, Proc.LinuxProperties.EMPTY.getValueInBytesZero("blabla"));
        assertNull(Proc.LinuxProperties.EMPTY.getValueInBytesNull("blabla"));
    }

    @Test
    public void parseKbValue() {
        assertEquals(1024L, new Proc.LinuxProperties(Collections.singletonMap("bla", "1 Kb")).getValueInBytes("bla"));
        assertEquals(20 * 1024L, new Proc.LinuxProperties(Collections.singletonMap("bla", "20 Kb")).getValueInBytesZero("bla"));
        assertEquals((Long) (30L * 1024), new Proc.LinuxProperties(Collections.singletonMap("bla", "30 Kb")).getValueInBytesNull("bla"));
    }
}
