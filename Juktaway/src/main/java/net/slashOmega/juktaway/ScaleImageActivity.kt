package net.slashOmega.juktaway

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
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
import net.slashOmega.juktaway.adapter.SimplePagerAdapter
import net.slashOmega.juktaway.fragment.ScaleImageFragment
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.StatusUtil
import net.slashOmega.juktaway.util.tryAndTrace
import net.slashOmega.juktaway.util.tryAndTraceGet
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.regex.Pattern

class ScaleImageActivity: FragmentActivity() {
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
                getString("status")?.toJsonObject()?.parse<Status>()?.let {
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
        return item?.run {
            if(itemId == R.id.save) {
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
            val url = try {
                    URL(imageUrls[pager.currentItem])
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                    MessageUtil.showToast(R.string.toast_save_image_failure)
                    return@launch
                }
            launch(Dispatchers.Default) {
                tryAndTrace {
                    url.openConnection().connect()
                    val file = File(File(Environment.getExternalStorageDirectory(), "/download/"),
                            "${Date().time}.jpg")
                    BufferedInputStream(url.openStream(), 10 * 1024).use { input ->
                        val data = ByteArray(1024)
                        var count: Int
                        FileOutputStream(file).use { output ->
                            while (input.read(data).let { res ->
                                        count = res
                                        res != -1
                                    }) {
                                output.write(data, 0, count)
                            }
                            output.flush()
                        }
                    }
                    MediaScannerConnection.scanFile(applicationContext, arrayOf(file.path), arrayOf("image/jpeg"), null)
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