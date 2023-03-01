package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.widget.Toast

class AppUtils(activity: Activity) {
    private var mActivity: Activity

    fun openApplicationSetting() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", mActivity.getPackageName(), null)
        intent.setData(uri)
        mActivity.startActivity(intent)
    }

    //check the options are enable or not
    fun openDeveloperOptionsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        mActivity.startActivity(intent)
    }


    fun showCenterToast(message: String?) {
        val toast: Toast = Toast.makeText(mActivity, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

//   show fun launchWebview(inApp: Boolean, url: String?, title: String?) {
//        if (mActivity != null) {
//            if (inApp) {
//                val webIntent = Intent(mActivity, GenericWebViewActivity::class.java)
//                webIntent.putExtra(GenericWebViewActivity.URL_KEY, url)
//                webIntent.putExtra(GenericWebViewActivity.TITLE_NAME, title)
//                mActivity.startActivity(webIntent)
//            } else {
//                mActivity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
//            }
//        }
//    }

    companion object {


    }

    init {
        mActivity = activity
    }
}