package info.justaway.fragment.mute

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import info.justaway.R
import kotlinx.android.synthetic.main.row_word.view.*


/**
 * Created on 2018/08/30.
 */
internal abstract class MuteTargetAdapter<T>(context: Context, val mLayout: Int): ArrayAdapter<T>(context, mLayout) {
    private val mInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val mList = ArrayList<T>()

    override fun add(source: T) {
        super.add(source)
        mList.add(source)
    }

    override fun remove(source: T) {
        super.remove(source)
        mList.remove(source)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val obj = mList[position]
        return (convertView?: mInflater.inflate(mLayout, null)).apply {
            word.text = obj.getDisplayText()
            trash.setOnClickListener {
                AlertDialog.Builder(context)
                        .setMessage(String.format(context.getString(R.string.confirm_destroy_mute), obj.getDisplayText()))
                        .setPositiveButton(R.string.button_yes, onPositiveButtonClicked(obj))
                        .setNegativeButton(R.string.button_no, onNegativeButtonClicked(obj))
                        .show()
            }
        }
    }

    protected abstract fun T.getDisplayText(): String
    protected abstract val onPositiveButtonClicked: (T) -> (DialogInterface, Int) -> Unit
    protected abstract val onNegativeButtonClicked: (T) -> (DialogInterface, Int) -> Unit
}