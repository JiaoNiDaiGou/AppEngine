package jiaoni.daigou.service.appengine.impls.wx;

import com.google.protobuf.ByteString;
import jiaoni.daigou.contentparser.Conf;
import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.lib.wx.WxClient;
import jiaoni.daigou.lib.wx.WxReply;
import jiaoni.daigou.lib.wx.model.Message;
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
    private final WxClient wxClient;

    @Inject
    public WxCustomerParseMessageHandler(final ParserFacade parserFacade,
                                         final CustomerFacade customerFacade,
                                         final WxClient wxClient) {
        this.parserFacade = parserFacade;
        this.customerFacade = customerFacade;
        this.wxClient = wxClient;
    }

    private ParseRequest buildParseRequest(final Session session,
                                           final Message message) {
        switch (message.getMessageType()) {
            case TEXT:
                return ParseRequest.newBuilder()
                        .addTexts(message.getContent())
                        .build();
            case IMAGE: {
                byte[] bytes = wxClient.getMessageImage(session, message.getMsgId());
                return ParseRequest.newBuilder()
                        .addDirectUploadImages(ParseRequest.DirectUploadImage.newBuilder()
                                .setBytes(ByteString.copyFrom(bytes)))
                        .build();
            }
            default:
                return null;
        }
    }

    private void handleHighConfParsedObject(final Session session,
                                            final Message message,
                                            final ParsedObject parsedObject) {
        switch (parsedObject.getContentCase()) {
            case CUSTOMER:
                handleHighConfParsedCustomer(session, message, parsedObject.getCustomer());
                break;
            default:
        }
    }

    private void handleHighConfParsedCustomer(final Session session,
                                              final Message message,
                                              final Customer customer) {
        Customer existingCustomer = customerFacade.getCustomer(customer.getPhone(), customer.getName());
        if (existingCustomer == null) {
            String prompt = String.format("我发现一个新客户! 姓名:%s, 电话:%s, 地址:%s %s %s, %s. 我已经把他记录了.",
                    customer.getName(),
                    customer.getPhone().getPhone(),
                    customer.getAddresses(0).getRegion(),
                    customer.getAddresses(0).getCity(),
                    customer.getAddresses(0).getZone(),
                    customer.getAddresses(0).getAddress());
            customerFacade.putCustomer(customer);
            wxClient.sendReply(session, WxReply.builder().text(message.getFromUserName(), prompt).build());
        }
    }

    @Override
    public boolean handle(Session session, Message message) {
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
