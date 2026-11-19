package com.abisor.ai

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private val latestReleaseApi = "https://api.github.com/repos/abironahmed077-hash/Abisor-AI-/releases/latest"
    private val apkUrl = "https://github.com/abironahmed077-hash/Abisor-AI-/releases/latest/download/app-debug.apk"

    @Suppress("DEPRECATION")
    private fun currentVersionCode(): Long {
        val info = packageManager.getPackageInfo(packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    }

    private fun checkForUpdate() {
        Thread {
            try {
                val conn = URL(latestReleaseApi).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val tag = JSONObject(body).getString("tag_name")
                    val latest = tag.removePrefix("v").toLongOrNull()
                    if (latest != null && latest > currentVersionCode()) {
                        runOnUiThread { showUpdateDialog() }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
            }
        }.start()
    }

    private fun showUpdateDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("নতুন সংস্করণ এসেছে")
            .setMessage("abisor ai-এর নতুন সংস্করণ পাওয়া যাচ্ছে। এখন আপডেট করবেন?")
            .setPositiveButton("আপডেট করুন") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
            }
            .setNegativeButton("পরে", null)
            .show()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/index.html")

        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()
        val match = ViewGroup.LayoutParams.MATCH_PARENT
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
        val green = Color.parseColor("#3DDC97")
        val appName = "Abisor AI"

        val glow = View(this)
        val glowShape = GradientDrawable()
        glowShape.gradientType = GradientDrawable.RADIAL_GRADIENT
        glowShape.setGradientRadius(dp(180).toFloat())
        glowShape.setColors(intArrayOf(Color.parseColor("#553DDC97"), Color.parseColor("#003DDC97")))
        glow.background = glowShape
        glow.alpha = 0f

        val icon = ImageView(this)
        icon.setImageResource(R.drawable.abisor_icon)
        icon.layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
        icon.alpha = 0f
        icon.scaleX = 0.6f
        icon.scaleY = 0.6f

        val title = TextView(this)
        title.textSize = 30f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(Color.WHITE)
        title.setShadowLayer(18f, 0f, 0f, Color.parseColor("#803DDC97"))
        val titleParams = LinearLayout.LayoutParams(wrap, wrap)
        titleParams.leftMargin = dp(14)
        title.layoutParams = titleParams
        title.visibility = View.GONE

        val group = LinearLayout(this)
        group.orientation = LinearLayout.HORIZONTAL
        group.gravity = Gravity.CENTER_VERTICAL
        group.addView(icon)
        group.addView(title)

        val line = View(this)
        val lineShape = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor("#003DDC97"), Color.parseColor("#FF3DDC97"), Color.parseColor("#003DDC97"))
        )
        line.background = lineShape
        val lineParams = LinearLayout.LayoutParams(dp(200), dp(2))
        lineParams.topMargin = dp(16)
        line.layoutParams = lineParams
        line.scaleX = 0f

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.gravity = Gravity.CENTER
        content.addView(group)
        content.addView(line)

        val splash = FrameLayout(this)
        splash.setBackgroundColor(Color.parseColor("#050D0A"))
        splash.isClickable = true
        splash.addView(glow, FrameLayout.LayoutParams(dp(360), dp(360), Gravity.CENTER))
        splash.addView(content, FrameLayout.LayoutParams(wrap, wrap, Gravity.CENTER))

        val root = FrameLayout(this)
        root.addView(webView, FrameLayout.LayoutParams(match, match))
        root.addView(splash, FrameLayout.LayoutParams(match, match))
        setContentView(root)

        glow.animate().alpha(1f).setDuration(900).start()
        icon.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(600).setInterpolator(OvershootInterpolator(1.4f)).start()

        val typing = ValueAnimator.ofInt(0, appName.length)
        typing.duration = 900
        typing.startDelay = 800
        typing.interpolator = LinearInterpolator()
        typing.addUpdateListener { anim ->
            val n = anim.animatedValue as Int
            if (n > 0) {
                val styled = SpannableStringBuilder(appName.substring(0, n))
                if (n > 7) {
                    styled.setSpan(ForegroundColorSpan(green), 7, n, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                title.visibility = View.VISIBLE
                title.text = styled
            }
        }
        typing.start()

        line.animate().scaleX(1f).setStartDelay(1600).setDuration(700).start()

        Handler(Looper.getMainLooper()).postDelayed({
            splash.animate().alpha(0f).setDuration(450).withEndAction {
                root.removeView(splash)
            }.start()
        }, 3500)

        Handler(Looper.getMainLooper()).postDelayed({ checkForUpdate() }, 4200)
    }
}
