import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.writeText

val file = Path.of("uuids.txt").toFile()
val uuids = file.readLines()

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

val names = uuids.mapNotNull { uuid ->
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
            null
        }
    }
}

val nameCount = names.toSet().associateWith { name -> names.count { it == name } }

Path.of("uuids_out.txt").writeText(
    buildString {
        append("Username,Count")
        nameCount.forEach { (name, count) ->
            appendLine()
            append("$name,$count")
        }
    }
)
