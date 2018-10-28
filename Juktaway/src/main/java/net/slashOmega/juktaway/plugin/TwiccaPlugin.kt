package net.slashOmega.juktaway.plugin

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.text.Html

import twitter4j.Status
import twitter4j.User

/**
 * Twiccaプラグイン用クラス
 * PICK_TREND,UPLOAD,EDIT_TWEETに関しては、onActivityResultにて受信まで行う必要があるので注意が必要
 * また、他のアプリに誤った値を渡してエラーを吐いた場合バグレポートが他アプリに行くため、弄る際はtwiccaのデベロッパーページを読み、できるだけ忠実な挙動にする必要がある
 * twiccaデベロッパーページ: http://twicca.r246.jp/developers/
 *
 * @author oboenikui
 */
object TwiccaPlugin {

    const val TWICCA_ACTION_SHOW_TWEET = "jp.r246.twicca.ACTION_SHOW_TWEET"
    const val TWICCA_ACTION_SHOW_USER = "jp.r246.twicca.ACTION_SHOW_USER"
    const val TWICCA_ACTION_PICK_TREND = "jp.r246.twicca.ACTION_PICK_TREND"
    const val TWICCA_ACTION_UPLOAD = "jp.r246.twicca.ACTION_UPLOAD"
    const val TWICCA_ACTION_EDIT_TWEET = "jp.r246.twicca.ACTION_EDIT_TWEET"
    const val TWICCA_ACTION_PLUGIN_SETTINGS = "jp.r246.twicca.ACTION_PLUGIN_SETTINGS"
    const val TWICCA_CATEGORY_OWNER = "jp.r246.twicca.category.OWNER"
    const val TWICCA_CATEGORY_USER = "jp.r246.twicca.category.USER"
    const val TWICCA_USER_SCREEN_NAME = "jp.r246.twicca.USER_SCREEN_NAME"

    /**
     * ツイートからtwiccaプラグインに飛ばすためのIntentを作成
     *
     * @param status    ステータス
     * @param pkgName   パッケージ名
     * @param className クラス名
     * @return アプリを起動するためのIntent
     */
    fun createIntentShowTweet(status: Status, pkgName: String, className: String): Intent {
        val intent = Intent(TWICCA_ACTION_SHOW_TWEET)
                .putExtra(Intent.EXTRA_TEXT, status.text)
                .putExtra("id", status.id.toString())
                .putExtra("created_at", status.createdAt.time.toString())
                .putExtra("source", Html.fromHtml(status.source).toString())
                .putExtra("in_reply_to_status_id", status.inReplyToStatusId.toString())
                .putExtra("user_screen_name", status.user.screenName)
                .putExtra("user_name", status.user.name)
                .putExtra("user_id", status.user.id.toString())
                .putExtra("user_profile_image_url", status.user.originalProfileImageURL)
                .putExtra("user_profile_image_url_mini", status.user.miniProfileImageURL)
                .putExtra("user_profile_image_url_normal", status.user.profileImageURL)
                .putExtra("user_profile_image_url_bigger", status.user.biggerProfileImageURL)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setClassName(pkgName, className)

        val geoLocation = status.geoLocation
        if (geoLocation != null) {
            intent.putExtra("latitude", geoLocation.latitude.toString())
                    .putExtra("longitude", geoLocation.longitude.toString())
        }
        return intent
    }

    /**
     * ユーザー画面からtwiccaプラグインに飛ばすIntentを作成
     *
     * @param user      ユーザー
     * @param owner     Justaway使用者
     * @param pkgName   パッケージ名
     * @param className クラス名
     * @return アプリを起動するためのIntent
     */
    fun createIntentShowUser(user: User, owner: User, pkgName: String, className: String)
        = Intent(TWICCA_ACTION_SHOW_USER)
                .putExtra(Intent.EXTRA_TEXT, user.screenName)
                .putExtra("name", user.name)
                .putExtra("id", user.id.toString())
                .putExtra("location", user.location)
                .putExtra("url", user.url)
                .putExtra("description", user.description)
                .putExtra("profile_image_url", user.originalProfileImageURL)
                .putExtra("profile_image_url_mini", user.miniProfileImageURL)
                .putExtra("profile_image_url_normal", user.profileImageURL)
                .putExtra("profile_image_url_bigger", user.biggerProfileImageURL)
                .putExtra("owner_screen_name", owner.screenName)
                .putExtra("owner_name", owner.name)
                .putExtra("owner_id", owner.id.toString())
                .putExtra("owner_location", owner.location)
                .putExtra("owner_url", owner.url)
                .putExtra("owner_description", owner.description)
                .putExtra("owner_profile_image_url", owner.originalProfileImageURL)
                .putExtra("owner_profile_image_url_mini", owner.miniProfileImageURL)
                .putExtra("owner_profile_image_url_normal", owner.profileImageURL)
                .putExtra("owner_profile_image_url_bigger", owner.biggerProfileImageURL)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .addCategory(if (user.id == owner.id) TWICCA_CATEGORY_OWNER else TWICCA_CATEGORY_USER)
                .setClassName(pkgName, className)

