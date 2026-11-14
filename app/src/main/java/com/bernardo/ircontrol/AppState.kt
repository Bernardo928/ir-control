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

enum class Screen { BRANDS, MODELS, REMOTE, AC_BRANDS, AC_MODELS, PROJ_BRANDS, PROJ_MODELS }

class AppState(private val context: Context) {
    private val prefs = context.getSharedPreferences("remote", Context.MODE_PRIVATE)
    private val scope: CoroutineScope = MainScope()
    val transmitter = IrTransmitter(context)

    var brands by mutableStateOf<List<Brand>>(emptyList()); private set
    var brand by mutableStateOf<String?>(prefs.getString("brand", null)); private set
    var models by mutableStateOf<List<IrModel>>(emptyList()); private set
    var modelId by mutableStateOf<String?>(prefs.getString("model", null)); private set
    var loading by mutableStateOf(false); private set
    /** Quem não usa TV abre direto na aba do aparelho que já configurou (AC ou projetor). */
    private val firstTab = when {
        brand != null -> 0
        prefs.getString("proj_brand", null) != null -> 1
        prefs.getString("ac_brand", null) != null -> 2
        else -> 0
    }
    var screen by mutableStateOf(
        when {
            firstTab != 0 -> Screen.REMOTE
            brand == null -> Screen.BRANDS
            modelId == null -> Screen.MODELS
            else -> Screen.REMOTE
        }
    )
    var tab by mutableStateOf(firstTab)
    var status by mutableStateOf<String?>(null); private set
    var sendingLabel by mutableStateOf<String?>(null); private set

    // ---------- Projetor ----------
    var projBrands by mutableStateOf<List<Brand>>(emptyList()); private set
    var projBrand by mutableStateOf(prefs.getString("proj_brand", null)); private set
    var projModels by mutableStateOf<List<IrModel>>(emptyList()); private set
    /** Id do controle escolhido, ou [ALL] para "Não sei o modelo". */
    var projModelId by mutableStateOf(prefs.getString("proj_model", null)); private set
    var projLoading by mutableStateOf(false); private set

    val projModel: IrModel? get() = projModels.firstOrNull { it.id == projModelId }

    val projModelLabel: String
        get() = when {
            projModelId == ALL -> "Todos os códigos (${projModels.size} controles)"
            projModel != null -> projModel!!.title + " · " + projModel!!.file
            else -> "Nenhum modelo"
        }

    private suspend fun loadProjModels(b: String) {
        projLoading = true
        projModels = emptyList()
        projModels = withContext(Dispatchers.IO) { IrDatabase.models(context, b).filter { it.isProjector } }
        projLoading = false
    }

    fun openProjBrands() {
        screen = Screen.PROJ_BRANDS
        if (projBrands.isEmpty()) scope.launch { projBrands = withContext(Dispatchers.IO) { IrDatabase.projectorBrands(context) } }
    }

    fun selectProjBrand(b: String) {
        if (b != projBrand) {
            projBrand = b
            projModelId = null
            prefs.edit().putString("proj_brand", b).remove("proj_model").apply()
            scope.launch { loadProjModels(b) }
        }
        screen = Screen.PROJ_MODELS
    }

    fun selectProjModel(id: String) {
        projModelId = id
        prefs.edit().putString("proj_model", id).apply()
        screen = Screen.REMOTE
        tab = 1
    }

    fun pressProj(key: RemoteKey) = send(
        if (projModelId == ALL) projModels.mapNotNull { it.code(key) } else listOfNotNull(projModel?.code(key))
    )

    // ---------- Ar-condicionado ----------
    var acBrand by mutableStateOf(AcBrands.find(prefs.getString("ac_brand", null))); private set
    /** Protocolo escolhido, ou [ALL] para "Não sei o modelo". */
    var acModel by mutableStateOf(prefs.getString("ac_model", null)); private set
    var ac by mutableStateOf(loadAcState()); private set

    val acProtocols: List<AcProtocol>
        get() = if (acModel == ALL) acBrand?.protocols.orEmpty()
        else listOfNotNull(AcProtocol.entries.firstOrNull { it.name == acModel })

    val acModelLabel: String
        get() = when {
            acModel == ALL -> "Todos os protocolos (${acProtocols.size})"
            acProtocols.isNotEmpty() -> "Protocolo " + acProtocols[0].title
            else -> "Nenhum modelo"
        }

    val acTempRange: IntRange
        get() = acProtocols.map { it.tempRange }.let { r ->
            if (r.isEmpty()) 16..30 else r.maxOf { it.first }..r.minOf { it.last }
        }

    private fun loadAcState(): AcState = runCatching {
        val p = prefs.getString("ac_state", null)!!.split(',')
        AcState(p[0] == "1", AcMode.valueOf(p[1]), p[2].toInt(), AcFan.valueOf(p[3]), p[4] == "1")
    }.getOrDefault(AcState())

    private fun saveAcState() {
        val s = ac
        prefs.edit().putString("ac_state", listOf(if (s.power) "1" else "0", s.mode.name, s.temp, s.fan.name, if (s.swing) "1" else "0").joinToString(",")).apply()
    }

    fun selectAcBrand(b: AcBrand) {
        if (b != acBrand) {
            acBrand = b
            acModel = null
            prefs.edit().putString("ac_brand", b.name).remove("ac_model").apply()
        }
        screen = Screen.AC_MODELS
    }

    fun selectAcModel(id: String) {
        acModel = id
        prefs.edit().putString("ac_model", id).apply()
        ac = ac.copy(temp = ac.temp.coerceIn(acTempRange))
        saveAcState()
        screen = Screen.REMOTE
        tab = 2
    }

    fun testAc(p: AcProtocol) = sendSignals(listOf(p.encode(ac.copy(power = true))))

    /** Aplica a mudança no estado do AC e envia (só envia mudanças de ajuste com o AC ligado). */
    fun updateAc(change: (AcState) -> AcState) {
        val old = ac
        ac = change(old).let { it.copy(temp = it.temp.coerceIn(acTempRange)) }
        saveAcState()
        if (ac.power || old.power) sendSignals(acProtocols.map { it.encode(ac) })
        else status = "Ar desligado: o ajuste será enviado ao ligar"
    }

    fun acSwing() {
        val next = ac.copy(swing = !ac.swing)
        val signals = acProtocols.mapNotNull { if (it.swingInState) it.encode(next) else it.swingToggle() }
        if (signals.isEmpty()) { status = "Este protocolo não tem comando de vaivém"; return }
        ac = next
        saveAcState()
        sendSignals(signals)
    }

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
            projBrand?.let { loadProjModels(it) }
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

    private fun sendSignals(signals: List<IrSignal>) {
        status = null
        transmitter.sendSignals(signals, onProgress = { done, total ->
            sendingLabel = when {
                total == 0 -> null
                total == 1 -> "Enviando…"
                else -> "Enviando código $done de $total"
            }
        }, onError = { status = it; sendingLabel = null })
    }

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
