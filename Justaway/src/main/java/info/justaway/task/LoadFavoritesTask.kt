package info.justaway.task

import android.os.AsyncTask

import info.justaway.model.AccessTokenManager
import info.justaway.model.FavRetweetManager
import info.justaway.model.TwitterManager
import twitter4j.ResponseList

class LoadFavoritesTask : AsyncTask<Long, Void, Boolean>() {

    override fun doInBackground(vararg params: Long?): Boolean? {
        return try {
            val favorites = TwitterManager.getTwitter().getFavorites(AccessTokenManager.getUserId())
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