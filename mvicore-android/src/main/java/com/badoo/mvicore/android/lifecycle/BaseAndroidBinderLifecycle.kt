package com.badoo.mvicore.android.lifecycle


import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle as AndroidLifecycle
import androidx.lifecycle.LifecycleObserver
import io.reactivex.ObservableSource
import io.reactivex.subjects.BehaviorSubject
import com.badoo.binder.lifecycle.Lifecycle as BinderLifecycle

abstract class BaseAndroidBinderLifecycle private constructor(
        androidLifecycle: AndroidLifecycle,
        observerFactory: ((BinderLifecycle.Event) -> Unit) -> DefaultLifecycleObserver,
        subject: BehaviorSubject<BinderLifecycle.Event>
): BinderLifecycle,
    ObservableSource<BinderLifecycle.Event> by subject,
        LifecycleObserver {

    constructor(
        androidLifecycle: AndroidLifecycle,
        observerFactory: ((BinderLifecycle.Event) -> Unit) -> DefaultLifecycleObserver
    ): this(androidLifecycle, observerFactory, BehaviorSubject.create())

    init {
        androidLifecycle.addObserver(observerFactory(subject::onNext))
    }
}
