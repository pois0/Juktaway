package net.slashOmega.juktaway.listener

import net.slashOmega.juktaway.twitter.Identifier

interface RemoveAccountListener {
    fun removeIdentifier(identifier: Identifier)
}
