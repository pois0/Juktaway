package net.slash_omega.juktaway

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
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.RemoteInput
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ImageView
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.core.exceptions.TwitterErrorMessage
import jp.nephy.penicillin.extensions.models.fullText
import jp.nephy.penicillin.models.Status
import kotlinx.android.synthetic.main.action_bar_post.*
import kotlinx.android.synthetic.main.activity_post.*
import kotlinx.android.synthetic.main.list.view.*
import kotlinx.android.synthetic.main.row_word.view.*
import kotlinx.android.synthetic.main.spinner_switch_account.view.*
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.model.UserIconManager
import net.slash_omega.juktaway.model.UserIconManager.displayUserIcon
import net.slash_omega.juktaway.settings.PostStockSettings
import net.slash_omega.juktaway.settings.PostStockSettings.drafts
import net.slash_omega.juktaway.settings.PostStockSettings.hashtags
import net.slash_omega.juktaway.twitter.Core
import net.slash_omega.juktaway.twitter.Identifier
import net.slash_omega.juktaway.twitter.currentIdentifier
import net.slash_omega.juktaway.twitter.identifierList
import net.slash_omega.juktaway.util.*
import org.jetbrains.anko.toast
import org.jetbrains.anko.wrapContent
import java.io.File
import java.io.FileNotFoundException

@SuppressLint("SetTextI18n", "InflateParams")
class PostActivity: DividedFragmentActivity() {
    companion object {
        private const val REQUEST_GALLERY = 1
        private const val REQUEST_CAMERA = 2
        private const val REQUEST_PERMISSIONS_CAMERA = 1
        private const val MAX_IMAGE = 4
    }

    private lateinit var mContext: Activity
    private var mInReplyToStatusId: Long = 0
    private var mWidgetMode: Boolean = false
    private val mTextHistory = mutableListOf<String>()
    private var mHashtagDialog: AlertDialog? = null
    private var mDraftDialog: AlertDialog? = null
    private var mImgPath: File? = null
    private var mImageUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_post)
        mContext = this

        // Wear からリプライを返す
        RemoteInput.getResultsFromIntent(intent)?.run {
            var inReplyToStatus = intent.getStringExtra("inReplyToStatus")?.toJsonObject()?.parseWithClient<Status>() ?: return@run
            mInReplyToStatusId = inReplyToStatus.id
            var inReplyToUserScreenName = inReplyToStatus.user.screenName
            if (inReplyToStatus.retweetedStatus != null) {
                inReplyToStatus = inReplyToStatus.retweetedStatus!!
                inReplyToUserScreenName = inReplyToStatus.user.screenName
            }
            mInReplyToStatusId = inReplyToStatus.id
            status_text.setText("@$inReplyToUserScreenName")

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
        switch_account_spinner.adapter = IdentifierAdapter(this, R.layout.spinner_switch_account).apply {
            setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
            addAll(identifierList)
        }

        switch_account_spinner.setSelection(identifierList.indexOfFirst { currentIdentifier == it })

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

        intent.getStringExtra("inReplyToStatus")?.toJsonObject()?.parseWithClient<Status>()?.run { retweetedStatus ?: this }?.run {
            mInReplyToStatusId = id
            ImageUtil.displayRoundedImage(user.profileImageUrl, in_reply_to_user_icon)
            in_reply_to_status.text = fullText()

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
                val text = (getString(Intent.EXTRA_TEXT).nullToBlank()) +
                        (getString(Intent.EXTRA_SUBJECT)?.let { " $it" }.nullToBlank())
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
                    updateCount(it.toString())
                    if (it.toString().startsWith("D ")) {
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
            it.list_list.apply {
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

            it.list_list.apply {
                this.adapter = adapter
                setOnItemClickListener { _, _, i, _ ->
                    status_text?.run {
                        val draft = adapter.getItem(i)!!
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
                REQUEST_GALLERY -> setImage(this.data!!)
                REQUEST_CAMERA -> mImageUri?.let { setImage(it) }
            }
        }
    }

    private fun setImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)!!
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
            toast(R.string.toast_set_image_success)
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
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
        launch {
            MessageUtil.showProgressDialog(this@PostActivity, getString(R.string.progress_sending))
            val text = status_text.text.toString()
            if (text.startsWith("D ")) {
                val e = (switch_account_spinner.selectedItem as Identifier).sendDirectMessage(text)
                MessageUtil.dismissProgressDialog()

                e?.apply {
                    toast(if (this is PenicillinException && error == TwitterErrorMessage.YouCannotSendMessagesToUsersWhoAreNotFollowingYou)
                                R.string.toast_update_status_not_Follow
                            else
                                R.string.toast_update_status_failure)
                } ?: run {
                    status_text.setText("")
                    if (!mWidgetMode) finish()
                }
            } else {
                val mImagePathList = arrayListOf<File>().apply {
                    image_preview_container.let { container ->
                        for (i in 0 until container.childCount) {
                            add(container.getChildAt(i).tag as File)
                        }
                    }
                }

                val e = (switch_account_spinner.selectedItem as Identifier).updateStatus(text,
                        mInReplyToStatusId.takeIf { it > 0 },
                        mImagePathList)
                e?.printStackTrace()
                MessageUtil.dismissProgressDialog()
                (e as? PenicillinException)?.let {
                    if (e.error == TwitterErrorMessage.StatusIsADuplicate) R.string.toast_update_status_already
                    else R.string.toast_update_status_failure
                } ?: run {
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

    private class DraftAdapter(context: PostActivity, val mLayout: Int): ArrayAdapter<String>(context, mLayout) {
        private val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val draft = getItem(position)!!

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
        private val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val hashtag = getItem(position)!!

            return (convertView ?: mInflater.inflate(mLayout, null)).apply {
                word.text = hashtag
                trash.setOnClickListener {
                    remove(hashtag)
                    PostStockSettings.removeDraft(hashtag)
                }
            }
        }
    }

    private class IdentifierAdapter(private val activity: PostActivity, val mLayout: Int)
            : ArrayAdapter<Identifier>(activity, mLayout) {
        private val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val identifier = getItem(position)!!

            return (convertView ?: mInflater.inflate(mLayout, null)).apply {
                setPadding(16, 0, 0, 0)

                spinner_switch_account_icon.displayUserIcon(identifier.userId)
                spinner_switch_account_screen_name.text = identifier.screenName
                activity.launch {
                    spinner_switch_account_consumer_name.text = Core.getConsumer(identifier.consumerId)?.name
                    spinner_switch_account_consumer_name.width = wrapContent
                }
            }
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val identifier = getItem(position)!!

            return (convertView ?: mInflater.inflate(mLayout, null)).apply {
                spinner_switch_account_icon?.displayUserIcon(identifier.userId)
                spinner_switch_account_screen_name.text = identifier.screenName
                activity.launch {
                    spinner_switch_account_consumer_name.text = Core.getConsumer(identifier.consumerId)?.name
                    spinner_switch_account_consumer_name.width = wrapContent
                    spinner_switch_account_layout.minimumWidth = wrapContent
                }
            }
        }
    }
}