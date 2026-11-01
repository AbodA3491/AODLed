package com.binaryunit.aodled.service

import android.app.NotificationManager
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.binaryunit.aodled.config.AppRules
import com.binaryunit.aodled.notify.NotifInfo
import com.binaryunit.aodled.notify.qualifies

/** Feeds only user-enabled apps' qualifying notifications into the shared tracker. */
class LightListenerService : NotificationListenerService() {
    private lateinit var colours: ColourResolver
    private lateinit var appRules: AppRules

    override fun onCreate() {
        super.onCreate()
        colours = ColourResolver(this)
        appRules = AppRules(this)
    }

    override fun onListenerConnected() {
        AppState.rebuild = { rebuild(currentRanking) }
        rebuild(currentRanking)
        AppState.changed()
    }

    override fun onListenerDisconnected() {
        AppState.rebuild = null
        requestRebind(ComponentName(this, LightListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        val info = toInfo(sbn, rankingMap)
        val changed = if (info != null) AppState.tracker.posted(info) else AppState.tracker.removed(sbn.key)
        if (changed) AppState.changed()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap) {
        if (AppState.tracker.removed(sbn.key)) AppState.changed()
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap) {
        if (rebuild(rankingMap)) AppState.changed()
    }

    private fun rebuild(rankingMap: RankingMap?): Boolean {
        val active = try { activeNotifications } catch (e: SecurityException) { null } ?: return false
        return AppState.tracker.reset(active.mapNotNull { toInfo(it, rankingMap) })
    }

    private fun toInfo(sbn: StatusBarNotification, rankingMap: RankingMap?): NotifInfo? {
        val n = sbn.notification
        val ranking = Ranking()
        val ranked = rankingMap?.getRanking(sbn.key, ranking) == true
        val importance = if (ranked) ranking.importance else NotificationManager.IMPORTANCE_DEFAULT
        val intercepted = ranked && (ranking.isSuspended || !ranking.matchesInterruptionFilter())
        if (!qualifies(n.flags, importance, intercepted, sbn.packageName, n.channelId ?: "", packageName)) return null
        if (!appRules.enabled(sbn.packageName)) return null
        return NotifInfo(sbn.key, sbn.postTime, n.`when`, colours.colourFor(sbn), sbn.packageName)
    }
}
