package jiaoni.common.appengine.access.ocr;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageSource;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.Word;
import com.google.protobuf.ByteString;
import jiaoni.common.model.InternalIOException;
import jiaoni.common.model.Snippet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static jiaoni.common.utils.Preconditions2.checkNotBlank;

@Singleton
public class GoogleVisionOcrClient implements OcrClient {

    @Override
    public List<Snippet> annotateFromBytes(byte[] bytes) {
        checkArgument(bytes != null && bytes.length > 0);

        Image image = Image.newBuilder()
                .setContent(ByteString.copyFrom(bytes))
                .build();

        return annotate(AnnotateImageRequest.newBuilder()
                .addFeatures(Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION))
                .setImage(image)
                .build());
    }

    @Override
    public List<Snippet> annotateFromMediaPath(final String mediaPath) {
        checkNotBlank(mediaPath);

        ImageSource imgSource = ImageSource.newBuilder()
                .setGcsImageUri(mediaPath)
                .build();
        Image image = Image.newBuilder()
                .setSource(imgSource)
                .build();

        return annotate(AnnotateImageRequest.newBuilder()
                .addFeatures(Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION))
                .setImage(image)
                .build());
    }

    private List<Snippet> annotate(final AnnotateImageRequest request) {
        List<Snippet> toReturn = new ArrayList<>();
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(Collections.singletonList(request));

            for (AnnotateImageResponse res : response.getResponsesList()) {
                if (res.hasError()) {
                    continue;
                }

                // For full list of available annotations, see http://g.co/cloud/vision/docs
                TextAnnotation annotation = res.getFullTextAnnotation();
                for (Page page : annotation.getPagesList()) {
                    for (Block block : page.getBlocksList()) {
                        StringBuilder blockText = new StringBuilder();
                        for (Paragraph para : block.getParagraphsList()) {
                            StringBuilder paraText = new StringBuilder();
                            for (Word word : para.getWordsList()) {
                                StringBuilder wordText = new StringBuilder();
                                for (Symbol symbol : word.getSymbolsList()) {
                                    wordText.append(symbol.getText());
                                }
                                paraText.append(" ").append(wordText.toString());
                            }
                            blockText.append(" ").append(paraText);
                        }

                        toReturn.add(new Snippet(blockText.toString(), block.getConfidence()));
                    }
                }

            }
            return toReturn;
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }
}
