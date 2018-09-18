package jiaonidaigou.appengine.api.interfaces;

import com.google.common.base.Enums;
import jiaonidaigou.appengine.api.access.storage.StorageClient;
import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.contentparser.Answers;
import jiaonidaigou.appengine.contentparser.CnAddressParser;
import jiaonidaigou.appengine.contentparser.CnCustomerContactParser;
import jiaonidaigou.appengine.contentparser.Conf;
import jiaonidaigou.appengine.lib.ocrspace.OcrSpaceClient;
import jiaonidaigou.appengine.lib.ocrspace.model.ParseRequest.FileType;
import jiaonidaigou.appengine.lib.ocrspace.model.ParseRequest.Language;
import jiaonidaigou.appengine.wiremodel.api.ParseRequest;
import jiaonidaigou.appengine.wiremodel.api.ParseResponse;
import jiaonidaigou.appengine.wiremodel.api.ParsedObject;
import jiaonidaigou.appengine.wiremodel.entity.Address;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static jiaonidaigou.appengine.api.utils.MediaUtils.toStoragePath;
import static jiaonidaigou.appengine.common.utils.LocalMeter.meterOff;
import static jiaonidaigou.appengine.common.utils.LocalMeter.meterOn;

@Path("/api/parse")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class ParserInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParserInterface.class);

    private final CnAddressParser addressParser;
    private final CnCustomerContactParser customerContactParser;
    private final OcrSpaceClient ocrSpaceClient;
    private final StorageClient storageClient;

    @Inject
    public ParserInterface(final CnAddressParser addressParser,
                           final CnCustomerContactParser customerContactParser,
                           final OcrSpaceClient ocrSpaceClient,
                           final StorageClient storageClient) {
        this.addressParser = addressParser;
        this.customerContactParser = customerContactParser;
        this.ocrSpaceClient = ocrSpaceClient;
        this.storageClient = storageClient;
    }

    @POST
    public Response parse(final ParseRequest parseRequest) {
        RequestValidator.validateNotNull(parseRequest, "parseRequest");

        ParseResponse response;
        switch (parseRequest.getDomain()) {
            case ADDRESS:
                response = parseAddress(parseRequest);
                break;
            case CUSTOMER:
                response = parseCustomerContact(parseRequest);
                break;
            case ALL:
            case UNRECOGNIZED:
            case PRODUCT:
            default:
                throw new BadRequestException();
        }
        return Response.ok(response).build();
    }

    private ParseResponse parseAddress(final ParseRequest request) {
        meterOn();

        final String text = extractRequestText(request);

        Answers<Address> addressAnswers = addressParser.parse(text);

        ParseResponse toReturn = ParseResponse
                .newBuilder()
                .addAllResults(addressAnswers
                        .stream()
                        .filter(t -> t.getConfidence() > Conf.ZERO)
                        .map(t -> ParsedObject.newBuilder().setAddress(t.getTarget()).build())
                        .limit(request.getLimit() == 0 ? Integer.MAX_VALUE : request.getLimit())
                        .collect(Collectors.toList()))
                .build();

        meterOff();
        return toReturn;
    }

    private ParseResponse parseCustomerContact(final ParseRequest request) {
        meterOn();
        final String text = extractRequestText(request);

        Answers<Customer> addressAnswers = customerContactParser.parse(text);

        ParseResponse toReturn = ParseResponse
                .newBuilder()
                .addAllResults(addressAnswers
                        .stream()
                        .filter(t -> t.getConfidence() > Conf.ZERO)
                        .map(t -> ParsedObject.newBuilder().setCustomer(t.getTarget()).build())
                        .limit(request.getLimit() == 0 ? Integer.MAX_VALUE : request.getLimit())
                        .collect(Collectors.toList()))
                .build();

        meterOff();
        return toReturn;
    }

    public String extractRequestText(final ParseRequest request) {
        StringBuilder sb = new StringBuilder();

        for (String text : request.getTextsList()) {
            sb.append(text).append(" ");
        }

        for (String mediaId : request.getMediaIdsList()) {
            String extension = StringUtils.substringAfterLast(mediaId, ".");
            FileType fileType = Enums.getIfPresent(FileType.class, StringUtils.upperCase(extension)).orNull();
            RequestValidator.validateNotNull(fileType, extension + " is not supported.");

            // Download URL somehow is not working for ocrspace.
            // We have to download the bytes here.

            byte[] bytes = storageClient.read(toStoragePath(mediaId));

            jiaonidaigou.appengine.lib.ocrspace.model.ParseRequest ocrParseRequest =
                    jiaonidaigou.appengine.lib.ocrspace.model.ParseRequest.builder()
                            .withImageBytes(bytes)
                            .withFileType(fileType)
                            .withLanguage(Language.CHINESE_SIMPLIFIED)
                            .build();
            jiaonidaigou.appengine.lib.ocrspace.model.ParseResponse ocrParseResponse = ocrSpaceClient
                    .parse(ocrParseRequest);

            LOGGER.info("Parse OCR: {}", ocrParseResponse);
            sb.append(ocrParseResponse.getAllParsedText()).append(" ");
        }

        for (ParseRequest.DirectUploadImage image : request.getDirectUploadImagesList()) {
            String extension = image.getExt();
            FileType fileType = Enums.getIfPresent(FileType.class, StringUtils.upperCase(extension)).orNull();
            RequestValidator.validateNotNull(fileType, extension + " is not supported.");

            jiaonidaigou.appengine.lib.ocrspace.model.ParseRequest ocrParseRequest =
                    jiaonidaigou.appengine.lib.ocrspace.model.ParseRequest.builder()
                            .withImageBytes(image.getBytes().toByteArray())
                            .withFileType(fileType)
                            .withLanguage(Language.CHINESE_SIMPLIFIED)
                            .build();
            jiaonidaigou.appengine.lib.ocrspace.model.ParseResponse ocrParseResponse = ocrSpaceClient
                    .parse(ocrParseRequest);

            LOGGER.info("Parse OCR: {}", ocrParseResponse);
            sb.append(ocrParseResponse.getAllParsedText()).append(" ");
        }
        return sb.toString();
    }
}
