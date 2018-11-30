package net.slashOmega.juktaway

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.app.RemoteInput
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import kotlinx.android.synthetic.main.action_bar_post.*
import kotlinx.android.synthetic.main.activity_post.*
import kotlinx.android.synthetic.main.row_word.view.*
import kotlinx.android.synthetic.main.spinner_switch_account.view.*
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.UserIconManager
import net.slashOmega.juktaway.plugin.TwiccaPlugin
import net.slashOmega.juktaway.settings.PostStockSettings
import net.slashOmega.juktaway.settings.PostStockSettings.drafts
import net.slashOmega.juktaway.settings.PostStockSettings.hashtags
import net.slashOmega.juktaway.task.SendDirectMessageTask
import net.slashOmega.juktaway.util.*
import twitter4j.Status
import twitter4j.StatusUpdate
import twitter4j.TwitterException
import twitter4j.auth.AccessToken
import java.io.File
import java.io.FileNotFoundException
import java.lang.ref.WeakReference

@SuppressLint("SetTextI18n", "InflateParams")
class PostActivity: FragmentActivity() {
    companion object {
        private const val REQUEST_GALLERY = 1
        private const val REQUEST_CAMERA = 2
        private const val REQUEST_TWICCA = 3
        private const val REQUEST_PERMISSIONS_CAMERA = 1
        private const val OPTION_MENU_GROUP_TWICCA = 1
        private const val ERROR_CODE_DUPLICATE_STATUS = 187
        private const val ERROR_CODE_NOT_FOLLOW_DM = 150
        private const val MAX_IMAGE = 4

        private class SendDMTask(context: PostActivity, token: AccessToken): SendDirectMessageTask(token) {
            val ref = WeakReference(context)
            override fun onPostExecute(e: TwitterException?) {
                MessageUtil.dismissProgressDialog()
                e?.apply {
                    MessageUtil.showToast(
                            if (errorCode == ERROR_CODE_NOT_FOLLOW_DM)
                                R.string.toast_update_status_not_Follow
                            else
                                R.string.toast_update_status_failure)
                } ?: ref.get()?.run {
                    status_text.setText("")
                    if (!mWidgetMode) {
                        finish()
                    }
                }
            }
        }

        private class UpdateStatusTask(context: PostActivity, token: AccessToken, list: ArrayList<File>)
                : net.slashOmega.juktaway.task.UpdateStatusTask(token, list) {
            val ref = WeakReference(context)
            override fun onPostExecute(e: TwitterException?) {
                MessageUtil.dismissProgressDialog()
                e?.apply {
                    MessageUtil.showToast(
                            if(errorCode == ERROR_CODE_DUPLICATE_STATUS)
                                R.string.toast_update_status_already
                            else
                                R.string.toast_update_status_failure)
                } ?: ref.get()?.run {
                    status_text.setText("")
                    if (!mWidgetMode) {
                        finish()
                    } else {
                        image_preview_container.removeAllViews()
                        tweet_button.isEnabled = false
                    }
                }
            }
        }
    }

