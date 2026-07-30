package com.example.classroomconnect.data.models

data class Discussion(
    var userNAME: String? = "",
    var message: String? = "",
    var discussionId: String? = "",
    val senderUid: String = "",
    var timestamp: Long = 0L
)
