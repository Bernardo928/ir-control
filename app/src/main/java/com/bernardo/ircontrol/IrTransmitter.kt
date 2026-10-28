package com.bernardo.ircontrol

import android.content.Context
import android.hardware.ConsumerIrManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/** Envia sinais pelo emissor infravermelho (ConsumerIrManager) em uma thread dedicada. */
class IrTransmitter(context: Context) {
    private val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    private val irThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    val available: Boolean get() = runCatching { ir?.hasIrEmitter() == true }.getOrDefault(false)

    private val ranges by lazy { runCatching { ir?.carrierFrequencies?.toList() }.getOrNull().orEmpty() }

    private fun fixFrequency(f: Int): Int {
        if (ranges.isEmpty() || ranges.any { f in it.minFrequency..it.maxFrequency }) return f
        return ranges.minBy { minOf(kotlin.math.abs(it.minFrequency - f), kotlin.math.abs(it.maxFrequency - f)) }
            .let { f.coerceIn(it.minFrequency, it.maxFrequency) }
    }

    /**
     * Envia os códigos em sequência (cancela um envio anterior ainda em andamento).
     * [onProgress] recebe (enviados, total); total 0 = nada a enviar.
     */
    fun send(codes: List<IrCode>, onProgress: (Int, Int) -> Unit, onError: (String) -> Unit) =
        sendSignals(codes.mapNotNull { IrEncoder.encode(it) }, onProgress, onError)

    fun sendSignals(all: List<IrSignal>, onProgress: (Int, Int) -> Unit, onError: (String) -> Unit) {
        job?.cancel()
        val signals = all.distinctBy { it.key }
        if (signals.isEmpty()) { onProgress(0, 0); return }
        if (!available) { onError("Este aparelho não tem emissor infravermelho disponível"); return }
        job = scope.launch {
            signals.forEachIndexed { i, s ->
                ensureActive()
                onProgress(i + 1, signals.size)
                val err = withContext(irThread) {
                    runCatching { ir!!.transmit(fixFrequency(s.frequency), s.pattern) }.exceptionOrNull()
                }
                if (err != null) { onError("Falha no IR: ${err.message}"); return@launch }
                if (signals.size > 1) delay(90)
            }
            onProgress(signals.size, signals.size)
            delay(400)
            onProgress(0, 0)
        }
    }

    fun cancel() { job?.cancel(); job = null }
}
