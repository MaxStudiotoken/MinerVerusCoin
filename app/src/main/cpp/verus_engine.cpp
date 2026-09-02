#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_vargas_maximo_minerveruscoin_NativeVerusEngine_nativeEngineInfo(
    JNIEnv* env,
    jobject /* instance */
) {
#if defined(__aarch64__)
    return env->NewStringUTF("Puente NDK ARM64 listo; mineria real desactivada hasta validar VerusHash");
#elif defined(__arm__)
    return env->NewStringUTF("Puente NDK ARM detectado; el motor real requiere ARM64");
#elif defined(__x86_64__)
    return env->NewStringUTF("Puente NDK x86_64 detectado; el APK de produccion se limita a ARM64");
#else
    return env->NewStringUTF("ABI no compatible con el motor VerusHash");
#endif
}
