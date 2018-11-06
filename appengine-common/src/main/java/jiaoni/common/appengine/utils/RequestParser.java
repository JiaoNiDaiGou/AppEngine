package jiaoni.common.appengine.utils;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;

public class RequestParser {
    /**
     * Parse a string to a list of enum.
     * If any parsing failed, throw BadRequest.
     */
    public static <E extends Enum<E>> List<E> parseEnumsList(final Class<E> enumClass, final String str) {
        if (StringUtils.isBlank(str)) {
            return ImmutableList.of();
        }
        String[] parts = StringUtils.split(str, ",");
        try {
            return Arrays.stream(parts).map(t -> Enum.valueOf(enumClass, t)).collect(Collectors.toList());
        } catch (Exception e) {
            throw new BadRequestException("invalid enum list for " + enumClass.getSimpleName() + ":" + str);
        }
    }
}
