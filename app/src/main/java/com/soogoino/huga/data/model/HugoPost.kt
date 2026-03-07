package com.soogoino.huga.data.model

/**
 * Represents the parsed front matter + body of a Hugo post.
 * Supports both YAML (---) and TOML (+++) delimiters.
 */
data class HugoFrontMatter(
    val title: String = "",
    val date: String = "",          // ISO-8601 e.g. "2026-03-06T12:00:00+08:00"
    val draft: Boolean = false,
    val tags: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val description: String = "",
    val slug: String = "",
    val weight: Int? = null,
    val coverImage: String = "",     // Hugo cover field (plain string path)
    val extra: Map<String, Any> = emptyMap(), // catch-all for custom fields
)

enum class FrontMatterFormat { YAML, TOML }

/**
 * Domain representation of a Hugo post file on disk.
 */
data class HugoPost(
    /** Absolute path to the file (either index.md or a flat .md file). */
    val filePath: String,
    /** Relative path from the content root, used as display key. */
    val relativePath: String,
    /** Folder slug – the page bundle folder name, or file stem for flat posts. */
    val slug: String,
    val frontMatter: HugoFrontMatter,
    val frontMatterFormat: FrontMatterFormat,
    val bodyMarkdown: String,
    val lastModified: Long, // epoch milliseconds
) {
    val isPageBundle: Boolean get() = filePath.endsWith("index.md")
    val bundleDir: String get() = java.io.File(filePath).parent ?: ""
}
