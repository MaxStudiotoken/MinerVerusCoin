# Avisos de terceros

Este proyecto no incluye todavia codigo de minado VerusHash dentro del APK. El
submodulo `third_party/nheqminer` se conserva como fuente de referencia para un
port NDK posterior y esta fijado a un commit concreto.

## nheqminer

- Repositorio: <https://github.com/VerusCoin/nheqminer>
- Licencia superior: MIT, con copyright de John Tromp indicado en
  `third_party/nheqminer/LICENSE_MIT`.
- Uso previsto: estudiar y portar un subconjunto de VerusHash para ARM64, solo
  despues de compilarlo y verificarlo con vectores conocidos.

## Componentes con avisos propios

Si una futura version copia, modifica o compila fuentes concretas del
submodulo, debe conservar sus avisos originales. En particular:

- `nheqminer/crypto/haraka.c` y `haraka.h` incluyen aviso MIT de kste.
- `nheqminer/crypto/verus_clhash.cpp`, `verus_clhash_portable.cpp` y
  `verus_clhash.h` incluyen aviso Apache-2.0 de Michael Toutonghi.

Las rutas CUDA y cualquier dependencia no necesaria para ARM64 quedan fuera del
alcance del port Android inicial.

## Referencias descartadas

No se incorpora codigo de `ansient/VerusMiner9000`. Su repositorio se distribuye
bajo GPLv3; reutilizarlo requeriria relicenciar el trabajo derivado bajo GPLv3.
Solo se han considerado patrones generales no expresivos, como pausa termica y
proteccion de bateria, que ya se implementan de forma independiente.

