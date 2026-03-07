package com.soogoino.huga.data.model

import com.akuleshov7.ktoml.Toml
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.common.FlowStyle

private val YAML_PATTERN = Regex("""^\s*---\s*\n(.*?)\n---\s*\n?(.*)""", setOf(RegexOption.DOT_MATCHES_ALL))
private val TOML_PATTERN = Regex("""^\s*\+\+\+\s*\n(.*?)\n\+\+\+\s*\n?(.*)""", setOf(RegexOption.DOT_MATCHES_ALL))

object FrontMatterParser {

    private val yamlLoader = Load(LoadSettings.builder().build())
    private val yamlDumper = Dump(
        DumpSettings.builder()
            .setDefaultFlowStyle(FlowStyle.BLOCK)  // multi-line block style, not inline {}
            .setIndent(4)                           // 4-space indent for list items
            .setIndentWithIndicator(true)           // include '-' within the indent count
            .build()
    )

    /** Parse a raw Markdown file string into a [HugoFrontMatter] and body. */
    fun parse(raw: String): Triple<HugoFrontMatter, FrontMatterFormat, String> {
        YAML_PATTERN.find(raw)?.let { match ->
            val fmStr = match.groupValues[1]
            val body = stripLeadingFrontMatter(match.groupValues[2])
            return Triple(parseYaml(fmStr), FrontMatterFormat.YAML, body)
        }
        TOML_PATTERN.find(raw)?.let { match ->
            val fmStr = match.groupValues[1]
            val body = stripLeadingFrontMatter(match.groupValues[2])
            return Triple(parseToml(fmStr), FrontMatterFormat.TOML, body)
        }
        // No front matter found – return defaults
        return Triple(HugoFrontMatter(), FrontMatterFormat.YAML, raw)
    }

    /**
     * Strips any leading front-matter blocks (--- ... --- or +++ ... +++) from [body].
     * Public so ViewModel can use it when inserting a new FM block on corrupted content.
     */
    fun stripLeadingFrontMatter(body: String): String {
        var current = body
        while (true) {
            val trimmed = current.trimStart()
            val delimiter = when {
                trimmed.startsWith("---") -> "---"
                trimmed.startsWith("+++") -> "+++"
                else -> break
            }
            val afterOpen = trimmed.indexOf('\n') + 1
            if (afterOpen <= 0) break
            val closeIdx = trimmed.indexOf("\n$delimiter", afterOpen)
            if (closeIdx < 0) break
            var end = closeIdx + 1 + delimiter.length
            if (end < trimmed.length && trimmed[end] == '\n') end++
            current = trimmed.substring(end)
        }
        return current
    }

    /** Serialise a [HugoFrontMatter] + body back into a raw Markdown string. */
    fun serialise(fm: HugoFrontMatter, format: FrontMatterFormat, body: String): String {
        val fmStr = when (format) {
            FrontMatterFormat.YAML -> toYaml(fm)
            FrontMatterFormat.TOML -> toToml(fm)
        }
        return when (format) {
            FrontMatterFormat.YAML -> "---\n${fmStr}---\n$body"
            FrontMatterFormat.TOML -> "+++\n${fmStr}+++\n$body"
        }
    }

    /**
     * Returns the character offset immediately after the closing front-matter delimiter
     * (`---` or `+++`) in [raw], so the cursor can be placed at the start of the body.
     * Returns 0 if no front matter is found.
     */
    fun bodyStartOffset(raw: String): Int {
        val trimmed = raw.trimStart()
        val trimOffset = raw.length - trimmed.length
        val delimiter = when {
            trimmed.startsWith("---") -> "---"
            trimmed.startsWith("+++") -> "+++"
            else -> return 0
        }
        val afterOpen = trimmed.indexOf('\n') + 1
        if (afterOpen <= 0) return 0
        val closeIdx = trimmed.indexOf("\n$delimiter", afterOpen)
        if (closeIdx < 0) return 0
        var end = closeIdx + 1 + delimiter.length   // past \n + delimiter
        if (end < trimmed.length && trimmed[end] == '\n') end++  // skip trailing newline
        return trimOffset + end
    }

    // ─── YAML ────────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun parseYaml(yaml: String): HugoFrontMatter {
        val map = runCatching {
            yamlLoader.loadFromString(yaml) as? Map<String, Any>
        }.getOrNull() ?: return HugoFrontMatter()

        val knownKeys = setOf("title", "date", "draft", "tags", "categories", "description", "slug", "weight", "cover")
        val extra = map.filterKeys { it !in knownKeys }

        // cover can be a plain string or a map { image: "path" } (Hugo page params style)
        val coverImage = when (val cover = map["cover"]) {
            is String -> cover
            is Map<*, *> -> cover["image"]?.toString() ?: ""
            else -> ""
        }

