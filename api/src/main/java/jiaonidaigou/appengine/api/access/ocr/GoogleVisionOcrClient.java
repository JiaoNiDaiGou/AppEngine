package jiaonidaigou.appengine.api.access.ocr;

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
import jiaonidaigou.appengine.common.model.InternalIOException;
import jiaonidaigou.appengine.common.model.Snippet;
import jiaonidaigou.appengine.wiremodel.entity.MediaObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
    public List<Snippet> annotateFromMediaObject(MediaObject mediaObject) {
        checkNotNull(mediaObject);

        ImageSource imgSource = ImageSource.newBuilder()
                .setGcsImageUri(mediaObject.getFullPath())
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

        BatchAnnotateImagesResponse response = null;
        try {
            response = ImageAnnotatorClient.create()
                    .batchAnnotateImages(Collections.singletonList(request));
        } catch (IOException e) {
            throw new InternalIOException(e);
        }

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
    }
}
