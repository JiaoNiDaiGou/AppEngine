package jiaoni.daigou.service.appengine.guice;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jiaoni.common.appengine.access.email.EmailClient;
import jiaoni.common.appengine.access.email.GaeEmailSender;
import jiaoni.common.appengine.access.gcp.GoogleClientFactory;
import jiaoni.common.appengine.access.ocr.GoogleVisionOcrClient;
import jiaoni.common.appengine.access.ocr.OcrClient;
import jiaoni.common.appengine.access.productsearch.GoogleProductSearchClient;
import jiaoni.common.appengine.access.productsearch.ProSearchClient;
import jiaoni.common.appengine.access.sms.ConsoleSmsClient;
import jiaoni.common.appengine.access.sms.SmsClient;
import jiaoni.common.appengine.access.storage.GcsClient;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.common.appengine.access.taskqueue.PubSubClient;
import jiaoni.common.appengine.access.taskqueue.TaskQueueClient;
import jiaoni.common.appengine.auth.WxSessionDbClient;
import jiaoni.common.appengine.guice.ENV;
import jiaoni.common.httpclient.BrowserClient;
import jiaoni.daigou.contentparser.CnAddressParser;
import jiaoni.daigou.contentparser.CnCellPhoneParser;
import jiaoni.daigou.contentparser.CnCustomerContactParser;
import jiaoni.daigou.contentparser.CnPeopleNameParser;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClient;
import jiaoni.daigou.lib.teddy.TeddyClientImpl;
import jiaoni.daigou.lib.wx.WxClient;
import jiaoni.daigou.lib.wx.WxClientImpl;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.service.appengine.impls.teddy.CallAwareTeddyClient;

import java.io.IOException;
import javax.inject.Named;
import javax.inject.Singleton;

public class ServiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bindConstant().annotatedWith(ENV.class).to(AppEnvs.getEnv());

        bind(StorageClient.class).to(GcsClient.class);
        bind(EmailClient.class).to(GaeEmailSender.class);
        bind(PubSubClient.class).to(TaskQueueClient.class);
        bind(OcrClient.class).to(GoogleVisionOcrClient.class);
        bind(SmsClient.class).to(ConsoleSmsClient.class);
        bind(ProSearchClient.class).toProvider(() -> new GoogleProductSearchClient(AppEnvs.getServiceName(), AppEnvs.getEnv()));

        bind(CnCustomerContactParser.class).toInstance(new CnCustomerContactParser());
        bind(CnAddressParser.class).toInstance(new CnAddressParser());
        bind(CnPeopleNameParser.class).toInstance(new CnPeopleNameParser());
        bind(CnCellPhoneParser.class).toInstance(new CnCellPhoneParser());
    }

    @Provides
    @Singleton
    WxSessionDbClient provideWxSessionDbClient(final DatastoreService datastoreService,
                                               final MemcacheService memcacheService) {
        return new WxSessionDbClient(
                AppEnvs.getServiceName(),
                AppEnvs.getEnv(),
                datastoreService,
                memcacheService);
    }

    @Provides
    @Singleton
    DatastoreService provideDatastoreService() {
        return DatastoreServiceFactory.getDatastoreService();
    }

    @Provides
    @Singleton
    AppIdentityService provideAppIdentityService() {
        return AppIdentityServiceFactory.getAppIdentityService();
    }

    @Provides
    @Singleton
    MemcacheService provideMemcacheService() {
        return MemcacheServiceFactory.getMemcacheService(AppEnvs.getServiceName() + "." + AppEnvs.getEnv());
    }

    @Provides
    @Singleton
    @Named(TeddyAdmins.JIAONI)
    TeddyClient provideTeddyClientJiaoni(final MemcacheService memcache) {
        return new CallAwareTeddyClient(new TeddyClientImpl(TeddyAdmins.JIAONI, new BrowserClient()), memcache);
    }

    @Provides
    @Singleton
    @Named(TeddyAdmins.HACK)
    TeddyClient provideTeddyClientHack(final MemcacheService memcache) {
        return new CallAwareTeddyClient(new TeddyClientImpl(TeddyAdmins.HACK, new BrowserClient()), memcache);
    }

    @Provides
    @Singleton
    @Named(TeddyAdmins.BY_ENV)
    TeddyClient provideTeddyClientByEnv(final MemcacheService memcache) {
        switch (AppEnvs.getEnv()) {
            case PROD:
                return new CallAwareTeddyClient(new TeddyClientImpl(TeddyAdmins.JIAONI, new BrowserClient()), memcache);
            default:
                return new CallAwareTeddyClient(new TeddyClientImpl(TeddyAdmins.HACK, new BrowserClient()), memcache);
        }
    }

    @Provides
    @Singleton
    @Named(TeddyAdmins.FOR_WARM_UP)
    TeddyClient provideTeddyClientForWarmUp(final MemcacheService memcache) {
        switch (AppEnvs.getEnv()) {
            case PROD:
                return new TeddyClientImpl(TeddyAdmins.JIAONI, new BrowserClient());
            default:
                return new TeddyClientImpl(TeddyAdmins.HACK, new BrowserClient());
        }
    }

    @Provides
    @Singleton
    ImageAnnotatorClient provideImageAnnotatorClient() {
        try {
            return ImageAnnotatorClient.create();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Provides
    @Singleton
    Storage provideStorage() {
        return GoogleClientFactory.storage();
    }

    @Provides
    @Singleton
    WxClient provideWxClient() {
        return new WxClientImpl(new BrowserClient());
    }
}
