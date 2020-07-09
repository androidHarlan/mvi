package com.badoo.mvicore.common.binder

import com.badoo.mvicore.common.Sink
import com.badoo.mvicore.common.Source

data class Connection<Out, In>(
    val from: Source<Out>? = null,
    val to: Sink<In>,
    val connector: Connector<Out, In>? = null,
    val name: String? = null
) {
    companion object {
        private const val ANONYMOUS: String = "anonymous"
    }

    fun isAnonymous(): Boolean =
        name == null

    override fun toString(): String {
        val connectorName = connector?.let { " using $it" } ?: ""
        return "<${name ?: ANONYMOUS}> (${from ?: "?"} --> $to$connectorName)"
    }
}

infix fun <Out, In> Pair<Source<Out>, Sink<In>>.using(transformer: (Out) -> In?): Connection<Out, In> =
    using(NotNullConnector(transformer))

infix fun <Out, In> Pair<Source<Out>, Sink<In>>.using(connector: Connector<Out, In>): Connection<Out, In> =
    Connection(
        from = first,
        to = second,
        connector = connector
    )

infix fun <T> Pair<Source<T>, Sink<T>>.named(name: String): Connection<T, T> =
    Connection(
        from = first,
        to = second,
        name = name
    )

infix fun <Out, In> Connection<Out, In>.named(name: String) =
    copy(
        name = name
    )
