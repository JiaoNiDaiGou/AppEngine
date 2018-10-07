package jiaonidaigou.appengine.api.guice;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class HK2toGuiceModuleTest {
    @Test
    public void testFindInterfaceClasses() {
        List<Class> classes = HK2toGuiceModule.findInterfaceClasses();
        assertTrue(CollectionUtils.isNotEmpty(classes));
    }
}
