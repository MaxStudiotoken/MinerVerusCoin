#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_vargas_maximo_minerveruscoin_NativeVerusEngine_nativeEngineInfo(
    JNIEnv* env,
    jobject /* instance */
) {
#if defined(__aarch64__)
    return env->NewStringUTF("NDK listo para ARM64; VerusHash pendiente de validacion");
#elif defined(__arm__)
    return env->NewStringUTF("NDK listo para ARM; VerusHash requiere ARM64");
#elif defined(__x86_64__)
    return env->NewStringUTF("NDK listo para x86_64; VerusHash pendiente de validacion");
#else
    return env->NewStringUTF("ABI no compatible con el motor VerusHash");
#endif
}
