package tgound.example.myaccessibilityservice

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import org.w3c.dom.Document
import org.w3c.dom.Element
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
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }

    override fun onInterrupt() {

    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val deque: Deque<AccessibilityNodeInfo> = ArrayDeque()
        deque.add(root)
        while (!deque.isEmpty()) {
            val node = deque.removeFirst()
            if (node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)) {
                return node
            }
            for (i in 0 until node.childCount) {
                deque.addLast(node.getChild(i))
            }
        }
        return null
    }

    fun scrollDown() {
        val scrollable = findScrollableNode(rootInActiveWindow)
        scrollable?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id)
    }

    fun scrollUp() {
        val scrollable = findScrollableNode(rootInActiveWindow)
        scrollable?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id)
    }

    fun getRootViewUsingWindowManager(context: Context): View? {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val point = Point()
        display.getSize(point)

        val rootView = FrameLayout(context)
        rootView.layoutParams = ViewGroup.LayoutParams(point.x, point.y)

        return rootView
    }
    fun nodeToString(context: Context) {
        val ls = getAllNodes(getRootViewUsingWindowManager(context)?.createAccessibilityNodeInfo())
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

    private fun getRootFromContext(context: Context) : AccessibilityNodeInfo? {
       return getRootViewUsingWindowManager(context)?.createAccessibilityNodeInfo()
    }

    fun saveXmlToFile(context: Context, filePath: String) {
        val xmlString = convertToXml(getAllNodes(getRootFromContext(context)));
        try {
            FileWriter(filePath).use { writer ->
                writer.write(xmlString)
                println("XML file has been saved successfully to: $filePath")
            }
        } catch (e: IOException) {
            System.err.println("An error occurred while saving the XML file: " + e.message)
            e.printStackTrace()
        }
    }

    fun clickAtPosition(x: Int, y: Int) {
        val clickPath = Path()
        clickPath.moveTo(x.toFloat(), y.toFloat())
        val clickBuilder = GestureDescription.Builder()
        clickBuilder.addStroke(StrokeDescription(clickPath, 0, 100))
        dispatchGesture(clickBuilder.build(), null, null)
    }

    fun performActionAtPosition(context: Context, x: Int, y: Int, textToType: String?) {
        clickAtPosition(x, y)

        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
        getRootFromContext(context)?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
}