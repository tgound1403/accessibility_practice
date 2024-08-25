package tgound.example.myaccessibilityservice

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.StringWriter
import java.util.ArrayDeque
import java.util.Deque
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


class MyAccessibilityService : AccessibilityService() {
    companion object {
        const val ACTION_PERFORM_CLICK = "ACTION_PERFORM_CLICK"
        const val EXTRA_X = "EXTRA_X"
        const val EXTRA_Y = "EXTRA_Y"
        const val ACTION_PERFORM_TYPE = "ACTION_PERFORM_TYPE"
        const val TYPE_VALUE = "TYPE_VALUE"
        const val ACTION_PERFORM_SCROLL = "ACTION_PERFORM_SCROLL"
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PERFORM_CLICK -> {
                    val x = intent.getIntExtra(EXTRA_X, 0)
                    val y = intent.getIntExtra(EXTRA_Y, 0)
                    performClick(x, y)
                }

                ACTION_PERFORM_TYPE -> {
                    val x = intent.getIntExtra(EXTRA_X, 0)
                    val y = intent.getIntExtra(EXTRA_Y, 0)
                    val text = intent.getStringExtra(TYPE_VALUE)
                    performTextInput(x, y, text!!)
                }

                ACTION_PERFORM_SCROLL -> {
                    continuouslyScroll()
                }
            }

        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(receiver, IntentFilter(ACTION_PERFORM_CLICK))
        registerReceiver(receiver, IntentFilter(ACTION_PERFORM_SCROLL))
        registerReceiver(receiver, IntentFilter(ACTION_PERFORM_TYPE))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null) {
            val x = getNodePosition(event.source)?.first
            val y = getNodePosition(event.source)?.second
//            Log.d("AccessibilityPoint", "Point is: x = $x, y = $y, ${event.eventType}")
        }
    }

    override fun onInterrupt() {

    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("SERVICE", serviceInfo.toString())
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root == null) return null

        // Check if the current node is scrollable
        if (root.isScrollable) {
            return root
        }

        // If not, search through child nodes
        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            if (child != null) {
                val scrollable = findScrollableNode(child)
                if (scrollable != null) {
                    return scrollable
                }
                child.recycle()
            }
        }

        return null
    }

    fun scrollDown(context: Context) {
        val scrollable = getRootFromContext(context)?.let { findScrollableNode(it) }
        scrollable?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id)
        Log.d("SCROLL", "called")
    }

    fun scrollUp(context: Context) {
        val scrollable = getRootFromContext(context)?.let { findScrollableNode(it) }
        scrollable?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.id)
        Log.d("SCROLL", "called")
    }

    fun continuouslyScroll() {
        Handler().postDelayed(Runnable {
            if (canScroll(rootInActiveWindow)) {
                // If scroll was successful, schedule next scroll
                continuouslyScroll()
            } else {
                // If scroll failed, we might have reached the end of the feed
                Log.d("YouTubeScroll", "Reached end of feed or failed to scroll")
            }
        }, 2000) // 2 second delay between scrolls
    }


    private fun canScroll(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false

        // Find the scrollable view (usually a RecyclerView)
        val scrollableNode = findScrollableNode(rootNode)

        if (scrollableNode != null) {
            // Perform the scroll action
            val scrolled =
                scrollableNode.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id)
            scrollableNode.recycle()
            return scrolled
        }

        return false
    }

    fun nodeToString(context: Context) {
        val ls = getAllNodes(getRootFromContext(context))
        val xmlString = convertToXml(ls)
        Log.d("XML", xmlString!!)
        for (node in ls) {
            node.recycle()
        }
    }

    private fun getAllNodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        val allNodes: MutableList<AccessibilityNodeInfo> = ArrayList()
        if (root == null) return allNodes

        val deque: Deque<AccessibilityNodeInfo> = ArrayDeque()
        deque.add(root)

        while (!deque.isEmpty()) {
            val node = deque.removeFirst()
            allNodes.add(node)

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    deque.addLast(child)
                }
            }
        }

        return allNodes
    }

    fun convertToXml(nodes: List<AccessibilityNodeInfo>): String? {
        try {
            val docFactory = DocumentBuilderFactory.newInstance()
            val docBuilder = docFactory.newDocumentBuilder()

            // Tạo root element
            val doc = docBuilder.newDocument()
            val rootElement = doc.createElement("hierarchy")
            rootElement.setAttribute("rotation", "0")
            doc.appendChild(rootElement)

            // Xây dựng cây XML từ danh sách nodes
            buildXmlTree(doc, rootElement, nodes[0])

            // Chuyển đổi Document thành String
            val transformerFactory = TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            val writer = StringWriter()
            transformer.transform(DOMSource(doc), StreamResult(writer))

            return writer.buffer.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun buildXmlTree(doc: Document, parentElement: Element, node: AccessibilityNodeInfo) {
        val element = doc.createElement("node")

        // Thêm các thuộc tính cho node
        element.setAttribute("text", if (node.text != null) node.text.toString() else "")
        element.setAttribute(
            "resource-id",
            if (node.viewIdResourceName != null) node.viewIdResourceName else ""
        )
        element.setAttribute("class", if (node.className != null) node.className.toString() else "")
        element.setAttribute(
            "package",
            if (node.packageName != null) node.packageName.toString() else ""
        )
        element.setAttribute(
            "content-desc",
            if (node.contentDescription != null) node.contentDescription.toString() else ""
        )
        element.setAttribute("checkable", node.isCheckable.toString())
        element.setAttribute("checked", node.isChecked.toString())
        element.setAttribute("clickable", node.isClickable.toString())
        element.setAttribute("enabled", node.isEnabled.toString())
        element.setAttribute("focusable", node.isFocusable.toString())
        element.setAttribute("focused", node.isFocused.toString())
        element.setAttribute("scrollable", node.isScrollable.toString())
        element.setAttribute("long-clickable", node.isLongClickable.toString())
        element.setAttribute("password", node.isPassword.toString())
        element.setAttribute("selected", node.isSelected.toString())

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        element.setAttribute(
            "bounds",
            String.format("[%d,%d][%d,%d]", bounds.left, bounds.top, bounds.right, bounds.bottom)
        )

        parentElement.appendChild(element)

        // Đệ quy cho các node con
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                buildXmlTree(doc, element, childNode)
            }
        }
    }

    private fun getRootFromContext(context: Context): AccessibilityNodeInfo? {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val point = Point()
        display.getSize(point)

        val rootView = FrameLayout(context)
        rootView.layoutParams = ViewGroup.LayoutParams(point.x, point.y)
        return rootView.createAccessibilityNodeInfo()
    }

    fun saveXmlToFile(context: Context, fileName: String, usePublicDirectory: Boolean = false) {
        val xmlString = convertToXml(getAllNodes(getRootFromContext(context)))

        try {
            if (usePublicDirectory) {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null) {
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { outputStream ->
                        if (xmlString != null) {
                            outputStream.write(xmlString.toByteArray())
                        }
                        println("XML file has been saved successfully to: ${file.absolutePath}")
                    }
                } else {
                    println("Unable to access the public Downloads directory")
                }
            } else {
                // Use the provided file path (internal storage)
                FileWriter(fileName).use { writer ->
                    writer.write(xmlString)
                    println("XML file has been saved successfully to: $fileName")
                }
            }
        } catch (e: IOException) {
            System.err.println("An error occurred while saving the XML file: " + e.message)
            e.printStackTrace()
        }
    }

    fun performClick(x: Int, y: Int) {
        Log.d("TOUCH", "Try to click at $x, $y")
        val clickBuilder = GestureDescription.Builder()
        val clickPath = Path()

        clickPath.moveTo(x.toFloat(), y.toFloat())

        clickBuilder.addStroke(GestureDescription.StrokeDescription(clickPath, 0, 100))

        dispatchGesture(
            clickBuilder.build(),
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d("TOUCH", "Touch complete")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.d("TOUCH", "Touch failed")
                }
            },
            null
        )
    }

    fun performTextInput(x: Int, y: Int, text: String) {
        Log.d("TEXT_INPUT", "Try to input text '$text' at $x, $y")

        // Đầu tiên, thực hiện click vào vị trí đó
//      performClick(x, y)
        // Sau đó, sử dụng Bundle để chứa text và gửi nó tới hệ thống
        val arguments = Bundle()
        arguments.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)

        // Tạo một root node
        val rootInActiveWindow = rootInActiveWindow
        if (rootInActiveWindow != null) {
            // Tìm node focus hiện tại (giả sử đó là trường input text sau khi click)
            val focusedNode = rootInActiveWindow.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

            if (focusedNode != null) {
                // Thực hiện hành động SET_TEXT
                focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                Log.d("TEXT_INPUT", "Text input complete")
                Thread.sleep(2000)
                performClick(1011, 2000)
            } else {
                Log.d("TEXT_INPUT", "No focused input field found")
            }

            rootInActiveWindow.recycle()
        } else {
            Log.d("TEXT_INPUT", "No active window")
        }
    }

    private fun getNodePosition(node: AccessibilityNodeInfo?): Pair<Int, Int>? {
        if (node == null) return null

        val rect = Rect()
        node.getBoundsInScreen(rect)
        val centerX = rect.left + (rect.right - rect.left) / 2
        val centerY = rect.top + (rect.bottom - rect.top) / 2
        return Pair(centerX, centerY)
    }
}