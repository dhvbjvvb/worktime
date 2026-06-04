package com.worktime.checkin.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ProfileState {
    var nickname by mutableStateOf("用户昵称")
    var avatarUri by mutableStateOf<String?>(null)
}
