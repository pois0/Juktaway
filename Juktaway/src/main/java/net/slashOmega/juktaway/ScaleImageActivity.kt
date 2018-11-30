package net.slashOmega.juktaway

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import net.slashOmega.juktaway.adapter.SimplePagerAdapter
import net.slashOmega.juktaway.fragment.ScaleImageFragment
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.StatusUtil
import kotlinx.android.synthetic.main.activity_scale_image.*
import twitter4j.Status
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.regex.Pattern

class ScaleImageActivity: FragmentActivity() {
    companion object {
        val pattern: Pattern = Pattern.compile("https?://twitter\\.com/\\w+/status/(\\d+)/photo/(\\d+)/?.*")
        const val REQUEST_PERMISSIONS_STORAGE = 1

        private class GetStatus(context: ScaleImageActivity): AsyncTask<Long, Void, Status?>() {
            private val ref = WeakReference(context)

            override fun doInBackground(vararg p: Long?): twitter4j.Status? {
                return try {
                    TwitterManager.twitter.showStatus(p[0]!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            override fun onPostExecute(result: twitter4j.Status?) {
                result?.let { ref.get()?.showStatus(it, 0) }
            }
        }

        private class SaveImage(context: ScaleImageActivity): AsyncTask<URL, Void, Boolean>() {
            private val ref = WeakReference(context)

            override fun doInBackground(vararg url: URL?): Boolean {
                return url[0]?.let {
                    try {
                        it.openConnection().connect()
                        val input = BufferedInputStream(it.openStream(), 10 * 1024)
                        val file = File(File(Environment.getExternalStorageDirectory(), "/download/"),
                                Date().time.toString() + ".jpg")
                        val output = FileOutputStream(file)
                        val data = ByteArray(1024)
                        var count: Int
                        while (input.read(data).let {res ->
                                    count = res
                                    res != -1
                                }) {
                            output.write(data, 0, count)
                        }
                        output.flush()
                        output.close()
                        input.close()
                        ref.get()?.let { acc ->
                            MediaScannerConnection.scanFile(acc.applicationContext,
                                    arrayOf(file.path), arrayOf("image/jpeg"), null)
                        }
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                } ?: false
            }

        }

        private fun ScaleImageActivity.showStatus(status: Status, index: Int) {
            val urls = StatusUtil.getImageUrls(status)
            if (urls.size == 1)
                symbol.visibility = View.GONE
            for (url: String in urls) {
                imageUrls.add(url)
                simplePagerAdapter.addTab(ScaleImageFragment::class, Bundle().apply {
                    putString("url", url)
                })
            }
            simplePagerAdapter.notifyDataSetChanged()
            pager.currentItem = index
        }
    }

    private val imageUrls = mutableListOf<String>()
    private lateinit var simplePagerAdapter: SimplePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_scale_image)
        simplePagerAdapter = SimplePagerAdapter(this, pager)

        symbol.setViewPager(pager)
        pager.offscreenPageLimit = 4

        var firstUrl: String? = null

        if (Intent.ACTION_VIEW == intent.action) {
            intent.data?.let { firstUrl == it.toString() } ?: return
        } else {
            intent.extras?.run {
                (getSerializable("status") as Status?)?.let {
                    showStatus(it, getInt("index", 0))
                }
                firstUrl = getString("url")
            } ?: return
        }

        firstUrl?.let {
            pattern.matcher(it).let { m ->
                if (m.find()) {
                    GetStatus(this).execute(m.group(1).toLong())
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
            if (!grantResults.isEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                saveImage()
            else
                MessageUtil.showToast(R.string.toast_save_image_failure)

        }
    }

    private fun saveImage() {
        val url = try {
                URL(imageUrls[pager.currentItem])
            } catch (e: MalformedURLException) {
                e.printStackTrace()
                MessageUtil.showToast(R.string.toast_save_image_failure)
                return
            }
        SaveImage(this).execute(url)
    }
}