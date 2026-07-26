package com.whatsapp.scheduler.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whatsapp.scheduler.data.model.ScheduleStatus
import com.whatsapp.scheduler.data.model.ScheduledMessage
import com.whatsapp.scheduler.data.repository.SchedulerRepository
import com.whatsapp.scheduler.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val messages: List<ScheduledMessage> = emptyList(),
    val filterStatus: String = "ALL",
    val searchQuery: String = "",
    val isAccessibilityEnabled: Boolean = true,
    val isExactAlarmGranted: Boolean = true,
    val isBatteryOptIgnored: Boolean = true
)

class MainViewModel(private val repository: SchedulerRepository) : ViewModel() {

    private val _filterStatus = MutableStateFlow("ALL")
    private val _searchQuery = MutableStateFlow("")
    private val _permissionState = MutableStateFlow(Triple(true, true, true))

    val uiState: StateFlow<MainUiState> = combine(
        repository.getAllMessages(),
        _filterStatus,
        _searchQuery,
        _permissionState
    ) { allMessages, filter, query, permissions ->
        val filtered = allMessages.filter { msg ->
            val matchesFilter = when (filter) {
                "PENDING" -> msg.status == ScheduleStatus.PENDING.name
                "SENT" -> msg.status == ScheduleStatus.SENT.name
                "FAILED" -> msg.status == ScheduleStatus.FAILED.name
                "CANCELLED" -> msg.status == ScheduleStatus.CANCELLED.name
                else -> true
            }

            val matchesSearch = query.isEmpty() ||
                    msg.contactName.contains(query, ignoreCase = true) ||
                    msg.phoneNumber.contains(query, ignoreCase = true) ||
                    msg.message.contains(query, ignoreCase = true)

            matchesFilter && matchesSearch
        }

        MainUiState(
            messages = filtered,
            filterStatus = filter,
            searchQuery = query,
            isAccessibilityEnabled = permissions.first,
            isExactAlarmGranted = permissions.second,
            isBatteryOptIgnored = permissions.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun checkPermissions(context: Context) {
        val accessibility = PermissionUtils.isAccessibilityServiceEnabled(context)
        val exactAlarm = PermissionUtils.canScheduleExactAlarms(context)
        val batteryOpt = PermissionUtils.isBatteryOptimizationIgnored(context)
        _permissionState.value = Triple(accessibility, exactAlarm, batteryOpt)
    }

    fun setFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteSchedule(message: ScheduledMessage) {
        viewModelScope.launch {
            repository.deleteSchedule(message)
        }
    }

    fun duplicateSchedule(message: ScheduledMessage) {
        viewModelScope.launch {
            repository.duplicateSchedule(message)
        }
    }

    fun retrySchedule(message: ScheduledMessage) {
        viewModelScope.launch {
            val updated = message.copy(
                status = ScheduleStatus.PENDING.name,
                failureReason = null,
                scheduledDateTime = System.currentTimeMillis() + (60 * 1000),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSchedule(updated)
        }
    }

    class Factory(private val repository: SchedulerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
