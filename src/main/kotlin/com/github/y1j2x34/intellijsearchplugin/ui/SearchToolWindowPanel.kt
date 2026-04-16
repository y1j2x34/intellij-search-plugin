package com.github.y1j2x34.intellijsearchplugin.ui

import com.github.y1j2x34.intellijsearchplugin.model.SearchResult
import com.github.y1j2x34.intellijsearchplugin.services.ProjectSearchService
import com.github.y1j2x34.intellijsearchplugin.services.ReplaceService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * 搜索工具窗口主面板
 */
class SearchToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val searchFormPanel = SearchFormPanel(project)
    private val searchResultsPanel = SearchResultsPanel(project)
    private val searchService = project.service<ProjectSearchService>()
    private val replaceService = project.service<ReplaceService>()

    private var currentResultCount = 0
    private var currentFileCount = 0
    private var lastSearchResult: SearchResult? = null

    init {
        setupUI()
        setupCallbacks()
    }

    private fun setupUI() {
        border = JBUI.Borders.empty()

        val toolbar = createToolbar()
        add(toolbar.component, BorderLayout.NORTH)
        add(searchFormPanel, BorderLayout.NORTH)
        add(searchResultsPanel, BorderLayout.CENTER)
    }

    private fun createToolbar(): ActionToolbar {
        val actionGroup = DefaultActionGroup().apply {
            add(ClearResultsAction())
        }
        return ActionManager.getInstance().createActionToolbar(
            "SearchToolWindow",
            actionGroup,
            true
        ).apply {
            targetComponent = this@SearchToolWindowPanel
        }
    }

    private fun setupCallbacks() {
        searchFormPanel.setOnSearchTriggered { options ->
            searchResultsPanel.clearResults()
            currentResultCount = 0
            currentFileCount = 0
            searchResultsPanel.showLoading()

            searchService.searchAsync(
                options = options,
                onProgress = { fileResult ->
                    searchResultsPanel.hideLoading()
                    searchResultsPanel.addFileResult(fileResult)
                    currentFileCount++
                    currentResultCount += fileResult.matchCount
                    searchResultsPanel.updateSummary(currentResultCount, currentFileCount)
                },
                onComplete = { result ->
                    searchResultsPanel.hideLoading()
                    lastSearchResult = result
                    searchResultsPanel.displaySearchResult(result)
                    // Wire up inline replace buttons now that we have results
                    setupInlineReplaceCallbacks()
                }
            )
        }

        // Replace selected match (form button) — re-search after replacing
        searchFormPanel.setOnReplaceTriggered { options, replaceText ->
            if (replaceText.isEmpty()) return@setOnReplaceTriggered
            val result = lastSearchResult ?: run {
                Messages.showWarningDialog(project, "No search results. Please search first.", "Replace")
                return@setOnReplaceTriggered
            }
            // Replace first match across all files, then re-search
            val firstFile = result.fileResults.firstOrNull() ?: return@setOnReplaceTriggered
            val firstMatch = firstFile.matches.firstOrNull() ?: return@setOnReplaceTriggered
            val replaced = replaceService.replaceInFile(firstFile.file, options, replaceText, firstMatch.lineNumber)
            if (replaced) {
                triggerSearch()
            }
        }

        searchFormPanel.setOnReplaceAllTriggered { options, replaceText ->
            val result = lastSearchResult
            if (result == null || result.fileResults.isEmpty()) {
                Messages.showWarningDialog(
                    project,
                    "No search results to replace. Please perform a search first.",
                    "Replace All"
                )
                return@setOnReplaceAllTriggered
            }

            val confirmed = Messages.showYesNoDialog(
                project,
                "Replace ${result.totalMatches} occurrences in ${result.totalFiles} files?",
                "Confirm Replace All",
                Messages.getQuestionIcon()
            ) == Messages.YES

            if (confirmed) {
                val replacedFiles = replaceService.replaceAllInFiles(result.fileResults, options, replaceText)
                val totalReplaced = replacedFiles.values.sum()
                Messages.showInfoMessage(
                    project,
                    "Replaced $totalReplaced occurrences in ${replacedFiles.size} files.",
                    "Replace All Complete"
                )
                searchResultsPanel.clearResults()
                lastSearchResult = null
            }
        }
    }

    /**
     * Wire up the inline replace buttons on tree nodes after search completes.
     * These use the last known search options from the form.
     */
    private fun setupInlineReplaceCallbacks() {
        searchResultsPanel.onReplaceMatch = { file, lineMatch ->
            val replaceText = searchFormPanel.getReplaceText()
            val options = searchFormPanel.buildCurrentSearchOptions()
            val replaced = replaceService.replaceInFile(file, options, replaceText, lineMatch.lineNumber)
            if (replaced) triggerSearch()
        }

        searchResultsPanel.onReplaceAllInFile = { file ->
            val replaceText = searchFormPanel.getReplaceText()
            val options = searchFormPanel.buildCurrentSearchOptions()
            val count = replaceService.replaceAllInFile(file, options, replaceText)
            if (count > 0) triggerSearch()
        }
    }

    /**
     * Re-trigger the last search to refresh results after a replace.
     */
    private fun triggerSearch() {
        searchFormPanel.invokeSearch()
    }

    fun focusSearchField() {
        searchFormPanel.focusSearchField()
    }

    private inner class ClearResultsAction : AnAction(
        "Clear Results",
        "Clear all search results",
        AllIcons.Actions.GC
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            searchResultsPanel.clearResults()
            currentResultCount = 0
            currentFileCount = 0
            lastSearchResult = null
        }
    }
}
