package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.contentparser.Answers;
import jiaonidaigou.appengine.contentparser.CnAddressParser;
import jiaonidaigou.appengine.contentparser.CnCustomerContactParser;
import jiaonidaigou.appengine.contentparser.Conf;
import jiaonidaigou.appengine.wiremodel.api.ParseRequest;
import jiaonidaigou.appengine.wiremodel.api.ParseResponse;
import jiaonidaigou.appengine.wiremodel.api.ParsedObject;
import jiaonidaigou.appengine.wiremodel.entity.Address;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;

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
    private final CnAddressParser addressParser;
    private final CnCustomerContactParser customerContactParser;

    @Inject
    public ParserInterface(final CnAddressParser addressParser,
                           final CnCustomerContactParser customerContactParser) {
        this.addressParser = addressParser;
        this.customerContactParser = customerContactParser;
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

        final String text = concatRequestText(request);

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
        final String text = concatRequestText(request);

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

    private static String concatRequestText(final ParseRequest request) {
        return String.join(" ",
                request.getTextsList()
                        .stream()
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList()));
    }
}
