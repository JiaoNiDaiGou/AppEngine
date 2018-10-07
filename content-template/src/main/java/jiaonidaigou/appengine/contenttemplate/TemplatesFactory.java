package jiaonidaigou.appengine.contenttemplate;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import jiaoni.common.model.InternalIOException;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class TemplatesFactory {
    private static final String TEMPLATE_RESOURCE_ROOT = "templates/";
    private final Configuration cfg;

    private TemplatesFactory() {
        cfg = new Configuration(new Version(2, 3, 20));
        try {
            cfg.setDirectoryForTemplateLoading(new File(Resources.getResource(TEMPLATE_RESOURCE_ROOT).toURI()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        cfg.setDefaultEncoding(Charsets.UTF_8.name());
        cfg.setLocale(Locale.US);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    public static TemplatesFactory instance() {
        return TemplatesFactory.LazyHolder.INSTANCE;
    }

    public Templates getTemplate(final String templateName) {
        if (Resources.getResource(TEMPLATE_RESOURCE_ROOT + templateName) == null) {
            throw new IllegalStateException(templateName + " not exists");
        }
        try {
            return new Templates(cfg.getTemplate(templateName, Charsets.UTF_8.name()));
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    private static class LazyHolder {
        static final TemplatesFactory INSTANCE = new TemplatesFactory();
    }
}
