package com.freedu.myinterviews.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(repo: TrackerRepository) : ViewModel() {
    val monthOffset = MutableStateFlow(0)
    private val _selectedDay = MutableStateFlow(startOfToday())
    val selectedDay = _selectedDay.asStateFlow()

    fun selectDay(dayStart: Long) { _selectedDay.value = dayStart }
    fun shiftMonth(delta: Int) { monthOffset.value += delta }

    val rounds = repo.observeAllRounds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedRounds = combine(rounds, _selectedDay) { list, day ->
        list.filter { it.scheduledAt in day until day + 86_400_000L }
            .sortedBy { it.scheduledAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun startOfToday(): Long {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }
    }
}

/** Cells for the currently visible month grid. Null = leading blank. */
fun monthCells(offset: Int): List<Long?> {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, offset)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val cells = mutableListOf<Long?>()
    repeat(firstDow - 1) { cells.add(null) }
    for (d in 1..maxDay) {
        val c = Calendar.getInstance()
        c.set(year, month, d, 0, 0, 0); c.set(Calendar.MILLISECOND, 0)
        cells.add(c.timeInMillis)
    }
    // Pad trailing blanks so the grid always forms complete weeks.
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

fun monthTitle(offset: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, offset)
    return java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(cal.time)
}
