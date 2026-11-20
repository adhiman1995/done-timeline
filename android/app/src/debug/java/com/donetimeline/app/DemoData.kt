package com.donetimeline.app

import com.donetimeline.app.data.Task
import java.time.LocalDate
import java.time.ZoneId

/** Debug builds only: sample tasks for previewing the UI without signing in (`adb shell am start ... --ez demo true`). */
object DemoData {
    fun tasks(): List<Task>? {
        val zone = ZoneId.systemDefault()
        fun at(daysAgo: Long, hour: Int, minute: Int = 0) =
            LocalDate.now().minusDays(daysAgo).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
                .coerceAtMost(System.currentTimeMillis() - daysAgo * 60_000 - hour * 1000L)
        return listOf(
            Task("d1", "Shipped the onboarding flow", "Feature flag on for 10% of new users.", "Work", true, at(0, 9, 30)),
            Task("d2", "Morning run · 5 km", "28 minutes, easy pace.", "Health", false, at(0, 8)),
            Task("d3", "Reviewed pull requests", "", "Work", false, at(0, 7, 12)),
            Task("d4", "Finished chapter 4 of Designing Data-Intensive Apps", "Notes on replication lag.", "Learning", false, at(1, 18)),
            Task("d5", "Paid electricity bill", "", "Personal", false, at(1, 13)),
            Task("d6", "Called mom", "", "Family", true, at(2, 20)),
            Task("d7", "Fixed flaky login test", "Race condition in the session refresh.", "Work", false, at(2, 9)),
            Task("d8", "Yoga class", "", "Health", false, at(3, 19)),
            Task("d9", "Wrote design doc for sync", "", "Work", true, at(4, 11)),
            Task("d10", "Grocery shopping", "", "Personal", false, at(5, 12)),
            Task("d11", "Quarterly goals review", "", "Work", false, at(9, 9)),
        )
    }
}
