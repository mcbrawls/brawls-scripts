package scripts

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.UUID
import kotlin.time.measureTimedValue

// setup

println("Analyzing experience files...")

val playerDataDirectory = Path.of("player_data").toFile()
val experienceDirectory: File = playerDataDirectory.resolve("experience")

val usercacheFile = Path.of("usercache.json").toFile()

fun parseUsercacheJson(): JsonObject {
    return try {
        usercacheFile.reader().use(JsonParser::parseReader).asJsonObject
    } catch (exception: Exception) {
        exception.printStackTrace()
        usercacheFile.writeText("{}")
        parseUsercacheJson()
    }
}
val usercacheJson = parseUsercacheJson()

data class ExperienceData(
    val totalExperience: Int,
    val levelExperience: Int,
    val level: Int
)

val experienceMap = mutableMapOf<String, ExperienceData>()

// create map
println("Walking experience directory")

val files: Array<File> = experienceDirectory.listFiles()!!
val size = files.size
var runningAverageMs = -1L
files.forEachIndexed { index, file ->
    val executionTime = measureTimedValue {
        try {
            val uuid: String = UUID.fromString(file.nameWithoutExtension).toString()
            val name =
                if (usercacheJson.has(uuid)) {
                    // get username from cache
                    usercacheJson[uuid].asString
                } else {
                    try {
                        // lookup username
                        val lookup = URI("https://playerdb.co/api/player/minecraft/$uuid").toURL().readText()
                        val lookupJson = JsonParser.parseString(lookup) as JsonObject
                        val name = lookupJson["data"].asJsonObject["player"].asJsonObject["username"].asString
                        println("Fetched username of $uuid as $name ($index/$size)")
                        usercacheJson.addProperty(uuid, name)
                        usercacheFile.writeText(Gson().toJson(usercacheJson))
                        name
                    } catch (_: Exception) {
                        "Unknown"
                    }
                }

            val json = file.reader().use(JsonParser::parseReader) as JsonObject
            val experience =
                ExperienceData(json["total_experience"].asInt, json["level_experience"].asInt, json["level"].asInt + 1)
            experienceMap[name] = experience
        } catch (_: Exception) {
        }
    }

    val executionTimeMs = executionTime.duration.inWholeMilliseconds
    runningAverageMs = if (runningAverageMs == -1L) {
        executionTimeMs
    } else {
        (runningAverageMs + executionTimeMs) / 2
    }

    if (index % 100 == 0) {
        val remaining = size - index
        val timeRemaining = (runningAverageMs * remaining) / 1000

        if (timeRemaining != 0L) {
            println("Time remaining: ${timeRemaining / 60}m ${timeRemaining % 60}s")
        }
    }
}

// print
val sortedExperienceMap = experienceMap.entries.sortedBy { it.value.totalExperience }.reversed()

// write to file
println("Writing to file")

val statsCsvFile = playerDataDirectory.resolve("experience-statistics.csv")
statsCsvFile.writeText(buildString {
    appendLine("Username,Total Experience,Level Experience,Level")
    sortedExperienceMap.forEach { (username, experience) -> appendLine("$username,${experience.totalExperience},${experience.levelExperience},${experience.level}") }
})

println("Written to file")
