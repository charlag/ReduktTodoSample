package io.charlag.sample.reduktsample

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.observables.ConnectableObservable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlin.reflect.KClass

/**
 * Created by charlag on 02/09/17.
 */

fun <S, E> createConnectableKnot(
        initial: S,
        eventsSource: Observable<E>,
        reducer: (S, E) -> S,
        transformer: ObservableTransformer<in Triple<S, S, E>, out E>
): ConnectableObservable<E> {
    return Observable.create<E> { observer ->
        val state = BehaviorSubject.createDefault(initial)
        val events = PublishSubject.create<E>()

        events.withLatestFrom(state, BiFunction<E, S, Triple<S, S, E>> { ev, oldState ->
            val newState = reducer(oldState, ev)
            state.onNext(newState)
            Triple(oldState, newState, ev)
        })
                .compose(transformer)
                .doOnError { RxJavaPlugins.onError(it) }
                .subscribe { t ->
                    observer.onNext(t)
                    events.onNext(t)
                }
        eventsSource.subscribe(events)
    }
            .publish()
}

typealias Transformer<S, E, R> = ObservableTransformer<Triple<S, S, E>, R>

inline fun <A, C, reified T> Observable<Triple<A, A, C>>.ofEventType()
        : Observable<Triple<A, A, T>> =
        filter { it.third is T }
                .map {
                    @Suppress("UNCHECKED_CAST")
                    it as Triple<A, A, T>
                }

inline fun <A, C, reified T : Any> Observable<Triple<A, A, C>>.ofEventType(@Suppress("unused") type: KClass<T>)
        : Observable<Triple<A, A, T>> = ofEventType()
