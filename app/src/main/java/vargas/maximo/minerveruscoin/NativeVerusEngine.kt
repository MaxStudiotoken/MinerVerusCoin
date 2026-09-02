package vargas.maximo.minerveruscoin

object NativeVerusEngine {

    private val loadResult = runCatching {
        System.loadLibrary("verus_engine")
    }

    val status: String
        get() = loadResult.fold(
            onSuccess = { nativeEngineInfo() },
            onFailure = { "Puente NDK no disponible; la mineria real permanece desactivada" }
        )

    private external fun nativeEngineInfo(): String
}
