package com.github.y1j2x34.intellijsearchplugin.ui

import com.github.y1j2x34.intellijsearchplugin.model.FileSearchResult
import com.github.y1j2x34.intellijsearchplugin.model.LineMatch
import com.github.y1j2x34.intellijsearchplugin.model.SearchResult
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.AsyncProcessIcon
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * 搜索结果面板 - 显示搜索结果树，支持 VS Code 风格的内联替换按钮
 */
class SearchResultsPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val rootNode = DefaultMutableTreeNode("Search Results")
    private val treeModel = DefaultTreeModel(rootNode)
    private val resultTree = Tree(treeModel)
    private val summaryLabel = JLabel("No results")

    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)

    // Hovered row tracking for button visibility
    private var hoveredRow = -1

    // Replace callbacks — set these to enable inline replace buttons
    var onReplaceMatch: ((VirtualFile, LineMatch) -> Unit)? = null
    var onReplaceAllInFile: ((VirtualFile) -> Unit)? = null

    // Remove/close callbacks
    var onRemoveMatch: ((VirtualFile, LineMatch) -> Unit)? = null
    var onRemoveFile: ((VirtualFile) -> Unit)? = null

    companion object {
        private const val CARD_RESULTS = "results"
        private const val CARD_LOADING = "loading"
        private val BTN_SIZE get() = JBUI.scale(20)
        private val BTN_GAP get() = JBUI.scale(2)
        private val BTN_MARGIN get() = JBUI.scale(4)
        // Total width for two buttons: [replace][gap][close][margin]
        private val TWO_BTNS_WIDTH get() = BTN_MARGIN + BTN_SIZE + BTN_GAP + BTN_SIZE + BTN_MARGIN
    }

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        border = JBUI.Borders.empty(8)

        // Header panel with summary label and collapse/expand button
        val headerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(4)

            add(summaryLabel, BorderLayout.WEST)

            val collapseExpandBtn = createCollapseExpandButton()
            add(collapseExpandBtn, BorderLayout.EAST)
        }
        add(headerPanel, BorderLayout.NORTH)

        resultTree.isRootVisible = false
        resultTree.showsRootHandles = true
        resultTree.cellRenderer = SearchResultTreeCellRenderer()

        val scrollPane = JBScrollPane(resultTree)

        val loadingPanel = JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints()
            val innerPanel = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
                isOpaque = false
                val spinnerIcon = AsyncProcessIcon("Searching")
                add(spinnerIcon, BorderLayout.CENTER)
                val textLabel = JLabel("Searching...", SwingConstants.CENTER).apply {
                    foreground = SimpleTextAttributes.GRAYED_ATTRIBUTES.fgColor
                }
                add(textLabel, BorderLayout.SOUTH)
            }
            add(innerPanel, gbc)
        }

        contentPanel.add(scrollPane, CARD_RESULTS)
        contentPanel.add(loadingPanel, CARD_LOADING)
        cardLayout.show(contentPanel, CARD_RESULTS)

        add(contentPanel, BorderLayout.CENTER)
    }

    fun showLoading() {
        summaryLabel.text = "Searching..."
        cardLayout.show(contentPanel, CARD_LOADING)
    }

    fun hideLoading() {
        cardLayout.show(contentPanel, CARD_RESULTS)
    }

    private fun setupListeners() {
        resultTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = resultTree.getRowForLocation(e.x, e.y)
                val path = resultTree.getPathForLocation(e.x, e.y) ?: return
                val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return

                // Check if click landed on an action button
                if (row >= 0) {
                    val compWidth = resultTree.width
                    // Close button: rightmost
                    val closeX = compWidth - BTN_MARGIN - BTN_SIZE
                    // Replace button: to the left of close
                    val replaceX = closeX - BTN_GAP - BTN_SIZE

                    if (e.x >= closeX && e.x <= closeX + BTN_SIZE) {
                        when (val userObject = node.userObject) {
                            is FileResultNode -> { onRemoveFile?.invoke(userObject.file); return }
                            is LineMatchNode -> { onRemoveMatch?.invoke(userObject.file, userObject.lineMatch); return }
                        }
                    }
                    if (e.x >= replaceX && e.x <= replaceX + BTN_SIZE) {
                        when (val userObject = node.userObject) {
                            is FileResultNode -> { onReplaceAllInFile?.invoke(userObject.file); return }
                            is LineMatchNode -> { onReplaceMatch?.invoke(userObject.file, userObject.lineMatch); return }
                        }
                    }
                }

                // Double-click to open file
                if (e.clickCount == 2) {
                    handleNodeDoubleClick(node)
                }
            }

            override fun mouseExited(e: MouseEvent) {
                hoveredRow = -1
                resultTree.repaint()
            }
        })

        resultTree.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val row = resultTree.getRowForLocation(e.x, e.y)
                if (row != hoveredRow) {
                    hoveredRow = row
                    resultTree.repaint()
                }
            }
        })
    }

    fun displaySearchResult(result: SearchResult) {
        rootNode.removeAllChildren()
        result.fileResults.forEach { addFileResult(it) }
        treeModel.reload()
        expandAllNodes()
        summaryLabel.text = "${result.totalMatches} results in ${result.totalFiles} files (${result.searchTimeMs}ms)"
    }

    fun addFileResult(fileResult: FileSearchResult) {
        val fileNode = DefaultMutableTreeNode(FileResultNode(fileResult.file, fileResult.matchCount))
        fileResult.matches.forEach { lineMatch ->
            fileNode.add(DefaultMutableTreeNode(LineMatchNode(fileResult.file, lineMatch)))
        }
        rootNode.add(fileNode)
        treeModel.reload(rootNode)
    }

    fun clearResults() {
        rootNode.removeAllChildren()
        treeModel.reload()
        summaryLabel.text = "No results"
    }

    fun updateSummary(totalMatches: Int, totalFiles: Int) {
        summaryLabel.text = "$totalMatches results in $totalFiles files"
    }

    private fun expandAllNodes() {
        for (i in 0 until resultTree.rowCount) {
            resultTree.expandRow(i)
        }
    }

    private fun collapseAllNodes() {
        for (i in resultTree.rowCount - 1 downTo 0) {
            resultTree.collapseRow(i)
        }
    }

    private fun createCollapseExpandButton(): JButton {
        return object : JButton() {
            private var isExpanded = true

            init {
                isFocusable = false
                isFocusPainted = false
                isContentAreaFilled = false
                isBorderPainted = false
                isOpaque = false
                isRolloverEnabled = true
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                toolTipText = "Collapse All"
                val sz = JBUI.scale(20)
                preferredSize = Dimension(sz, sz)
                minimumSize = Dimension(sz, sz)
                maximumSize = Dimension(sz, sz)

                addActionListener {
                    isExpanded = !isExpanded
                    if (isExpanded) {
                        expandAllNodes()
                        toolTipText = "Collapse All"
                    } else {
                        collapseAllNodes()
                        toolTipText = "Expand All"
                    }
                    repaint()
                }
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                if (model.isRollover) {
                    g2.color = UIManager.getColor("ActionButton.hoverBackground")
                        ?: Color(0, 0, 0, 20)
                    g2.fillRoundRect(1, 1, width - 2, height - 2, JBUI.scale(4), JBUI.scale(4))
                }

                val icon = if (isExpanded) AllIcons.Actions.Collapseall else AllIcons.Actions.Expandall
                icon.paintIcon(this, g2, (width - icon.iconWidth) / 2, (height - icon.iconHeight) / 2)
            }
        }
    }

    private fun handleNodeDoubleClick(node: DefaultMutableTreeNode) {
        when (val userObject = node.userObject) {
            is FileResultNode -> openFile(userObject.file, 0, 0)
            is LineMatchNode -> {
                // Navigate to the first match position in the line
                val column = if (userObject.lineMatch.matchRanges.isNotEmpty()) {
                    userObject.lineMatch.matchRanges[0].first
                } else {
                    0
                }
                openFile(userObject.file, userObject.lineMatch.lineNumber, column)
            }
        }
    }

    private fun openFile(file: VirtualFile, lineNumber: Int, column: Int) {
        val descriptor = if (lineNumber > 0) {
            OpenFileDescriptor(project, file, lineNumber - 1, column)
        } else {
            OpenFileDescriptor(project, file)
        }
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
    }

    data class FileResultNode(val file: VirtualFile, val matchCount: Int)
    data class LineMatchNode(val file: VirtualFile, val lineMatch: LineMatch)

    /**
     * Renders each tree row. On hover, paints inline action buttons on the right:
     * - File node: one "Replace All in File" button
     * - Match node: one "Replace" button
     */
    private inner class SearchResultTreeCellRenderer : ColoredTreeCellRenderer() {

        private val replaceIcon = AllIcons.Actions.Replace
        private val closeIcon = AllIcons.Actions.Close

        override fun customizeCellRenderer(
            tree: javax.swing.JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val node = value as? DefaultMutableTreeNode ?: return

            when (val userObject = node.userObject) {
                is FileResultNode -> {
                    icon = AllIcons.FileTypes.Any_type
                    append(userObject.file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    append(" (${userObject.matchCount})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    append(" ${userObject.file.parent?.path ?: ""}", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
                }
                is LineMatchNode -> {
                    icon = null
                    append("  ${userObject.lineMatch.lineNumber}: ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    val originalLine = userObject.lineMatch.lineContent
                    val trimStart = originalLine.indexOfFirst { !it.isWhitespace() }
                    val line = originalLine.trim()

                    // Adjust ranges to account for trimmed leading whitespace
                    val adjustedRanges = if (trimStart > 0) {
                        userObject.lineMatch.matchRanges.mapNotNull { range ->
                            val newFirst = range.first - trimStart
                            val newLast = range.last - trimStart
                            // Only include ranges that are still valid after trimming
                            if (newFirst >= 0 && newLast < line.length) {
                                newFirst..newLast
                            } else null
                        }
                    } else {
                        userObject.lineMatch.matchRanges
                    }

                    var lastIndex = 0
                    adjustedRanges.forEach { range ->
                        if (range.first > lastIndex) {
                            append(line.substring(lastIndex, range.first), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                        }
                        // range.last is inclusive, so we need to add 1 for substring's exclusive end
                        val endIndex = (range.last + 1).coerceAtMost(line.length)
                        append(line.substring(range.first, endIndex), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                        lastIndex = endIndex
                    }
                    if (lastIndex < line.length) {
                        append(line.substring(lastIndex), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    }
                }
            }
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            val row = getRowForThisComponent() ?: return
            if (row != hoveredRow) return

            val node = getNodeForRow(row) ?: return
            val userObject = node.userObject

            val btnY = (height - BTN_SIZE) / 2
            val closeX = width - BTN_MARGIN - BTN_SIZE
            val replaceX = closeX - BTN_GAP - BTN_SIZE

            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            when (userObject) {
                is FileResultNode -> {
                    paintActionButton(g2, replaceIcon, replaceX, btnY)
                    paintActionButton(g2, closeIcon, closeX, btnY)
                }
                is LineMatchNode -> {
                    paintActionButton(g2, replaceIcon, replaceX, btnY)
                    paintActionButton(g2, closeIcon, closeX, btnY)
                }
            }
        }

        private fun paintActionButton(g2: Graphics2D, icon: Icon, x: Int, y: Int) {
            // Subtle hover background
            g2.color = UIManager.getColor("ActionButton.hoverBackground")
                ?: Color(0, 0, 0, 20)
            g2.fillRoundRect(x - JBUI.scale(2), y - JBUI.scale(2),
                BTN_SIZE + JBUI.scale(4), BTN_SIZE + JBUI.scale(4),
                JBUI.scale(4), JBUI.scale(4))
            icon.paintIcon(this, g2, x + (BTN_SIZE - icon.iconWidth) / 2, y + (BTN_SIZE - icon.iconHeight) / 2)
        }

        private fun getRowForThisComponent(): Int? {
            val tree = parent as? JTree ?: return null
            for (i in 0 until tree.rowCount) {
                val bounds = tree.getRowBounds(i) ?: continue
                if (bounds.y == y) return i
            }
            return null
        }

        private fun getNodeForRow(row: Int): DefaultMutableTreeNode? {
            val tree = parent as? JTree ?: return null
            val path = tree.getPathForRow(row) ?: return null
            return path.lastPathComponent as? DefaultMutableTreeNode
        }
    }
}
