package com.github.y1j2x34.intellijsearchplugin.ui

import com.github.y1j2x34.intellijsearchplugin.model.SearchOptions
import com.github.y1j2x34.intellijsearchplugin.icons.PluginIcons
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

private class PlaceholderTextField(private val placeholder: String) : JBTextField() {
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (text.isEmpty() && !isFocusOwner) {
            val g2 = g as Graphics2D
            g2.color = UIManager.getColor("TextField.inactiveForeground") ?: JBColor.GRAY
            g2.font = font
            val insets = insets
            val fm = g2.fontMetrics
            g2.drawString(placeholder, insets.left + 6, insets.top + fm.ascent + (height - insets.top - insets.bottom - fm.height) / 2)
        }
    }
}

/**
 * 搜索表单面板 - VS Code 风格：搜索框左侧箭头按钮展开/收起替换输入框
 */
class SearchFormPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val searchField = PlaceholderTextField("Search(↑↓ for history)")
    private val replaceField = PlaceholderTextField("Replace")

    private val searchHistory = mutableListOf<String>()
    private var historyIndex = -1

    private val matchCaseToggle = createOptionToggle("Aa", "Match Case")
    private val wholeWordToggle = createOptionToggle("Ab", "Match Whole Word")
    private val regexToggle = createOptionToggle(".*", "Use Regex")
    private val preserveCaseToggle = createOptionToggle("aB", "Preserve Case")

    private val includeField = PlaceholderTextField("e.g. src/**, *.ts")
    private val excludeField = PlaceholderTextField("e.g. **/node_modules/**")
    private val openEditorsToggle = createOptionToggle("⊞", "Search Only in Open Editors")
    private val useExcludeSettingsToggle = createOptionToggle("◎", "Use Exclude Settings and Ignore Files").apply { isSelected = true }

    // VS Code 风格：左侧展开箭头
    private val toggleArrowButton = createArrowButton()

    private var showReplaceMode = false
    private lateinit var replaceRow: JPanel

    private var onSearchTriggered: ((SearchOptions) -> Unit)? = null
    private var onReplaceTriggered: ((SearchOptions, String) -> Unit)? = null
    private var onReplaceAllTriggered: ((SearchOptions, String) -> Unit)? = null

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        border = JBUI.Borders.empty(4, 4, 4, 4)

        val mainPanel = JPanel(GridBagLayout())
        val arrowW = JBUI.scale(20)

        // ── 搜索行 + 替换行共享一个容器，箭头按钮跨两行 ──────
        val inputPanel = JPanel(GridBagLayout()).apply { isOpaque = false }

        // 箭头按钮：col=0, row=0, gridheight=2，垂直填充
        val arrowGbc = GridBagConstraints().apply {
            gridx = 0; gridy = 0; gridheight = 2
            fill = GridBagConstraints.BOTH
            insets = JBUI.insets(0, 0, 0, JBUI.scale(4))
        }
        inputPanel.add(toggleArrowButton, arrowGbc)

        // 搜索输入框：col=1, row=0
        val searchFieldGbc = GridBagConstraints().apply {
            gridx = 1; gridy = 0
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets = JBUI.insets(0, 0, JBUI.scale(2), 0)
        }
        inputPanel.add(createSearchFieldWithOptions(), searchFieldGbc)

        // 替换输入框：col=1, row=1（初始隐藏）
        val replaceFieldComp = createReplaceFieldWithButtons()
        replaceRow = JPanel(BorderLayout()).apply {
            isOpaque = false
            isVisible = false
            add(replaceFieldComp, BorderLayout.CENTER)
        }
        val replaceFieldGbc = GridBagConstraints().apply {
            gridx = 1; gridy = 1
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
        }
        inputPanel.add(replaceRow, replaceFieldGbc)

        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            gridx = 0; gridy = 0
            insets = JBUI.insets(2, 0, 2, 0)
        }
        mainPanel.add(inputPanel, gbc)

        // ── 其余选项行（label 在上，输入框在下）────────────────
        fun optionRow(label: String, comp: JComponent): JPanel {
            return JPanel(BorderLayout(0, JBUI.scale(2))).apply {
                isOpaque = false
                add(Box.createHorizontalStrut(arrowW + JBUI.scale(4)), BorderLayout.WEST)
                val inner = JPanel(BorderLayout(0, JBUI.scale(2))).apply {
                    isOpaque = false
                    add(JBLabel(label).apply {
                        foreground = SimpleTextAttributes.GRAYED_ATTRIBUTES.fgColor
                    }, BorderLayout.NORTH)
                    add(comp, BorderLayout.CENTER)
                }
                add(inner, BorderLayout.CENTER)
            }
        }

        gbc.gridy++
        mainPanel.add(optionRow("Include:", createFieldWithToggle(includeField, openEditorsToggle)), gbc)

        gbc.gridy++
        excludeField.text = "**/node_modules/**, **/dist/**, **/build/**"
        mainPanel.add(optionRow("Exclude:", createFieldWithToggle(excludeField, useExcludeSettingsToggle)), gbc)

        add(mainPanel, BorderLayout.NORTH)
    }

    /**
     * VS Code 风格的展开箭头按钮（► / ▼）
     */
    private fun createArrowButton(): JButton {
        return object : JButton() {
            init {
                isFocusable = false
                isFocusPainted = false
                isContentAreaFilled = false
                isBorderPainted = false
                isOpaque = false
                isRolloverEnabled = true
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                toolTipText = "Toggle Replace"
                val sz = JBUI.scale(20)
                preferredSize = Dimension(sz, sz)
                minimumSize = Dimension(sz, 0)
                maximumSize = Dimension(sz, Int.MAX_VALUE)
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                if (model.isRollover) {
                    g2.color = JBColor(Color(0, 0, 0, 20), Color(255, 255, 255, 25))
                    g2.fillRoundRect(1, 1, width - 2, height - 2, JBUI.scale(4), JBUI.scale(4))
                }

                g2.color = UIManager.getColor("Label.foreground") ?: JBColor.foreground()
                val cx = width / 2
                val cy = height / 2
                val s = JBUI.scale(4)

                if (showReplaceMode) {
                    // ▼ 向下箭头
                    val xp = intArrayOf(cx - s, cx + s, cx)
                    val yp = intArrayOf(cy - s / 2, cy - s / 2, cy + s / 2)
                    g2.fillPolygon(xp, yp, 3)
                } else {
                    // ► 向右箭头
                    val xp = intArrayOf(cx - s / 2, cx + s / 2, cx - s / 2)
                    val yp = intArrayOf(cy - s, cy, cy + s)
                    g2.fillPolygon(xp, yp, 3)
                }
            }
        }
    }

    /**
     * 搜索输入框 + 右侧选项按钮（Aa / Ab / .*）
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

        searchField.border = JBUI.Borders.empty(2, 6)
        searchField.isOpaque = false

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

        val buttonsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(2, 0, 2, 4)
            add(matchCaseToggle)
            add(Box.createHorizontalStrut(JBUI.scale(1)))
            add(wholeWordToggle)
            add(Box.createHorizontalStrut(JBUI.scale(1)))
            add(regexToggle)
        }

        wrapper.add(searchField, BorderLayout.CENTER)
        wrapper.add(buttonsPanel, BorderLayout.EAST)
        return wrapper
    }

    /**
     * 替换输入框（内含 Preserve Case 切换）+ 右侧 Replace All 按钮
     * Layout: [wrapper: replaceField + preserveCaseToggle] [replaceAllBtn]
     */
    private fun createReplaceFieldWithButtons(): JComponent {
        val borderColor = JBColor.border()
        val focusColor = UIManager.getColor("Component.focusColor")
            ?: JBColor(Color(0x3d, 0x98, 0xf5), Color(0x35, 0x92, 0xC4))

        val wrapper = object : JPanel(BorderLayout(0, 0)) {
            override fun getBackground(): Color = replaceField.background
        }
        wrapper.isOpaque = true
        wrapper.border = JBUI.Borders.customLine(borderColor, 1)

        replaceField.border = JBUI.Borders.empty(2, 6)
        replaceField.isOpaque = false

        replaceField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                wrapper.border = JBUI.Borders.customLine(focusColor, 1)
                wrapper.repaint()
            }
            override fun focusLost(e: FocusEvent) {
                wrapper.border = JBUI.Borders.customLine(borderColor, 1)
                wrapper.repaint()
            }
        })

        // Preserve Case toggle inside the wrapper (right side)
        val innerToggles = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(2, 0, 2, 4)
            add(preserveCaseToggle)
        }
        wrapper.add(replaceField, BorderLayout.CENTER)
        wrapper.add(innerToggles, BorderLayout.EAST)

        // Replace All button outside the wrapper
        val replaceAllIconBtn = createIconActionButton(AllIcons.Actions.Replace, "Replace All")
        replaceAllIconBtn.addActionListener { triggerReplaceAll() }

        val row = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(wrapper)
            add(Box.createHorizontalStrut(JBUI.scale(2)))
            add(replaceAllIconBtn)
        }
        return row
    }

    /**
     * Wraps a text field with a bordered wrapper and places a toggle button inside on the right.
     */
    private fun createFieldWithToggle(field: JBTextField, toggle: JToggleButton): JComponent {
        val borderColor = JBColor.border()
        val focusColor = UIManager.getColor("Component.focusColor")
            ?: JBColor(Color(0x3d, 0x98, 0xf5), Color(0x35, 0x92, 0xC4))

        val wrapper = object : JPanel(BorderLayout(0, 0)) {
            override fun getBackground(): Color = field.background
        }
        wrapper.isOpaque = true
        wrapper.border = JBUI.Borders.customLine(borderColor, 1)

        field.border = JBUI.Borders.empty(2, 6)
        field.isOpaque = false

        field.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                wrapper.border = JBUI.Borders.customLine(focusColor, 1)
                wrapper.repaint()
            }
            override fun focusLost(e: FocusEvent) {
                wrapper.border = JBUI.Borders.customLine(borderColor, 1)
                wrapper.repaint()
            }
        })

        val togglePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(2, 0, 2, 4)
            add(toggle)
        }
        wrapper.add(field, BorderLayout.CENTER)
        wrapper.add(togglePanel, BorderLayout.EAST)
        return wrapper
    }

    private fun createIconActionButton(icon: Icon, tooltip: String): JButton {
        return object : JButton(icon) {
            init {
                toolTipText = tooltip
                isFocusable = false
                isFocusPainted = false
                isContentAreaFilled = false
                isBorderPainted = false
                isOpaque = false
                isRolloverEnabled = true
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                val sz = JBUI.scale(22)
                preferredSize = Dimension(sz, sz)
                minimumSize = preferredSize
                maximumSize = preferredSize
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                if (model.isRollover) {
                    g2.color = JBColor(Color(0, 0, 0, 20), Color(255, 255, 255, 25))
                    g2.fillRoundRect(1, 1, width - 2, height - 2, JBUI.scale(4), JBUI.scale(4))
                }
                super.paintComponent(g)
            }
        }
    }

    /**
     * 创建搜索选项切换按钮（Aa / Ab / .*）
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
                when {
                    isSelected -> {
                        g2.color = JBColor(Color(0, 0, 0, 30), Color(255, 255, 255, 40))
                        g2.fillRoundRect(0, 0, width, height, arc, arc)
                        g2.color = UIManager.getColor("Component.focusColor")
                            ?: JBColor(Color(0x3d, 0x98, 0xf5), Color(0x35, 0x92, 0xC4))
                        g2.fillRect(JBUI.scale(2), height - JBUI.scale(2), width - JBUI.scale(4), JBUI.scale(2))
                    }
                    model.isRollover -> {
                        g2.color = JBColor(Color(0, 0, 0, 15), Color(255, 255, 255, 20))
                        g2.fillRoundRect(0, 0, width, height, arc, arc)
                    }
                }
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
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> triggerSearch()
                    KeyEvent.VK_UP -> {
                        if (searchHistory.isEmpty()) return
                        historyIndex = (historyIndex + 1).coerceAtMost(searchHistory.size - 1)
                        searchField.text = searchHistory[historyIndex]
                    }
                    KeyEvent.VK_DOWN -> {
                        if (historyIndex <= 0) {
                            historyIndex = -1
                            searchField.text = ""
                        } else {
                            historyIndex--
                            searchField.text = searchHistory[historyIndex]
                        }
                    }
                }
            }
        })

        val enterKeyListener = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) triggerSearch()
            }
        }
        replaceField.addKeyListener(enterKeyListener)
        includeField.addKeyListener(enterKeyListener)
        excludeField.addKeyListener(enterKeyListener)

        toggleArrowButton.addActionListener { toggleReplaceMode() }
    }

    fun toggleReplaceMode() {
        showReplaceMode = !showReplaceMode
        replaceRow.isVisible = showReplaceMode
        toggleArrowButton.repaint()
        if (showReplaceMode) replaceField.requestFocusInWindow()
        revalidate()
        repaint()
    }

    private fun triggerSearch() {
        if (searchField.text.isBlank()) return
        val query = searchField.text
        searchHistory.remove(query)
        searchHistory.add(0, query)
        historyIndex = -1
        onSearchTriggered?.invoke(buildSearchOptions())
    }

    private fun triggerReplace() {
        onReplaceTriggered?.invoke(buildSearchOptions(), replaceField.text)
    }

    private fun triggerReplaceAll() {
        onReplaceAllTriggered?.invoke(buildSearchOptions(), replaceField.text)
    }

    private fun buildSearchOptions(): SearchOptions {
        return SearchOptions(
            query = searchField.text,
            matchCase = matchCaseToggle.isSelected,
            matchWholeWord = wholeWordToggle.isSelected,
            useRegex = regexToggle.isSelected,
            preserveCase = preserveCaseToggle.isSelected,
            includePatterns = includeField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            excludePatterns = excludeField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            searchOnlyInOpenEditors = openEditorsToggle.isSelected,
            useExcludeSettings = useExcludeSettingsToggle.isSelected
        )
    }

    fun setOnSearchTriggered(callback: (SearchOptions) -> Unit) { onSearchTriggered = callback }
    fun setOnReplaceTriggered(callback: (SearchOptions, String) -> Unit) { onReplaceTriggered = callback }
    fun setOnReplaceAllTriggered(callback: (SearchOptions, String) -> Unit) { onReplaceAllTriggered = callback }

    fun focusSearchField() { searchField.requestFocusInWindow() }
    fun getSearchQuery(): String = searchField.text
    fun setSearchQuery(query: String) { searchField.text = query }
    fun getReplaceText(): String = replaceField.text
    fun buildCurrentSearchOptions(): SearchOptions = buildSearchOptions()
    fun invokeSearch() { onSearchTriggered?.invoke(buildSearchOptions()) }
}
