# plugin_arepacore

Plugin propio del server **ArepaLandia** (Paper 26.3).

## Qué hace

- **Entrada/salida**: `[+] <jugador> ha entrado.` (con AuthMe se muestra al iniciar sesión) y `[-] <jugador> ha salido.` + bienvenida privada.
- **Chat**: cabeza del jugador + prefijo de su rol en LuckPerms + nombre + mensaje.
- **Casas**: `/sethome [nombre]`, `/home [nombre]`, `/delhome`, `/homes` (máx. 3; importa las casas del plugin SetHome la primera vez). Si mueres sin cama, reapareces en tu casa.
- **TPA**: `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel`, con botones clicables.
- **Spawn y back**: `/spawn`, `/setspawn` (admin), `/back` (vuelve a donde moriste o a antes del último tp).
- Todos los teletransportes tienen cuenta atrás (3 s) y se cancelan si te mueves o te pegan.
- **Mensajes privados**: `/msg`, `/r`.
- **Talar**: agachado + hacha rompe el árbol entero (solo árboles con hojas naturales; respeta protecciones).
- **MOTD** de la lista de servidores con el día del mundo.
- **Estadísticas**: `/stats [jugador]`, `/top <horas|muertes|mobs|bloques|diamantes> [semana]`.
- **Muertes**: mensajes graciosos en español + contador de muertes.
- **Eventos**: XP doble los fines de semana; Noche de Sangre (10 % de las noches: doble de monstruos y de botín).
- **Logros del server**: primeros diamantes, Nether, End, dragón, Wither, horas jugadas...
- **Recompensa diaria** al conectarse.
- **Días y AFK**: `/dia`, aviso cada amanecer, `/afk` y AFK automático a los 5 min.
- **Anuncios** automáticos con tips.
- **Admin**: `/arepa reload`, `/arepa evento <xp|sangre> <on|off|auto>`, `/freeze`, `/invsee`, `/endersee`.
- `/ayuda` muestra todos los comandos.

Todo se configura en `plugins/arepaCore/config.yml` del server. Los datos (casas, estadísticas, logros) quedan en `plugins/arepaCore/data/`.

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
├── Msg.java / DataFile.java# utilidades: mensajes y archivos de datos
├── JoinQuitListener.java   # entrada/salida (+ AuthMe)
├── ChatListener.java       # formato del chat
├── Teleports.java          # teletransportes con espera + /back
├── HomesModule.java        # casas
├── TpaModule.java          # tpa
├── SpawnBackMsgModule.java # spawn, back, msg, r
├── TimberListener.java     # talar árboles
├── MotdListener.java       # MOTD
├── StatsModule.java        # stats y top
├── DeathModule.java        # mensajes de muerte
├── EventsModule.java       # XP doble y Noche de Sangre
├── AchievementsModule.java # logros
├── DailyAfkModule.java     # bienvenida, recompensa diaria, días, AFK, anuncios
└── AdminModule.java        # /arepa, freeze, invsee
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
