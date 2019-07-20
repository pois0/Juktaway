package net.slash_omega.juktaway

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.endpoints.statuses
import jp.nephy.penicillin.endpoints.statuses.show
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.Status
import kotlinx.android.synthetic.main.activity_scale_image.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.slash_omega.juktaway.adapter.SimplePagerAdapter
import net.slash_omega.juktaway.fragment.ScaleImageFragment
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.util.MessageUtil
import net.slash_omega.juktaway.util.imageUrls
import net.slash_omega.juktaway.util.parseWithClient
import net.slash_omega.juktaway.util.tryAndTraceGet
import org.jetbrains.anko.toast
import java.net.URL
import java.util.regex.Pattern

class ScaleImageActivity: ScopedFragmentActivity() {
    companion object {
        val pattern: Pattern = Pattern.compile("https?://twitter\\.com/\\w+/status/(\\d+)/photo/(\\d+)/?.*")
        const val REQUEST_PERMISSIONS_STORAGE = 1
    }

    private val imageUrls = mutableListOf<String>()
    private lateinit var simplePagerAdapter: SimplePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_scale_image)
        simplePagerAdapter = SimplePagerAdapter(this, pager)

        pager.offscreenPageLimit = 4

        val firstUrl = if (Intent.ACTION_VIEW == intent.action) {
            intent.data?.toString()
        } else {
            intent.extras?.run {
                getString("status")?.toJsonObject()?.parseWithClient<Status>()?.also {
                    showStatus(it, getInt("index", 0))
                }
                getString("url")
            }
        } ?: return

        firstUrl.let {
            pattern.matcher(it).let { m ->
                if (m.find()) {
                    showStatus(m.group(1).toLong())
                    return
                }
            }
            symbol.visibility = View.GONE
            imageUrls.add(it)
            simplePagerAdapter.addTab(ScaleImageFragment::class, Bundle().apply {
                putString("url", it)
            })
            simplePagerAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.scale_image, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return item?.let {
            if(item.itemId == R.id.save) {
                if(ContextCompat.checkSelfPermission(this@ScaleImageActivity,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED)
                    saveImage()
                else
                    ActivityCompat.requestPermissions(this@ScaleImageActivity,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            REQUEST_PERMISSIONS_STORAGE)
            }
            true
        } ?: false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSIONS_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                saveImage()
            else
                MessageUtil.showToast(R.string.toast_save_image_failure)
        }
    }

    private fun saveImage() {
        launch {
            runCatching {
                val url = URL(imageUrls[pager.currentItem])
                withContext(Dispatchers.Default) {
                    (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(DownloadManager.Request(Uri.parse(url.toString())).apply {
                        val fileName = url.path.split("/").last()
                        setTitle(fileName)
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                        allowScanningByMediaScanner()
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    })
                }
            }.onFailure { e ->
                toast(R.string.toast_save_image_failure)
            }
        }
    }

    private fun showStatus(id: Long) {
        launch {
            tryAndTraceGet { currentClient.statuses.show(id).await() }?.result?.let { showStatus(it, 0) }
        }
    }

    private fun showStatus(status: Status, index: Int) {
        val urls = status.imageUrls
        if (urls.size == 1) symbol.visibility = View.GONE
        for (url in urls) {
            imageUrls.add(url)
            simplePagerAdapter.addTab(ScaleImageFragment::class, Bundle().apply {
                putString("url", url)
            })
        }
        simplePagerAdapter.notifyDataSetChanged()
        pager.currentItem = index
    }
}
