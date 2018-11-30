package net.slashOmega.juktaway.listener

/**
 * ごみ箱がタップされた時
 */
interface OnTrashListener {
    fun onTrash(position: Int)
}