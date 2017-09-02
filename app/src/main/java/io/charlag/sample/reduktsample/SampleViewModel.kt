package io.charlag.sample.reduktsample

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.persistence.room.Room
import android.util.Log
import io.charlag.sample.reduktsample.SampleViewModel.Event.DbUpdateEvent
import io.charlag.sample.reduktsample.data.db.AppDatabase
import io.charlag.sample.reduktsample.data.db.TodoEntity
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

/**
 * Created by charlag on 8/20/17.
 */

class SampleViewModel(application: Application) : AndroidViewModel(application), LifecycleObserver {

    val viewState: Observable<ViewState>

    private val db =
            Room.databaseBuilder(application, AppDatabase::class.java, "todo.db").build()
    private val viewEvents = PublishSubject.create<SampleViewEvent>()
    private val disposable = CompositeDisposable()

    private val addNewTodoEffect = Transformer<State, Event, Event> { upstream ->
        upstream.ofEventType(Event.NewTodoEvent::class)
                .observeOn(Schedulers.io())
                .switchMap { (_, _, event) ->
                    db.todoDao().add(
                            TodoEntity(
                                    id = 0,
                                    text = event.text,
                                    completed = false
                            )
                    )
                    Observable.empty<Event>()
                }
    }

    private val checkTodoEffect = Transformer<State, Event, Event> { upstream ->
        upstream.ofEventType(Event.TodoCheckedEvent::class)
                .observeOn(Schedulers.io())
                .switchMap { (_, _, event) ->
                    db.todoDao().markCompleted(event.id, event.completed)
                    Observable.empty<Event>()
                }
    }

    private val viewStateEffect = Transformer<State, Event, Event.ViewStateEvent> { upstream ->
        upstream.distinctUntilChanged { old, new -> old.second == new.second }
                .map { (_, state, _) ->
                    val viewState = state.todos.map { TodoViewData(it.id, it.text, it.completed) }
                            .sortedBy { it.completed }
                    Event.ViewStateEvent(viewState)
                }
    }

    init {
        val eventsFromView = viewEvents.map { viewEvent ->
            when (viewEvent) {
                is SampleViewEvent.TodoCheckedViewEvent ->
                    Event.TodoCheckedEvent(viewEvent.id, viewEvent.isChecked)
                is SampleViewEvent.TodoAddedViewEvent ->
                    Event.NewTodoEvent(viewEvent.text)
            }
        }

        val dbEvents = db.todoDao().getAll().toObservable().map(::DbUpdateEvent)

        val externalEvents = Observable.merge(eventsFromView, dbEvents)

        val transformer = Transformer<State, Event, Event> { upstream ->
            upstream
                    .doOnNext {
                        Log.d("SampleViewModel", "Event: ${it.third}")
                        Log.d("SampleViewModel", "State: ${it.second}")
                    }
                    .publish { shared ->
                Observable.merge(
                        shared.compose(addNewTodoEffect),
                        shared.compose(checkTodoEffect),
                        shared.compose(viewStateEffect)
                )
            }
        }

        val events = createConnectableKnot(
                State(listOf()),
                externalEvents,
                this::reducer,
                transformer
        )

        viewState = events.ofType(Event.ViewStateEvent::class.java).map { it.viewState }
                .replay().autoConnect()

        events.connect()
    }

    fun init(view: SampleView) {
        view.events.subscribe(viewEvents::onNext).addTo(disposable)
        view.lifecycle.addObserver(this)
    }

    private fun reducer(state: State, event: Event): State = when (event) {
        is Event.TodoCheckedEvent -> {
            val todos = state.todos.map { todo ->
                if (todo.id == event.id) todo.copy(completed = event.completed)
                else todo
            }
            state.copy(todos = todos)
        }
        is DbUpdateEvent -> state.copy(todos = event.todos)
        else -> state
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        disposable.clear()
    }

    private data class State(
            val todos: List<TodoEntity>
    )

    private sealed class Event {
        data class TodoCheckedEvent(val id: Long, val completed: Boolean) : Event()
        data class NewTodoEvent(val text: String) : Event()
        data class DbUpdateEvent(val todos: List<TodoEntity>) : Event()
        data class ViewStateEvent(val viewState: ViewState) : Event()
    }
}