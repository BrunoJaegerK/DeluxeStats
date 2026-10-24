# 💜 DeluxeStats

**Un plugin de estadísticas, rankings y temporadas para servidores Nukkit.**

DeluxeStats permite registrar las estadísticas de los jugadores, consultar su progreso y organizar temporadas competitivas con recompensas configurables.

> 💜 **Creado con cariño para DELUXEMINE NETWORK.**
>
> Este proyecto nace con la intención de ofrecer una experiencia de supervivencia más competitiva, organizada y divertida para la comunidad de DELUXEMINE NETWORK. Cada función está pensada para reconocer el esfuerzo de los jugadores y motivarlos a seguir progresando.

---

## ✨ Características principales

📊 **Estadísticas de jugadores**
- Eliminaciones (`kills`).
- Muertes (`deaths`).
- Minerales extraídos (`ores_mined`).
- Dinero obtenido (`money_earned`).
- Racha actual de eliminaciones (`current_streak`).
- Mejor racha de eliminaciones (`best_streak`).
- Tiempo de juego activo (`active_seconds`).

🏆 **Rankings competitivos**
- Ranking por eliminaciones.
- Ranking por minerales extraídos.
- Ranking por tiempo de juego activo.

📅 **Sistema de temporadas**
- Temporadas de 30 días.
- Registro de los ganadores por categoría.
- Recompensas configurables según la posición alcanzada.
- Preparación de la siguiente temporada al finalizar la anterior.

🎁 **Recompensas automáticas**
- Ejecución de comandos de consola configurados por el administrador.
- Registro de las recompensas pendientes.
- Entrega de recompensas pendientes cuando el jugador vuelve a entrar al servidor.

💾 **Persistencia de datos**
- Base de datos H2 local.
- Conservación de las estadísticas entre reinicios.
- Almacenamiento de información de temporadas, ganadores y recompensas.

💰 **Integración económica**
- Integración con EconomyAPI para consultar el saldo del jugador.

🔌 **Integración opcional**
- Compatibilidad con PlaceholderAPI, si está instalada y configurada correctamente.

---

## 📋 Requisitos

Antes de instalar DeluxeStats, asegúrate de disponer de:

- Un servidor compatible con **NukkitPetteriM1Edition**.
- Java 8 o un entorno compatible con la versión de Java utilizada por tu servidor.
- **EconomyAPI 2.0.5**.
- Los archivos de dependencias necesarios para compilar el proyecto desde el código fuente.

> ⚠️ DeluxeStats está desarrollado para el ecosistema Nukkit. No se garantiza compatibilidad con Paper, Spigot u otros servidores que utilicen APIs diferentes.

---

## 📥 Instalación

1. Descarga el archivo JAR compilado de DeluxeStats.
2. Colócalo en la carpeta `plugins/` de tu servidor.
3. Comprueba que EconomyAPI esté instalado y funcione correctamente.
4. Inicia el servidor para que el plugin cree sus archivos y su base de datos.
5. Revisa la configuración de temporadas en `plugins/DeluxeStats/seasons.yml`, si el archivo se genera en tu versión.
6. Configura las recompensas que quieras entregar a los mejores jugadores.
7. Reinicia el servidor para aplicar los cambios.

**Importante:** conserva la base de datos existente al actualizar el plugin. Haz una copia de seguridad antes de reemplazar archivos o modificar configuraciones.

---

## 🛠️ Compilar desde el código fuente

Puedes abrir el proyecto en IntelliJ IDEA y compilarlo con Maven.

### Pasos

1. Clona o descarga el repositorio.
2. Abre el proyecto desde IntelliJ IDEA.
3. Configura un JDK compatible con Java 8.
4. Comprueba que las dependencias locales requeridas estén disponibles.
5. Ejecuta la tarea Maven `clean package`.

También puedes ejecutar el siguiente comando desde una terminal en la carpeta del proyecto:

```bash
mvn clean package
```

Si Maven está instalado y configurado correctamente, el JAR compilado debería aparecer dentro de `target/`.

### Dependencias locales

