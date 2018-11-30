package net.slashOmega.juktaway.task;

import android.os.AsyncTask;

import de.greenrobot.event.EventBus;
import net.slashOmega.juktaway.event.model.StreamingCreateFavoriteEvent;
import net.slashOmega.juktaway.model.Row;
import net.slashOmega.juktaway.model.TwitterManager;
import net.slashOmega.juktaway.util.MessageUtil;

public class ReFetchFavoriteStatus extends AsyncTask<Row, Void, twitter4j.Status> {

    private Row mRow;
    // TODO: use http://cdn.api.twitter.com/1/urls/count.json

    public ReFetchFavoriteStatus() {
        super();
    }

    @Override
    protected twitter4j.Status doInBackground(Row... params) {
        mRow = params[0];
        try {
            return TwitterManager.INSTANCE.getTwitter().showStatus(mRow.getStatus().getId());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(twitter4j.Status status) {
        if (status != null) {
            mRow.setStatus(status);
            EventBus.getDefault().post(new StreamingCreateFavoriteEvent(mRow));
            MessageUtil.INSTANCE.showToast(mRow.getSource().getScreenName() + " fav "
                    + mRow.getStatus().getText());
        }
    }
}