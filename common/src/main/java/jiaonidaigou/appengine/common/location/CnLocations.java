package jiaonidaigou.appengine.common.location;

import com.google.common.collect.ImmutableList;
import jiaonidaigou.appengine.common.model.InternalIOException;
import jiaonidaigou.appengine.common.utils.DsvParser;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class CnLocations {
    public static final List<String> ZONE_FLAGS = ImmutableList.of("县", "区", "市", "旗", "州");

    private final DsvParser dsvParser = new DsvParser();

    private final List<CnRegion> regions;
    private final List<CnCity> cities;

    private CnLocations() {
        try {
            regions = loadRegions();
            List<CnZone> zones = loadZones();
            cities = loadCities(regions, zones);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    public static CnLocations getInstance() {
        return LazyHolder.INSTANCE;
    }

    private List<CnRegion> loadRegions()
            throws IOException {
        return dsvParser.parseResource("location/regions.csv", t -> {
            String name = t.get("name");
            List<String> alias = Arrays.asList(StringUtils.split(t.get("alias"), ","));
            return CnRegion.builder()
                    .withName(name)
                    .withAlias(alias)
                    .build();
        });
    }

    private List<CnZone> loadZones()
            throws IOException {
        return dsvParser.parseResource("location/zones.csv", t -> {
            String region = t.get("region");
            String city = t.get("city");
            String[] zones = StringUtils.split(t.get("zones"), "|");
            List<CnZone> toReturn = new ArrayList<>();

            for (String zone : zones) {
                List<String> alias = new ArrayList<>();
                for (String flag : ZONE_FLAGS) {
                    if (zone.endsWith(flag)) {
                        alias.add(zone.substring(0, zone.length() - flag.length()));
                        break;
                    }
                }

                toReturn.add(CnZone.builder()
                        .withRegionName(region)
                        .withCityName(city)
                        .withName(zone)
                        .withAlias(alias)
                        .build());
            }
            return toReturn;
        }).stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    private List<CnCity> loadCities(final List<CnRegion> regions, final List<CnZone> zones)
            throws IOException {
        Map<String, CnRegion> regionsByName = regions
                .stream()
                .collect(Collectors.toMap(
                        CnRegion::getName,
                        t -> t
                ));
        List<CnCity> toReturn = dsvParser.parseResource("location/cities.csv", t -> {
            String name = t.get("name");
            Set<String> alias = new HashSet<>();
            String[] aliasPart = StringUtils.split(t.get("alias"), ",");
            if (ArrayUtils.isNotEmpty(aliasPart)) {
                alias.addAll(Arrays.asList(aliasPart));
            }
            int flag = name.indexOf("市");
            if (flag > 0) {
                alias.add(name.substring(0, flag) + name.substring(flag + 1, name.length()));
            }
            CnRegion region = checkNotNull(regionsByName.get(t.get("region")));
            boolean municipality = name.equals(region.getName());

            List<CnZone> itsZones = zones.stream()
                    .filter(z -> z.getRegionName().equals(region.getName()) && z.getCityName().equals(name))
                    .collect(Collectors.toList());

            return CnCity.builder()
                    .withName(name)
                    .withRegion(region)
                    .withZones(itsZones)
                    .withAlias(new ArrayList<>(alias))
                    .withMunicipality(municipality)
                    .build();
        });
        toReturn.sort((a, b) -> Integer.compare(a.getName().length(), b.getName().length()) * -1);
        return toReturn;
    }

    public List<CnRegion> allRegions() {
        return regions;
    }

    public List<CnCity> allCities() {
        return cities;
    }

    public List<CnCity> allMunicipalityCities() {
        return cities.stream().filter(CnCity::isMunicipality).collect(Collectors.toList());
    }

    public List<CnCity> allNonMunicipalityCities() {
        return cities.stream().filter(t -> !t.isMunicipality()).collect(Collectors.toList());
    }

    public List<CnRegion> searchRegion(final String text) {
        return regions.stream().filter(t -> t.getAllPossibleNames().contains(text)).collect(Collectors.toList());
    }

    public List<CnCity> searchCity(final String text) {
        return cities.stream().filter(t -> t.getAllPossibleNames().contains(text)).collect(Collectors.toList());
    }

    public List<CnCity> searchCity(final CnRegion region, final String text) {
        return cities.stream()
                .filter(t -> t.getRegion().equals(region))
                .filter(t -> t.getAllPossibleNames().contains(text))
                .collect(Collectors.toList());
    }

    private static class LazyHolder {
        private static final CnLocations INSTANCE = new CnLocations();
    }
}
