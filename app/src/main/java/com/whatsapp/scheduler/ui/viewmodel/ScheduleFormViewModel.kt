package com.whatsapp.scheduler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whatsapp.scheduler.data.model.RepeatType
import com.whatsapp.scheduler.data.model.ScheduleStatus
import com.whatsapp.scheduler.data.model.ScheduledMessage
import com.whatsapp.scheduler.data.repository.SchedulerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FormState(
    val id: Long = 0,
    val contactName: String = "",
    val phoneNumber: String = "",
    val messageText: String = "",
    val attachmentPath: String? = null,
    val scheduledDateTime: Long = System.currentTimeMillis() + (10 * 60 * 1000),
    val repeatType: String = RepeatType.ONCE.name,
    val isEditMode: Boolean = false,
    val validationError: String? = null,
    val isSaved: Boolean = false
)

class ScheduleFormViewModel(private val repository: SchedulerRepository) : ViewModel() {

    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    fun loadScheduleForEdit(messageId: Long) {
        if (messageId <= 0) return
        viewModelScope.launch {
            val msg = repository.getMessageById(messageId) ?: return@launch
            _formState.value = FormState(
                id = msg.id,
                contactName = msg.contactName,
                phoneNumber = msg.phoneNumber,
                messageText = msg.message,
                attachmentPath = msg.attachmentPath,
                scheduledDateTime = msg.scheduledDateTime,
                repeatType = msg.repeatType,
                isEditMode = true
            )
        }
    }

    fun updateContactName(name: String) {
        _formState.value = _formState.value.copy(contactName = name, validationError = null)
    }

    fun updatePhoneNumber(phone: String) {
        _formState.value = _formState.value.copy(phoneNumber = phone, validationError = null)
    }

    fun updateMessageText(text: String) {
        _formState.value = _formState.value.copy(messageText = text, validationError = null)
    }

    fun updateAttachmentPath(path: String?) {
        _formState.value = _formState.value.copy(attachmentPath = path)
    }

    fun updateScheduledDateTime(timeInMillis: Long) {
        _formState.value = _formState.value.copy(scheduledDateTime = timeInMillis, validationError = null)
    }

    fun updateRepeatType(repeat: String) {
        _formState.value = _formState.value.copy(repeatType = repeat)
    }

    fun saveSchedule() {
        val state = _formState.value

        if (state.contactName.isBlank()) {
            _formState.value = state.copy(validationError = "Please enter a contact name")
            return
        }
        if (state.phoneNumber.isBlank()) {
            _formState.value = state.copy(validationError = "Please enter a valid phone number")
            return
        }
        if (state.scheduledDateTime <= System.currentTimeMillis()) {
            _formState.value = state.copy(validationError = "Scheduled time must be in the future")
            return
        }

        viewModelScope.launch {
            val scheduledMessage = ScheduledMessage(
                id = state.id,
                contactName = state.contactName.trim(),
                phoneNumber = state.phoneNumber.trim(),
                message = state.messageText.trim(),
                attachmentPath = state.attachmentPath,
                scheduledDateTime = state.scheduledDateTime,
                repeatType = state.repeatType,
                status = ScheduleStatus.PENDING.name,
                failureReason = null,
                createdAt = if (state.isEditMode) System.currentTimeMillis() else System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            if (state.isEditMode) {
                repository.updateSchedule(scheduledMessage)
            } else {
                repository.insertSchedule(scheduledMessage)
            }

            _formState.value = state.copy(isSaved = true)
        }
    }

    class Factory(private val repository: SchedulerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScheduleFormViewModel(repository) as T
        }
    }
}
