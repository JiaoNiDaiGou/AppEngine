package jiaoni.daigou.service.appengine.interfaces;

import jiaoni.common.appengine.access.ocr.OcrClient;
import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.Snippet;
import jiaoni.daigou.contentparser.Answer;
import jiaoni.daigou.contentparser.Answers;
import jiaoni.daigou.contentparser.CnAddressParser;
import jiaoni.daigou.contentparser.Conf;
import jiaoni.daigou.contentparser.Parser;
import jiaoni.daigou.wiremodel.api.ParseRequest;
import jiaoni.daigou.wiremodel.api.ParseResponse;
import jiaoni.daigou.wiremodel.api.ParsedObject;
import jiaoni.daigou.service.appengine.impls.DbEnhancedCustomerParser;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static jiaoni.common.appengine.utils.MediaUtils.toStoragePath;
import static jiaoni.common.utils.LocalMeter.meterOff;
import static jiaoni.common.utils.LocalMeter.meterOn;

@Path("/api/parse")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class ParserInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParserInterface.class);

    private static final double MIN_INPUT_SNIPPET_CONF = 0.9;
    private static final double MIN_INPUT_SNIPPET_LENGTH = 5;

    private final CnAddressParser addressParser;
    private final DbEnhancedCustomerParser customerParser;
    private final OcrClient ocrClient;

    @Inject
    public ParserInterface(final CnAddressParser addressParser,
                           final DbEnhancedCustomerParser customerParser,
                           final OcrClient ocrClient) {
        this.addressParser = addressParser;
        this.customerParser = customerParser;
        this.ocrClient = ocrClient;
    }

    @POST
    public Response parse(final ParseRequest parseRequest) {
        RequestValidator.validateNotNull(parseRequest, "parseRequest");

        ParseResponse response;
        switch (parseRequest.getDomain()) {
            case ADDRESS:
                response = parse(parseRequest, addressParser, ParsedObject.Builder::setAddress);
                break;
            case CUSTOMER:
                response = parse(parseRequest, customerParser, ParsedObject.Builder::setCustomer);
                break;
            case ALL:
            case UNRECOGNIZED:
            case PRODUCT:
            default:
                throw new BadRequestException();
        }
        return Response.ok(response).build();
    }

    private <T> ParseResponse parse(final ParseRequest request,
                                    final Parser<T> parser,
                                    final BiConsumer<ParsedObject.Builder, T> resultSetter) {
        meterOn();

        LOGGER.info("request {}", ObjectMapperProvider.prettyToJson(request));

        List<Snippet> snippets = extractFromRequest(request);

        List<Answer<T>> answers = new ArrayList<>();
        for (Snippet snippet : snippets) {
            parser.parse(snippet.getText())
                    .stream()
                    .filter(t -> t.getConfidence() > Conf.ZERO)
                    .forEach(answers::add);
        }

        // Form an Answers to sort
        Answers<T> addressAnswers = Answers.of(answers);

        ParseResponse toReturn = ParseResponse
                .newBuilder()
                .addAllResults(addressAnswers
                        .stream()
                        .map(t -> {
                            ParsedObject.Builder builder = ParsedObject.newBuilder();
                            resultSetter.accept(builder, t.getTarget());
                            return builder.build();
                        })
                        .limit(request.getLimit() == 0 ? Integer.MAX_VALUE : request.getLimit())
                        .collect(Collectors.toList()))
                .build();

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
            String fullPath = toStoragePath(mediaId);
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
}
