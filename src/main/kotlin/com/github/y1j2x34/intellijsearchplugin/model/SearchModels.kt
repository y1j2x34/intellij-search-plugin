package com.github.y1j2x34.intellijsearchplugin.model

import com.intellij.openapi.vfs.VirtualFile

/**
 * 搜索选项配置
 */
data class SearchOptions(
    val query: String,
    val matchCase: Boolean = false,
    val matchWholeWord: Boolean = false,
    val useRegex: Boolean = false,
    val preserveCase: Boolean = false,
    val searchScope: SearchScope = SearchScope.PROJECT,
    val includePatterns: List<String> = emptyList(),
    val excludePatterns: List<String> = listOf("**/node_modules/**", "**/dist/**", "**/build/**", "**/.git/**"),
    val respectGitIgnore: Boolean = true,
    val searchOnlyInOpenEditors: Boolean = false,
    val useExcludeSettings: Boolean = true
)

/**
 * 搜索范围
 */
enum class SearchScope {
    PROJECT,        // 整个工程
    MODULE,         // 当前模块
    DIRECTORY,      // 当前目录
    CUSTOM          // 自定义范围
}

/**
 * 单行匹配结果
 */
data class LineMatch(
    val lineNumber: Int,
    val lineContent: String,
    val matchRanges: List<IntRange>  // 匹配的字符范围
)

/**
 * 单个文件的搜索结果
 */
data class FileSearchResult(
    val file: VirtualFile,
    val matches: List<LineMatch>
) {
    val matchCount: Int get() = matches.size
}

/**
 * 完整搜索结果
 */
data class SearchResult(
    val options: SearchOptions,
    val fileResults: List<FileSearchResult>,
    val totalFiles: Int,
    val totalMatches: Int,
    val searchTimeMs: Long
)
