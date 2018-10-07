package jiaoni.common.utils;

import com.google.common.collect.Table;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CollectionUtils2 {
    public static <K> void incCnt(final Map<K, Long> map, final K key) {
        incCnt(map, key, 1L);
    }

    public static <K> void incCnt(final Map<K, Long> map, final K key, long increase) {
        if (map != null && key != null) {
            map.put(key, map.getOrDefault(key, 0L) + increase);
        }
    }

    public static <R, C> void incCnt(final Table<R, C, Long> table, final R row, final C col) {
        incCnt(table, row, col, 1L);
    }

    public static <R, C> void incCnt(final Table<R, C, Long> table, final R row, final C col, long increase) {
        if (table != null && row != null && col != null) {
            Long val = table.get(row, col);
            if (val == null) {
                val = 0L;
            }
            table.put(row, col, val + increase);
        }
    }

    public static <K> List<Map.Entry<K, Long>> topAsc(final Map<K, Long> map, final int limit) {
        return map.entrySet()
                .stream()
                .sorted(Comparator.comparingLong(Map.Entry::getValue))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public static <K> List<Map.Entry<K, Long>> topDesc(final Map<K, Long> map, final int limit) {
        return map.entrySet()
                .stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public static <R, C> List<Table.Cell<R, C, Long>> topAsc(final Table<R, C, Long> table, final int limit) {
        return table.cellSet()
                .stream()
                .sorted(Comparator.comparingLong(Table.Cell::getValue))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public static <R, C> List<Table.Cell<R, C, Long>> topDesc(final Table<R, C, Long> table, final int limit) {
        return table.cellSet()
                .stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public static <K> List<Map.Entry<K, Long>> rankAsc(final Map<K, Long> map) {
        return map.entrySet()
                .stream()
                .sorted(Comparator.comparingLong(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    public static <K> List<Map.Entry<K, Long>> rankDesc(final Map<K, Long> map) {
        return map.entrySet()
                .stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
    }

    public static <R, C> Map<R, List<Map.Entry<C, Long>>> rankByRowAsc(final Table<R, C, Long> table) {
        Map<R, List<Map.Entry<C, Long>>> toReturn = new HashMap<>();
        for (R r : table.rowKeySet()) {
            toReturn.put(r, rankAsc(table.row(r)));
        }
        return toReturn;
    }

    public static <R, C> Map<R, List<Map.Entry<C, Long>>> rankByRowDesc(final Table<R, C, Long> table) {
        Map<R, List<Map.Entry<C, Long>>> toReturn = new HashMap<>();
        for (R r : table.rowKeySet()) {
            toReturn.put(r, rankDesc(table.row(r)));
        }
        return toReturn;
    }

    public static <T> int indexOf(final List<T> list, final Predicate<T> predicate) {
        for (int i = 0; i < list.size(); i++) {
            if (predicate.test(list.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
