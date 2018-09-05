package net.slashOmega.juktaway.listener

import twitter4j.auth.AccessToken

interface RemoveAccountListener {
    fun removeAccount(accessToken: AccessToken)
}
