package jiaoni.daigou.service.appengine.impls.teddy;

import jiaoni.common.appengine.access.ocr.OcrClient;
import jiaoni.common.model.Snippet;
import jiaoni.common.utils.StringUtils2;
import jiaoni.daigou.lib.teddy.TeddyClient;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GOcrTeddyLoginGuidRecognizer implements TeddyClient.LoginGuidRecognizer {
    private OcrClient ocrClient;

    @Inject
    public GOcrTeddyLoginGuidRecognizer(final OcrClient ocrClient) {
        this.ocrClient = ocrClient;
    }

    @Override
    public String recognize(byte[] bytes) {
        List<Snippet> snippets = ocrClient.annotateFromBytes(bytes);
        for (Snippet snippet : snippets) {
            String content = snippet.getText();
            content = StringUtils2.removeNonCharTypesWith(
                    content,
                    StringUtils2.CharType.A2Z,
                    StringUtils2.CharType.DIGIT);
            if (content.length() == 4) {
                return content.toLowerCase();
            }
        }
        return null;
    }
}
