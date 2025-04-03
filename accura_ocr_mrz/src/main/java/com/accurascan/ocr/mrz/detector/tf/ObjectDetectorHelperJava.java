package com.accurascan.ocr.mrz.detector.tf;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.util.List;

public class ObjectDetectorHelperJava {
   private final String TAG = "AccuraDetector";
   private ObjectDetector objectDetector;
   @NonNull
   private final Context context;

   public void setupObjectDetector() {

       String modelName = "card_with_metadata.tflite";

       ObjectDetector.ObjectDetectorOptions.Builder optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
               .setScoreThreshold(0.10f)
               .setMaxResults(10);
       try {
           this.objectDetector = ObjectDetector.createFromFileAndOptions(
                   this.context, modelName, optionsBuilder.build());
       } catch (Exception e) {
       }
   }

   public List<Detection> detect(@NonNull Bitmap image, int imageRotation, boolean isTakePicture) {

         if (this.objectDetector == null) {
            this.setupObjectDetector();
         }
         ImageProcessor imageProcessor = (new ImageProcessor.Builder()).build();
         TensorImage tensorImage = imageProcessor.process(TensorImage.fromBitmap(image));
         ObjectDetector var10000 = this.objectDetector;
         List<Detection> results = var10000 != null ? var10000.detect(tensorImage) : null;
         return results;
   }

   public ObjectDetectorHelperJava(float threshold, int numThreads, int maxResults, int currentDelegate, int currentModel, @NonNull Context context) {
      this.context = context;
   }

}
