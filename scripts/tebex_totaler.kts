package scripts

import java.nio.file.Path

data class User(val id: String, val username: String) {
    override fun equals(other: Any?): Boolean {
        return other is User && other.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

val data = mutableMapOf<User, Double>()

val file = Path.of("tebex.csv").toFile()
file.readLines().forEachIndexed { index, line ->
    if (index == 0) {
        return@forEachIndexed
    }

    val datas = line.split(",")
    val username = datas[0]
    val uuid = datas[1]
    val paid = try {
        datas[2].toDouble()
    } catch (exception: NumberFormatException) {
        0.0
    }

    val user = User(uuid, username)
    val previousAmount = data[user] ?: 0.0
    data[user] = previousAmount + paid
}

val out = buildString {
    data.entries.sortedWith { o1, o2 -> o2.value.compareTo(o1.value) }.forEachIndexed { index, (user, amount) ->
        val placement = index + 1
        val username = user.username
        append("$placement. $username: $amount")
        appendLine()
    }
}

Path.of("tebex_out.txt").toFile().writeText(out)
