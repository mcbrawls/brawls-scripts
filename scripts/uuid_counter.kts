import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.readLines
import kotlin.io.path.writeText

val file: Path = Path.of("data/uuids.txt")
val lines = file.readLines()
val uuids = lines.map {
    try {
        val str = it.filter { char -> "[a-z0-9-]+".toRegex().matches("$char") }
        UUID.fromString(str)
    } catch (exception: IllegalArgumentException) {
        println(it)
        println(it[0].code)
        exception.printStackTrace()
        throw exception
    }
}
val counts = uuids.toSet().associateWith { uuid -> uuids.count { it == uuid } }
val sortedCounts = counts
    .entries
    .sortedWith { o1, o2 -> o2.value - o1.value }
println(sortedCounts)

val csv = buildString {
    append("Username,Count")

    sortedCounts.forEach { (uuid, count) ->
        val name = try {
            // lookup username
            val lookup = URI("https://playerdb.co/api/player/minecraft/$uuid").toURL().readText()
            val lookupJson = JsonParser.parseString(lookup) as JsonObject
            val name = lookupJson["data"].asJsonObject["player"].asJsonObject["username"].asString
            println("Fetched username of $uuid as $name")
            name
        } catch (_: Exception) {
            "Unknown"
        }

        appendLine()
        append("$name,$count")
    }
}

Path.of("data/uuids_out.csv").writeText(csv)
