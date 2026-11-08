# plugin_arepacore

Plugin propio del server **ArepaLandia** (Paper 26.3).

## Qué hace

- **Entrada/salida**: `[+] <jugador> ha entrado.` (lo ven todos menos el que entra; con AuthMe se muestra al iniciar sesión) y `[-] <jugador> ha salido.`
- **Chat**: cabeza del jugador + prefijo de su rol en LuckPerms + nombre + mensaje.

Todo se configura en `plugins/arepaCore/config.yml` del server.

## Requisitos para programar

- **JDK 25** (Paper 26.3 lo exige). En Mac: `brew install openjdk@25`
- **Maven**. En Mac: `brew install maven`

## Compilar

```bash
mvn package
```

El plugin queda en `target/arepaCore.jar`. Se sube a la carpeta `plugins/` del server y se reinicia.

## Estructura

```
src/main/java/com/arepalandia/core/
├── ArepaCore.java          # clase principal: registra todo
├── JoinQuitListener.java   # mensajes de entrada/salida (+ integración con AuthMe)
└── ChatListener.java       # formato del chat (cabeza + prefijo LuckPerms)
src/main/resources/
├── plugin.yml              # nombre, versión, dependencias
└── config.yml              # textos y formatos por defecto
```

## Agregar algo nuevo

1. `git pull` antes de empezar, para tener lo último.
2. Crea una clase nueva (ej. `MiCosaListener.java`) y regístrala en `ArepaCore#onEnable`.
3. Si necesita opciones, agrégalas a `config.yml`.
4. Compila con `mvn package` y prueba.
5. `git add`, `git commit -m "qué hiciste"`, `git pull` (por si alguien subió algo mientras) y `git push`.

Dependencias que el server ya tiene y puedes usar: Paper API, LuckPerms API (`provided`), AuthMe (soft-depend).
