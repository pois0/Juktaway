package info.justaway.listener

import twitter4j.auth.AccessToken

interface RemoveAccountListener {
    fun removeAccount(accessToken: AccessToken)
}