        return HugoFrontMatter(
            title = map["title"]?.toString() ?: "",
            date = map["date"]?.toString() ?: "",
            draft = map["draft"] as? Boolean ?: false,
            // map() always creates a new list, so tags and categories are never the same
            // object even if the YAML source used an alias (*id) to share the same node.
            tags = (map["tags"] as? List<*>)?.map { it.toString() } ?: emptyList(),
            categories = (map["categories"] as? List<*>)?.map { it.toString() } ?: emptyList(),
            description = map["description"]?.toString() ?: "",
            slug = map["slug"]?.toString() ?: "",
            weight = (map["weight"] as? Number)?.toInt(),
            coverImage = coverImage,
            extra = extra,
        )
    }

    private fun toYaml(fm: HugoFrontMatter): String {
        val map = linkedMapOf<String, Any>()
        map["title"] = fm.title
        if (fm.date.isNotBlank()) map["date"] = fm.date
        map["draft"] = fm.draft
        if (fm.slug.isNotBlank()) map["slug"] = fm.slug
        // Use ArrayList() to ensure tags and categories are always distinct list instances.
        // emptyList() is a Kotlin singleton; if both fields share the same reference, SnakeYAML
        // emits YAML anchors (&id001 / *id001) which corrupt subsequent round-trips.
        map["tags"] = ArrayList(fm.tags)
        map["categories"] = ArrayList(fm.categories)
        map["description"] = fm.description
        fm.weight?.let { map["weight"] = it }
        if (fm.coverImage.isNotBlank()) map["cover"] = fm.coverImage
        fm.extra.forEach { (k, v) -> map[k] = v }
        return yamlDumper.dumpToString(map)
    }

    // ─── TOML ────────────────────────────────────────────────────────────────

    private fun parseToml(toml: String): HugoFrontMatter {
        // Manual TOML parsing via line scanning (ktoml requires @Serializable data classes;
        // we use a simple line-based approach for flexibility with unknown fields)
        val map = mutableMapOf<String, Any>()
        val listAccumulators = mutableMapOf<String, MutableList<String>>()
        var currentList: String? = null

        for (line in toml.lines()) {
            val trimmed = line.trim()
            when {
                // Start of array
                trimmed.matches(Regex("""(\w+)\s*=\s*\[""")) -> {
                    val key = trimmed.substringBefore("=").trim()
                    currentList = key
                    listAccumulators[key] = mutableListOf()
                    // Check if single-line array
                    val inline = trimmed.substringAfter("[").substringBefore("]")
                    if (trimmed.contains("]")) {
                        map[key] = inline.split(",").map { it.trim().trim('"') }.filter { it.isNotEmpty() }
                        currentList = null
                    }
                }
                // End of multi-line array
                trimmed == "]" && currentList != null -> {
                    map[currentList!!] = listAccumulators[currentList].orEmpty()
                    currentList = null
                }
                // Array item
                currentList != null && trimmed.startsWith("\"") -> {
                    listAccumulators[currentList]?.add(trimmed.trim('"', ',', ' '))
                }
                // Key = value
                trimmed.contains("=") && currentList == null -> {
                    val key = trimmed.substringBefore("=").trim()
                    val value = trimmed.substringAfter("=").trim()
                    map[key] = when {
                        value == "true" -> true
                        value == "false" -> false
                        value.startsWith("\"") -> value.trim('"')
                        value.toIntOrNull() != null -> value.toInt()
                        else -> value
                    }
                }
            }
        }

        val knownKeys = setOf("title", "date", "draft", "tags", "categories", "description", "slug", "weight", "cover")
        return HugoFrontMatter(
            title = map["title"]?.toString() ?: "",
            date = map["date"]?.toString() ?: "",
            draft = map["draft"] as? Boolean ?: false,
            tags = (map["tags"] as? List<*>)?.map { it.toString() } ?: emptyList(),
            categories = (map["categories"] as? List<*>)?.map { it.toString() } ?: emptyList(),
            description = map["description"]?.toString() ?: "",
            slug = map["slug"]?.toString() ?: "",
            weight = (map["weight"] as? Number)?.toInt(),
            coverImage = map["cover"]?.toString() ?: "",
            extra = map.filterKeys { it !in knownKeys },
        )
    }

    private fun toToml(fm: HugoFrontMatter): String {
        val sb = StringBuilder()
        sb.appendLine("""title = "${fm.title}"""")
        if (fm.date.isNotBlank()) sb.appendLine("""date = "${fm.date}"""")
        sb.appendLine("draft = ${fm.draft}")
        if (fm.slug.isNotBlank()) sb.appendLine("""slug = "${fm.slug}"""")
        // Always emit all common fields
        val tagsStr = fm.tags.joinToString(", ") { "\"$it\"" }
        sb.appendLine("tags = [$tagsStr]")
        val catStr = fm.categories.joinToString(", ") { "\"$it\"" }
        sb.appendLine("categories = [$catStr]")
        sb.appendLine("""description = "${fm.description}"""")
        fm.weight?.let { sb.appendLine("weight = $it") }
        if (fm.coverImage.isNotBlank()) sb.appendLine("""cover = "${fm.coverImage}"""")
        fm.extra.forEach { (k, v) ->
            when (v) {
                is String  -> sb.appendLine("""$k = "$v"""")
                is Boolean -> sb.appendLine("$k = $v")
                is Number  -> sb.appendLine("$k = $v")
                else       -> sb.appendLine("""$k = "${v}"""")
            }
        }
        return sb.toString()
    }
}
