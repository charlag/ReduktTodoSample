package io.charlag.sample.reduktsample

import android.arch.lifecycle.LifecycleOwner
import io.reactivex.Observable

/**
 * Created by charlag on 8/20/17.
 */

interface SampleView : LifecycleOwner {
    val events: Observable<SampleViewEvent>
}

sealed class SampleViewEvent {
    data class TodoCheckedViewEvent(val id: Long, val isChecked: Boolean) : SampleViewEvent()
    data class TodoAddedViewEvent(val text: String) : SampleViewEvent()
}

data class TodoViewData(val id: Long, val text: String, val completed: Boolean)

typealias ViewState = List<TodoViewData>
