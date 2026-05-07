package com.university.marketplace.data.search

import android.content.Context
import android.content.res.AssetFileDescriptor
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.tensorflow.lite.Interpreter
import kotlin.math.sqrt
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class SemanticSearchEngine(private val context: Context) {
    private val dimensions = 384
    private val maxTokens = 32
    private val onnxModelAssetName = "model.onnx"
    private val tfliteModelAssetName = "all_minilm_l6_v2.tflite"
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var tfliteInterpreter: Interpreter? = null
    private var vocab: Map<String, Int>? = null
    private var unkId: Int = 100
    private var clsId: Int = 101
    private var sepId: Int = 102


    init {
        try {
            val tempFile = File(context.cacheDir, onnxModelAssetName)
            if (!tempFile.exists() || tempFile.length() == 0L) {
                context.assets.open(onnxModelAssetName).use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            ortEnv = OrtEnvironment.getEnvironment()
            ortSession = ortEnv?.createSession(tempFile.absolutePath)
        } catch (_: Exception) {
            ortEnv = null
            ortSession = null
        }

        try {
            val afd: AssetFileDescriptor? = try {
                context.assets.openFd(tfliteModelAssetName)
            } catch (_: Exception) {
                null
            }
            afd?.let {
                val inputStream = FileInputStream(it.fileDescriptor)
                val channel: FileChannel = inputStream.channel
                val startOffset = it.startOffset
                val declaredLength = it.declaredLength
                val mapped: MappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                val options = Interpreter.Options()
                tfliteInterpreter = Interpreter(mapped, options)
                try {
                    val vocabLines = context.assets.open("vocab.txt").bufferedReader().useLines { it.toList() }
                    val map = mutableMapOf<String, Int>()
                    vocabLines.forEachIndexed { idx, token -> map[token] = idx }
                    vocab = map
                    unkId = map["[UNK]"] ?: unkId
                    clsId = map["[CLS]"] ?: clsId
                    sepId = map["[SEP]"] ?: sepId
                } catch (_: Exception) {
                    vocab = null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tfliteInterpreter = null
        }
    }

    fun getEmbedding(text: String): FloatArray {
        val onnxSession = ortSession
        val onnxEnv = ortEnv
        if (onnxSession != null && onnxEnv != null) {
            val normalizedText = SearchTextNormalizer.normalize(text)
            try {
                val inputIds = IntArray(maxTokens) { 0 }
                val inputMask = IntArray(maxTokens) { 0 }
                val ids = vocab?.let { v ->
                    tokenizeToIds(normalizedText, v)
                } ?: run {
                    val tokens = normalizedText.split(" ").filter { it.isNotBlank() }
                    val simpleIds = tokens.map { (it.hashCode() and Int.MAX_VALUE) % 30000 }
                    simpleIds.take(maxTokens).toIntArray()
                }

                for (i in ids.indices) {
                    if (i >= maxTokens) break
                    inputIds[i] = ids[i]
                    inputMask[i] = 1
                }

                val inputIdsLong = LongArray(maxTokens) { inputIds[it].toLong() }
                val inputMaskLong = LongArray(maxTokens) { inputMask[it].toLong() }

                val inputNames = onnxSession.inputNames.toList()
                val inputIdsName = if (inputNames.contains("input_ids")) "input_ids" else inputNames.first()
                val attentionName = if (inputNames.contains("attention_mask")) {
                    "attention_mask"
                } else {
                    inputNames.getOrNull(1)
                }

                val inputTensors = mutableMapOf<String, OnnxTensor>()
                val idsTensor = OnnxTensor.createTensor(onnxEnv, arrayOf(inputIdsLong))
                inputTensors[inputIdsName] = idsTensor
                val maskTensor = attentionName?.let {
                    OnnxTensor.createTensor(onnxEnv, arrayOf(inputMaskLong))
                }
                if (maskTensor != null && attentionName != null) {
                    inputTensors[attentionName] = maskTensor
                }

                val result = onnxSession.run(inputTensors)
                val outputNames = onnxSession.outputNames.toList()
                val outputName = when {
                    outputNames.contains("sentence_embedding") -> "sentence_embedding"
                    outputNames.contains("pooler_output") -> "pooler_output"
                    else -> outputNames.first()
                }
                
                val outputValue = result.get(outputName)
                val output = if (outputValue.isPresent) {
                    val onnxValue = outputValue.get()
                    if (onnxValue is OnnxTensor) onnxValue.value else null
                } else null

                val embedding = extractOnnxEmbedding(output, inputMask)
                result.close()
                idsTensor.close()
                maskTensor?.close()
                if (embedding != null) return l2Normalize(embedding)
            } catch (_: Exception) {
            }
        }

        tfliteInterpreter?.let { interpreter ->
            try {
                val normalizedText = SearchTextNormalizer.normalize(text)

                val inputIds = IntArray(maxTokens) { 0 }
                val inputMask = IntArray(maxTokens) { 0 }

                val ids = vocab?.let { v ->
                    tokenizeToIds(normalizedText, v)
                } ?: run {
                    val tokens = normalizedText.split(" ").filter { it.isNotBlank() }
                    val simpleIds = tokens.map { (it.hashCode() and Int.MAX_VALUE) % 30000 }
                    simpleIds.take(maxTokens).toIntArray()
                }

                for (i in ids.indices) {
                    if (i >= maxTokens) break
                    inputIds[i] = ids[i]
                    inputMask[i] = 1
                }

                val inputIdsBatch = Array(1) { inputIds }
                val inputMaskBatch = Array(1) { inputMask }
                val inputs: Array<Any> = arrayOf(inputIdsBatch, inputMaskBatch)

                val outputBuffer = Array(1) { FloatArray(dimensions) }

                val outputs = HashMap<Int, Any>()
                outputs[0] = outputBuffer
                interpreter.runForMultipleInputsOutputs(inputs, outputs)

                return l2Normalize(outputBuffer[0])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val normalizedText = SearchTextNormalizer.normalize(text)
        val baseTokens = SearchTextNormalizer.tokenize(normalizedText)
        val expandedTokens = SearchQueryExpander.expandTokens(baseTokens)
        if (expandedTokens.isEmpty()) return FloatArray(dimensions)

        val vector = FloatArray(dimensions)

        expandedTokens.forEach { token ->
            addTokenContribution(vector, token)
            addTrigramContribution(vector, token)
        }

        return l2Normalize(vector)
    }

    fun calculateCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size) return 0f
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        val result = dotProduct / (sqrt(normA) * sqrt(normB))
        return if (result.isNaN()) 0f else result
    }

    private fun addTokenContribution(vector: FloatArray, token: String) {
        if (token.isBlank()) return
        val index = hashToIndex(token, vector.size)
        val tokenWeight = 1.0f + (token.length.coerceAtMost(12) * 0.03f)
        vector[index] += tokenWeight
    }

    private fun addTrigramContribution(vector: FloatArray, token: String) {
        val n = 3
        if (token.length < n) return
        for (i in 0..token.length - n) {
            val ngram = token.substring(i, i + n)
            val index = hashToIndex("ng_$ngram", vector.size)
            vector[index] += 0.35f
        }
    }

    private fun hashToIndex(value: String, size: Int): Int {
        val hash = value.hashCode()
        return (hash and Int.MAX_VALUE) % size
    }

    private fun tokenizeToIds(text: String, vocabMap: Map<String, Int>): IntArray {
        val tokens = mutableListOf<Int>()
        tokens.add(clsId)

        val words = text.split(Regex("\\s+"))
        for (word in words) {
            if (word.isBlank()) continue
            val subTokens = wordPieceTokenize(word, vocabMap)
            for (st in subTokens) {
                val id = vocabMap[st] ?: vocabMap["[UNK]"] ?: unkId
                tokens.add(id)
                if (tokens.size >= maxTokens - 1) break
            }
            if (tokens.size >= maxTokens - 1) break
        }

        tokens.add(sepId)

        val out = IntArray(maxTokens) { 0 }
        for (i in 0 until minOf(tokens.size, maxTokens)) out[i] = tokens[i]
        return out
    }

    private fun wordPieceTokenize(token: String, vocabMap: Map<String, Int>): List<String> {
        val lowered = token.lowercase()
        val chars = lowered.toCharArray()
        val subTokens = mutableListOf<String>()
        var start = 0
        while (start < chars.size) {
            var end = chars.size
            var curSubStr: String? = null
            while (start < end) {
                var substr = String(chars, start, end - start)
                if (start > 0) substr = "##$substr"
                if (vocabMap.containsKey(substr)) {
                    curSubStr = substr
                    break
                }
                end -= 1
            }
            if (curSubStr == null) {
                subTokens.add("[UNK]")
                break
            }
            subTokens.add(curSubStr)
            start = end
        }
        return subTokens
    }

    private fun extractOnnxEmbedding(output: Any?, attentionMask: IntArray): FloatArray? {
        if (output !is Array<*>) return null
        if (output.isEmpty()) return null
        val first = output[0]
        if (first is FloatArray) return first
        if (first is Array<*> && first.isNotEmpty() && first[0] is FloatArray) {
            @Suppress("UNCHECKED_CAST")
            val tokenEmbeddings = output as Array<Array<FloatArray>>
            return meanPool(tokenEmbeddings[0], attentionMask)
        }
        return null
    }

    private fun meanPool(tokenEmbeddings: Array<FloatArray>, attentionMask: IntArray): FloatArray {
        val hidden = tokenEmbeddings.firstOrNull()?.size ?: dimensions
        val result = FloatArray(hidden)
        var count = 0
        for (i in tokenEmbeddings.indices) {
            if (i >= attentionMask.size) break
            if (attentionMask[i] == 0) continue
            val tokenVec = tokenEmbeddings[i]
            for (j in 0 until hidden) {
                result[j] += tokenVec[j]
            }
            count += 1
        }
        if (count > 0) {
            for (j in 0 until hidden) {
                result[j] /= count.toFloat()
            }
        }
        return result
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var norm = 0f
        for (element in vector) {
            norm += element * element
        }
        norm = sqrt(norm)
        if (norm == 0f) return vector
        for (i in vector.indices) {
            vector[i] /= norm
        }
        return vector
    }
}
