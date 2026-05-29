import java.io.File

fun consolidateReleaseNotes(bodies: List<String>): String {
    val sections = mutableMapOf<String, MutableList<String>>()
    val sectionOrder = mutableListOf<String>()
    
    for (body in bodies) {
        var currentHeading = ""
        for (line in body.lines()) {
            val headingMatch = Regex("^#+\\s+.*").matches(line)
            if (headingMatch) {
                currentHeading = line.trim()
                if (!sectionOrder.contains(currentHeading)) {
                    sectionOrder.add(currentHeading)
                    sections[currentHeading] = mutableListOf()
                }
            } else {
                if (!sectionOrder.contains(currentHeading)) {
                    sectionOrder.add(currentHeading)
                    sections[currentHeading] = mutableListOf()
                }
                if (line.trim().isEmpty() && sections[currentHeading]!!.isEmpty()) {
                    continue
                }
                sections[currentHeading]!!.add(line)
            }
        }
    }

    val result = java.lang.StringBuilder()
    for (heading in sectionOrder) {
        if (heading.isNotEmpty()) {
            result.appendLine(heading)
        }
        val contentLines = sections[heading]!!
        var lastNonEmpty = contentLines.indexOfLast { it.trim().isNotEmpty() }
        if (lastNonEmpty != -1) {
            for (i in 0..lastNonEmpty) {
                result.appendLine(contentLines[i])
            }
        }
        if (heading.isNotEmpty() || lastNonEmpty != -1) {
             result.appendLine()
        }
    }
    
    return result.toString().trimEnd()
}

val b1 = """
### ✨ Features
- feature 1
### 🐛 Bug Fixes
- fix 1
"""

val b2 = """
### ✨ Features
- feature 2
### 🐛 Bug Fixes
- fix 2
### 📝 Documentation
- doc 1
"""

println(consolidateReleaseNotes(listOf(b1, b2)))
