package com.example.apkreport

import org.w3c.dom.Element
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.filechooser.FileNameExtensionFilter
import javax.xml.parsers.DocumentBuilderFactory

fun main() {
    SwingUtilities.invokeLater {
        ApkReportApp().isVisible = true
    }
}

private data class ApkReport(
    val fileName: String,
    val fileSizeBytes: Long,
    val sha256: String,
    val packageName: String,
    val versionName: String,
    val versionCode: String,
    val minSdk: String,
    val targetSdk: String,
    val appLabel: String,
    val appIcon: String,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val nativeLibraries: List<String>,
    val metaInfFiles: List<String>
)

class ApkReportApp : JFrame("APK Analyzer Pro") {
    private val fileField = JTextField()
    private val statusLabel = JLabel("Select an APK file to begin")
    private val reportArea = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
    }

    private val summaryCards = JPanel().apply {
        layout = GridLayout(0, 2, 12, 12)
        border = EmptyBorder(10, 10, 10, 10)
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        size = Dimension(1200, 820)
        setLocationRelativeTo(null)
        buildUi()
    }

    private fun buildUi() {
        val topPanel = JPanel(BorderLayout(12, 12)).apply {
            border = EmptyBorder(16, 16, 16, 16)
            add(
                JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                    add(JLabel("APK File:"))
                    fileField.preferredSize = Dimension(520, 32)
                    add(fileField)

                    val chooseButton = JButton("Browse")
                    chooseButton.addActionListener { pickApkFile() }
                    add(chooseButton)

                    val analyzeButton = JButton("Analyze APK")
                    analyzeButton.addActionListener { analyzeCurrentFile() }
                    add(analyzeButton)
                },
                BorderLayout.CENTER
            )
        }

        val statusPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            border = EmptyBorder(0, 16, 0, 16)
            statusLabel.foreground = Color(52, 93, 160)
            add(statusLabel)
        }

        val contentSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            top = createSummaryPanel()
            bottom = createReportPanel()
            resizeWeight = 0.34
            dividerSize = 8
            isOneTouchExpandable = true
        }

        contentPane.layout = BorderLayout()
        contentPane.add(topPanel, BorderLayout.NORTH)
        contentPane.add(statusPanel, BorderLayout.CENTER)
        contentPane.add(contentSplit, BorderLayout.SOUTH)
    }

    private fun createSummaryPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(10, 16, 10, 16)
        panel.add(JLabel("APK Summary").apply {
            font = font.deriveFont(18f)
            border = EmptyBorder(0, 0, 10, 0)
        }, BorderLayout.NORTH)

        summaryCards.background = Color(245, 247, 250)
        panel.add(summaryCards, BorderLayout.CENTER)
        return panel
    }

    private fun createReportPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(10, 16, 16, 16)
        panel.add(JLabel("Detailed Report").apply {
            font = font.deriveFont(18f)
            border = EmptyBorder(0, 0, 10, 0)
        }, BorderLayout.NORTH)

        val scroll = JScrollPane(reportArea)
        reportArea.text = "<html><body style='font-family:Arial,sans-serif;padding:16px'>Select an APK to view the report.</body></html>"
        panel.add(scroll, BorderLayout.CENTER)
        return panel
    }

    private fun pickApkFile() {
        val chooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Android APK", "apk")
            fileSelectionMode = JFileChooser.FILES_ONLY
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val selected = chooser.selectedFile
            fileField.text = selected.absolutePath
            statusLabel.text = "Ready to analyze: ${selected.name}"
        }
    }

    private fun analyzeCurrentFile() {
        val path = fileField.text.trim()
        if (path.isEmpty()) {
            statusLabel.text = "Please choose an APK file first."
            statusLabel.foreground = Color(168, 58, 58)
            return
        }

        val apkFile = File(path)
        if (!apkFile.exists() || !apkFile.name.endsWith(".apk", ignoreCase = true)) {
            statusLabel.text = "The selected file is not a valid APK."
            statusLabel.foreground = Color(168, 58, 58)
            return
        }

        try {
            val report = analyzeApk(apkFile)
            populateSummary(report)
            val html = buildHtmlReport(report)
            reportArea.text = html
            statusLabel.text = "Analysis complete for ${apkFile.name}"
            statusLabel.foreground = Color(22, 124, 76)

            val reportFile = File(apkFile.parentFile, "${apkFile.nameWithoutExtension}_report.html")
            FileWriter(reportFile).use { it.write(html) }
            statusLabel.text = "Analysis complete for ${apkFile.name} • report saved: ${reportFile.name}"
        } catch (e: Exception) {
            statusLabel.text = "Analysis failed: ${e.message}"
            statusLabel.foreground = Color(168, 58, 58)
            reportArea.text = "<html><body style='font-family:Arial,sans-serif;padding:16px;color:#a33'>Failed to analyze APK.<br>${e.message}</body></html>"
        }
    }

    private fun populateSummary(report: ApkReport) {
        summaryCards.removeAll()

        val cards = listOf(
            CardItem("Package", report.packageName),
            CardItem("Version", "${report.versionName} (${report.versionCode})"),
            CardItem("Min SDK", report.minSdk),
            CardItem("Target SDK", report.targetSdk),
            CardItem("Size", "${report.fileSizeBytes} bytes"),
            CardItem("SHA-256", report.sha256.take(32) + "..."),
            CardItem("Activities", report.activities.size.toString()),
            CardItem("Permissions", report.permissions.size.toString())
        )

        cards.forEach { item ->
            summaryCards.add(createSummaryCard(item.title, item.value))
        }

        summaryCards.revalidate()
        summaryCards.repaint()
    }

    private fun createSummaryCard(title: String, value: String): JPanel {
        val card = JPanel(BorderLayout()).apply {
            background = Color.WHITE
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(220, 220, 220)),
                EmptyBorder(12, 12, 12, 12)
            )
            preferredSize = Dimension(240, 90)
        }

        card.add(JLabel(title).apply {
            foreground = Color(100, 100, 100)
            font = font.deriveFont(11f)
        }, BorderLayout.NORTH)

        card.add(JLabel("<html><body><b>${value}</b></body></html>").apply {
            foreground = Color(28, 28, 28)
            font = font.deriveFont(14f)
            verticalAlignment = SwingConstants.TOP
        }, BorderLayout.CENTER)

        return card
    }

    private data class CardItem(
        val title: String,
        val value: String
    )
}

