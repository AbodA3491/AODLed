package com.binaryunit.aodled.notify

import com.binaryunit.aodled.config.LightOff

data class NotifInfo(
    val key: String,
    val postTime: Long,
    val whenMillis: Long,
    val colour: Int,
    val packageName: String = "",
)

class PendingTracker {
    private class Entry(val info: NotifInfo, val alertAt: Long)
    private val entries = HashMap<String, Entry>()
    private var seenAt = 0L

    fun reset(all: List<NotifInfo>): Boolean = changes {
        val old = HashMap(entries)
        entries.clear()
        all.forEach { entries[it.key] = entryFor(it, old[it.key]) }
    }

    fun posted(n: NotifInfo): Boolean = changes { entries[n.key] = entryFor(n, entries[n.key]) }
    fun removed(key: String): Boolean = changes { entries.remove(key) }
    fun markSeen(nowMillis: Long) { seenAt = nowMillis }

    fun current(mode: LightOff): NotifInfo? = entries.values
        .filter { mode == LightOff.ON_DISMISS || it.alertAt > seenAt }
        .maxByOrNull { it.alertAt }?.info

    private inline fun changes(update: () -> Unit): Boolean {
        val before = shown()
        update()
        return shown() != before
    }

    private fun shown() = LightOff.entries.map { current(it)?.let { n -> n.packageName to n.colour } }

    private fun entryFor(n: NotifInfo, previous: Entry?): Entry =
        if (previous == null || previous.info.whenMillis != n.whenMillis) Entry(n, n.postTime)
        else Entry(n, previous.alertAt)
}
