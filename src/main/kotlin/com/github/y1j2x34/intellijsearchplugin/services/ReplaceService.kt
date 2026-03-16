package com.github.y1j2x34.intellijsearchplugin.services

import com.github.y1j2x34.intellijsearchplugin.model.FileSearchResult
import com.github.y1j2x34.intellijsearchplugin.model.SearchOptions
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 替换服务 - 负责执行搜索和替换操作
 */
@Service(Service.Level.PROJECT)
class ReplaceService(private val project: Project) {

    /**
     * 在单个文件中替换第一个匹配项
     */
    fun replaceInFile(
        file: VirtualFile,
        searchOptions: SearchOptions,
        replaceText: String,
        lineNumber: Int
    ): Boolean {
        return WriteCommandAction.runWriteCommandAction<Boolean>(project) {
            try {
                if (!file.isValid || !file.isWritable) return@runWriteCommandAction false

                val content = String(file.contentsToByteArray(), file.charset)
                val lines = content.lines().toMutableList()

                if (lineNumber < 1 || lineNumber > lines.size) return@runWriteCommandAction false

                val line = lines[lineNumber - 1]
                val pattern = createSearchPattern(searchOptions)
                val matcher = pattern.matcher(line)

                if (matcher.find()) {
                    val actualReplace = if (searchOptions.preserveCase) {
                        applyPreserveCase(matcher.group(), replaceText)
                    } else {
                        replaceText
                    }
                    val newLine = matcher.replaceFirst(Matcher.quoteReplacement(actualReplace))
                    lines[lineNumber - 1] = newLine

                    val newContent = lines.joinToString("\n")
                    WriteAction.run<Exception> {
                        file.setBinaryContent(newContent.toByteArray(file.charset))
                    }
                    return@runWriteCommandAction true
                }

                false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * 在单个文件中替换所有匹配项
     */
    fun replaceAllInFile(
        file: VirtualFile,
        searchOptions: SearchOptions,
        replaceText: String
    ): Int {
        return WriteCommandAction.runWriteCommandAction<Int>(project) {
            try {
                if (!file.isValid || !file.isWritable) return@runWriteCommandAction 0

                val content = String(file.contentsToByteArray(), file.charset)
                val pattern = createSearchPattern(searchOptions)
                val matcher = pattern.matcher(content)

                var count = 0
                val result = StringBuffer()

                while (matcher.find()) {
                    val actualReplace = if (searchOptions.preserveCase) {
                        applyPreserveCase(matcher.group(), replaceText)
                    } else {
                        replaceText
                    }
                    matcher.appendReplacement(result, Matcher.quoteReplacement(actualReplace))
                    count++
                }
                matcher.appendTail(result)

                if (count > 0) {
                    WriteAction.run<Exception> {
                        file.setBinaryContent(result.toString().toByteArray(file.charset))
                    }
                }

                count
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    /**
     * 在多个文件中替换所有匹配项
     */
    fun replaceAllInFiles(
        fileResults: List<FileSearchResult>,
        searchOptions: SearchOptions,
        replaceText: String
    ): Map<VirtualFile, Int> {
        val results = mutableMapOf<VirtualFile, Int>()

        WriteCommandAction.runWriteCommandAction(project) {
            fileResults.forEach { fileResult ->
                val count = replaceAllInFile(fileResult.file, searchOptions, replaceText)
                if (count > 0) {
                    results[fileResult.file] = count
                }
            }
        }

        return results
    }

    /**
     * Apply preserve-case transformation: match the casing pattern of the original
     * text and apply it to the replacement. Follows VS Code's preserve case rules:
     * - ALL UPPER → replacement becomes ALL UPPER
     * - all lower → replacement becomes all lower
     * - First Letter Upper → replacement becomes First Letter Upper
     * - Otherwise → replacement used as-is
     */
    private fun applyPreserveCase(matched: String, replacement: String): String {
        if (matched.isEmpty() || replacement.isEmpty()) return replacement

        val allUpper = matched.all { !it.isLetter() || it.isUpperCase() } && matched.any { it.isLetter() }
        val allLower = matched.all { !it.isLetter() || it.isLowerCase() } && matched.any { it.isLetter() }
        val firstUpper = matched[0].isUpperCase() && matched.drop(1).all { !it.isLetter() || it.isLowerCase() } && matched.any { it.isLetter() }

        return when {
            allUpper -> replacement.uppercase()
            allLower -> replacement.lowercase()
            firstUpper -> replacement.replaceFirstChar { it.uppercaseChar() }
            else -> replacement
        }
    }

    /**
     * 创建搜索模式（与 ProjectSearchService 中的逻辑一致）
     */
    private fun createSearchPattern(options: SearchOptions): Pattern {
        var patternString = options.query

        if (!options.useRegex) {
            patternString = Pattern.quote(patternString)
        }

        if (options.matchWholeWord) {
            patternString = "\\b$patternString\\b"
        }

        val flags = if (options.matchCase) 0 else Pattern.CASE_INSENSITIVE

        return try {
            Pattern.compile(patternString, flags)
        } catch (e: Exception) {
            Pattern.compile(Pattern.quote(options.query), flags)
        }
    }
}
