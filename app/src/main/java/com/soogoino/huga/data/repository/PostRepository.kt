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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
    // ─── Observe from Room (fast, reactive) ──────────────────────────────────

    fun observePosts(): Flow<List<HugoPost>> =
        postDao.observeAll().map { entities -> entities.map { it.toDomain() } }

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
                val (fm, format, _) = FrontMatterParser.parse(raw)
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
                relativePath = filePath,
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
        ))
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
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()
        try {
            stream.write(rawContent.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            atomicFile.failWrite(stream)
        }
        draftDao.markClean(filePath)
    }

    /**
     * Create a new page-bundle post folder + index.md scaffold.
     * Returns the path to index.md.
     */
    suspend fun createPost(repoPath: String, slug: String, format: FrontMatterFormat, section: String = "posts"): String =
        withContext(Dispatchers.IO) {
            val now = Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val postDir = File(repoPath, "content/$section/$slug").also { it.mkdirs() }
            val indexMd = File(postDir, "index.md")
            val title = slug.replace("-", " ").capitalizeWords()
            // Write scaffold directly — bypasses SnakeYAML empty-collection quirks
            // and guarantees all fields appear in the file from the start.
            val scaffold = buildString {
                appendLine("---")
                appendLine("title: $title")
                appendLine("date: $now")
                appendLine("draft: true")
                appendLine("slug: $slug")
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
        val (fm, format, body) = runCatching {
            val raw = File(filePath).readText()
            FrontMatterParser.parse(raw)
        }.getOrDefault(
            Triple(
                com.soogoino.huga.data.model.HugoFrontMatter(title = title, date = date, draft = draft),
                FrontMatterFormat.YAML,
                ""
            )
        )
        return HugoPost(
            filePath = filePath,
            relativePath = relativePath,
            slug = slug,
            frontMatter = fm,
            frontMatterFormat = format,
            bodyMarkdown = body,
            lastModified = lastModified,
        )
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
