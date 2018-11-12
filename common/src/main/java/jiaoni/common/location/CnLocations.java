package jiaoni.common.location;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.InternalIOException;
import jiaoni.common.wiremodel.City;
import jiaoni.common.wiremodel.Region;
import jiaoni.common.wiremodel.Regions;
import jiaoni.common.wiremodel.Zone;
import org.apache.commons.lang3.tuple.Pair;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CnLocations {
    public static final List<String> ZONE_FLAGS = ImmutableList.of("县", "区", "市", "旗", "州");

    private Regions regions;

    private CnLocations() {
        try (InputStream inputStream = Resources.getResource("location/locations.json").openStream()) {
            regions = ObjectMapperProvider.get().readValue(inputStream, Regions.class);
        } catch (Exception e) {
            throw new InternalIOException(e);
        }
    }

    public static CnLocations getInstance() {
        return LazyHolder.INSTANCE;
    }

    public Regions getRegions() {
        return regions;
    }

    public List<Region> allRegions() {
        return regions.getRegionsList();
    }

    public List<City> allCities() {
        return regions.getRegionsList()
                .stream()
                .map(Region::getCitiesList)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<City> allNonMunicipalityCities() {
        return regions.getRegionsList()
                .stream()
                .map(Region::getCitiesList)
                .flatMap(Collection::stream)
                .filter(t -> !t.getMunicipality())
                .collect(Collectors.toList());
    }

    public List<Region> searchRegion(final String text) {
        return regions.getRegionsList()
                .stream()
                .filter(t -> t.getName().equals(text) || t.getAliasList().contains(text))
                .collect(Collectors.toList());
    }

    public Region getRegionForCity(final City city) {
        return regions.getRegionsList().stream().filter(t -> t.getName().equals(city.getRegionName())).findFirst().orElse(null);
    }

    public List<Pair<Region, City>> searchCity(final String text) {
        List<Pair<Region, City>> toReturn = new ArrayList<>();
        for (Region region : regions.getRegionsList()) {
            for (City city : region.getCitiesList()) {
                if (city.getName().equals(text) || city.getAliasList().contains(text)) {
                    toReturn.add(Pair.of(region, city));
                }
            }
        }
        return toReturn;
    }

    public List<City> searchCity(final Region region, final String text) {
        return region.getCitiesList()
                .stream()
                .filter(t -> t.getName().equals(text) || t.getAliasList().contains(text))
                .collect(Collectors.toList());
    }

    public boolean matchAnyName(final Region region, String name) {
        return region.getName().equals(name) || region.getAliasList().contains(name);
    }

    public boolean matchAnyName(final City city, String name) {
        return city.getName().equals(name) || city.getAliasList().contains(name);
    }

    public boolean matchAnyName(final Zone zone, String name) {
        return zone.getName().equals(name) || zone.getAliasList().contains(name);
    }

    public List<String> allPossibleNames(final Region region) {
        return ImmutableList.<String>builder().add(region.getName()).addAll(region.getAliasList()).build();
    }

    public List<String> allPossibleNames(final City city) {
        return ImmutableList.<String>builder().add(city.getName()).addAll(city.getAliasList()).build();
    }

    public List<String> allPossibleNames(final Zone zone) {
        return ImmutableList.<String>builder().add(zone.getName()).addAll(zone.getAliasList()).build();
    }

    private static class LazyHolder {
        private static final CnLocations INSTANCE = new CnLocations();
    }
}
