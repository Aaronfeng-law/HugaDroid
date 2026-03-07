package com.soogoino.huga.data.repository

import android.util.Log
import androidx.core.util.AtomicFile
import com.soogoino.huga.data.local.DraftDao
import com.soogoino.huga.data.local.DraftEntity
import com.soogoino.huga.data.local.PostDao
import com.soogoino.huga.data.local.PostEntity
import com.soogoino.huga.data.model.FrontMatterFormat
import com.soogoino.huga.data.model.FrontMatterParser
import com.soogoino.huga.data.model.HugoPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.soogoino.huga.ui.util.countWords
import org.json.JSONArray
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PostRepository"

@Singleton
class PostRepository @Inject constructor(
    private val postDao: PostDao,
    private val draftDao: DraftDao,
) {
    // ROB-05: Serialise all AtomicFile writes to prevent concurrent-write races
    private val saveMutex = Mutex()
    // ─── Observe from Room (fast, reactive) ──────────────────────────────────

    fun observePosts(): Flow<List<HugoPost>> =
        postDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    // ─── Scan file system and refresh Room cache ──────────────────────────────

    suspend fun scanAndRefresh(repoPath: String) = withContext(Dispatchers.IO) {
        val contentDir = File(repoPath, "content")
        if (!contentDir.exists()) {
            Log.w(TAG, "content/ dir not found at $repoPath")
            return@withContext
        }

        val mdFiles = contentDir.walkTopDown()
            .filter { it.isFile && it.extension == "md" }
            .toList()

        val entities = mdFiles.mapNotNull { file ->
            runCatching {
                val raw = file.readText()
                val (fm, format, body) = FrontMatterParser.parse(raw)
                val rel = file.relativeTo(contentDir).path
                val slug = if (file.name == "index.md") file.parentFile?.name ?: "" else file.nameWithoutExtension
                PostEntity(
                    filePath = file.absolutePath,
                    relativePath = rel,
                    slug = slug,
                    title = fm.title.ifBlank { slug },
                    date = fm.date,
                    draft = fm.draft,
                    tags = JSONArray(fm.tags).toString(),
                    categories = JSONArray(fm.categories).toString(),
                    description = fm.description,
                    lastModified = file.lastModified(),
                    wordCount = countWords(body),
                )
            }.onFailure { Log.e(TAG, "Failed to scan ${file.path}", it) }.getOrNull()
        }

        postDao.deleteAll()
        postDao.upsertAll(entities)
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    /** Read the full [HugoPost] from disk (bypasses Room cache). */
    suspend fun readPost(filePath: String): HugoPost? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(filePath)
            val raw = file.readText()
            val (fm, format, body) = FrontMatterParser.parse(raw)
            val slug = if (file.name == "index.md") file.parentFile?.name ?: "" else file.nameWithoutExtension
            HugoPost(
                filePath = filePath,
                // ROB-10: derive relative path from existing Room entity to avoid
                // storing absolute path where a repo-relative path is expected.
                relativePath = postDao.getByPath(filePath)?.relativePath ?: filePath,
                slug = slug,
                frontMatter = fm,
                frontMatterFormat = format,
                bodyMarkdown = body,
                lastModified = file.lastModified(),
            )
        }.onFailure { Log.e(TAG, "readPost failed: $filePath", it) }.getOrNull()
    }

    /**
     * Write the full raw content to disk using [AtomicFile] to prevent partial writes.
     * Also updates the Room cache entry.
     */
    suspend fun savePost(post: HugoPost) = withContext(Dispatchers.IO) {
        val raw = FrontMatterParser.serialise(post.frontMatter, post.frontMatterFormat, post.bodyMarkdown)
        val file = File(post.filePath)
        file.parentFile?.mkdirs()

        saveMutex.withLock {  // ROB-05
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()
        try {
            stream.write(raw.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            atomicFile.failWrite(stream)
            throw e
        }

        // Update Room cache
        postDao.upsert(PostEntity(
            filePath = post.filePath,
            relativePath = post.relativePath,
            slug = post.slug,
            title = post.frontMatter.title.ifBlank { post.slug },
            date = post.frontMatter.date,
            draft = post.frontMatter.draft,
            tags = JSONArray(post.frontMatter.tags).toString(),
            categories = JSONArray(post.frontMatter.categories).toString(),
            description = post.frontMatter.description,
            lastModified = System.currentTimeMillis(),
            wordCount = countWords(post.bodyMarkdown),
        ))
        } // end saveMutex.withLock
    }

    /**
     * Auto-save raw content to the draft table (does NOT write to disk).
     * The editor writes to disk directly via [saveRawToDisk].
     */
    suspend fun saveDraft(filePath: String, rawContent: String) {
        draftDao.upsert(DraftEntity(
            filePath = filePath,
            content = rawContent,
            savedAt = System.currentTimeMillis(),
            isDirty = true,
        ))
    }

    /** Write raw content directly to disk (used by auto-save debounce). */
    suspend fun saveRawToDisk(filePath: String, rawContent: String) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        saveMutex.withLock {  // ROB-05
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()
        try {
            stream.write(rawContent.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            atomicFile.failWrite(stream)
        }
        // ROB-11: update Room cache so observers reflect the latest saved state
        postDao.getByPath(filePath)?.let { existing ->
            runCatching {
                val (fm, _, body) = FrontMatterParser.parse(rawContent)
                postDao.upsert(existing.copy(
                    title = fm.title.ifBlank { existing.title },
                    date = fm.date.ifBlank { existing.date },
                    draft = fm.draft,
                    tags = JSONArray(fm.tags).toString(),
                    categories = JSONArray(fm.categories).toString(),
                    description = fm.description,
                    lastModified = System.currentTimeMillis(),
                    wordCount = countWords(body),
                ))
            }
        }
        } // end saveMutex.withLock
        draftDao.markClean(filePath)
    }

    /**
     * Create a new page-bundle post folder + index.md scaffold.
     * Returns the path to index.md.
     */
    suspend fun createPost(repoPath: String, slug: String, format: FrontMatterFormat, section: String = "posts"): String =
        withContext(Dispatchers.IO) {
            // SEC-06: Validate slug and section to prevent path traversal
            val safeSlug = slug.trim()
            require(!safeSlug.contains("..") && !safeSlug.startsWith("/") && !safeSlug.contains("/")) {
                "Invalid slug: must not contain '..', leading '/' or path separators"
            }
            val safeSection = section.trim()
            require(!safeSection.contains("..") && !safeSection.startsWith("/")) {
                "Invalid section: must not contain '..' or leading '/'"
            }
            val now = Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val postDir = File(repoPath, "content/$safeSection/$safeSlug").also { it.mkdirs() }
            val indexMd = File(postDir, "index.md")
            val title = safeSlug.replace("-", " ").capitalizeWords()
            // Write scaffold directly — bypasses SnakeYAML empty-collection quirks
            // and guarantees all fields appear in the file from the start.
            val scaffold = buildString {
                appendLine("---")
                appendLine("title: $title")
                appendLine("date: $now")
                appendLine("draft: true")
                appendLine("slug: $safeSlug")
                appendLine("tags: []")
                appendLine("categories: []")
                appendLine("description: ''")
                appendLine("---")
                appendLine()
                appendLine()
                append("Write your post here...")
            }
            indexMd.writeText(scaffold)
            indexMd.absolutePath
        }

    suspend fun deletePost(filePath: String) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.name == "index.md") {
            // Delete entire page bundle directory
            file.parentFile?.deleteRecursively()
        } else {
            file.delete()
        }
        postDao.deleteByPath(filePath)
        draftDao.delete(filePath)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun PostEntity.toDomain(): HugoPost {
        // Parse tags & categories from JSON (no disk IO)
        val tagsList = runCatching {
            val arr = org.json.JSONArray(tags)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())
        val categoriesList = runCatching {
            val arr = org.json.JSONArray(categories)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())

        return HugoPost(
            filePath = filePath,
            relativePath = relativePath,
            slug = slug,
            frontMatter = com.soogoino.huga.data.model.HugoFrontMatter(
                title = title,
                date = date,
                draft = draft,
                tags = tagsList,
                categories = categoriesList,
                description = description,
            ),
            frontMatterFormat = FrontMatterFormat.YAML,
            bodyMarkdown = "", // body not needed for list; readPost() loads full content
            lastModified = lastModified,
            wordCount = wordCount,
        )
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