El proyecto utiliza archivos JAR locales para las APIs del servidor y EconomyAPI. Revisa el `pom.xml` para conocer sus rutas y nombres exactos.

Por motivos de seguridad y distribución, no incluyas en el repositorio bases de datos de servidores, archivos privados del servidor ni dependencias cuya redistribución no esté autorizada.

---

## 🏅 Sistema de temporadas

DeluxeStats organiza la competición en temporadas de **30 días**.

Las categorías contempladas son:

| Categoría | Identificador | Descripción |
|---|---|---|
| ⚔️ Eliminaciones | `kills` | Eliminaciones registradas |
| ⛏️ Minerales | `ores_mined` | Minerales extraídos |
| ⏱️ Tiempo activo | `active_seconds` | Tiempo de juego activo registrado |

Al finalizar una temporada, el plugin registra a los ganadores y prepara las recompensas correspondientes según la configuración.

### 🎁 Configuración de recompensas

Las recompensas se configuran mediante comandos de consola. El siguiente ejemplo ilustra cómo podrías organizar los premios para las primeras posiciones:

```yaml
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

**Cómo funciona:**

- `kills`: define premios para el ranking de eliminaciones.
- `ores_mined`: define premios para el ranking de minería.
- `active_seconds`: define premios para el ranking de tiempo activo.
- `"1"`, `"2"` y `"3"` representan las posiciones premiadas.
- `commands`: contiene los comandos que ejecutará la consola al procesar la recompensa.
- `{player}` representa el nombre del jugador que recibirá el premio.

Puedes personalizar los premios para adaptarlos a la economía y al estilo de tu servidor.

> ⚠️ Este bloque es un ejemplo de estructura. Antes de copiarlo directamente, compáralo con el `seasons.yml` real generado por tu versión de DeluxeStats. Los nombres de los objetos y la sintaxis de los comandos pueden variar según la versión del servidor y los plugins instalados.

---

## 📊 Consultar estadísticas

Los jugadores pueden utilizar:

```text
/stats
```

Este comando permite consultar las estadísticas personales registradas por DeluxeStats, como eliminaciones, muertes, minería, saldo, rachas y tiempo de juego activo.

El proyecto también incluye una implementación separada para los rankings. Consulta los comandos registrados en `plugin.yml` para conocer el nombre exacto del comando disponible en tu compilación.

---

## 💾 Almacenamiento de datos

DeluxeStats utiliza una base de datos H2 local. El archivo de datos se encuentra normalmente en:

```text
plugins/DeluxeStats/deluxestats.mv.db
```

La base de datos almacena información de estadísticas, temporadas, ganadores y recompensas.

**Recomendaciones:**
- Realiza copias de seguridad periódicas.
- No elimines el archivo de base de datos para intentar solucionar errores.
- Antes de actualizar el plugin, detén el servidor y respalda los datos.
- Evita modificar la base de datos manualmente sin conocer su estructura.

---

## 🐛 Reportar errores y sugerir mejoras

¿Encontraste un error o tienes una idea para mejorar DeluxeStats?

Puedes utilizar el repositorio de GitHub para comunicar problemas, proponer funciones o compartir sugerencias.

🔗 **Repositorio oficial:**  
https://github.com/BrunoJaegerK/DeluxeStats

Al reportar un error, incluye cuando sea posible:
- La versión del servidor.
- La versión de DeluxeStats.
- Los pasos necesarios para reproducir el problema.
- El mensaje completo de la consola.
- La configuración relacionada con el error, ocultando cualquier información privada.

---

## 💜 Un proyecto para DELUXEMINE NETWORK

DeluxeStats fue creado con cariño para **DELUXEMINE NETWORK**, con el objetivo de aportar una experiencia de juego más completa, competitiva y entretenida.

Gracias a todos los jugadores y colaboradores que forman parte de la comunidad. Sus ideas y sugerencias ayudan a que el proyecto siga creciendo.

**Hecho con 💜 para DELUXEMINE NETWORK.**

---

*DeluxeStats — estadísticas, competición y progreso para tu aventura.*