fun analyzeApk(apkFile: File): ApkReport {
    require(apkFile.exists()) { "APK file does not exist: ${apkFile.absolutePath}" }

    ZipFile(apkFile).use { zip ->
        val manifestEntry = zip.getEntry("AndroidManifest.xml")
            ?: throw IllegalArgumentException("AndroidManifest.xml not found in APK")

        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(zip.getInputStream(manifestEntry))

        val root = document.documentElement
        val packageName = root.getAttribute("package").trim()

        val appNode = getSingleElementByTagName(root, "application")
        val appLabel = appNode?.let { getAndroidAttribute(it, "label") } ?: "Unknown"
        val appIcon = appNode?.let { getAndroidAttribute(it, "icon") } ?: "Unknown"

        val usesSdkNode = getSingleElementByTagName(root, "uses-sdk")
        val minSdk = usesSdkNode?.let { getAndroidAttribute(it, "minSdkVersion") } ?: "Unknown"
        val targetSdk = usesSdkNode?.let { getAndroidAttribute(it, "targetSdkVersion") } ?: "Unknown"
        val versionName = getAndroidAttribute(root, "versionName")
        val versionCode = getAndroidAttribute(root, "versionCode")

        val permissions = mutableListOf<String>()
        val permissionNodes = document.getElementsByTagName("uses-permission")
        for (i in 0 until permissionNodes.length) {
            val node = permissionNodes.item(i) as Element
            val permissionName = getAndroidAttribute(node, "name")
            if (permissionName.isNotBlank()) {
                permissions.add(permissionName)
            }
        }

        val activities = collectComponentNames(document, "activity")
        val services = collectComponentNames(document, "service")
        val receivers = collectComponentNames(document, "receiver")
        val providers = collectComponentNames(document, "provider")

        val nativeLibraries = mutableListOf<String>()
        val metaInfFiles = mutableListOf<String>()

        zip.entries().asSequence().forEach { entry ->
            val name = entry.name
            if (name.startsWith("lib/")) nativeLibraries.add(name)
            if (name.startsWith("META-INF/")) metaInfFiles.add(name)
        }

        return ApkReport(
            fileName = apkFile.name,
            fileSizeBytes = apkFile.length(),
            sha256 = computeSha256(apkFile),
            packageName = packageName,
            versionName = versionName.ifBlank { "Unknown" },
            versionCode = versionCode.ifBlank { "Unknown" },
            minSdk = minSdk,
            targetSdk = targetSdk,
            appLabel = appLabel,
            appIcon = appIcon,
            permissions = permissions.distinct(),
            activities = activities.distinct(),
            services = services.distinct(),
            receivers = receivers.distinct(),
            providers = providers.distinct(),
            nativeLibraries = nativeLibraries.distinct(),
            metaInfFiles = metaInfFiles.distinct()
        )
    }
}

