package com.ternaryop.feedly

suspend fun FeedlyClient.markSaved(ids: List<String>, saved: Boolean) {
    mark(ids, if (saved) MarkerAction.MarkAsSaved else MarkerAction.MarkAsUnsaved)
}

suspend fun FeedlyClient.markRead(ids: List<String>, read: Boolean) {
    mark(ids, if (read) MarkerAction.MarkAsRead else MarkerAction.KeepUnread)
}
