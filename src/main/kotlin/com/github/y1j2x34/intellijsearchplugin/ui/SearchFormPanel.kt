package com.github.y1j2x34.intellijsearchplugin.ui

import com.github.y1j2x34.intellijsearchplugin.model.SearchOptions
import com.github.y1j2x34.intellijsearchplugin.model.SearchScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * 搜索表单面板 - 包含搜索输入框和各种选项
 */
class SearchFormPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val searchField = JBTextField()
    private val replaceField = JBTextField()

    private val matchCaseCheckBox = JBCheckBox("Match Case (Aa)")
    private val matchWholeWordCheckBox = JBCheckBox("Match Whole Word (Ab)")
    private val useRegexCheckBox = JBCheckBox("Use Regex (.*)")

    private val scopeComboBox = ComboBox(arrayOf("Project", "Module", "Directory"))
    private val includeField = JBTextField()
    private val excludeField = JBTextField()

    private val searchButton = JButton("Search", AllIcons.Actions.Search)
    private val replaceButton = JButton("Replace", AllIcons.Actions.Replace)
    private val replaceAllButton = JButton("Replace All")

    private var showReplaceMode = false
    private var onSearchTriggered: ((SearchOptions) -> Unit)? = null
    private var onReplaceTriggered: ((SearchOptions, String) -> Unit)? = null
    private var onReplaceAllTriggered: ((SearchOptions, String) -> Unit)? = null

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        border = JBUI.Borders.empty(8)

        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            gridx = 0
            gridy = 0
            insets = JBUI.insets(2)
        }

        // 搜索输入行
        val searchPanel = JPanel(BorderLayout(5, 0))
        searchPanel.add(JBLabel("Search:"), BorderLayout.WEST)
        searchPanel.add(searchField, BorderLayout.CENTER)

        val searchOptionsPanel = JPanel()
        searchOptionsPanel.add(matchCaseCheckBox)
        searchOptionsPanel.add(matchWholeWordCheckBox)
        searchOptionsPanel.add(useRegexCheckBox)
        searchPanel.add(searchOptionsPanel, BorderLayout.EAST)

        mainPanel.add(searchPanel, gbc)

        // 替换输入行（初始隐藏）
        gbc.gridy++
        val replacePanel = JPanel(BorderLayout(5, 0))
        replacePanel.add(JBLabel("Replace:"), BorderLayout.WEST)
        replacePanel.add(replaceField, BorderLayout.CENTER)
        replacePanel.isVisible = false
        mainPanel.add(replacePanel, gbc)

        // 搜索范围
        gbc.gridy++
        val scopePanel = JPanel(BorderLayout(5, 0))
        scopePanel.add(JBLabel("Scope:"), BorderLayout.WEST)
        scopePanel.add(scopeComboBox, BorderLayout.CENTER)
        mainPanel.add(scopePanel, gbc)

        // Include patterns
        gbc.gridy++
        val includePanel = JPanel(BorderLayout(5, 0))
        includePanel.add(JBLabel("Include:"), BorderLayout.WEST)
        includeField.toolTipText = "e.g., src/**, *.ts (comma separated)"
        includePanel.add(includeField, BorderLayout.CENTER)
        mainPanel.add(includePanel, gbc)

        // Exclude patterns
        gbc.gridy++
        val excludePanel = JPanel(BorderLayout(5, 0))
        excludePanel.add(JBLabel("Exclude:"), BorderLayout.WEST)
        excludeField.text = "**/node_modules/**, **/dist/**, **/build/**"
        excludeField.toolTipText = "e.g., node_modules/**, dist/** (comma separated)"
        excludePanel.add(excludeField, BorderLayout.CENTER)
        mainPanel.add(excludePanel, gbc)

        // 按钮行
        gbc.gridy++
        val buttonPanel = JPanel()
        buttonPanel.add(searchButton)
        buttonPanel.add(replaceButton)
        buttonPanel.add(replaceAllButton)
        replaceButton.isVisible = false
        replaceAllButton.isVisible = false
        mainPanel.add(buttonPanel, gbc)

        add(mainPanel, BorderLayout.NORTH)
    }

    private fun setupListeners() {
        // 回车触发搜索
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    triggerSearch()
                }
            }
        })

        searchButton.addActionListener { triggerSearch() }
        replaceButton.addActionListener { triggerReplace() }
        replaceAllButton.addActionListener { triggerReplaceAll() }
    }

    /**
     * 切换替换模式
     */
    fun toggleReplaceMode() {
        showReplaceMode = !showReplaceMode
        updateReplaceVisibility()
    }

    private fun updateReplaceVisibility() {
        // 找到 replacePanel 并更新可见性
        val mainPanel = getComponent(0) as JPanel
        val replacePanel = mainPanel.getComponent(1) as JPanel
        replacePanel.isVisible = showReplaceMode
        replaceButton.isVisible = showReplaceMode
        replaceAllButton.isVisible = showReplaceMode

        revalidate()
        repaint()
    }

    private fun triggerSearch() {
        val options = buildSearchOptions()
        onSearchTriggered?.invoke(options)
    }

    private fun triggerReplace() {
        val options = buildSearchOptions()
        val replaceText = replaceField.text
        onReplaceTriggered?.invoke(options, replaceText)
    }

    private fun triggerReplaceAll() {
        val options = buildSearchOptions()
        val replaceText = replaceField.text
        onReplaceAllTriggered?.invoke(options, replaceText)
    }

    private fun buildSearchOptions(): SearchOptions {
        val scope = when (scopeComboBox.selectedIndex) {
            0 -> SearchScope.PROJECT
            1 -> SearchScope.MODULE
            2 -> SearchScope.DIRECTORY
            else -> SearchScope.PROJECT
        }

        val includePatterns = includeField.text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val excludePatterns = excludeField.text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return SearchOptions(
            query = searchField.text,
            matchCase = matchCaseCheckBox.isSelected,
            matchWholeWord = matchWholeWordCheckBox.isSelected,
            useRegex = useRegexCheckBox.isSelected,
            searchScope = scope,
            includePatterns = includePatterns,
            excludePatterns = excludePatterns
        )
    }

    fun setOnSearchTriggered(callback: (SearchOptions) -> Unit) {
        onSearchTriggered = callback
    }

    fun setOnReplaceTriggered(callback: (SearchOptions, String) -> Unit) {
        onReplaceTriggered = callback
    }

    fun setOnReplaceAllTriggered(callback: (SearchOptions, String) -> Unit) {
        onReplaceAllTriggered = callback
    }

    fun focusSearchField() {
        searchField.requestFocusInWindow()
    }

    fun getSearchQuery(): String = searchField.text

    fun setSearchQuery(query: String) {
        searchField.text = query
    }
}
