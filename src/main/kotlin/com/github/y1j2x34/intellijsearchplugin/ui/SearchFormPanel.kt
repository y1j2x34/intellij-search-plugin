package com.github.y1j2x34.intellijsearchplugin.ui

import com.github.y1j2x34.intellijsearchplugin.model.SearchOptions
import com.github.y1j2x34.intellijsearchplugin.model.SearchScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * 搜索表单面板 - 包含搜索输入框和各种选项
 */
class SearchFormPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val searchField = JBTextField()
    private val replaceField = JBTextField()

    // 搜索选项切换按钮（内嵌在搜索输入框内）
    private val matchCaseToggle = createOptionToggle("Aa", "Match Case")
    private val wholeWordToggle = createOptionToggle("Ab", "Match Whole Word")
    private val regexToggle = createOptionToggle(".*", "Use Regex")

    private val scopeComboBox = ComboBox(arrayOf("Project", "Module", "Directory"))
    private val includeField = JBTextField()
    private val excludeField = JBTextField()

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

        // 搜索输入行（输入框内嵌选项按钮）
        val searchPanel = JPanel(BorderLayout(5, 0))
        searchPanel.add(JBLabel("Search:"), BorderLayout.WEST)
        searchPanel.add(createSearchFieldWithOptions(), BorderLayout.CENTER)
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

        // 按钮行（仅替换相关按钮）
        gbc.gridy++
        val buttonPanel = JPanel()
        buttonPanel.add(replaceButton)
        buttonPanel.add(replaceAllButton)
        replaceButton.isVisible = false
        replaceAllButton.isVisible = false
        mainPanel.add(buttonPanel, gbc)

        add(mainPanel, BorderLayout.NORTH)
    }

    /**
     * 创建带有内嵌选项按钮的搜索输入框
     * 视觉上看起来是一个输入框，右侧有小图标切换按钮
     */
    private fun createSearchFieldWithOptions(): JComponent {
        val borderColor = JBColor.border()
        val focusColor = UIManager.getColor("Component.focusColor")
            ?: JBColor(Color(0x3d, 0x98, 0xf5), Color(0x35, 0x92, 0xC4))

        val wrapper = object : JPanel(BorderLayout(0, 0)) {
            override fun getBackground(): Color = searchField.background
        }
        wrapper.isOpaque = true
        wrapper.border = JBUI.Borders.customLine(borderColor, 1)

        // 搜索框本身去除边框，融入包装面板
        searchField.border = JBUI.Borders.empty(2, 6)
        searchField.isOpaque = false

        // 聚焦时改变包装面板边框颜色
        searchField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                wrapper.border = JBUI.Borders.customLine(focusColor, 1)
                wrapper.repaint()
            }

            override fun focusLost(e: FocusEvent) {
                wrapper.border = JBUI.Borders.customLine(borderColor, 1)
                wrapper.repaint()
            }
        })

        // 选项按钮面板
        val buttonsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(2, 0, 2, 4)
        }
        buttonsPanel.add(matchCaseToggle)
        buttonsPanel.add(Box.createHorizontalStrut(JBUI.scale(1)))
        buttonsPanel.add(wholeWordToggle)
        buttonsPanel.add(Box.createHorizontalStrut(JBUI.scale(1)))
        buttonsPanel.add(regexToggle)

        wrapper.add(searchField, BorderLayout.CENTER)
        wrapper.add(buttonsPanel, BorderLayout.EAST)

        return wrapper
    }

    /**
     * 创建搜索选项切换按钮
     * 模拟 VS Code 风格：小图标按钮，悬停高亮，选中高亮，tooltip 提示
     */
    private fun createOptionToggle(label: String, tooltip: String): JToggleButton {
        return object : JToggleButton() {
            init {
                toolTipText = tooltip
                isFocusable = false
                isFocusPainted = false
                isContentAreaFilled = false
                isBorderPainted = false
                isOpaque = false
                isRolloverEnabled = true
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                preferredSize = Dimension(JBUI.scale(24), JBUI.scale(20))
                minimumSize = preferredSize
                maximumSize = preferredSize
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

                val arc = JBUI.scale(4)

                // 背景：选中 > 悬停 > 无
                when {
                    isSelected -> {
                        g2.color = JBColor(Color(0, 0, 0, 30), Color(255, 255, 255, 40))
                        g2.fillRoundRect(0, 0, width, height, arc, arc)
                        // 选中时底部加一条高亮线
                        g2.color = UIManager.getColor("Component.focusColor")
                            ?: JBColor(Color(0x3d, 0x98, 0xf5), Color(0x35, 0x92, 0xC4))
                        g2.fillRect(JBUI.scale(2), height - JBUI.scale(2), width - JBUI.scale(4), JBUI.scale(2))
                    }
                    model.isRollover -> {
                        g2.color = JBColor(Color(0, 0, 0, 15), Color(255, 255, 255, 20))
                        g2.fillRoundRect(0, 0, width, height, arc, arc)
                    }
                }

                // 文字颜色：选中时正常色，未选中时灰色
                g2.color = if (isSelected) {
                    UIManager.getColor("Label.foreground") ?: JBColor.foreground()
                } else {
                    UIManager.getColor("Label.disabledForeground") ?: JBColor.GRAY
                }
                g2.font = JBUI.Fonts.smallFont()

                val fm = g2.fontMetrics
                val x = (width - fm.stringWidth(label)) / 2
                val y = (height + fm.ascent - fm.descent) / 2
                g2.drawString(label, x, y)
            }
        }
    }

    private fun setupListeners() {
        // 所有输入框回车触发搜索
        val enterKeyListener = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    triggerSearch()
                }
            }
        }

        searchField.addKeyListener(enterKeyListener)
        replaceField.addKeyListener(enterKeyListener)
        includeField.addKeyListener(enterKeyListener)
        excludeField.addKeyListener(enterKeyListener)

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
            matchCase = matchCaseToggle.isSelected,
            matchWholeWord = wholeWordToggle.isSelected,
            useRegex = regexToggle.isSelected,
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
