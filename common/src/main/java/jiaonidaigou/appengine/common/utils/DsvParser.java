package jiaonidaigou.appengine.common.utils;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.io.Resources;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Read DSV resources.
 */
public class DsvParser {
    private boolean parseHeaderOnly = false;
    private boolean skipHead = false;

    public DsvParser setParseHeaderOnly(boolean parseHeaderOnly) {
        this.parseHeaderOnly = parseHeaderOnly;
        this.skipHead = false;
        return this;
    }

    public DsvParser setSkipHead(boolean skipHead) {
        checkArgument(!parseHeaderOnly);
        this.skipHead = skipHead;
        return this;
    }

    public <T> List<T> parseFile(final File file,
                                 final Function<CSVRecord, T> transform)
            throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return parse(file.getAbsolutePath(), inputStream, transform);
        }
    }

    public <T> List<T> parseResource(final String resourceName,
                                     final Function<CSVRecord, T> transform)
            throws IOException {
        return parseResource(Resources.getResource(resourceName), transform);
    }

    public <T> List<T> parseResource(final URL resource,
                                     final Function<CSVRecord, T> transform)
            throws IOException {
        try (InputStream inputStream = resource.openStream()) {
            return parse(resource.getPath(), inputStream, transform);
        }
    }

    private <T> List<T> parse(final String path,
                              final InputStream inputStream,
                              final Function<CSVRecord, T> transfrom)
            throws IOException {
        List<T> toReturn = new ArrayList<>();

        CSVFormat csvFormat;
        if (skipHead) {
            csvFormat = CSVFormat.DEFAULT
                    .withSkipHeaderRecord(skipHead)
                    .withDelimiter(inferDelimiter(path));
        } else {
            csvFormat = CSVFormat.DEFAULT
                    .withHeader()
                    .withDelimiter(inferDelimiter(path));
        }
        CSVParser csvParser = csvFormat.parse(new InputStreamReader(inputStream, Charsets.UTF_8));

        for (CSVRecord record : csvParser.getRecords()) {
            if (record.isConsistent()) {
                T item = transfrom.apply(record);
                if (item != null) {
                    toReturn.add(item);
                }
            } else {
                throw new IllegalStateException(String.format("CSV file %s line is not consistent with header. Record is %s", path, record));
            }
            if (parseHeaderOnly) {
                break;
            }
        }
        return toReturn;
    }

    private static char inferDelimiter(final String fileName) {
        if (fileName.endsWith(".tsv")) {
            return '\t';
        } else if (fileName.endsWith(".csv")) {
            return ',';
        }
        throw new UnsupportedOperationException("Cannot parse file " + fileName);
    }
}