private fun getSingleElementByTagName(root: Element, tagName: String): Element? {
    val elements = root.getElementsByTagName(tagName)
    return if (elements.length > 0) elements.item(0) as? Element else null
}

private fun collectComponentNames(document: org.w3c.dom.Document, tagName: String): List<String> {
    val items = mutableListOf<String>()
    val elements = document.getElementsByTagName(tagName)
    for (i in 0 until elements.length) {
        val element = elements.item(i) as Element
        val name = getAndroidAttribute(element, "name")
        if (name.isNotBlank()) items.add(name)
    }
    return items
}

private fun getAndroidAttribute(element: Element, attributeName: String): String {
    val attributes = element.attributes
    for (i in 0 until attributes.length) {
        val attribute = attributes.item(i)
        val attrName = attribute.nodeName
        if (attrName == attributeName || attrName.endsWith(":$attributeName")) {
            return attribute.nodeValue
        }
    }
    return ""
}

private fun computeSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun buildHtmlReport(report: ApkReport): String {
    val summaryTable = listOf(
        "File Name" to report.fileName,
        "File Size" to "${report.fileSizeBytes} bytes",
        "SHA-256" to report.sha256,
        "Package Name" to report.packageName,
        "Version Name" to report.versionName,
        "Version Code" to report.versionCode,
        "Min SDK" to report.minSdk,
        "Target SDK" to report.targetSdk,
        "App Label" to report.appLabel,
        "App Icon" to report.appIcon
    )

    fun section(title: String, data: List<String>): String {
        val body = if (data.isEmpty()) "None" else data.joinToString("<br/>")
        return "<h3>$title</h3><table border='1' cellpadding='7' cellspacing='0' style='width:100%;margin-bottom:18px;border-collapse:collapse'><tr><td>$body</td></tr></table>"
    }

    val summaryRows = summaryTable.joinToString("") { (key, value) ->
        "<tr><td style='font-weight:bold;background:#f3f4f6;padding:8px'>$key</td><td style='padding:8px'>$value</td></tr>"
    }

    return """
        <html>
        <head>
            <meta charset="UTF-8" />
            <title>APK Full Report</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 24px; color: #1d1d1d; }
                table { width: 100%; border-collapse: collapse; }
                h2 { margin-top: 0; }
                h3 { margin: 18px 0 8px; }
                td { vertical-align: top; }
            </style>
        </head>
        <body>
            <h2>APK Full Report</h2>
            <table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;margin-bottom:20px'>
                $summaryRows
            </table>

            ${section("Permissions", report.permissions)}
            ${section("Activities", report.activities)}
            ${section("Services", report.services)}
            ${section("Receivers", report.receivers)}
            ${section("Providers", report.providers)}
            ${section("Native Libraries", report.nativeLibraries)}
            ${section("META-INF Files", report.metaInfFiles)}
        </body>
        </html>
    """.trimIndent()
}
