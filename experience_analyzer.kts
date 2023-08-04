import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.UUID
import kotlin.math.exp

// setup

println("Analyzing experience files...")

val playerDataDirectory = Path.of("player_data").toFile()
val persistentDirectory: File = playerDataDirectory.resolve("experience")

val usercacheFile = Path.of("usercache.json").toFile()
val usercacheJson = usercacheFile.reader().use(JsonParser::parseReader).asJsonObject

data class ExperienceData(
    val totalExperience: Int,
    val levelExperience: Int,
    val level: Int
)

val experienceMap = mutableMapOf<String, ExperienceData>()

// create map
println("Walking experience directory")

persistentDirectory.walk().forEach { file ->
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
                    println("Fetched username of $uuid as $name")
                    usercacheJson.addProperty(uuid, name)
                    usercacheFile.writeText(Gson().toJson(usercacheJson))
                    name
                } catch (_: Exception) {
                    "Unknown"
                }
            }

        val json = file.reader().use(JsonParser::parseReader) as JsonObject
        val experience = ExperienceData(json["total_experience"].asInt, json["level_experience"].asInt, json["level"].asInt + 1)
        experienceMap[name] = experience
    } catch (_: Exception) {
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
