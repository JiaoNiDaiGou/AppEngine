package jiaoni.daigou.service.appengine.impls.wx;

import com.google.protobuf.ByteString;
import jiaoni.daigou.contentparser.Conf;
import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.lib.wx.WxReply;
import jiaoni.daigou.lib.wx.WxWebClient;
import jiaoni.daigou.service.appengine.impls.customer.CustomerFacade;
import jiaoni.daigou.service.appengine.impls.parser.ParserFacade;
import jiaoni.daigou.wiremodel.api.ParseRequest;
import jiaoni.daigou.wiremodel.api.ParseResponse;
import jiaoni.daigou.wiremodel.api.ParsedObject;
import jiaoni.daigou.wiremodel.entity.Customer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WxCustomerParseMessageHandler implements WxMessageHandler {
    private final ParserFacade parserFacade;
    private final CustomerFacade customerFacade;
    private final WxWebClient wxClient;

    @Inject
    public WxCustomerParseMessageHandler(final ParserFacade parserFacade,
                                         final CustomerFacade customerFacade,
                                         final WxWebClient wxClient) {
        this.parserFacade = parserFacade;
        this.customerFacade = customerFacade;
        this.wxClient = wxClient;
    }

    private ParseRequest buildParseRequest(final Session session, final RichMessage message) {
        switch (message.type()) {
            case TEXT:
                return ParseRequest.newBuilder()
                        .addTexts(message.getTextContent())
                        .build();
            case IMAGE: {
                return ParseRequest.newBuilder()
                        .addDirectUploadImages(ParseRequest.DirectUploadImage.newBuilder()
                                .setBytes(ByteString.copyFrom(message.getImageBytes())))
                        .build();
            }
            default:
                return null;
        }
    }

    private void handleHighConfParsedObject(final Session session,
                                            final RichMessage message,
                                            final ParsedObject parsedObject) {
        switch (parsedObject.getContentCase()) {
            case CUSTOMER:
                handleHighConfParsedCustomer(session, message, parsedObject.getCustomer());
                break;
            default:
        }
    }

    private void handleHighConfParsedCustomer(final Session session,
                                              final RichMessage message,
                                              final Customer customer) {
        Customer existingCustomer = customerFacade.getCustomer(customer.getPhone(), customer.getName());
        if (existingCustomer == null) {
            String prompt = String.format("%s 我发现一个新客户! 姓名:%s, 电话:%s, 地址:%s %s %s, %s. 我已经把他记录了.",
                    message.getFromContact() != null ? "@" + message.getFromContact().getNickName() : "",
                    customer.getName(),
                    customer.getPhone().getPhone(),
                    customer.getAddresses(0).getRegion(),
                    customer.getAddresses(0).getCity(),
                    customer.getAddresses(0).getZone(),
                    customer.getAddresses(0).getAddress());
            customerFacade.putCustomer(customer);
            wxClient.sendReply(session, WxReply.builder().text(message.getOriginalMessage().getFromUserName(), prompt).build());
        }
    }

    @Override
    public boolean handle(Session session, RichMessage message) {
        ParseRequest request = buildParseRequest(session, message);
        ParseResponse response = parserFacade.parse(request);

        // Only consider top 1 high conf
        if (response.getResultsCount() > 0) {
            ParsedObject parsedObject = response.getResults(0);
            if (parsedObject.getConfidence() > Conf.HIGH) {
                handleHighConfParsedObject(session, message, parsedObject);
                return true;
            }
        }

        return false;
    }
}
