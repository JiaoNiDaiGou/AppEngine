package jiaonidaigou.appengine.common.location;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CnLocationsTest {
    @Test
    public void testGetAllRegions() {
        List<CnRegion> regions = CnLocations.getInstance().allRegions();
        assertTrue(regions.size() > 0);
    }

    @Test
    public void testGetAllCities() {
        List<CnCity> cities = CnLocations.getInstance().allCities();
        assertTrue(cities.size() > 0);
    }

    @Test
    public void testBeijingZones() {
        assertEquals(16, CnLocations.getInstance().searchCity("北京").get(0).getZones().size());
    }
}