    private lateinit var mContext: Activity
    private var mInReplyToStatusId: Long = 0
    private var mWidgetMode: Boolean = false
    private val mTextHistory = mutableListOf<String>()
    private var mHashtagDialog: AlertDialog? = null
    private var mDraftDialog: AlertDialog? = null
    private var mImgPath: File? = null
    private var mImageUri: Uri? = null
    private var mTwiccaPlugins: List<ResolveInfo>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_post)
        mContext = this

        // Wear からリプライを返す
        RemoteInput.getResultsFromIntent(intent)?.run {
            val charSequence = getCharSequence(NotificationService.EXTRA_VOICE_REPLY)
            var inReplyToStatus = intent.getSerializableExtra("inReplyToStatus") as Status
            mInReplyToStatusId = inReplyToStatus.id
            var inReplyToUserScreenName = inReplyToStatus.user.screenName
            if (inReplyToStatus.retweetedStatus != null) {
                inReplyToStatus = inReplyToStatus.retweetedStatus
                inReplyToUserScreenName = inReplyToStatus.user.screenName
            }
            mInReplyToStatusId = inReplyToStatus.id
            status_text.setText("@" + inReplyToUserScreenName + " " + charSequence.toString())

            tweet()
            return
        }

        // Wear からツイート
        if (intent.getBooleanExtra("wearable", false)) {
            status_text.setText(intent.getStringExtra("status"))
            tweet()
        }

        actionBar?.run {
            if ((displayOptions and ActionBar.DISPLAY_SHOW_CUSTOM) == ActionBar.DISPLAY_SHOW_CUSTOM) {
                setDisplayShowCustomEnabled(false)
            } else {
                setDisplayShowCustomEnabled(true)
                if (customView == null) setCustomView(R.layout.action_bar_post)
            }
        }

        UserIconManager.warmUpUserIconMap()
        registerForContextMenu(img_button)

        // アカウント切り替え
        switch_account_spinner.adapter = AccessTokenAdapter(this, R.layout.spinner_switch_account).apply {
            setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
            AccessTokenManager.getAccessTokens().forEachIndexed { i, token ->
                add(token)
                if (AccessTokenManager.getUserId() == token.userId) switch_account_spinner.setSelection(i)
            }
        }

        if (intent.getBooleanExtra("notification", false)) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        }
        mWidgetMode = intent.getBooleanExtra("widget", false)
        if (mWidgetMode) action_bar_post_title.setText(R.string.widget_title_post_mode)
        else {
            action_bar_post_title.setText(R.string.title_post)
            actionBar?.apply {
                setHomeButtonEnabled(true)
                setDisplayHomeAsUpEnabled(true)
            }
        }

        intent.getStringExtra("status")?.let {
            status_text.setText(it)
        }

        intent.getIntExtra("selection", 0).takeIf{it > 0}?.let { start ->
            intent.getIntExtra("selection_stop", 0).takeIf{it > 0}?.let { stop ->
                status_text.setSelection(start, stop)
            } ?: status_text.setSelection(start)
        }

        (intent.getSerializableExtra("inReplyToStatus") as Status?)?.run {retweetedStatus?:this}?.run {
            mInReplyToStatusId = id
            ImageUtil.displayRoundedImage(user.profileImageURL, in_reply_to_user_icon)
            in_reply_to_status.text = text

            // スクロール可能にするのに必要
            in_reply_to_status.movementMethod = ScrollingMovementMethod.getInstance()
        } ?: let {
            in_reply_to_layout.visibility = View.GONE
        }

        intent.data?.run {
            getQueryParameter("in_reply_to").takeNotEmpty()?.let {
                mInReplyToStatusId = it.toLong()
            }
            val text = (getQueryParameter("text") ?: "").apply {
                getQueryParameter("url")?.let { plus(" $it") }
                getQueryParameter("hashtags")?.let { plus(" $it") }
            }
            status_text.setText(text)
        }

        // ブラウザから来たとき
        if (Intent.ACTION_SEND == intent.action) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { setImage(it) }
            ?: intent.extras?.run {
                val text = (getString(Intent.EXTRA_TEXT) ?: "") + (getString(Intent.EXTRA_SUBJECT)?.let { " $it" } ?: "")
                status_text.setText(text)
            }
        }

        status_text.text?.let { updateCount(it.toString()) }

        // 下書きとハッシュタグがあるかチェック
        draft_button.isEnabled = drafts.isNotEmpty()
        hashtag_button.isEnabled = hashtags.isNotEmpty()


        // 文字数をカウントしてボタンを制御する
        status_text.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // 直近のと一緒なら保存しない
                with (mTextHistory) {
                    s?.let { if (isEmpty() || it.toString() != mTextHistory[size-1]) add(it.toString()) }
                    undo.isEnabled = isNotEmpty()
                }
            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                s?.let {
                    updateCount(s.toString())
                    if (s.toString().startsWith("D ")) {
                        image_preview_container.removeAllViews()
                        img_button.isEnabled = false
                    } else {
                        img_button.isEnabled = true
                    }
                }
            }

        })

        in_reply_to_cancel.setOnClickListener { in_reply_to_layout.visibility = View.GONE }

        hashtag_button.setOnClickListener {
            val view = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.list, null)
            val adapter = HashtagAdapter(this, R.layout.row_word)
            hashtags.forEach { tag -> adapter.add(tag) }
            view.findViewById<ListView>(R.id.list_list).apply {
                this.adapter = adapter
                setOnItemClickListener { _, _, i, _ ->
                    status_text?.run {
                        setText(text.toString() + adapter.getItem(i))
                        mHashtagDialog?.dismiss()
                    }
                }
            }

            mHashtagDialog = AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_hashtag)
                    .setView(view)
                    .show()
        }

        draft_button.setOnClickListener {
            val view = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.list, null)
            val adapter = DraftAdapter(this, R.layout.row_word)
            drafts.forEach { tag -> adapter.add(tag) }
            //TODO
            view.findViewById<ListView>(R.id.list_list).apply {
                this.adapter = adapter
                setOnItemClickListener { _, _, i, _ ->
                    status_text?.run {
                        val draft = adapter.getItem(i)
                        setText(draft)
                        mDraftDialog?.dismiss()
                        adapter.remove(draft)
                        PostStockSettings.removeDraft(draft)
                    }
                }
            }

            mDraftDialog = AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_draft)
                    .setView(view)
                    .show()
        }

        suddenly_button.setOnClickListener {
            status_text?.run {
                setText(TwitterUtil.convertSuddenly(text.toString(), selectionStart, selectionEnd))
            }
        }

        img_button.setOnClickListener { img_button.showContextMenu() }
        tweet_button.setOnClickListener { tweet() }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putParcelable("image_url", mImageUri)
        outState?.putSerializable("image_path", mImgPath)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        mImageUri = savedInstanceState.getParcelable<Parcelable>("image_uri") as Uri
        mImgPath = savedInstanceState.getSerializable("image_path") as File

        mImgPath?.run {
            if (exists()) tweet_button.isEnabled = true
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        menu?.run {
            super.onCreateContextMenu(this, v, menuInfo)
            setHeaderTitle(getString(R.string.context_menu_title_photo_method))
            add(0, REQUEST_GALLERY, 0, R.string.context_menu_photo_gallery)
            add(0, REQUEST_CAMERA, 0, R.string.context_menu_photo_camera)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            REQUEST_GALLERY ->
                startActivityForResult(Intent(Intent.ACTION_PICK).apply { type = "image/*" }, REQUEST_GALLERY)
            REQUEST_CAMERA ->
                if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSIONS_CAMERA)
                }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSIONS_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                }
            }
        }
    }

    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, System.currentTimeMillis().toString() + ".jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        mImageUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        startActivityForResult(
                Intent().apply {
                    action = MediaStore.ACTION_IMAGE_CAPTURE
                    putExtra(MediaStore.EXTRA_OUTPUT, mImageUri)
                }, REQUEST_CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        data?.apply {
            when (requestCode) {
                REQUEST_GALLERY -> setImage(this.data)
                REQUEST_CAMERA -> mImageUri?.let { setImage(it) }
                REQUEST_TWICCA -> status_text.setText(getStringExtra(Intent.EXTRA_TEXT))
            }
        }
    }

    private fun setImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val imageView = layoutInflater.inflate(R.layout.image_preview, null) as ImageView
            imageView.tag = FileUtil.writeToTempFile(cacheDir, inputStream)

            with (image_preview_container) {
                imageView.setOnClickListener { v ->
                    removeView(v)
                    img_button.isEnabled = true

                    if (childCount == 0) {
                        removeAllViews()
                        updateCount(status_text.text.toString())
                    }
                }
                imageView.setImageURI(uri)
                addView(imageView)
                if (status_text.text != null) updateCount(status_text.text.toString())

                if (childCount >= MAX_IMAGE) {
                    img_button.isEnabled = false
                    return
                }
            }
            MessageUtil.showToast(R.string.toast_set_image_success)
            tweet_button.isEnabled = true
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun updateCount(str: String) {
        val textColor: Int

        var length = TwitterUtil.count(str)
        if (image_preview_container.childCount > 0) length -= 24
        // 文字数をオーバーした時は文字数を赤色に
        textColor = if (length < 0) Color.RED else ThemeUtil.getThemeTextColor(R.attr.menu_text_color)
        count.setTextColor(textColor)
        count.text = length.toString()

        when {
            length < 0 -> // 文字数オーバー
                tweet_button.isEnabled = false
            str.isEmpty() -> // 何も入力されていない時も画像があれば投稿可
                tweet_button.isEnabled = image_preview_container.childCount > 0
            else -> tweet_button.isEnabled = true
        }
    }

    override fun onBackPressed() {
        status_text.text.takeNotEmpty()?.let { txt ->
            AlertDialog.Builder(this@PostActivity)
                    .setMessage(R.string.confirm_save_draft)
                    .setPositiveButton(R.string.button_save) { _, _ ->
                            // 下書きとして保存する
                            PostStockSettings.addDraft(txt.toString())
                            finish()
                        }
                    .setNegativeButton(R.string.button_destroy) { _, _ -> finish() }
                    .show()
        } ?: finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.post, menu)
        mTwiccaPlugins = mTwiccaPlugins
                ?: TwiccaPlugin.getResolveInfo(this.packageManager, TwiccaPlugin.TWICCA_ACTION_EDIT_TWEET)
        if (mTwiccaPlugins?.isNotEmpty() == true) {
            packageManager?.let { pm ->
                mTwiccaPlugins!!.forEachIndexed { i, info ->
                    menu.add(OPTION_MENU_GROUP_TWICCA, i, 100, info.activityInfo.loadLabel(pm))
                }
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.groupId == OPTION_MENU_GROUP_TWICCA) {
            mTwiccaPlugins?.let { plugins -> status_text.text?.let {text ->
                plugins[item.itemId].activityInfo?.let { info ->
                    startActivityForResult(TwiccaPlugin.createIntentEditTweet(
                            "", text.toString(), "", 0, info.packageName, info.name
                    ), REQUEST_TWICCA)
                }
            }}
            return true
        }
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.tweet_clear -> status_text.setText("")
            R.id.tweet_battery ->
                // バッテリー情報をセットする
                status_text.setText(TwitterUtil.getBatteryStatus(mContext))
        }
        return true
    }

    private fun tweet() {
        MessageUtil.showProgressDialog(this, getString(R.string.progress_sending))
        val text = status_text.text.toString()
        if (text.startsWith("D ")) {
            SendDMTask(this, switch_account_spinner.selectedItem as AccessToken).execute(text)
        } else {
            val sUpdate = StatusUpdate(text).apply {
                if (mInReplyToStatusId > 0) inReplyToStatusId = mInReplyToStatusId
            }
            val mImagePathList = arrayListOf<File>().apply {
                image_preview_container.let { container ->
                    for (i in 0 until container.childCount) {
                        add(container.getChildAt(i).tag as File)
                    }
                }
            }
            UpdateStatusTask(this, switch_account_spinner.selectedItem as AccessToken, mImagePathList)
                    .execute(sUpdate)
        }
    }

    private class DraftAdapter(context: PostActivity, val mLayout: Int): ArrayAdapter<String>(context, mLayout) {
        private val ref = WeakReference(context)
        private val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val draft = getItem(position)

            return (convertView ?: mInflater.inflate(mLayout, null)).apply {
                word.text = draft
                trash.setOnClickListener {
                    remove(draft)
                    PostStockSettings.removeDraft(draft)
                }
            }
        }
    }



    private class HashtagAdapter(context: PostActivity, val mLayout: Int): ArrayAdapter<String>(context, mLayout) {
        private val ref = WeakReference(context)
        private val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val hashtag = getItem(position)

            return (convertView ?: mInflater.inflate(mLayout, null)).apply {
                word.text = hashtag
                trash.setOnClickListener {
                    remove(hashtag)
                    PostStockSettings.removeDraft(hashtag)
                }
            }
        }
    }

    private class AccessTokenAdapter(context: Context, val mLayout: Int)
            : ArrayAdapter<AccessToken>(context, mLayout) {
        private val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val token = getItem(position)

            return (convertView ?: mInflater.inflate(mLayout, null)).apply {
                setPadding(16, 0, 0, 0)

                UserIconManager.displayUserIcon(token.userId, spinner_switch_account_icon)
                spinner_switch_account_screen_name.text = token.screenName
            }
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val token = getItem(position)

            return (convertView ?: mInflater.inflate(mLayout, null)).apply {
                UserIconManager.displayUserIcon(token.userId, spinner_switch_account_icon)
                spinner_switch_account_screen_name.text = token.screenName
            }
        }
    }
}