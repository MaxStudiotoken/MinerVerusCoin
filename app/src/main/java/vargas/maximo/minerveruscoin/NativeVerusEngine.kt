package vargas.maximo.minerveruscoin

object NativeVerusEngine {

    private val loadResult = runCatching {
        System.loadLibrary("verus_engine")
    }

    val status: String
        get() = loadResult.fold(
            onSuccess = { nativeEngineInfo() },
            onFailure = { "Motor nativo no disponible en este dispositivo" }
        )

    private external fun nativeEngineInfo(): String
}
