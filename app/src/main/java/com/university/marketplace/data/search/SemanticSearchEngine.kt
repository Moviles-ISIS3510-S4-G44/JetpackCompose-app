package com.university.marketplace.data.search

import android.content.Context
import android.content.res.AssetFileDescriptor
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.sqrt
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * SemanticSearchEngine ahora intenta usar un modelo TFLite (all-MiniLM-L6-v2) si se encuentra
 * en assets con el nombre "all_minilm_l6_v2.tflite". Si no está disponible, cae al encoder
 * local antigua implementación basada en hash + ngrams.
 *
 * Nota: Este soporte TFLite es un *fallback opcional* y requiere que el modelo y su tokenizer
 * apropiado estén empaquetados correctamente en la app. El tokenizer usado aquí es un
 * placeholder muy simple: para producción necesitas el vocab y tokenization real de MiniLM
 * (por ejemplo, usando SentencePiece o WordPiece) y/o preprocesamiento correcto.
 */
class SemanticSearchEngine(private val context: Context) {
    private val dimensions = 384
    private val tfliteModelAssetName = "all_minilm_l6_v2.tflite"
    private var tfliteInterpreter: Interpreter? = null
    private var vocab: Map<String, Int>? = null
    private var unkId: Int = 100
    private var clsId: Int = 101
    private var sepId: Int = 102

    init {
        // Intentar cargar el intérprete TFLite si existe el asset
        try {
            val afd: AssetFileDescriptor? = try {
                context.assets.openFd(tfliteModelAssetName)
            } catch (e: Exception) {
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
                // intentar cargar vocab desde assets/vocab.txt
                try {
                    val vocabLines = context.assets.open("vocab.txt").bufferedReader().useLines { it.toList() }
                    val map = mutableMapOf<String, Int>()
                    vocabLines.forEachIndexed { idx, token -> map[token] = idx }
                    vocab = map
                    unkId = map["[UNK]"] ?: unkId
                    clsId = map["[CLS]"] ?: clsId
                    sepId = map["[SEP]"] ?: sepId
                } catch (e: Exception) {
                    // vocab no disponible -> seguir sin tokenizador real
                    vocab = null
                }
            }
        } catch (e: Exception) {
            // Si ocurre cualquier error, dejamos tfliteInterpreter como null y usamos fallback
            e.printStackTrace()
            tfliteInterpreter = null
        }
    }

    fun getEmbedding(text: String): FloatArray {
        // Si tenemos intérprete TFLite disponible intentamos generar embedding con MiniLM
        tfliteInterpreter?.let { interpreter ->
            try {
                val normalizedText = SearchTextNormalizer.normalize(text)

                val maxLen = 32
                // Si tenemos vocab cargado, usar tokenizador WordPiece; sino fallback a split
                val inputIds = IntArray(maxLen) { 0 }
                val inputMask = IntArray(maxLen) { 0 }

                val ids = vocab?.let { v ->
                    tokenizeToIds(normalizedText, v, maxLen)
                } ?: run {
                    // fallback simple: split por espacios y hash
                    val tokens = normalizedText.split(" ").filter { it.isNotBlank() }
                    val simpleIds = tokens.map { (it.hashCode() and Int.MAX_VALUE) % 30000 }
                    simpleIds.take(maxLen).toIntArray()
                }

                for (i in ids.indices) {
                    if (i >= maxLen) break
                    inputIds[i] = ids[i]
                    inputMask[i] = 1
                }

                // Preparar inputs para modelo: a menudo MiniLM espera input_ids y attention_mask
                val inputs: Array<Any> = arrayOf(inputIds, inputMask)

                // Salida: float[1][dimensions]
                val outputBuffer = Array(1) { FloatArray(dimensions) }

                // Ejecutar modelo con múltiples inputs -> outputs por índice
                val outputs = HashMap<Int, Any>()
                outputs[0] = outputBuffer
                interpreter.runForMultipleInputsOutputs(inputs, outputs)

                // Normalizar L2 y devolver
                return l2Normalize(outputBuffer[0])
            } catch (e: Exception) {
                e.printStackTrace()
                // fallback al método local si TFLite falla
            }
        }

        // Fallback: implementación heurística local (token hashing y ngrams)
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

    private fun tokenizeToIds(text: String, vocabMap: Map<String, Int>, maxLen: Int): IntArray {
        // Implementación WordPiece (similar a BERT). Retorna lista de ids truncada a maxLen-2 y con [CLS]/[SEP]
        val tokens = mutableListOf<Int>()
        tokens.add(clsId)

        // Basic tokenization: split por espacios y limpiar
        val words = text.split(Regex("\\s+"))
        for (word in words) {
            if (word.isBlank()) continue
            val subTokens = wordPieceTokenize(word, vocabMap)
            for (st in subTokens) {
                val id = vocabMap[st] ?: vocabMap["[UNK]"] ?: unkId
                tokens.add(id)
                if (tokens.size >= maxLen - 1) break
            }
            if (tokens.size >= maxLen - 1) break
        }

        // add SEP
        tokens.add(sepId)

        // pad/truncate to maxLen
        val out = IntArray(maxLen) { 0 }
        for (i in 0 until minOf(tokens.size, maxLen)) out[i] = tokens[i]
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
                // fallback: unknown token
                subTokens.add("[UNK]")
                break
            }
            subTokens.add(curSubStr)
            start = end
        }
        return subTokens
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
