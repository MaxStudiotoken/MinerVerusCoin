# Port VerusHash para Android ARM64

El submodulo `third_party/nheqminer` esta fijado al commit `0b46244021a83a3adba6648e21cc11ad0aa90ee5` y se usa solo como fuente de referencia MIT.

## Estado verificado

- El puente JNI `verus_engine` compila con Android NDK para `arm64-v8a`.
- La fuente oficial contiene implementaciones portables y `SSE2NEON`, pero el arbol completo de escritorio no compila directamente para Android.
- La prueba de compilacion detecto dos dependencias a sustituir antes de enlazar el hash real: cabeceras `immintrin` x86 y tipos/threads de Boost.

## Siguiente implementacion

1. Extraer el minimo de VerusHash/Haraka portable a un modulo Android propio, conservando los avisos de licencia.
2. Reemplazar las cabeceras x86 por una capa ARM NEON comprobada.
3. Reemplazar los tipos de Boost usados por el nucleo con biblioteca estandar de C++ o aislarlos fuera del hash.
4. Ejecutar vectores de prueba en ARM64 y compararlos con la salida de referencia antes de habilitar shares.
5. Conectar un cliente Stratum en un servicio Android visible, respetando los limites termicos y de bateria ya incluidos.

No se debe marcar la app como minero real hasta completar los pasos 1 a 4.
