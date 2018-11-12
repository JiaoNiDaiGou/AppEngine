package jiaoni.common.location;

import jiaoni.common.wiremodel.City;
import jiaoni.common.wiremodel.Region;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CnLocationsTest {
    @Test
    public void testGetAllRegions() {
        List<Region> regions = CnLocations.getInstance().allRegions();
        assertTrue(regions.size() > 0);
    }

    @Test
    public void testGetAllCities() {
        List<City> cities = CnLocations.getInstance().allCities();
        assertTrue(cities.size() > 0);
    }

    @Test
    public void testBeijingZones() {
        List<Pair<Region, City>> results = CnLocations.getInstance().searchCity("北京");
        assertEquals(1, results.size());
        Region region = results.get(0).getKey();
        City city = results.get(0).getRight();
        assertEquals("北京市", region.getName());
        assertEquals("北京市", city.getName());
        assertEquals(16, city.getZonesCount());
    }

    @Test
    public void testXuChangPostalCode() {
        City city = CnLocations.getInstance().searchCity("许昌").get(0).getRight();
        System.out.println(city.getPostalCodesList());
    }
}
