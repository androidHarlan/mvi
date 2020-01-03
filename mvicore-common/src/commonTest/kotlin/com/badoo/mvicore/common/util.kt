package com.badoo.mvicore.common

import com.badoo.mvicore.common.element.Actor
import com.badoo.mvicore.common.element.Bootstrapper
import com.badoo.mvicore.common.element.NewsPublisher
import com.badoo.mvicore.common.element.Reducer
import kotlin.test.assertEquals

class TestSink<T>: Sink<T> {
    private val _values = AtomicRef<List<T>>(emptyList())
    val values: List<T>
        get() = _values.get()

    override fun invoke(value: T) {
        _values.update { it + value }
    }
}

fun <T> TestSink<T>.assertValues(vararg values: T) =
    assertEquals(values.toList(), this.values)

fun <T> TestSink<T>.assertNoValues() =
    assertEquals(emptyList(), this.values)

fun <State, Effect> reducer(block: (state: State, effect: Effect) -> State) =
    object : Reducer<State, Effect> {
        override fun invoke(state: State, effect: Effect): State = block(state, effect)
    }

fun <State, Wish, Effect> actor(block: (state: State, wish: Wish) -> Source<out Effect>) =
    object : Actor<State, Wish, Effect> {
        override fun invoke(state: State, action: Wish): Source<out Effect> =
            block(state, action)
    }

fun <Action> bootstrapper(block: () -> Source<Action>) =
    object : Bootstrapper<Action> {
        override fun invoke(): Source<Action> =
            block()
    }

fun <Action, Effect, State, News> newsPublisher(block: (old: State, action: Action, effect: Effect, new: State) -> News?) =
    object : NewsPublisher<Action, Effect, State, News> {
        override fun invoke(old: State, action: Action, effect: Effect, new: State): News? =
            block(old, action, effect, new)
    }