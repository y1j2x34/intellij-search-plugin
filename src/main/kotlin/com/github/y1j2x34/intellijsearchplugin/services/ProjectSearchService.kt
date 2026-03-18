package com.github.y1j2x34.intellijsearchplugin.services

import com.github.y1j2x34.intellijsearchplugin.model.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * 项目搜索服务 - 负责执行全文搜索
 */
@Service(Service.Level.PROJECT)
class ProjectSearchService(private val project: Project) {

    private val textFileExtensions = setOf(
        "java", "kt", "kts", "js", "ts", "tsx", "jsx", "html", "css", "scss", "sass",
        "xml", "json", "yaml", "yml", "md", "txt", "properties", "gradle", "py", "rb",
        "go", "rs", "c", "cpp", "h", "hpp", "cs", "php", "sql", "sh", "bash", "vue"
    )

    /**
     * 执行搜索（异步，带进度条）
     * 注意：onProgress 和 onComplete 回调会在 EDT 上执行，可以安全地更新 UI
     */
    fun searchAsync(
        options: SearchOptions,
        onProgress: (FileSearchResult) -> Unit,
        onComplete: (SearchResult) -> Unit
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Searching in Project...", true) {
            override fun run(indicator: ProgressIndicator) {
                val startTime = System.currentTimeMillis()
                val results = mutableListOf<FileSearchResult>()
                var totalMatches = 0

                try {
                    val files = getFilesToSearch(options, indicator)
                    val pattern = createSearchPattern(options)

                    indicator.isIndeterminate = false
                    files.forEachIndexed { index, file ->
                        if (indicator.isCanceled) return@forEachIndexed

                        indicator.fraction = index.toDouble() / files.size
                        indicator.text2 = "Searching in ${file.name}"

                        val fileResult = ReadAction.compute<FileSearchResult?, Exception> {
                            searchInFile(file, pattern)
                        }
                        if (fileResult != null && fileResult.matches.isNotEmpty()) {
                            results.add(fileResult)
                            totalMatches += fileResult.matchCount
                            ApplicationManager.getApplication().invokeLater {
                                onProgress(fileResult)
                            }
                        }
                    }

                    val searchTime = System.currentTimeMillis() - startTime
                    val searchResult = SearchResult(
                        options = options,
                        fileResults = results,
                        totalFiles = results.size,
                        totalMatches = totalMatches,
                        searchTimeMs = searchTime
                    )
                    // 切换到 EDT 线程更新 UI
                    ApplicationManager.getApplication().invokeLater {
                        onComplete(searchResult)
                    }
                } catch (e: Exception) {
                    // 处理搜索异常
                    e.printStackTrace()
                }
            }
        })
    }

    /**
     * 获取需要搜索的文件列表
     */
    private fun getFilesToSearch(options: SearchOptions, indicator: ProgressIndicator): List<VirtualFile> {
        indicator.text = "Collecting files..."
        val allFiles = mutableListOf<VirtualFile>()
        ReadAction.run<Exception> {
            if (options.searchOnlyInOpenEditors) {
                FileEditorManager.getInstance(project).openFiles.forEach { file ->
                    if (!indicator.isCanceled && !file.isDirectory &&
                        isTextFile(file) &&
                        matchesIncludePatterns(file, options.includePatterns) &&
                        !matchesExcludePatterns(file, options.excludePatterns)
                    ) {
                        allFiles.add(file)
                    }
                }
            } else {
                ProjectFileIndex.getInstance(project).iterateContent { file ->
                    if (!indicator.isCanceled && !file.isDirectory &&
                        isTextFile(file) &&
                        matchesIncludePatterns(file, options.includePatterns) &&
                        !matchesExcludePatterns(file, options.excludePatterns)
                    ) {
                        allFiles.add(file)
                    }
                    true
                }
            }
        }
        return allFiles
    }

    /**
     * 在单个文件中搜索（调用方需持有读锁）
     */
    private fun searchInFile(file: VirtualFile, pattern: Pattern): FileSearchResult? {
        return try {
            if (!file.isValid || file.isDirectory) return null

            val content = String(file.contentsToByteArray(), file.charset)
            val lines = content.lines()
            val matches = mutableListOf<LineMatch>()

            lines.forEachIndexed { index, line ->
                val lineMatches = findMatchesInLine(line, pattern)
                if (lineMatches.isNotEmpty()) {
                    matches.add(LineMatch(index + 1, line, lineMatches))
                }
            }

            if (matches.isEmpty()) null else FileSearchResult(file, matches)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 在单行中查找所有匹配
     */
    private fun findMatchesInLine(line: String, pattern: Pattern): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        val matcher = pattern.matcher(line)

        while (matcher.find()) {
            // Use inclusive range: start..end-1
            ranges.add(matcher.start()..matcher.end() - 1)
        }

        return ranges
    }

    /**
     * 创建搜索模式
     */
    private fun createSearchPattern(options: SearchOptions): Pattern {
        var patternString = options.query

        if (!options.useRegex) {
            // 转义正则表达式特殊字符
            patternString = Pattern.quote(patternString)
        }

        if (options.matchWholeWord) {
            patternString = "\\b$patternString\\b"
        }

        val flags = if (options.matchCase) 0 else Pattern.CASE_INSENSITIVE

        return try {
            Pattern.compile(patternString, flags)
        } catch (e: PatternSyntaxException) {
            // 如果正则表达式无效，使用字面量匹配
            Pattern.compile(Pattern.quote(options.query), flags)
        }
    }

    /**
     * 判断是否为文本文件
     */
    private fun isTextFile(file: VirtualFile): Boolean {
        val extension = file.extension?.lowercase() ?: return false
        return extension in textFileExtensions
    }

    /**
     * 检查文件是否匹配包含模式
     */
    private fun matchesIncludePatterns(file: VirtualFile, patterns: List<String>): Boolean {
        if (patterns.isEmpty()) return true
        val path = file.path
        return patterns.any { pattern ->
            matchesGlobPattern(path, pattern)
        }
    }

    /**
     * 检查文件是否匹配排除模式
     */
    private fun matchesExcludePatterns(file: VirtualFile, patterns: List<String>): Boolean {
        val path = file.path
        return patterns.any { pattern ->
            matchesGlobPattern(path, pattern)
        }
    }

    /**
     * 简单的 glob 模式匹配
     */
    private fun matchesGlobPattern(path: String, pattern: String): Boolean {
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("**/", ".*")
            .replace("**", ".*")
            .replace("*", "[^/]*")
            .replace("?", ".")

        return Regex(regexPattern).containsMatchIn(path)
    }
}
