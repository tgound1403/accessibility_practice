package tgound.example.myaccessibilityservice

import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
           GlobalScope.launch(Dispatchers.Main) {
               continueExecution()
           }
        }
    }

    private fun sendClickCommand(x: Int, y: Int) {
        val intent = Intent(MyAccessibilityService.ACTION_PERFORM_CLICK)
        intent.putExtra(MyAccessibilityService.EXTRA_X, x)
        intent.putExtra(MyAccessibilityService.EXTRA_Y, y)
        sendBroadcast(intent)
    }

    private fun sendTypeCommand(x: Int, y: Int, text: String) {
        val intent = Intent(MyAccessibilityService.ACTION_PERFORM_TYPE)
        intent.putExtra(MyAccessibilityService.EXTRA_X, x)
        intent.putExtra(MyAccessibilityService.EXTRA_Y, y)
        intent.putExtra(MyAccessibilityService.TYPE_VALUE, text)
        sendBroadcast(intent)
    }

    private fun sendScrollCommand() {
        val intent = Intent(MyAccessibilityService.ACTION_PERFORM_SCROLL)
        sendBroadcast(intent)
    }

    private suspend fun continueExecution() = withContext(Dispatchers.Default) {
        openYoutube()
        myAccessibilityService.nodeToString(application)
        myAccessibilityService.saveXmlToFile(application, "window_dump.xml", true)

        delay(2000)

        withContext(Dispatchers.Main) {
            sendClickCommand(1011, 149)
            delay(2000)
            sendTypeCommand(1011, 149, "Vietnam")
            delay(2000)
            sendScrollCommand()
        }
    }

    private fun openYoutube() {
        val youtubePackageName = "com.google.android.youtube"

        val appIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage(youtubePackageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            startActivity(appIntent)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("ERROR on PM", e.toString());
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, ACCESSIBILITY_SETTINGS_REQUEST_CODE)
    }
}
