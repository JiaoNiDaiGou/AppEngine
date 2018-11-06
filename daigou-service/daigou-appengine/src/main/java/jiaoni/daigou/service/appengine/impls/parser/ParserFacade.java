package jiaoni.daigou.service.appengine.impls.parser;

import jiaoni.common.appengine.access.ocr.OcrClient;
import jiaoni.common.appengine.utils.MediaUtils;
import jiaoni.common.model.Snippet;
import jiaoni.daigou.contentparser.Answer;
import jiaoni.daigou.contentparser.Answers;
import jiaoni.daigou.contentparser.CnAddressParser;
import jiaoni.daigou.contentparser.Conf;
import jiaoni.daigou.contentparser.Parser;
import jiaoni.daigou.service.appengine.impls.db.DbEnhancedCustomerParser;
import jiaoni.daigou.wiremodel.api.ParseRequest;
import jiaoni.daigou.wiremodel.api.ParseResponse;
import jiaoni.daigou.wiremodel.api.ParsedObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;

import static jiaoni.common.utils.LocalMeter.meterOff;
import static jiaoni.common.utils.LocalMeter.meterOn;

@Singleton
public class ParserFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParserFacade.class);

    private static final double MIN_INPUT_SNIPPET_CONF = 0.9;
    private static final double MIN_INPUT_SNIPPET_LENGTH = 5;

    private final CnAddressParser addressParser;
    private final DbEnhancedCustomerParser customerParser;
    private final OcrClient ocrClient;

    @Inject
    public ParserFacade(final CnAddressParser addressParser,
                        final DbEnhancedCustomerParser customerParser,
                        final OcrClient ocrClient) {
        this.addressParser = addressParser;
        this.customerParser = customerParser;
        this.ocrClient = ocrClient;
    }

    /**
     * Filter low confidence snippets.
     * - confidence < 0.9
     * - size < 5 chars
     */
    private static List<Snippet> filterLowConfidenceSnippets(final List<Snippet> snippets) {
        return snippets.stream()
                .filter(t -> t.getConfidence() >= MIN_INPUT_SNIPPET_CONF
                        && StringUtils.length(t.getText()) >= MIN_INPUT_SNIPPET_LENGTH)
                .collect(Collectors.toList());
    }

    public ParseResponse parse(final ParseRequest request) {
        List<Snippet> snippets = extractFromRequest(request);
        List<ParsedObject> toReturn;
        switch (request.getDomain()) {
            case ADDRESS:
                toReturn = parse(snippets, addressParser, ParsedObject.Builder::setAddress);
                break;
            case CUSTOMER:
                toReturn = parse(snippets, customerParser, ParsedObject.Builder::setCustomer);
                break;
            case PRODUCT:
            case ALL:
            case UNRECOGNIZED:
            default:
                throw new BadRequestException();
        }

        return ParseResponse.newBuilder()
                .addAllResults(toReturn)
                .build();
    }

    private <T> List<ParsedObject> parse(final List<Snippet> snippets,
                                         final Parser<T> parser,
                                         final BiConsumer<ParsedObject.Builder, T> resultSetter) {
        meterOn();

        List<Answer<T>> answers = new ArrayList<>();
        for (Snippet snippet : snippets) {
            parser.parse(snippet.getText())
                    .stream()
                    .filter(t -> t.getConfidence() > Conf.ZERO)
                    .forEach(answers::add);
        }

        // Sort the answers again.
        Answers<T> sortedAnswers = Answers.of(answers);
        List<ParsedObject> toReturn = sortedAnswers.stream()
                .map(t -> {
                    ParsedObject.Builder builder = ParsedObject.newBuilder();
                    resultSetter.accept(builder, t.getTarget());
                    return builder.build();
                })
                .collect(Collectors.toList());

        meterOff();
        return toReturn;
    }

    private List<Snippet> extractFromRequest(final ParseRequest request) {
        List<Snippet> toReturn = new ArrayList<>();
        if (request.getTextsCount() > 0) {
            toReturn.addAll(extractFromText(request.getTextsList()));
        }
        if (request.getMediaIdsCount() > 0) {
            toReturn.addAll(extractFromMedia(request.getMediaIdsList()));
        }
        if (request.getDirectUploadImagesCount() > 0) {
            toReturn.addAll(extractFromDirectUploadBytes(request.getDirectUploadImagesList()));
        }
        return toReturn;
    }

    private List<Snippet> extractFromText(final List<String> texts) {
        // Treat all input texts as single one.
        String text = String.join(" ", texts);
        return Collections.singletonList(new Snippet(text, 1d));
    }

    private List<Snippet> extractFromMedia(final List<String> mediaIds) {
        List<Snippet> toReturn = new ArrayList<>();
        for (String mediaId : mediaIds) {
            String fullPath = MediaUtils.toGcsPath(mediaId);
            List<Snippet> snippets = ocrClient.annotateFromMediaPath(fullPath);
            toReturn.addAll(filterLowConfidenceSnippets(snippets));
        }
        return toReturn;
    }

    private List<Snippet> extractFromDirectUploadBytes(final List<ParseRequest.DirectUploadImage> images) {
        List<Snippet> toReturn = new ArrayList<>();
        for (ParseRequest.DirectUploadImage image : images) {
            List<Snippet> snippets = ocrClient.annotateFromBytes(image.getBytes().toByteArray());
            toReturn.addAll(filterLowConfidenceSnippets(snippets));
        }
        return toReturn;
    }
}
