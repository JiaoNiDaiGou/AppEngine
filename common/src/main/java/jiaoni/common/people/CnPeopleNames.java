package jiaoni.common.people;

import jiaoni.common.utils.DsvParser;

import java.io.IOException;
import java.util.List;

public class CnPeopleNames {
    public static CnPeopleNames getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final CnPeopleNames INSTANCE = new CnPeopleNames();
    }

    private final List<String> lastNames;

    private CnPeopleNames() {
        try {
            lastNames = new DsvParser()
                    .setSkipHead(true)
                    .parseResource("people/lastnames.csv", t -> t.get(0));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> allLastNames() {
        return lastNames;
    }

    public boolean startsWithAnyKnownLastName(final String text) {
        return lastNames.stream().anyMatch(text::startsWith);
    }
}
