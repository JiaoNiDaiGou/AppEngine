package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.access.ocr.OcrClient;
import jiaonidaigou.appengine.api.access.storage.StorageClient;
import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.impls.DbEnhancedCustomerParser;
import jiaonidaigou.appengine.api.utils.MediaUtils;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.common.model.Snippet;
import jiaonidaigou.appengine.contentparser.Answer;
import jiaonidaigou.appengine.contentparser.Answers;
import jiaonidaigou.appengine.contentparser.CnAddressParser;
import jiaonidaigou.appengine.contentparser.Conf;
import jiaonidaigou.appengine.contentparser.Parser;
import jiaonidaigou.appengine.wiremodel.api.ParseRequest;
import jiaonidaigou.appengine.wiremodel.api.ParseResponse;
import jiaonidaigou.appengine.wiremodel.api.ParsedObject;
import jiaonidaigou.appengine.wiremodel.entity.MediaObject;
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

import static jiaonidaigou.appengine.common.utils.LocalMeter.meterOff;
import static jiaonidaigou.appengine.common.utils.LocalMeter.meterOn;

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
    private final StorageClient storageClient;

    @Inject
    public ParserInterface(final CnAddressParser addressParser,
                           final DbEnhancedCustomerParser customerParser,
                           final OcrClient ocrClient,
                           final StorageClient storageClient) {
        this.addressParser = addressParser;
        this.customerParser = customerParser;
        this.ocrClient = ocrClient;
        this.storageClient = storageClient;
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
            String fullPath = MediaUtils.toStoragePath(mediaId);
            MediaObject object = MediaObject.newBuilder()
                    .setId(mediaId)
                    .setFullPath(fullPath)
                    .build();
            List<Snippet> snippets = ocrClient.annotateFromMediaObject(object);

            // Let's filter them by confidence and size first.
            // We don't want to parse
            // - confidence < 0.9
            // - size < 5 chars
            snippets.stream()
                    .filter(t -> t.getConfidence() >= MIN_INPUT_SNIPPET_CONF
                            && StringUtils.length(t.getText()) >= MIN_INPUT_SNIPPET_LENGTH)
                    .forEach(toReturn::add);
        }
        return toReturn;
    }
}
