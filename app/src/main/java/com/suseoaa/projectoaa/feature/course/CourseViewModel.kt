package com.suseoaa.projectoaa.feature.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.network.model.CourseResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val repository: CourseRepository
) : ViewModel() {
    //    状态管理
    private val _studentId = MutableStateFlow("")

    //    当前选中的学年或学期
    private val _selectedXnm = MutableStateFlow("")
    private val _selectedXqm = MutableStateFlow("")

    //    当前是第几周
    private val _currentWeek = MutableStateFlow(1)
    val currentWeek = _currentWeek.asStateFlow()


    // 只要 studentId, xnm, xqm 任意一个变化，就重新去仓库查数据
    // stateIn 把 Flow 转换成 UI 可以直接用的 StateFlow
    @OptIn(ExperimentalCoroutinesApi::class)
    val courseList = combine(_studentId, _selectedXnm, _selectedXqm) { id, xnm, xqm ->
        Triple(id, xnm, xqm)
    }.flatMapLatest { (id, xnm, xqm) ->
        // 去仓库拿数据流 (Repository -> DAO -> Database)
        repository.getCourseFlow(id, xnm, xqm)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // 省电策略：UI 不看时这就暂停
        initialValue = emptyList() // 初始值为空
    )

    //连接数据库

    //    启动时，自动计算当前周次
    init {
        calculateCurrentWeek()
    }

    //    从网络刷新课表
    fun refreshCourseData(response: CourseResponse) {
        viewModelScope.launch {
            try {
                //写入
                repository.updateCourseData(_studentId.value, response)
                _selectedXnm.value = response.xsxx.xNM
                _selectedXqm.value = response.xsxx.xQM
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //    计算当前周次
    private fun calculateCurrentWeek() {
        val startDate = LocalDate.of(2024, 9, 8)
        val today = LocalDate.now()
        if (today.isBefore(startDate)) {
            _currentWeek.value = 1
            return
        }

//        计算想差的天数
        val daysDiff = ChronoUnit.DAYS.between(startDate, today)
//        计算周次
        val week = (daysDiff / 7) + 1
        _currentWeek.value = week.toInt()
    }
}