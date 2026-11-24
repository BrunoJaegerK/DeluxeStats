# 💜 DeluxeStats

**Plugin de estadísticas y rankings para servidores Minecraft Bedrock con Nukkit.**

DeluxeStats registra estadísticas generales de los jugadores y permite consultar datos personales y rankings globales. No incluye temporadas, premios competitivos ni sistemas antifarmeo. Incluye rankings de kills y muertes sin restricciones artificiales.

## ✨ Características
- ⛏️ Registro de minerales extraídos.
- ⏱️ Seguimiento del tiempo de juego activo con detección de inactividad.
- 💰 Consulta del saldo actual mediante EconomyAPI.
- ⚔️ Registro y rankings de kills y muertes.
- 🏆 Rankings globales de minería y tiempo activo.
- 🔌 Placeholders personales y TOP para PlaceholderAPI.
- 💾 Almacenamiento persistente mediante H2.

## 📋 Requisitos
- NukkitPetteriM1Edition.
- Java 8 o compatible.
- EconomyAPI 2.0.5.
- PlaceholderAPI para Nukkit (opcional, para placeholders).

## 📥 Instalación
1. Descarga el JAR desde [Releases](https://github.com/BrunoJaegerK/DeluxeStats/releases).
2. Colócalo en `plugins/`.
3. Comprueba que EconomyAPI esté instalado.
4. Instala PlaceholderAPI si necesitas los placeholders.
5. Inicia el servidor y revisa la consola.

La base de datos se crea en `plugins/DeluxeStats/deluxestats.mv.db`. Para actualizar, detén el servidor y conserva la carpeta de datos.

## 🎮 Comandos
| Comando | Descripción |
|---|---|
| `/stats` | Muestra tus estadísticas personales. |
| `/topstats kills` | Muestra el TOP 10 de asesinatos. |
| `/topstats deaths` | Muestra el TOP 10 de muertes. |
| `/topstats ores` | Muestra el TOP 10 de minerales extraídos. |
| `/topstats playtime` | Muestra el TOP 10 de tiempo activo. |

## 📊 Estadísticas
| Estadística | Descripción |
|---|---|
| `kills` | Asesinatos de jugadores registrados. |
| `deaths` | Muertes de jugadores registradas. |
| `ores_mined` | Minerales extraídos registrados. |
| `active_seconds` | Tiempo de juego activo acumulado. |
| `money_earned` | Saldo consultado y almacenado desde EconomyAPI. |

El rastreador considera actividad el movimiento, las interacciones, el chat y la rotura de bloques. Tras 60 segundos sin actividad deja de acumular tiempo hasta detectar una nueva acción. El progreso se guarda periódicamente y al desconectarse.

## 🏆 Rankings y configuración
Configura los rankings en `plugins/DeluxeStats/config.yml`:

```yaml
rankings:
  top-size: 10
  cache-seconds: 30
```

- `top-size`: cantidad de posiciones TOP habilitadas (entre 1 y 100).
- `cache-seconds`: duración de la caché en segundos (mínimo 1).

## 🔌 Placeholders de PlaceholderAPI
Se registran únicamente si PlaceholderAPI está instalado.

| Placeholder | Descripción |
|---|---|
| `%deluxestats_kills%` | Kills personales. |
| `%deluxestats_deaths%` | Muertes personales. |
| `%deluxestats_ores%` | Minerales extraídos. |
| `%deluxestats_playtime%` | Tiempo activo formateado. |
| `%deluxestats_money%` | Saldo actual de EconomyAPI. |
| `%deluxestats_top_kills_<posición>_name%` | Nombre en el TOP de kills. |
| `%deluxestats_top_kills_<posición>_value%` | Valor en el TOP de kills. |
| `%deluxestats_top_deaths_<posición>_name%` | Nombre en el TOP de muertes. |
| `%deluxestats_top_deaths_<posición>_value%` | Valor en el TOP de muertes. |
| `%deluxestats_top_ores_<posición>_name%` | Nombre en el TOP de minería. |
| `%deluxestats_top_ores_<posición>_value%` | Valor en el TOP de minería. |
| `%deluxestats_top_playtime_<posición>_name%` | Nombre en el TOP de tiempo activo. |
| `%deluxestats_top_playtime_<posición>_value%` | Valor en el TOP de tiempo activo. |

Si no existe una posición, el nombre devuelve `-` y el valor `0`.

## 💾 Base de datos
DeluxeStats utiliza H2 y conserva las estadísticas entre reinicios. Las versiones anteriores podían crear tablas auxiliares de temporadas y antifarmeo de kills; esta versión ya no utiliza esos sistemas. No borres la base de datos al actualizar, ya que contiene las estadísticas existentes.

## 🛠️ Compilar
Coloca las dependencias locales en `libs/`:
```text
libs/
├── server.jar
└── economyapi-2.0.5.jar
```
Ejecuta `mvn clean package`. El JAR se genera en `target/`.

## 🐛 Reportar errores
Repositorio: https://github.com/BrunoJaegerK/DeluxeStats

**Hecho con 💜 para DELUXEMINE NETWORK.**  
Licencia: [Apache License 2.0](LICENSE).
