package net.slashOmega.juktaway

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.models.Status
import kotlinx.android.synthetic.main.activity_scale_image.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.slashOmega.juktaway.adapter.SimplePagerAdapter
import net.slashOmega.juktaway.fragment.ScaleImageFragment
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.StatusUtil
import net.slashOmega.juktaway.util.tryAndTraceGet
import org.jetbrains.anko.toast
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern

class ScaleImageActivity: FragmentActivity() {
    companion object {
        val pattern: Pattern = Pattern.compile("https?://twitter\\.com/\\w+/status/(\\d+)/photo/(\\d+)/?.*")
        const val REQUEST_PERMISSIONS_STORAGE = 1
    }

    private lateinit var status: Status
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
                status = getString("status")?.toJsonObject()?.parse<Status>() ?: return@run null
                showStatus(status, getInt("index", 0))
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
        GlobalScope.launch(Dispatchers.Main) {
            val url = runCatching {
                    URL(imageUrls[pager.currentItem])
            }.onFailure { e ->
                e.printStackTrace()
                toast(R.string.toast_save_image_failure)
            }.getOrNull() ?: return@launch
            withContext(Dispatchers.Default) {
                runCatching {
                    (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(DownloadManager.Request(Uri.parse(url.toString())).apply {
                        setTitle("Download ${status.user.name}'s image")
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.path.split("/").last())
                        allowScanningByMediaScanner()
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    })
                }.onFailure {
                    toast("Failed to save image.")
                }
            }
        }
    }

    private fun showStatus(id: Long) {
        GlobalScope.launch(Dispatchers.Main) {
            tryAndTraceGet { currentClient.statuses.show(id).await() }?.result?.let { showStatus(it, 0) }
        }
    }

    private fun showStatus(status: Status, index: Int) {
        val urls = StatusUtil.getImageUrls(status)
        if (urls.size == 1)
            symbol.visibility = View.GONE
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