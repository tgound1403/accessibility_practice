package tgound.example.myaccessibilityservice

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    var myAccessibilityService = MyAccessibilityService();

    private val ACCESSIBILITY_SETTINGS_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openAccessibilitySettings()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACCESSIBILITY_SETTINGS_REQUEST_CODE) {
            continueExecution()
        }
    }

    private fun continueExecution() {
        openYoutube()
        myAccessibilityService.nodeToString(application);
        myAccessibilityService.saveXmlToFile(application, "/window_dump.xml")
        myAccessibilityService.clickAtPosition(10, 100)
        myAccessibilityService.performActionAtPosition(application,10, 100, "Vietnam")
        myAccessibilityService.scrollDown()
        myAccessibilityService.scrollUp()
    }

    private fun openYoutube() {
        val youtubePackageName = "com.google.android.youtube"
        val searchQuery = "Vietnam"

        // Tạo Intent để mở YouTube app
        val appIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage(youtubePackageName)
            putExtra("query", searchQuery)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            startActivity(appIntent)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("ERROR on PM",e.toString());
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, ACCESSIBILITY_SETTINGS_REQUEST_CODE)
    }

}