    /**
     * ツイート編集中に呼び出すプラグイン 引数の説明はtwicca公式サイト(http://twicca.r246.jp/developers/edit_tweet_action/)より
     *
     * @param prefix     編集中のテキストの接頭辞(例："@screen_name ")
     * @param user_input 編集中のテキストのうち、接頭辞と接尾辞を取り除いた部分。
     * @param suffix     編集中のテキストの接尾辞(例：" RT @screen_name: quoted tweet")
     * @param cursor     カーソル位置
     * @param pkgName    パッケージ名
     * @param className  クラス名
     * @return アプリを起動するためのIntent
     */
    fun createIntentEditTweet(prefix: String?, user_input: String?, suffix: String?, cursor: Int, pkgName: String, className: String): Intent {
        val fixedPrefix = prefix ?: ""
        val fixedUserInput = user_input ?: ""
        val fixedSuffix = suffix ?: ""
        return Intent(TWICCA_ACTION_EDIT_TWEET)
                .putExtra(Intent.EXTRA_TEXT, fixedPrefix + fixedUserInput + fixedSuffix)
                .putExtra("prefix", fixedPrefix)
                .putExtra("suffix", fixedSuffix)
                .putExtra("user_input", fixedUserInput)
                .putExtra("cursor", cursor)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setClassName(pkgName, className)
    }

    /**
     * ツイート編集中に呼び出すプラグイン 引数の説明はtwicca公式サイト(http://twicca.r246.jp/developers/edit_tweet_action/)より
     *
     * @param prefix      編集中のテキストの接頭辞(例："@screen_name ")
     * @param user_input  編集中のテキストのうち、接頭辞と接尾辞を取り除いた部分。
     * @param suffix      編集中のテキストの接尾辞(例：" RT @screen_name: quoted tweet")
     * @param cursor      カーソル位置
     * @param in_reply_to 返信先のID
     * @param pkgName     パッケージ名
     * @param className   クラス名
     * @return アプリを起動するためのIntent
     */
    fun createIntentEditTweet(prefix: String, user_input: String, suffix: String, cursor: Int, in_reply_to: Long, pkgName: String, className: String)
        = createIntentEditTweet(prefix, user_input, suffix, cursor, pkgName, className)
                .putExtra("in_reply_to_status_id", in_reply_to)

    /**
     * 画像アップロード時に呼ばれるプラグイン 要らない気がする
     *
     * @param uri         画像/動画のContent Uri
     * @param tweet       画像アップロード時点でのツイート
     * @param screen_name ユーザーのスクリーンネーム
     * @param pkgName     パッケージ名
     * @param className   クラス名
     * @return アプリを起動するためのIntent
     */
    fun createIntentUpload(uri: Uri, tweet: String?, screen_name: String?, pkgName: String, className: String)
        = Intent(TWICCA_ACTION_UPLOAD)
                .putExtra(Intent.EXTRA_TEXT, tweet ?: "")
                .putExtra(TWICCA_USER_SCREEN_NAME, screen_name ?: "")
                .setData(uri)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setClassName(pkgName, className)

    /**
     * 画像アップロード時に呼ばれるプラグイン 要らない気がする
     *
     * @param uri         画像/動画のContent Uri
     * @param tweet       画像アップロード時点でのツイート
     * @param screen_name ユーザーのスクリーンネーム
     * @param in_reply_to 返信先ID
     * @param pkgName     パッケージ名
     * @param className   クラス名
     * @return アプリを起動するためのIntent
     */
    fun createIntentUpload(uri: Uri, tweet: String, screen_name: String, in_reply_to: Long, pkgName: String, className: String)
        = createIntentUpload(uri, tweet, screen_name, pkgName, className)
                .putExtra("in_reply_to_status_id", in_reply_to.toString())

    /**
     * カスタム「話題のトピック」プラグイン用 私はそのようなプラグインを見たことがありませんし今のところ「話題のトピック」機能もありませんが一応
     *
     * @param pkgName   パッケージ名
     * @param className クラス名
     * @return アプリを起動するためのIntent
     */
    fun createIntentPickTrend(pkgName: String, className: String)
        = Intent(TWICCA_ACTION_PICK_TREND)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setClassName(pkgName, className)

    /**
     * それぞれのプラグインの設定画面に飛ばす
     *
     * @param pkgName   パッケージ名
     * @param className クラス名
     * @return 設定画面を起動するためのIntent
     */
    fun createIntentPluginSettings(pkgName: String, className: String)
        = Intent(TWICCA_ACTION_PLUGIN_SETTINGS)
                .addCategory(Intent.CATEGORY_DEFAULT)

    /**
     * 飛ばせるtwiccaプラグインのリスト
     *
     * @param pm     パッケージマネージャ
     * @param action アクション名
     * @return ResolveInfoのList
     */
    fun getResolveInfo(pm: PackageManager, action: String): List<ResolveInfo>
        = pm.queryIntentActivities(Intent(action), PackageManager.MATCH_DEFAULT_ONLY)
}