package net.slashOmega.juktaway.fragment.main.tab;

import android.os.AsyncTask;
import android.view.View;

import java.util.ArrayList;

import net.slashOmega.juktaway.event.model.StreamingCreateFavoriteEvent;
import net.slashOmega.juktaway.event.model.StreamingUnFavoriteEvent;
import net.slashOmega.juktaway.model.AccessTokenManager;
import net.slashOmega.juktaway.model.FavRetweetManager;
import net.slashOmega.juktaway.model.Row;
import net.slashOmega.juktaway.model.TabManager;
import net.slashOmega.juktaway.model.TwitterManager;
import net.slashOmega.juktaway.settings.BasicSettings;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;

/**
 * お気に入りタブ
 */
public class FavoritesFragment extends BaseFragment {

    /**
     * このタブを表す固有のID、ユーザーリストで正数を使うため負数を使う
     */
    public long getTabId() {
        return TabManager.FAVORITES_TAB_ID;
    }

    /**
     * このタブに表示するツイートの定義
     * @param row ストリーミングAPIから受け取った情報（ツイート＋ふぁぼ）
     *            CreateFavoriteEventをキャッチしている為、ふぁぼイベントを受け取ることが出来る
     * @return trueは表示しない、falseは表示する
     */
    @Override
    protected boolean isSkip(Row row) {
        return !row.isFavorite() || row.getSource().getId() != AccessTokenManager.getUserId();
    }

    @Override
    protected void taskExecute() {
        new FavoritesTask().execute();
    }

    private class FavoritesTask extends AsyncTask<Void, Void, ResponseList<Status>> {
        @Override
        protected ResponseList<twitter4j.Status> doInBackground(Void... params) {
            try {
                Paging paging = new Paging();
                if (mMaxId > 0 && !mReloading) {
                    paging.setMaxId(mMaxId - 1);
                    paging.setCount(BasicSettings.INSTANCE.getPageCount());
                }
                return TwitterManager.getTwitter().getFavorites(paging);
            } catch (OutOfMemoryError e) {
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ResponseList<twitter4j.Status> statuses) {
            mFooter.setVisibility(View.GONE);
            if (statuses == null || statuses.size() == 0) {
                mReloading = false;
                mPullToRefreshLayout.setRefreshComplete();
                mListView.setVisibility(View.VISIBLE);
                return;
            }
            if (mReloading) {
                clear();
                for (twitter4j.Status status : statuses) {
                    FavRetweetManager.INSTANCE.setFav(status.getId());
                    if (mMaxId <= 0L || mMaxId > status.getId()) {
                        mMaxId = status.getId();
                    }
                    mAdapter.add(Row.Companion.newStatus(status));
                }
                mReloading = false;
                mPullToRefreshLayout.setRefreshComplete();
            } else {
                for (twitter4j.Status status : statuses) {
                    FavRetweetManager.INSTANCE.setFav(status.getId());
                    if (mMaxId <= 0L || mMaxId > status.getId()) {
                        mMaxId = status.getId();
                    }
                    mAdapter.extensionAdd(Row.Companion.newStatus(status));
                }
                mAutoLoader = true;
                mListView.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * ストリーミングAPIからふぁぼを受け取った時のイベント
     * @param event ふぁぼイベント
     */
    public void onEventMainThread(StreamingCreateFavoriteEvent event) {
        addStack(event.getRow());
    }

    /**
     * ストリーミングAPIからあんふぁぼイベントを受信
     * @param event ツイート
     */
    public void onEventMainThread(StreamingUnFavoriteEvent event) {
        ArrayList<Integer> removePositions = mAdapter.removeStatus(event.getStatus().getId());
        for (Integer removePosition : removePositions) {
            if (removePosition >= 0) {
                int visiblePosition = mListView.getFirstVisiblePosition();
                if (visiblePosition > removePosition) {
                    View view = mListView.getChildAt(0);
                    int y = view != null ? view.getTop() : 0;
                    mListView.setSelectionFromTop(visiblePosition - 1, y);
                    break;
                }
            }
        }
    }
}
