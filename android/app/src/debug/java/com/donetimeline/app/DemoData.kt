package com.donetimeline.app

import com.donetimeline.app.data.AiKind
import com.donetimeline.app.data.Goal
import com.donetimeline.app.data.GoalPeriod
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

    fun goals(): List<Goal>? = listOf(
        Goal("g1", "Health", 4, GoalPeriod.WEEK),
        Goal("g2", "", 3, GoalPeriod.DAY),
        Goal("g3", "Learning", 8, GoalPeriod.MONTH),
    )

    /** Canned answers so the AI screens can be previewed offline. */
    fun ai(kind: AiKind): String? = when (kind) {
        AiKind.STANDUP -> "*Yesterday*\n• Shipped the onboarding flow to 10% of new users\n• Reviewed pull requests\n• Morning run, 5 km\n*Today*\n• Monitor onboarding metrics\n*Blockers*\n• None"
        AiKind.WEEKLY -> "## Highlights\n- **Shipped the onboarding flow** to 10% of new users ★\n- Wrote the **design doc for sync**\n- Fixed a flaky login test\n## Where your time went\n- **Work**: 5 tasks, mostly shipping and reviews\n- **Health**: 2 tasks, runs and yoga\n## Patterns\n- You logged something on 6 of the last 7 days.\n## Next week\n- Keep the morning runs going and block time to finish chapter 5."
    }
}
