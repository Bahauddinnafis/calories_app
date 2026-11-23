package com.nafis.calories.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class DetectionResult(
    val label: String,
    val score: Float
)


class FoodDetector(context: Context) {
    private val interpreter: Interpreter
    private val labels: List<String>


    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        // NO normalization - raw pixel values [0, 255]
        .build()

    init {
        val modelBuffer = loadModelFile(context, MODEL_PATH)
        interpreter = Interpreter(modelBuffer)

        labels = loadLabels(context, LABELS_PATH)

        // Logging untuk debugging & evidence
        Log.d(TAG, "TFLite model loaded successfully")
        Log.d(TAG, "Classes: $labels")
        Log.d(TAG, "Input tensor count: ${interpreter.inputTensorCount}")
        Log.d(TAG, "Output tensor count: ${interpreter.outputTensorCount}")

        // Verify input tensor shape: [1, 224, 224, 3]
        val inputTensor = interpreter.getInputTensor(0)
        Log.d(TAG, "Input shape: ${inputTensor.shape().contentToString()}")
        Log.d(TAG, "Input data type: ${inputTensor.dataType()}")

        // Verify output tensor shape: [1, 20] (20 classes)
        val outputTensor = interpreter.getOutputTensor(0)
        Log.d(TAG, "Output shape: ${outputTensor.shape().contentToString()}")
        Log.d(TAG, "Output data type: ${outputTensor.dataType()}")
    }

    suspend fun detectFood(bitmap: Bitmap): Result<DetectionResult> = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Starting detection for bitmap: ${bitmap.width}x${bitmap.height}")

            // Step 1: Convert Bitmap to TensorImage
            // DataType.FLOAT32: Model expects float input (RGB values as floats)
            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)

            // Step 2: Preprocessing (resize to 224x224)
            val processedImage = imageProcessor.process(tensorImage)
            Log.d(TAG, "Image preprocessed to: ${processedImage.width}x${processedImage.height}")

            // Step 3: Prepare output buffer
            // Shape: [1, 20] -> 1 batch, 20 classes
            val outputBuffer = Array(1) { FloatArray(labels.size) }

            // Step 4: Run inference
            // interpreter.run() adalah blocking call (that's why we use Dispatchers.Default)
            interpreter.run(processedImage.buffer, outputBuffer)

            // Step 5: Parse results
            val probabilities = outputBuffer[0]

            // Logging untuk evidence & debugging
            Log.d(TAG, "All probabilities:")
            labels.forEachIndexed { index, label ->
                Log.d(TAG, "  [$index] $label: ${probabilities[index]}")
            }

            // Step 6: Get prediction (class dengan probability tertinggi)
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
            val maxConfidence = probabilities[maxIndex]
            val predictedLabel = labels[maxIndex]

            Log.d(TAG, "==> Prediction: $predictedLabel (index: $maxIndex, confidence: ${maxConfidence * 100}%)")

            // Return label with confidence score
            Result.success(DetectionResult(predictedLabel, maxConfidence))
        } catch (e: Exception) {
            Log.e(TAG, "Detection failed", e)
            Result.failure(e)
        }
    }

    fun close() {
        interpreter.close()
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels(context: Context, labelsPath: String): List<String> {
        val jsonString = context.assets
            .open(labelsPath)
            .bufferedReader()
            .use { it.readText() }

        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString(
            ListSerializer(String.serializer()),
            jsonString
        )
    }

    companion object {
        private const val TAG = "FoodDetector"
        private const val MODEL_PATH = "food_model.tflite"
        private const val LABELS_PATH = "labels.json"
        private const val IMAGE_SIZE = 224
        private const val CONFIDENCE_THRESHOLD = 0.5f
    }
}