package scripts

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.UUID

// setup

println("Analyzing old experience files...")

val playerDataDirectory = Path.of("player_data").toFile()
val persistentDirectory: File = playerDataDirectory.resolve("persistent")

val usercacheFile = Path.of("usercache.json").toFile()
val usercacheJson = usercacheFile.reader().use(JsonParser::parseReader).asJsonObject

val experienceMap = mutableMapOf<String, Int>()

// create map
println("Walking persistent directory")

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
        val experience = json["experience"].asInt
        experienceMap[name] = (experienceMap[name] ?: 0) + experience
    } catch (_: Exception) {
    }
}

// print
val sortedExperienceMap = experienceMap.entries.sortedBy { it.value }.reversed()
// println(sortedExperienceMap.joinToString(transform = { "${it.key}: ${it.value}" }, separator = "\n"))

// write to file
println("Writing to file")

val statsCsvFile = playerDataDirectory.resolve("experience-statistics.csv")
statsCsvFile.writeText(buildString {
    appendLine("Username,Experience,Level")
    sortedExperienceMap.forEach { (username, experience) -> appendLine("$username,$experience,${experience / 100}") }
})

println("Written to file")
