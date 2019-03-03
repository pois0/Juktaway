package net.slash_omega.juktaway.listener

import net.slash_omega.juktaway.twitter.Identifier

interface RemoveAccountListener {
    fun removeIdentifier(identifier: Identifier)
}
