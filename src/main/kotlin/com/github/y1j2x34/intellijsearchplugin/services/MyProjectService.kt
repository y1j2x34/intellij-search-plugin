package com.github.y1j2x34.intellijsearchplugin.services

import com.intellij.openapi.project.Project
import com.github.y1j2x34.intellijsearchplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
