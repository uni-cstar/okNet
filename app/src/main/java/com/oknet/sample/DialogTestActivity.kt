package com.oknet.sample

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment

/**
 * Create by luochao
 * on 2023/12/27
 */
class DialogTestActivity : AppCompatActivity(R.layout.activity_dialog_fragment_test) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        findViewById<View>(R.id.show).setOnClickListener {
//            DialogTest().show(supportFragmentManager, "hello")
//        }
//        findViewById<View>(R.id.go).setOnClickListener {
//            startActivity(Intent(this, OkDomainActivity::class.java))
//        }

    }

    override fun onResume() {
        super.onResume()
        val it = supportFragmentManager.findFragmentByTag("hello")
        log("onResume find = $it")
    }

    override fun onPause() {
        super.onPause()
        val it = supportFragmentManager.findFragmentByTag("hello")
        log("onPause find = $it")
    }

    override fun onStop() {
        super.onStop()
        val it = supportFragmentManager.findFragmentByTag("hello")
        log("onStop find = $it")
    }


}

private fun log(msg: String) {
    Log.i("DialogTest", "log: $msg")
}

class DialogTest : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(inflater.context).apply {
            text = "Hello"
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.setOnDismissListener { }
    }

    override fun onStart() {
        super.onStart()
        log("onStart")
    }

    override fun onStop() {
        super.onStop()
        log("onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        log("onDestroyView")
    }

    override fun onDetach() {
        super.onDetach()
        log("onDetach")
    }


}