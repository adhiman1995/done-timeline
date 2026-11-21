package com.donetimeline.app

import com.donetimeline.app.data.AiKind
import com.donetimeline.app.data.Goal
import com.donetimeline.app.data.Task

/** Release builds have no demo data. */
object DemoData {
    fun tasks(): List<Task>? = null
    fun goals(): List<Goal>? = null
    fun ai(kind: AiKind): String? = null
}
