package com.badoo.binder

import com.badoo.binder.lifecycle.Lifecycle
import com.badoo.binder.lifecycle.Lifecycle.Event.BEGIN
import com.badoo.binder.lifecycle.Lifecycle.Event.END
import com.badoo.binder.middleware.base.Middleware
import com.badoo.binder.middleware.wrapWithMiddleware
import io.reactivex.Observable
import io.reactivex.Observable.wrap
import io.reactivex.ObservableSource
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.UnicastSubject

class Binder(
    private val lifecycle: Lifecycle? = null,
) : Disposable {
    private val bindings = mutableListOf<Binding>()
    private val disposables = CompositeDisposable()
    private val connectionDisposables = CompositeDisposable()
    private var isActive = false

    init {
        lifecycle?.let {
            disposables += it.setupConnections()
        }

        disposables += connectionDisposables
    }

    // region bind

    fun <T : Any> bind(connection: Pair<ObservableSource<out T>, Consumer<in T>>) {
        bind(
            Connection(
                from = connection.first,
                to = connection.second,
                connector = null
            )
        )
    }

    fun <Out : Any, In : Any> bind(connection: Connection<Out, In>) {
        val consumer = connection.to
        val middleware = consumer.wrapWithMiddleware(
            standalone = false,
            name = connection.name
        ) as? Middleware<Out, In>

        when {
            lifecycle != null -> {
                with(Binding(connection, middleware)) {
                    bindings.add(this)
                    if (isActive) {
                        accumulate()
                        subscribeWithLifecycle<Out, In>(this)
                    }
                }
            }
            else -> subscribe(connection, middleware)
        }
    }

    private fun <Out, In> subscribeWithLifecycle(binding: Binding) {
        (binding.source as? UnicastSubject<Out>)?.let { source ->
            connectionDisposables += wrap(source)
                .subscribeWithMiddleware(
                    binding.connection as Connection<Out, In>,
                    binding.middleware as? Middleware<Out, In>
                )
        }
    }

    private fun <Out, In> subscribe(
        connection: Connection<Out, In>,
        middleware: Middleware<Out, In>?
    ) {
        disposables += wrap(connection.from)
            .subscribeWithMiddleware(connection, middleware)
    }

    private fun <Out, In> Observable<out Out>.subscribeWithMiddleware(
        connection: Connection<Out, In>,
        middleware: Middleware<Out, In>?
    ): Disposable =
        applyTransformer(connection)
            .run {
                if (middleware != null) {
                    middleware.onBind(connection)

                    this
                        .optionalObserveOn(connection.observeScheduler)
                        .doOnNext { middleware.onElement(connection, it) }
                        .doFinally { middleware.onComplete(connection) }
                        .subscribe(middleware)

                } else {
                    optionalObserveOn(connection.observeScheduler)
                        .subscribe(connection.to)
                }
            }

    private fun <Out, In> Observable<out Out>.applyTransformer(
        connection: Connection<Out, In>
    ): Observable<In> =
        connection.connector?.let {
            wrap(it.invoke(this))
        } ?: this as Observable<In>

    // endregion

    // region lifecycle

    private fun Lifecycle.setupConnections() =
        wrap(this)
            .distinctUntilChanged()
            .subscribe {
                when (it) {
                    BEGIN -> bindConnections()
                    END -> unbindConnections()
                }
            }

    private fun bindConnections() {
        isActive = true
        bindings.forEach { it.accumulate() }
        bindings.forEach { subscribeWithLifecycle<Any, Any>(it) }
    }

    private fun unbindConnections() {
        isActive = false
        connectionDisposables.clear()
    }

    // endregion

    override fun isDisposed(): Boolean =
        disposables.isDisposed

    override fun dispose() {
        disposables.dispose()
    }

    /**
     * Any binds that occur within [BinderObserveOnScope] will be observed on the provided scheduler
     */
    fun observeOn(scheduler: Scheduler, observeOnScopeFunc: BinderObserveOnScope.() -> Unit) {
        BinderObserveOnScope(this, scheduler).apply(observeOnScopeFunc)
    }

    fun clear() {
        disposables.clear()
    }

    private fun <T> Observable<T>.optionalObserveOn(scheduler: Scheduler?) =
        if (scheduler != null) {
            observeOn(scheduler)
        } else {
            this
        }

    class BinderObserveOnScope(
        private val binder: Binder,
        private val observeScheduler: Scheduler
    ) {
        fun <T : Any> bind(connection: Pair<ObservableSource<out T>, Consumer<in T>>) {
            binder.bind(
                Connection(
                    from = connection.first,
                    to = connection.second,
                    connector = null,
                    observeScheduler = observeScheduler
                )
            )
        }

        fun <Out : Any, In : Any> bind(connection: Connection<Out, In>) {
            binder.bind(connection.observeOn(observeScheduler))
        }
    }
}
