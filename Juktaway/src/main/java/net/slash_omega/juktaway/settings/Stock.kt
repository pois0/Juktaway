package net.slash_omega.juktaway.settings

/**
 * Created on 2018/11/24.
 */
abstract class Stock<T> {
    abstract operator fun plusAssign(t: T)

    abstract operator fun minusAssign(t: T)

    abstract fun getIds(t: T): List<Long>

    abstract fun getAllItems(): List<T>

    operator fun contains(t: T) = getIds(t).isNullOrEmpty().not()

    fun add(t: T) { plusAssign(t) }

    fun remove(t: T) { minusAssign(t) }
}