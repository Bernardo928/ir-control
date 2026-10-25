package com.bernardo.ircontrol

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { BRANDS, MODELS, REMOTE }

class AppState(private val context: Context) {
    private val prefs = context.getSharedPreferences("remote", Context.MODE_PRIVATE)
    private val scope: CoroutineScope = MainScope()
    val transmitter = IrTransmitter(context)

    var brands by mutableStateOf<List<Brand>>(emptyList()); private set
    var brand by mutableStateOf<String?>(prefs.getString("brand", null)); private set
    var models by mutableStateOf<List<IrModel>>(emptyList()); private set
    var modelId by mutableStateOf<String?>(prefs.getString("model", null)); private set
    var loading by mutableStateOf(false); private set
    var screen by mutableStateOf(if (brand == null) Screen.BRANDS else if (modelId == null) Screen.MODELS else Screen.REMOTE)
    var tab by mutableStateOf(0)
    var status by mutableStateOf<String?>(null); private set
    var sendingLabel by mutableStateOf<String?>(null); private set

    val allCodes: Boolean get() = modelId == ALL
    val model: IrModel? get() = models.firstOrNull { it.id == modelId }
    /** Controles usados no modo "Não sei o modelo". */
    val tvModels: List<IrModel> get() = models.filter { it.looksLikeTv }.ifEmpty { models }

    val modelLabel: String
        get() = when {
            allCodes -> "Todos os códigos (${tvModels.size} controles)"
            model != null -> model!!.title + " · " + model!!.file
            else -> "Nenhum modelo"
        }

    init {
        scope.launch {
            brands = withContext(Dispatchers.IO) { IrDatabase.brands(context) }
            brand?.let { loadModels(it) }
        }
    }

    private suspend fun loadModels(b: String) {
        loading = true
        models = emptyList()
        models = withContext(Dispatchers.IO) { IrDatabase.models(context, b) }
        loading = false
    }

    fun selectBrand(b: String) {
        if (b != brand) {
            brand = b
            modelId = null
            prefs.edit().putString("brand", b).remove("model").apply()
            scope.launch { loadModels(b) }
        }
        screen = Screen.MODELS
    }

    fun selectModel(id: String) {
        modelId = id
        prefs.edit().putString("model", id).apply()
        screen = Screen.REMOTE
        tab = 0
    }

    fun codesFor(key: RemoteKey): List<IrCode> = when {
        allCodes -> tvModels.mapNotNull { it.code(key) }
        else -> listOfNotNull(model?.code(key))
    }

    fun press(key: RemoteKey) = send(codesFor(key))

    fun testPower(m: IrModel) = send(listOfNotNull(m.code(RemoteKey.POWER)))

    private fun send(codes: List<IrCode>) {
        status = null
        transmitter.send(codes, onProgress = { done, total ->
            sendingLabel = when {
                total == 0 -> null
                total == 1 -> "Enviando…"
                else -> "Enviando código $done de $total"
            }
            if (codes.isEmpty() || (total == 0 && done == 0 && codes.none { IrEncoder.encode(it) != null }))
                status = "Esta tecla não existe para este controle"
        }, onError = { status = it; sendingLabel = null })
    }

    companion object { const val ALL = "*" }
}
