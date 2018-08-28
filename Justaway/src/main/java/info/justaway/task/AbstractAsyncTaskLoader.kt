package info.justaway.task

import android.content.Context
import android.support.v4.content.AsyncTaskLoader

/**
 * 一般的な AsyncTaskLoader の処理を全てに書くのは冗長なのでこれを継承して使う
 *
 * @param <D>
 * @author aska
</D> */
abstract class AbstractAsyncTaskLoader<D>(context: Context) : AsyncTaskLoader<D>(context) {

    private var data: D? = null

    override fun deliverResult(data: D?) {
        if (isReset) {
            // An async query came in while the loader is stopped
            return
        }

        this.data = data

        super.deliverResult(data)
    }

    override fun onStartLoading() {
        data?.let { deliverResult(it) }

        if (takeContentChanged() || data == null) {
            forceLoad()
        }
    }

    override fun onStopLoading() {
        super.onStopLoading()
        // Attempt to cancel the current load task if possible.
        cancelLoad()
    }

    override fun onReset() {
        super.onReset()

        // Ensure the loader is stopped
        onStopLoading()

        data = null
    }
}
