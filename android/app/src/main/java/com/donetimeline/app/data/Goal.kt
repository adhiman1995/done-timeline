package com.donetimeline.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class GoalPeriod(val key: String, val label: String, val per: String, val now: String) {
    DAY("day", "Day", "per day", "today"),
    WEEK("week", "Week", "per week", "this week"),
    MONTH("month", "Month", "per month", "this month");

    /** Start of the current period (weeks start on Monday). */
    fun start(today: LocalDate = LocalDate.now()): LocalDate = when (this) {
        DAY -> today
        WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        MONTH -> today.withDayOfMonth(1)
    }

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: WEEK
    }
}

/** A target such as "Health · 4 per week". [category] blank = any task. Stored at users/{uid}/goals/{id}. */
data class Goal(val id: String, val category: String, val target: Int, val period: GoalPeriod) {
    val name get() = category.ifBlank { "Any task" }
}

data class GoalProgress(val goal: Goal, val done: Int) {
    val ratio get() = (done.toFloat() / goal.target).coerceAtMost(1f)
    val met get() = done >= goal.target
}

fun Goal.progress(tasks: List<Task>, zone: ZoneId = ZoneId.systemDefault()): GoalProgress {
    val start = period.start().atStartOfDay(zone).toInstant()
    val cat = category.trim().lowercase()
    val done = tasks.count {
        !Instant.ofEpochMilli(it.completedAt).isBefore(start) && (cat.isEmpty() || it.category.trim().lowercase() == cat)
    }
    return GoalProgress(this, done)
}
