package com.github.y1j2x34.intellijsearchplugin.ui

import com.github.y1j2x34.intellijsearchplugin.model.SearchResult
import com.github.y1j2x34.intellijsearchplugin.services.ProjectSearchService
import com.github.y1j2x34.intellijsearchplugin.services.ReplaceService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
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

        // 顶部工具栏
        val toolbar = createToolbar()
        add(toolbar.component, BorderLayout.NORTH)

        // 搜索表单
        add(searchFormPanel, BorderLayout.NORTH)

        // 搜索结果
        add(searchResultsPanel, BorderLayout.CENTER)
    }

    private fun createToolbar(): ActionToolbarImpl {
        val actionGroup = DefaultActionGroup().apply {
            add(ToggleReplaceModeAction())
            add(ClearResultsAction())
        }

        return ActionToolbarImpl("SearchToolWindow", actionGroup, true).apply {
            setTargetComponent(this@SearchToolWindowPanel)
        }
    }

    private fun setupCallbacks() {
        // 搜索回调
        searchFormPanel.setOnSearchTriggered { options ->
            searchResultsPanel.clearResults()
            currentResultCount = 0
            currentFileCount = 0

            // 显示加载动画
            searchResultsPanel.showLoading()

            searchService.searchAsync(
                options = options,
                onProgress = { fileResult ->
                    // 收到第一个结果时切换到结果面板
                    searchResultsPanel.hideLoading()

                    // 渐进式显示结果
                    searchResultsPanel.addFileResult(fileResult)
                    currentFileCount++
                    currentResultCount += fileResult.matchCount
                    searchResultsPanel.updateSummary(currentResultCount, currentFileCount)
                },
                onComplete = { result ->
                    // 搜索完成，确保加载动画已隐藏
                    searchResultsPanel.hideLoading()
                    lastSearchResult = result
                    searchResultsPanel.displaySearchResult(result)
                }
            )
        }

        // 替换回调
        searchFormPanel.setOnReplaceTriggered { options, replaceText ->
            // 单个替换功能暂时不实现，提示用户使用全部替换
            Messages.showInfoMessage(
                project,
                "Please use 'Replace All' to replace all occurrences in the search results.",
                "Replace"
            )
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

            // 确认替换
            val confirmed = Messages.showYesNoDialog(
                project,
                "Replace ${result.totalMatches} occurrences in ${result.totalFiles} files?",
                "Confirm Replace All",
                Messages.getQuestionIcon()
            ) == Messages.YES

            if (confirmed) {
                val replacedFiles = replaceService.replaceAllInFiles(
                    result.fileResults,
                    options,
                    replaceText
                )

                val totalReplaced = replacedFiles.values.sum()
                Messages.showInfoMessage(
                    project,
                    "Replaced $totalReplaced occurrences in ${replacedFiles.size} files.",
                    "Replace All Complete"
                )

                // 清空结果，提示用户重新搜索
                searchResultsPanel.clearResults()
                lastSearchResult = null
            }
        }
    }

    /**
     * 聚焦搜索输入框
     */
    fun focusSearchField() {
        searchFormPanel.focusSearchField()
    }

    /**
     * 切换替换模式的 Action
     */
    private inner class ToggleReplaceModeAction : AnAction(
        "Toggle Replace Mode",
        "Show/hide replace field",
        AllIcons.Actions.Replace
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            searchFormPanel.toggleReplaceMode()
        }
    }

    /**
     * 清空结果的 Action
     */
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
