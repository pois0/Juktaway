//package net.slashOmega.juktaway.fragment.profile
//
//import android.os.AsyncTask
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import net.slashOmega.juktaway.R
//import net.slashOmega.juktaway.adapter.UserAdapter
//import net.slashOmega.juktaway.model.TwitterManager
//import net.slashOmega.juktaway.task.ReferenceAsyncTask
//import twitter4j.PagableResponseList
//import twitter4j.User
//
///**
// * Created on 2018/09/01.
// */
//internal class FollowingListFragment: ProfileListFragmentBase<User, Long, PagableResponseList<User>>() {
//    companion object {
//        private class FriendsListTask(fragment: FollowingListFragment): ReferenceAsyncTask<Long, Void, PagableResponseList<User>, FollowingListFragment>(fragment) {
//            override fun doInBackground(vararg params: Long?): PagableResponseList<User>? {
//                return try {
//                    params[0]?.let { p -> ref.get()?.run {
//                        TwitterManager.getTwitter().getFriendsList(p, mCursor).apply {
//                            mCursor = nextCursor
//                        }}}
//                } catch (e :Exception) {
//                    e.printStackTrace()
//                    null
//                }
//            }
//
//            override fun onPostExecute(friendsList: PagableResponseList<User>?) {
//                ref.get()?.run {friendsList?.let {
//                    mFooter.visibility = View.GONE
//                    for (friendUser in it) mAdapter.add(friendUser)
//                    if (it.hasNext()) mAutoLoader = true
//                    mListView.visibility = View.VISIBLE
//                }}
//            }
//        }
//    }
//
//    override val mAdapter by lazy { UserAdapter(activity, R.layout.row_user) }
//    override val task: AsyncTask<Long, Void, PagableResponseList<User>> = FriendsListTask(this)
//    override val taskParam by lazy { mUser.id }
//    override val layout: Int = R.layout.list_guruguru
//    private var mCursor: Long = -1
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        return super.onCreateView(inflater, container, savedInstanceState)?.apply {
//            registerForContextMenu(mListView)
//        }
//    }
//}