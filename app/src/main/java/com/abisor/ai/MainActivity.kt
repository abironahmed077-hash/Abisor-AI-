package com.abisor.ai

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashView: FrameLayout
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val webClientId =
        "123690801007-ougfqovf1n291ftik1ie4p4m146949pr.apps.googleusercontent.com"

    private val versionUrl =
        "https://raw.githubusercontent.com/abironahmed077-hash/Abisor-AI-/main/version.json"

    private val fileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        filePathCallback?.onReceiveValue(uri?.let { arrayOf(it) })
        filePathCallback = null
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    inner class AuthBridge {
        @JavascriptInterface
        fun signIn() {
            runOnUiThread { startGoogleSignIn() }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        splashView = buildSplash()

        val root = FrameLayout(this)
        root.addView(webView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        root.addView(splashView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setContentView(root)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
        }

        webView.addJavascriptInterface(AuthBridge(), "AbisorAuth")

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                fileLauncher.launch("image/*")
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        webView.loadUrl("file:///android_asset/index.html")

        // ২.৫ সেকেন্ড পর splash সরবে
        lifecycleScope.launch {
            delay(2500)
            splashView.animate().alpha(0f).setDuration(400).withEndAction {
                splashView.visibility = View.GONE
            }.start()
        }

        checkForUpdate()
    }

    private fun buildSplash(): FrameLayout {
        val bg = GradientDrawable(
            GradientDrawable.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#f7fbf9"), Color.parseColor("#e2f4ea"))
        )

        val container = FrameLayout(this).apply { background = bg }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(260, 260)
        }

        val name = TextView(this).apply {
            text = "Abisor AI"
            textSize = 26f
            setTextColor(Color.parseColor("#14211b"))
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        column.addView(icon)
        column.addView(name)

        container.addView(
            column,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        return container
    }

    private fun startGoogleSignIn() {
        val credentialManager = CredentialManager.create(this)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@MainActivity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleCred.idToken
                    val email = googleCred.id
                    val name = googleCred.displayName ?: ""
                    val js = "window.onGoogleSignIn && window.onGoogleSignIn(" +
                        JSONObject.quote(idToken) + "," +
                        JSONObject.quote(email) + "," +
                        JSONObject.quote(name) + ")"
                    webView.evaluateJavascript(js, null)
                }
            } catch (e: GetCredentialException) {
                Toast.makeText(this@MainActivity, "লগইন বাতিল হয়েছে", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "লগইনে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkForUpdate() {
        thread {
            try {
                val conn = URL(versionUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val remoteCode = json.getInt("versionCode")
                val apkUrl = json.getString("apkUrl")
                val versionName = json.optString("versionName")

                if (remoteCode > BuildConfig.VERSION_CODE) {
                    runOnUiThread { showUpdateDialog(apkUrl, versionName) }
                }
            } catch (e: Exception) {
                // ইন্টারনেট না থাকলে চুপচাপ উপেক্ষা
            }
        }
    }

    private fun showUpdateDialog(apkUrl: String, versionName: String) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("নতুন আপডেট আছে")
            .setMessage("নতুন সংস্করণ ($versionName) পাওয়া গেছে। আপডেট করতে চান?")
            .setPositiveButton("আপডেট") { _, _ -> startDownload(apkUrl) }
            .setNegativeButton("পরে", null)
            .show()
    }

    private fun startDownload(apkUrl: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !packageManager.canRequestPackageInstalls()
        ) {
            Toast.makeText(this, "অনুমতি দিয়ে আবার আপডেট চাপুন", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Abisor AI আপডেট")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, "abisor-ai-update.apk"
            )

        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(request)

        Toast.makeText(this, "ডাউনলোড শুরু হয়েছে", Toast.LENGTH_SHORT).show()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val done = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (done == id) {
                    val uri = dm.getUriForDownloadedFile(id)
                    if (uri != null) {
                        val install = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(install)
                    }
                    unregisterReceiver(this)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
