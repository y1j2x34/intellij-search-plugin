package com.github.y1j2x34.intellijsearchplugin.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * 搜索工具窗口工厂
 */
class SearchToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val searchPanel = SearchToolWindowPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(searchPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
