package com.github.y1j2x34.intellijsearchplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

/**
 * 打开搜索工具窗口的 Action
 */
class OpenSearchToolWindowAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("Project Search") ?: return

        toolWindow.show {
            // 工具窗口打开后，聚焦搜索输入框
            // 注意：这里需要获取到 SearchToolWindowPanel 实例
            // 可以通过 toolWindow.contentManager.getContent(0)?.component 获取
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
