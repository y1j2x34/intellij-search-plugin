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
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JLabel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

/**
 * 搜索结果面板 - 显示搜索结果树
 */
class SearchResultsPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val rootNode = DefaultMutableTreeNode("Search Results")
    private val treeModel = DefaultTreeModel(rootNode)
    private val resultTree = Tree(treeModel)
    private val summaryLabel = JLabel("No results")

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        border = JBUI.Borders.empty(8)

        // 顶部摘要
        summaryLabel.border = JBUI.Borders.empty(4)
        add(summaryLabel, BorderLayout.NORTH)

        // 结果树
        resultTree.isRootVisible = false
        resultTree.showsRootHandles = true
        resultTree.cellRenderer = SearchResultTreeCellRenderer()

        val scrollPane = JBScrollPane(resultTree)
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun setupListeners() {
        // 双击跳转到文件
        resultTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val path = resultTree.getPathForLocation(e.x, e.y) ?: return
                    val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                    handleNodeDoubleClick(node)
                }
            }
        })
    }

    /**
     * 显示搜索结果
     */
    fun displaySearchResult(result: SearchResult) {
        rootNode.removeAllChildren()

        result.fileResults.forEach { fileResult ->
            addFileResult(fileResult)
        }

        treeModel.reload()
        expandAllNodes()

        // 更新摘要
        summaryLabel.text = "${result.totalMatches} results in ${result.totalFiles} files (${result.searchTimeMs}ms)"
    }

    /**
     * 添加单个文件结果（用于渐进式显示）
     */
    fun addFileResult(fileResult: FileSearchResult) {
        val fileNode = DefaultMutableTreeNode(FileResultNode(fileResult.file, fileResult.matchCount))

        fileResult.matches.forEach { lineMatch ->
            val lineNode = DefaultMutableTreeNode(LineMatchNode(fileResult.file, lineMatch))
            fileNode.add(lineNode)
        }

        rootNode.add(fileNode)
        treeModel.reload(rootNode)
    }

    /**
     * 清空结果
     */
    fun clearResults() {
        rootNode.removeAllChildren()
        treeModel.reload()
        summaryLabel.text = "No results"
    }

    /**
     * 更新摘要信息
     */
    fun updateSummary(totalMatches: Int, totalFiles: Int) {
        summaryLabel.text = "$totalMatches results in $totalFiles files"
    }

    private fun expandAllNodes() {
        for (i in 0 until resultTree.rowCount) {
            resultTree.expandRow(i)
        }
    }

    private fun handleNodeDoubleClick(node: DefaultMutableTreeNode) {
        when (val userObject = node.userObject) {
            is FileResultNode -> {
                // 点击文件节点，打开文件
                openFile(userObject.file, 0)
            }
            is LineMatchNode -> {
                // 点击行节点，跳转到具体行
                openFile(userObject.file, userObject.lineMatch.lineNumber)
            }
        }
    }

    private fun openFile(file: VirtualFile, lineNumber: Int) {
        val descriptor = if (lineNumber > 0) {
            OpenFileDescriptor(project, file, lineNumber - 1, 0)
        } else {
            OpenFileDescriptor(project, file)
        }
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
    }

    /**
     * 文件结果节点数据
     */
    data class FileResultNode(
        val file: VirtualFile,
        val matchCount: Int
    )

    /**
     * 行匹配节点数据
     */
    data class LineMatchNode(
        val file: VirtualFile,
        val lineMatch: LineMatch
    )

    /**
     * 自定义树节点渲染器
     */
    private class SearchResultTreeCellRenderer : ColoredTreeCellRenderer() {
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

                    // 高亮显示匹配的文本
                    val line = userObject.lineMatch.lineContent.trim()
                    val ranges = userObject.lineMatch.matchRanges

                    var lastIndex = 0
                    ranges.forEach { range ->
                        if (range.first > lastIndex) {
                            append(line.substring(lastIndex, range.first), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                        }
                        append(
                            line.substring(range.first, range.last),
                            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                        )
                        lastIndex = range.last
                    }
                    if (lastIndex < line.length) {
                        append(line.substring(lastIndex), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    }
                }
            }
        }
    }
}
