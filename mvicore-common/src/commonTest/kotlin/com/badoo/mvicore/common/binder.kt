package com.badoo.mvicore.common

import com.badoo.mvicore.common.binder.NotNullConnector
import com.badoo.mvicore.common.binder.bind
import com.badoo.mvicore.common.binder.binder
import com.badoo.mvicore.common.binder.using
import com.badoo.mvicore.common.lifecycle.Lifecycle
import com.badoo.reaktive.utils.freeze
import kotlin.test.Test

class BinderTest {
    private val source = source<Int>()
    private val sink = TestSink<Int>()

    @Test
    fun binder_without_lifecycle_connects_source_and_sink() {
        binder().freeze().apply {
            bind(source to sink)
        }

        source.accept(0)
        sink.assertValues(0)
    }

    @Test
    fun binder_without_lifecycle_does_not_connect_source_and_sink_after_cancel() {
        val binder = binder().freeze().apply {
            bind(source to sink)
        }

        binder.cancel()

        source.accept(0)
        sink.assertNoValues()
    }

    @Test
    fun binder_without_lifecycle_connects_source_and_sink_using_mapper() {
        binder().freeze().apply {
            bind(source to sink using { it + 1 })
        }

        source.accept(0)
        sink.assertValues(1)
    }

    @Test
    fun binder_without_lifecycle_connects_source_and_sink_skips_nulls_from_mapper() {
        binder().freeze().apply {
            bind(source to sink using { if (it % 2 == 0) null else it })
        }

        source.accept(0)
        source.accept(1)
        source.accept(2)
        sink.assertValues(1)
    }

    @Test
    fun binder_without_lifecycle_connects_source_and_sink_using_connector() {
        val connector = NotNullConnector<Int, Int> { it }.freeze()
        binder().freeze().apply {
            bind(source to sink using connector)
        }

        source.accept(0)
        sink.assertValues(0)
    }

    @Test
    fun binder_with_lifecycle_connects_source_and_sink_when_active() {
        val lifecycle = Lifecycle.manual().freeze()
        binder(lifecycle).freeze().apply {
            bind(source to sink)
        }

        lifecycle.begin()
        source.accept(0)

        sink.assertValues(0)
    }

    @Test
    fun binder_with_lifecycle_does_not_connect_source_and_sink_before_active() {
        val lifecycle = Lifecycle.manual().freeze()
        binder(lifecycle).freeze().apply {
            bind(source to sink)
        }

        source.accept(0)
        lifecycle.begin()

        sink.assertNoValues()
    }

    @Test
    fun binder_with_lifecycle_disconnect_source_and_sink_after_end() {
        val lifecycle = Lifecycle.manual().freeze()
        binder(lifecycle).freeze().apply {
            bind(source to sink)
        }

        lifecycle.begin()
        source.accept(0)
        lifecycle.end()
        source.accept(1)

        sink.assertValues(0)
    }

    @Test
    fun binder_with_lifecycle_reconnect_source_and_sink_after_begin() {
        val lifecycle = Lifecycle.manual().freeze()
        binder(lifecycle).freeze().apply {
            bind(source to sink)
        }

        lifecycle.begin()
        source.accept(0)
        lifecycle.end()
        source.accept(1)
        lifecycle.begin()
        source.accept(2)

        sink.assertValues(0, 2)
    }

    @Test
    fun binder_with_lifecycle_does_not_reconnect_source_and_sink_after_cancel() {
        val lifecycle = Lifecycle.manual().freeze()
        val binder = binder(lifecycle).freeze().apply {
            bind(source to sink)
        }

        lifecycle.begin()
        source.accept(0)
        lifecycle.end()
        binder.cancel()
        lifecycle.begin()
        source.accept(2)

        sink.assertValues(0)
    }

    @Test
    fun binder_with_lifecycle_connects_source_and_sink_if_lifecycle_started() {
        val lifecycle = Lifecycle.manual().freeze()
        lifecycle.begin()

        binder(lifecycle).freeze().apply {
            bind(source to sink)
        }

        source.accept(0)

        sink.assertValues(0)
    }

    @Test
    fun binder_with_lifecycle_does_not_reconnect_on_duplicated_lifecycle_events() {
        val lifecycle = Lifecycle.manual()

        binder(lifecycle).apply {
            bind(source to sink)
        }

        lifecycle.begin()
        lifecycle.begin()

        source.accept(0)

        sink.assertValues(0)
    }

    @Test
    fun binder_covariant_endpoints_compile_for_pair() {
        val sink = sinkOf<Any> { /* no-op */ }
        binder().freeze().bind(source to sink)
    }

    @Test
    fun binder_covariant_endpoints_compile_for_connection() {
        val sink = sinkOf { _: Any -> /* no-op */ }
        val intToString: (Int) -> String = { it.toString() }
        binder().freeze().bind(source to sink using intToString)
    }

    @Test
    fun binder_delivers_message_to_all_sinks_on_dispose() {
        val binder = binder().freeze()

        val sink2 = sinkOf { _: Int -> binder.cancel() }

        binder.bind(source to sink2)
        binder.bind(source to sink)

        source.accept(0)

        sink.assertValues(0)
    }

    @Test
    fun binder_messages_sent_on_initialize_are_not_lost() {
        val passThroughSource = source<Int>()
        binder {
            bind(source to passThroughSource)
            source.accept(0)
            bind(passThroughSource to sink)
        }.freeze()

        sink.assertValues(0)
    }
}
