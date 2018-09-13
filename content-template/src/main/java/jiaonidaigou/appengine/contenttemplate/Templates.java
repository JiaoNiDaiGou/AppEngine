package jiaonidaigou.appengine.contenttemplate;

import com.google.common.collect.ImmutableMap;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jiaonidaigou.appengine.common.model.InternalRuntimeException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class Templates {
    private final Template freeMarkerTemplate;

    Templates(final Template freeMarkerTemplate) {
        this.freeMarkerTemplate = checkNotNull(freeMarkerTemplate);
    }

    public String toContent() {
        return toContent(null);
    }

    public String toContent(final Map<String, Object> props) {
        if (freeMarkerTemplate == null) {
            return null;
        }
        try (Writer writer = new StringWriter()) {
            freeMarkerTemplate.process(props == null ? ImmutableMap.of() : props, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new InternalRuntimeException(e);
        }
    }
}
