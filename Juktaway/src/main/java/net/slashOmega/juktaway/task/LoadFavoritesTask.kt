package net.slashOmega.juktaway.task

import android.os.AsyncTask

import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.FavRetweetManager
import net.slashOmega.juktaway.model.TwitterManager
import twitter4j.ResponseList

class LoadFavoritesTask : AsyncTask<Long, Void, Boolean>() {

    override fun doInBackground(vararg params: Long?): Boolean? {
        return try {
            val favorites = TwitterManager.twitter.getFavorites(AccessTokenManager.getUserId())
            for (status in favorites) {
                FavRetweetManager.setFav(status.id)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    }
}