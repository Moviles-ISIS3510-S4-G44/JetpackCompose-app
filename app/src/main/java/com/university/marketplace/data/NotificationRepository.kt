package com.university.marketplace.data

import com.university.marketplace.data.local.NotificationDao
import com.university.marketplace.data.local.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val notificationDao: NotificationDao) {
    val notifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadCount: Flow<Int> = notificationDao.getUnreadCount()

    suspend fun insertNotification(message: String, type: String) {
        notificationDao.insertNotification(
            NotificationEntity(
                message = message,
                type = type
            )
        )
    }

    suspend fun markAsRead(id: Int) {
        notificationDao.markAsRead(id)
    }

    suspend fun clearAll() {
        notificationDao.clearAll()
    }
}
