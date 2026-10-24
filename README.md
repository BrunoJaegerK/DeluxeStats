# 💜 DeluxeStats

**Plugin de estadísticas, rankings y temporadas competitivas para servidores Minecraft Bedrock con Nukkit.**

DeluxeStats registra el progreso de los jugadores, permite consultar estadísticas PvP y de minería, organiza rankings competitivos y gestiona recompensas de temporada.

> 💜 **Creado con cariño para DELUXEMINE NETWORK.**
>
> Este proyecto nace con la intención de ofrecer una experiencia de supervivencia más competitiva, organizada y divertida. Cada función está pensada para reconocer el esfuerzo de los jugadores y motivarlos a seguir progresando.

---

## 📌 Índice

- [✨ Características](#-características)
- [📋 Requisitos](#-requisitos)
- [📥 Instalación](#-instalación)
- [🎮 Comandos](#-comandos)
- [📊 Estadísticas registradas](#-estadísticas-registradas)
- [🏆 Rankings](#-rankings)
- [🔌 Placeholders de PlaceholderAPI](#-placeholders-de-placeholderapi)
- [⚙️ Configuración](#️-configuración)
- [📅 Sistema de temporadas](#-sistema-de-temporadas)
- [🎁 Recompensas de temporada](#-recompensas-de-temporada)
- [💾 Base de datos y actualizaciones](#-base-de-datos-y-actualizaciones)
- [🛠️ Compilar desde el código fuente](#️-compilar-desde-el-código-fuente)
- [🐛 Reportar errores](#-reportar-errores)
- [💜 DELUXEMINE NETWORK](#-deluxemine-network)

---

## ✨ Características

### 📊 Estadísticas de jugadores

- ⚔️ Eliminaciones PvP.
- ☠️ Muertes.
- ⛏️ Minerales extraídos.
- ⏱️ Tiempo de juego activo.
- 🔥 Racha actual de eliminaciones.
- 🏅 Mejor racha de eliminaciones.
- 💰 Consulta del saldo mediante EconomyAPI.

### 🏆 Rankings competitivos

- Ranking de eliminaciones.
- Ranking de muertes.
- Ranking de minerales extraídos.
- Ranking de tiempo activo.
- Ranking de racha actual.
- Ranking de mejor racha.
- Consulta de los mejores jugadores mediante `/topstats`.
- Placeholders para mostrar rankings en interfaces compatibles.

### 📅 Sistema de temporadas

- Temporadas de 30 días.
- Registro de ganadores por categoría.
- Clasificación de jugadores por temporada.
- Recompensas configurables por posición.
- Registro de recompensas pendientes.
- Entrega de recompensas pendientes cuando el jugador vuelve a conectarse.

### 💾 Almacenamiento persistente

- Base de datos H2 local.
- Conservación de estadísticas entre reinicios.
- Almacenamiento de datos de temporadas, ganadores y recompensas.

### 🔌 Integraciones

- **EconomyAPI:** consulta del saldo del jugador.
- **PlaceholderAPI:** integración opcional para estadísticas personales y rankings TOP.

---

## 📋 Requisitos

Antes de instalar DeluxeStats, asegúrate de disponer de:

- Un servidor compatible con **NukkitPetteriM1Edition**.
- Java compatible con el servidor; el proyecto está configurado para compilar con Java 8.
- **EconomyAPI 2.0.5**.
- **PlaceholderAPI para Nukkit**, si quieres utilizar los placeholders.

> ⚠️ DeluxeStats está desarrollado para la API de Nukkit. No se garantiza su compatibilidad con Paper, Spigot, Bukkit u otras plataformas con APIs diferentes.

---

## 📥 Instalación

1. Descarga el JAR compilado de DeluxeStats desde la sección de [Releases](https://github.com/BrunoJaegerK/DeluxeStats/releases).
2. Coloca el archivo en la carpeta `plugins/` del servidor.
3. Comprueba que EconomyAPI esté instalado y funcione correctamente.
4. Instala PlaceholderAPI si necesitas utilizar los placeholders.
5. Inicia o reinicia el servidor.
6. Comprueba la consola para verificar que DeluxeStats se haya iniciado correctamente.
7. Revisa los archivos de configuración que genera el plugin dentro de `plugins/DeluxeStats/`.

Al iniciar, DeluxeStats inicializa la base de datos y crea la configuración de recompensas `seasons.yml` si todavía no existe.

### 🔄 Actualizar el plugin

1. Detén el servidor.
2. Realiza una copia de seguridad de la carpeta `plugins/DeluxeStats/`.
3. Sustituye el JAR antiguo por el nuevo.
4. Inicia el servidor y revisa los registros.

**Importante:** no elimines la base de datos para actualizar el plugin. Borrar ese archivo puede provocar la pérdida de estadísticas y del historial almacenado.

---

## 🎮 Comandos

### Comandos disponibles para consultar estadísticas

| Comando | Descripción |
|---|---|
| `/stats` | Consulta las estadísticas personales. |
| `/topstats kills` | Muestra el TOP 10 de eliminaciones. |
| `/topstats deaths` | Muestra el TOP 10 de muertes. |
| `/topstats ores` | Muestra el TOP 10 de minerales extraídos. |
| `/topstats playtime` | Muestra el TOP 10 de tiempo activo. |
| `/topstats streak` | Muestra el TOP 10 de mejores rachas. |

### Ejemplos

Consultar tus estadísticas:

```text
/stats
```

Consultar quiénes tienen más eliminaciones:

```text
/topstats kills
```

Consultar quiénes han extraído más minerales:

```text
/topstats ores
```

Consultar quiénes tienen más tiempo activo:

```text
/topstats playtime
```

Consultar las mejores rachas:

```text
/topstats streak
```

**Nota:** el comando `/topstats streak` ordena por `best_streak`, es decir, por la mejor racha registrada. Los rankings de este comando muestran hasta 10 jugadores.

---

## 📊 Estadísticas registradas

| Estadística | Campo interno | Descripción |
|---|---|---|
| Eliminaciones | `kills` | Eliminaciones PvP registradas. |
| Muertes | `deaths` | Muertes del jugador. |
| Minería | `ores_mined` | Minerales extraídos registrados. |
| Tiempo activo | `active_seconds` | Tiempo activo acumulado. |
| Racha actual | `current_streak` | Racha actual de eliminaciones. |
| Mejor racha | `best_streak` | Mejor racha de eliminaciones registrada. |
| Dinero | `money_earned` | Campo de estadísticas económicas almacenado por el sistema. |
| Saldo actual | EconomyAPI | Saldo consultado a través de EconomyAPI. |

### ⏱️ Registro del tiempo activo

DeluxeStats utiliza un sistema de seguimiento de actividad para evitar contabilizar indefinidamente el tiempo de jugadores inactivos.

El rastreador considera como actividad el movimiento, las interacciones, el chat y la rotura de bloques. El sistema utiliza un intervalo de 60 segundos desde la última actividad para determinar el tiempo que puede contabilizarse.

El tiempo activo se guarda periódicamente para reducir la pérdida de progreso ante interrupciones.

---

## 🏆 Rankings

DeluxeStats permite consultar diferentes categorías competitivas.

| Categoría | Identificador | Qué muestra |
|---|---|---|
| ⚔️ Eliminaciones | `kills` | Jugadores con más eliminaciones. |
| ☠️ Muertes | `deaths` | Jugadores con más muertes. |
| ⛏️ Minería | `ores` | Jugadores con más minerales extraídos. |
| ⏱️ Tiempo activo | `playtime` | Jugadores con más tiempo activo. |
| 🔥 Racha actual | `streak` | Racha actual de eliminaciones. |
| 🏅 Mejor racha | `best_streak` | Mejor racha registrada. |

Los rankings disponibles mediante PlaceholderAPI permiten consultar el nombre del jugador y el valor correspondiente a cada posición. El tamaño se controla mediante `rankings.top-size`.

---

## 🔌 Placeholders de PlaceholderAPI

DeluxeStats registra placeholders cuando **PlaceholderAPI está instalado**. Puedes utilizarlos en plugins compatibles con PlaceholderAPI para mostrar estadísticas, saldos y rankings en interfaces, mensajes u otros elementos que admitan placeholders.

### 📊 Estadísticas personales

| Placeholder | Descripción |
|---|---|
| `%deluxestats_kills%` | Eliminaciones del jugador consultado. |
| `%deluxestats_deaths%` | Muertes del jugador consultado. |
| `%deluxestats_ores%` | Minerales extraídos por el jugador. |
| `%deluxestats_playtime%` | Tiempo activo del jugador, con formato de tiempo. |
| `%deluxestats_streak%` | Racha actual de eliminaciones. |
| `%deluxestats_best_streak%` | Mejor racha de eliminaciones. |
| `%deluxestats_money%` | Saldo actual consultado mediante EconomyAPI. |

### 🏆 Placeholders TOP

Cada categoría proporciona dos tipos de placeholders:

- `_name`: devuelve el nombre del jugador en esa posición.
- `_value`: devuelve el valor estadístico de esa posición.

Sustituye `<posición>` por un número del 1 al tamaño configurado en `rankings.top-size`.

| Categoría | Nombre del jugador | Valor |
|---|---|---|
| ⚔️ Eliminaciones | `%deluxestats_top_kills_<posición>_name%` | `%deluxestats_top_kills_<posición>_value%` |
| ☠️ Muertes | `%deluxestats_top_deaths_<posición>_name%` | `%deluxestats_top_deaths_<posición>_value%` |
| ⛏️ Minerales | `%deluxestats_top_ores_<posición>_name%` | `%deluxestats_top_ores_<posición>_value%` |
| ⏱️ Tiempo activo | `%deluxestats_top_playtime_<posición>_name%` | `%deluxestats_top_playtime_<posición>_value%` |
| 🔥 Racha actual | `%deluxestats_top_streak_<posición>_name%` | `%deluxestats_top_streak_<posición>_value%` |
| 🏅 Mejor racha | `%deluxestats_top_best_streak_<posición>_name%` | `%deluxestats_top_best_streak_<posición>_value%` |

### 💡 Ejemplos de uso

**Mostrar al jugador con más eliminaciones:**

```text
%deluxestats_top_kills_1_name%
```

**Mostrar sus eliminaciones:**

```text
%deluxestats_top_kills_1_value%
```

**Mostrar el jugador que más minerales ha extraído:**

```text
%deluxestats_top_ores_1_name%
```

**Mostrar el tiempo activo del primer puesto:**

```text
%deluxestats_top_playtime_1_value%
```

**Mostrar las estadísticas personales de un jugador:**

```text
Eliminaciones: %deluxestats_kills%
Muertes: %deluxestats_deaths%
Minerales: %deluxestats_ores%
Tiempo activo: %deluxestats_playtime%
Racha actual: %deluxestats_streak%
Mejor racha: %deluxestats_best_streak%
Saldo: %deluxestats_money%
```

### 📋 Valores cuando no hay resultados

Si la posición solicitada no tiene un jugador disponible:

- El placeholder `_name` devuelve `-`.
- El placeholder `_value` devuelve `0`.

Los valores TOP de tiempo activo se devuelven con formato de tiempo. Las estadísticas personales de tiempo activo también utilizan un formato de tiempo.

### ⚙️ Configuración de rankings y caché

Los rankings TOP se configuran en `plugins/DeluxeStats/config.yml`:

```yaml
# Configuración de los rankings de DeluxeStats
rankings:
  # Cantidad máxima de posiciones TOP disponibles.
  top-size: 10

  # Tiempo de caché de los rankings, en segundos.
  cache-seconds: 30
```

| Opción | Valor predeterminado | Descripción |
|---|---:|---|
| `top-size` | `10` | Número de posiciones disponibles para cada categoría TOP. El código limita el valor al rango de 1 a 100. |
| `cache-seconds` | `30` | Tiempo durante el cual se reutilizan los resultados TOP en caché. El mínimo aplicado es de 1 segundo. |

**Importante:** si PlaceholderAPI no está instalado, DeluxeStats muestra una advertencia en la consola y no registra los placeholders.

---

## ⚙️ Configuración

### Archivo `config.yml`

El archivo de configuración controla los rankings TOP:

```yaml
rankings:
  top-size: 10
  cache-seconds: 30
```

Ajusta `top-size` según el número de jugadores que quieras mostrar. Un valor de `10` permite consultar las posiciones del 1 al 10 mediante los placeholders TOP.

El valor `cache-seconds` controla durante cuánto tiempo se reutilizan los resultados almacenados en caché.

### Archivo `seasons.yml`

Este archivo contiene los ajustes de las recompensas de temporada.

El siguiente ejemplo corresponde a la estructura utilizada por el recurso de configuración del proyecto:

```yaml
settings:
  # Número de jugadores premiados por categoría.
  top-size: 10

  # Mensaje enviado al jugador cuando recibe una recompensa.
  reward-message: "§a¡Has recibido una recompensa de temporada!"

rewards:
  kills:
    "1":
      commands:
        - "give {player} diamond 10"
    "2":
      commands:
        - "give {player} diamond 5"
    "3":
      commands:
        - "give {player} diamond 3"

  ores_mined:
    "1":
      commands:
        - "give {player} emerald 10"
    "2":
      commands:
        - "give {player} emerald 5"
    "3":
      commands:
        - "give {player} emerald 3"

  active_seconds:
    "1":
      commands:
        - "give {player} gold_ingot 10"
    "2":
      commands:
        - "give {player} gold_ingot 5"
    "3":
      commands:
        - "give {player} gold_ingot 3"
```

> ⚠️ Los comandos de recompensa son ejemplos. Comprueba que los nombres de los objetos y los comandos sean compatibles con tu versión de Nukkit y los plugins instalados.

---

## 📅 Sistema de temporadas

DeluxeStats organiza la competición en temporadas de **30 días**.

Las categorías utilizadas para registrar los ganadores de temporada son:

| Categoría | Identificador interno |
|---|---|
| ⚔️ Eliminaciones | `kills` |
| ⛏️ Minerales extraídos | `ores_mined` |
| ⏱️ Tiempo activo | `active_seconds` |

Cuando finaliza una temporada, el sistema procesa las clasificaciones, registra a los ganadores y prepara las recompensas configuradas para las posiciones correspondientes.

Las recompensas pendientes se guardan en la base de datos y pueden entregarse cuando el jugador vuelve a conectarse.

### 🎁 Cómo funcionan las recompensas

- `settings.top-size` establece cuántos puestos se procesan por categoría.
- `rewards` contiene la configuración de premios.
- `kills`, `ores_mined` y `active_seconds` identifican las categorías premiadas.
- `"1"`, `"2"` y `"3"` representan las posiciones.
- `commands` contiene los comandos que se ejecutarán para entregar cada premio.
- `{player}` se sustituye por el nombre del jugador destinatario.

Puedes personalizar los comandos para entregar objetos, dinero u otras recompensas compatibles con los comandos disponibles en tu servidor.

**Recomendación:** haz una copia de seguridad de `seasons.yml` antes de modificarlo. Utiliza una configuración YAML válida para evitar errores al cargar las recompensas.

---

## 💾 Base de datos y actualizaciones

DeluxeStats utiliza **H2** como base de datos local.

El archivo suele encontrarse en:

```text
plugins/DeluxeStats/deluxestats.mv.db
```

La base de datos conserva la información de los jugadores, las estadísticas, las temporadas, los ganadores y las recompensas pendientes.

### 🔐 Buenas prácticas

- Realiza copias de seguridad periódicas.
- Detén el servidor antes de actualizar el JAR.
- No elimines la base de datos para intentar solucionar errores.
- No publiques archivos de base de datos de tu servidor.
- Evita editar manualmente los datos almacenados sin conocer su estructura.

---

## 🛠️ Compilar desde el código fuente

El proyecto utiliza Maven y está configurado para compilar con Java 8.

### Requisitos de compilación

- IntelliJ IDEA u otro IDE compatible con Maven.
- JDK compatible con Java 8.
- El JAR de NukkitPetteriM1Edition utilizado para desarrollar el plugin.
- EconomyAPI 2.0.5.
- Acceso al repositorio de dependencias Maven requerido por PlaceholderAPI.

### Dependencias locales

El proyecto espera encontrar las dependencias locales en la carpeta `libs/`:

```text
libs/
├── server.jar
└── economyapi-2.0.5.jar
```

Comprueba que los nombres y las rutas coincidan con los definidos en el `pom.xml`.

### Pasos de compilación

1. Clona o descarga el repositorio.
2. Abre el proyecto en IntelliJ IDEA.
3. Configura el JDK correspondiente.
4. Coloca los JAR locales requeridos en `libs/`.
5. Recarga el proyecto Maven.
6. Ejecuta:

```bash
mvn clean package
```

Si la compilación finaliza correctamente, el JAR se encontrará en `target/`.

**Nota:** las dependencias locales del servidor y EconomyAPI no necesariamente se distribuyen junto al código fuente. Asegúrate de disponer de ellas antes de compilar.

---

## 🐛 Reportar errores y sugerir mejoras

¿Encontraste un error o tienes una idea para mejorar DeluxeStats?

Puedes comunicar problemas, proponer funciones o consultar los cambios del proyecto en GitHub.

🔗 **Repositorio oficial:**  
https://github.com/BrunoJaegerK/DeluxeStats

Al reportar un error, incluye cuando sea posible:

- La versión de NukkitPetteriM1Edition.
- La versión del JAR de DeluxeStats.
- La versión de EconomyAPI y PlaceholderAPI, si están instalados.
- Los pasos para reproducir el problema.
- El mensaje completo de la consola.
- Los archivos de configuración relevantes, ocultando cualquier dato privado.

---

## 💜 DELUXEMINE NETWORK

DeluxeStats fue creado con cariño para **DELUXEMINE NETWORK**, con el objetivo de ofrecer una experiencia de supervivencia más competitiva, entretenida y organizada.

Gracias a todos los jugadores y colaboradores que forman parte de la comunidad. Sus ideas y sugerencias ayudan a que el proyecto siga creciendo.

**Hecho con 💜 para DELUXEMINE NETWORK.**

---

*DeluxeStats — estadísticas, competición y progreso para tu aventura.*

