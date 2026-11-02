package com.example.apkreport

import org.w3c.dom.Element
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

data class ApkReport(
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

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        return
    }

    val apkPath = args[0]
    val apkFile = File(apkPath)

    if (!apkFile.exists()) {
        System.err.println("APK file not found: $apkPath")
        return
    }

    if (!apkFile.name.endsWith(".apk", ignoreCase = true)) {
        System.err.println("The selected file is not an APK: $apkPath")
        return
    }

    try {
        val report = analyzeApk(apkFile)
        val html = buildHtmlReport(report)

        println("APK Full Report")
        println("=".repeat(80))
        println(html)

        val reportFile = File(apkFile.parentFile, "${apkFile.nameWithoutExtension}_report.html")
        FileWriter(reportFile).use { it.write(html) }
        println("\nReport saved to: ${reportFile.absolutePath}")
    } catch (e: Exception) {
        System.err.println("Failed to analyze APK: ${e.message}")
        e.printStackTrace(System.err)
    }
}

private fun printUsage() {
    println("Usage: gradle run --args=\"/path/to/app.apk\"")
    println("Example: gradle run --args=\"/Users/test/Desktop/sample.apk\"")
}

private fun analyzeApk(apkFile: File): ApkReport {
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
            if (name.startsWith("lib/")) {
                nativeLibraries.add(name)
            }
            if (name.startsWith("META-INF/")) {
                metaInfFiles.add(name)
            }
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
        if (name.isNotBlank()) {
            items.add(name)
        }
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

private fun buildHtmlReport(report: ApkReport): String {
    val permissionsHtml = report.permissions.ifEmpty { listOf("None") }
        .joinToString("<br/>")
    val activitiesHtml = report.activities.ifEmpty { listOf("None") }
        .joinToString("<br/>")
    val servicesHtml = report.services.ifEmpty { listOf("None") }
        .joinToString("<br/>")
    val receiversHtml = report.receivers.ifEmpty { listOf("None") }
        .joinToString("<br/>")
    val providersHtml = report.providers.ifEmpty { listOf("None") }
        .joinToString("<br/>")
    val nativeLibrariesHtml = report.nativeLibraries.ifEmpty { listOf("None") }
        .joinToString("<br/>")
    val metaInfHtml = report.metaInfFiles.ifEmpty { listOf("None") }
        .joinToString("<br/>")

    return """
        <html>
        <head>
            <meta charset="UTF-8" />
            <title>APK Full Report</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 30px; color: #222; }
                h1 { margin-bottom: 20px; }
                table { width: 100%; border-collapse: collapse; margin-bottom: 24px; }
                th, td { border: 1px solid #d0d0d0; text-align: left; vertical-align: top; padding: 10px; }
                th { background: #f5f5f5; }
                .section-title { font-size: 18px; font-weight: bold; margin-top: 12px; margin-bottom: 8px; }
            </style>
        </head>
        <body>
            <h1>APK Full Report</h1>

            <table>
                <tr><th>Field</th><th>Value</th></tr>
                <tr><td>File Name</td><td>${report.fileName}</td></tr>
                <tr><td>File Size</td><td>${report.fileSizeBytes} bytes</td></tr>
                <tr><td>SHA-256</td><td>${report.sha256}</td></tr>
                <tr><td>Package Name</td><td>${report.packageName}</td></tr>
                <tr><td>Version Name</td><td>${report.versionName}</td></tr>
                <tr><td>Version Code</td><td>${report.versionCode}</td></tr>
                <tr><td>Min SDK</td><td>${report.minSdk}</td></tr>
                <tr><td>Target SDK</td><td>${report.targetSdk}</td></tr>
                <tr><td>App Label</td><td>${report.appLabel}</td></tr>
                <tr><td>App Icon</td><td>${report.appIcon}</td></tr>
            </table>

            <div class="section-title">Permissions</div>
            <table><tr><td>${permissionsHtml}</td></tr></table>

            <div class="section-title">Activities</div>
            <table><tr><td>${activitiesHtml}</td></tr></table>

            <div class="section-title">Services</div>
            <table><tr><td>${servicesHtml}</td></tr></table>

            <div class="section-title">Receivers</div>
            <table><tr><td>${receiversHtml}</td></tr></table>

            <div class="section-title">Providers</div>
            <table><tr><td>${providersHtml}</td></tr></table>

            <div class="section-title">Native Libraries</div>
            <table><tr><td>${nativeLibrariesHtml}</td></tr></table>

            <div class="section-title">META-INF Files</div>
            <table><tr><td>${metaInfHtml}</td></tr></table>
        </body>
        </html>
    """.trimIndent()
}
