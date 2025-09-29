package com.example.streamingcameraapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class CameraControlFragment : Fragment() {

    private val CAM_WEST = "192.168.88.3"
    private val CAM_EAST = "192.168.88.2"
    private val USER = "admin"
    private val PASS = "oneroom"
    private val WEST_STREAM_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/mjpg/video.cgi"
    private val EAST_STREAM_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/mjpg/video.cgi"
    private val WEST_QUERY_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?query=presetposall"
    private val EAST_QUERY_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?query=presetposall"

    private var WEST_CHOIR_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=1"
    private var WEST_PULPIT_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=2"
    private var WEST_HOME_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=3"
    private var WEST_PANORAMA_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=4"
    private var WEST_PRESET5_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=5"
    private var WEST_PRESET6_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=6"
    private var WEST_PRESET7_URL = "http://$USER:$PASS@$CAM_WEST/axis-cgi/com/ptz.cgi?gotoserverpresetno=7"
    private var EAST_CHOIR_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Choir"
    private var EAST_PULPIT_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Pulpit"
    private var EAST_HOME_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Home"
    private var EAST_PANORAMA_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Panorama"
    private var EAST_PRESET5_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Preset5"
    private var EAST_PRESET6_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Preset6"
    private var EAST_PRESET7_URL = "http://$USER:$PASS@$CAM_EAST/axis-cgi/com/ptz.cgi?gotoserverpresetname=Preset7"

    private val westPresets: MutableMap<Int, String> = mutableMapOf()
    private val eastPresets: MutableMap<Int, String> = mutableMapOf()

    private lateinit var tvUrlWest: TextView
    private lateinit var tvUrlEast: TextView
    private lateinit var webViewWest: WebView
    private lateinit var webViewEast: WebView
    private lateinit var btnWestChoir: Button
    private lateinit var btnWestPulpit: Button
    private lateinit var btnWestHome: Button
    private lateinit var btnWestPanorama: Button
    private lateinit var btnWestPreset5: Button
    private lateinit var btnWestPreset6: Button
    private lateinit var btnWestPreset7: Button
    private lateinit var btnWestRefresh: Button
    private lateinit var btnEastChoir: Button
    private lateinit var btnEastPulpit: Button
    private lateinit var btnEastHome: Button
    private lateinit var btnEastPanorama: Button
    private lateinit var btnEastPreset5: Button
    private lateinit var btnEastPreset6: Button
    private lateinit var btnEastPreset7: Button
    private lateinit var btnEastRefresh: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                100
            )
        }

        tvUrlWest = view.findViewById(R.id.tv_url_west)
        tvUrlEast = view.findViewById(R.id.tv_url_east)
        tvUrlWest.text = "West Camera URL will appear here"
        tvUrlEast.text = "East Camera URL will appear here"

        webViewWest = view.findViewById(R.id.webview_west)
        webViewEast = view.findViewById(R.id.webview_east)

        setupWebView(webViewWest, WEST_STREAM_URL)
        setupWebView(webViewEast, EAST_STREAM_URL)

        btnWestChoir = view.findViewById(R.id.btn_west_choir)
        btnWestPulpit = view.findViewById(R.id.btn_west_pulpit)
        btnWestHome = view.findViewById(R.id.btn_west_home)
        btnWestPanorama = view.findViewById(R.id.btn_west_panorama)
        btnWestPreset5 = view.findViewById(R.id.btn_west_preset5)
        btnWestPreset6 = view.findViewById(R.id.btn_west_preset6)
        btnWestPreset7 = view.findViewById(R.id.btn_west_preset7)
        btnWestRefresh = view.findViewById(R.id.btn_west_refresh)
        btnEastChoir = view.findViewById(R.id.btn_east_choir)
        btnEastPulpit = view.findViewById(R.id.btn_east_pulpit)
        btnEastHome = view.findViewById(R.id.btn_east_home)
        btnEastPanorama = view.findViewById(R.id.btn_east_panorama)
        btnEastPreset5 = view.findViewById(R.id.btn_east_preset5)
        btnEastPreset6 = view.findViewById(R.id.btn_east_preset6)
        btnEastPreset7 = view.findViewById(R.id.btn_east_preset7)
        btnEastRefresh = view.findViewById(R.id.btn_east_refresh)

        queryPresets {
            setupButtonListeners()
        }
    }

    private fun setupWebView(webView: WebView, url: String) {
        webView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            domStorageEnabled = true
        }
        webView.loadUrl(url)
    }

    private fun queryPresets(onComplete: () -> Unit) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val westResponse = fetchQuery(WEST_QUERY_URL)
            parsePresets(westResponse, westPresets, CAM_WEST, true)

            val eastResponse = fetchQuery(EAST_QUERY_URL)
            parsePresets(eastResponse, eastPresets, CAM_EAST, false)

            activity?.runOnUiThread {
                if (westPresets.isEmpty() && eastPresets.isEmpty()) {
                    Toast.makeText(context, "Failed to sync presets—using defaults", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Presets synced successfully", Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }
        }
    }

    private fun fetchQuery(urlString: String): String {
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.connectTimeout = 5000
            urlConnection.readTimeout = 5000

            val responseCode = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return urlConnection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.e("PRESETS", "Query failed: Code $responseCode")
                File(requireContext().filesDir, "camera_log.txt").appendText("[Query Presets] Failed: Code $responseCode\n")
                return ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PRESETS", "Query error: ${e.message}")
            File(requireContext().filesDir, "camera_log.txt").appendText("[Query Presets] Error: ${e.message}\n")
            return ""
        } finally {
            urlConnection?.disconnect()
        }
    }

    private fun parsePresets(response: String, presetsMap: MutableMap<Int, String>, camIp: String, preferNumbers: Boolean) {
        if (response.isBlank() || response.contains("Error")) {
            Log.w("PRESETS", "Invalid response: $response")
            File(requireContext().filesDir, "camera_log.txt").appendText("[Parse Presets] Invalid response: $response\n")
            return
        }

        presetsMap.clear()
        response.lines().forEach { line ->
            if (line.startsWith("presetposno")) {
                val parts = line.split("=")
                if (parts.size == 2) {
                    val num = parts[0].substringAfter("presetposno").toIntOrNull() ?: return@forEach
                    val name = parts[1].trim()
                    presetsMap[num] = name
                }
            }
        }

        activity?.runOnUiThread {
            updateButtonsAndUrls(presetsMap, camIp, preferNumbers)
        }
    }

    private fun updateButtonsAndUrls(presetsMap: Map<Int, String>, camIp: String, preferNumbers: Boolean) {
        val buttons = if (camIp == CAM_WEST) {
            listOf(
                1 to btnWestChoir,
                2 to btnWestPulpit,
                3 to btnWestHome,
                4 to btnWestPanorama,
                5 to btnWestPreset5,
                6 to btnWestPreset6,
                7 to btnWestPreset7
            )
        } else {
            listOf(
                1 to btnEastChoir,
                2 to btnEastPulpit,
                3 to btnEastHome,
                4 to btnEastPanorama,
                5 to btnEastPreset5,
                6 to btnEastPreset6,
                7 to btnEastPreset7
            )
        }

        buttons.forEachIndexed { index, (num, button) ->
            val name = presetsMap[num] ?: "Preset ${num}"
            button.text = name
            if (index >= presetsMap.size) {
                button.visibility = View.GONE
            } else {
                button.visibility = View.VISIBLE
            }

            val url = if (!preferNumbers && name.isNotBlank() && name != "Preset $num") {
                "http://$USER:$PASS@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetname=$name"
            } else {
                "http://$USER:$PASS@$camIp/axis-cgi/com/ptz.cgi?gotoserverpresetno=$num"
            }

            when (num) {
                1 -> if (camIp == CAM_WEST) WEST_CHOIR_URL = url else EAST_CHOIR_URL = url
                2 -> if (camIp == CAM_WEST) WEST_PULPIT_URL = url else EAST_PULPIT_URL = url
                3 -> if (camIp == CAM_WEST) WEST_HOME_URL = url else EAST_HOME_URL = url
                4 -> if (camIp == CAM_WEST) WEST_PANORAMA_URL = url else EAST_PANORAMA_URL = url
                5 -> if (camIp == CAM_WEST) WEST_PRESET5_URL = url else EAST_PRESET5_URL = url
                6 -> if (camIp == CAM_WEST) WEST_PRESET6_URL = url else EAST_PRESET6_URL = url
                7 -> if (camIp == CAM_WEST) WEST_PRESET7_URL = url else EAST_PRESET7_URL = url
            }
        }
    }

    private fun setupButtonListeners() {
        btnWestChoir.setOnClickListener {
            tvUrlWest.text = WEST_CHOIR_URL
            sendRequest(WEST_CHOIR_URL, btnWestChoir.text.toString())
        }
        btnWestPulpit.setOnClickListener {
            tvUrlWest.text = WEST_PULPIT_URL
            sendRequest(WEST_PULPIT_URL, btnWestPulpit.text.toString())
        }
        btnWestHome.setOnClickListener {
            tvUrlWest.text = WEST_HOME_URL
            sendRequest(WEST_HOME_URL, btnWestHome.text.toString())
        }
        btnWestPanorama.setOnClickListener {
            tvUrlWest.text = WEST_PANORAMA_URL
            sendRequest(WEST_PANORAMA_URL, btnWestPanorama.text.toString())
        }
        btnWestPreset5.setOnClickListener {
            tvUrlWest.text = WEST_PRESET5_URL
            sendRequest(WEST_PRESET5_URL, btnWestPreset5.text.toString())
        }
        btnWestPreset6.setOnClickListener {
            tvUrlWest.text = WEST_PRESET6_URL
            sendRequest(WEST_PRESET6_URL, btnWestPreset6.text.toString())
        }
        btnWestPreset7.setOnClickListener {
            tvUrlWest.text = WEST_PRESET7_URL
            sendRequest(WEST_PRESET7_URL, btnWestPreset7.text.toString())
        }
        btnWestRefresh.setOnClickListener {
            webViewWest.reload()
            Toast.makeText(context, "Refreshing West Camera...", Toast.LENGTH_SHORT).show()
        }

        btnEastChoir.setOnClickListener {
            tvUrlEast.text = EAST_CHOIR_URL
            sendRequest(EAST_CHOIR_URL, btnEastChoir.text.toString())
        }
        btnEastPulpit.setOnClickListener {
            tvUrlEast.text = EAST_PULPIT_URL
            sendRequest(EAST_PULPIT_URL, btnEastPulpit.text.toString())
        }
        btnEastHome.setOnClickListener {
            tvUrlEast.text = EAST_HOME_URL
            sendRequest(EAST_HOME_URL, btnEastHome.text.toString())
        }
        btnEastPanorama.setOnClickListener {
            tvUrlEast.text = EAST_PANORAMA_URL
            sendRequest(EAST_PANORAMA_URL, btnEastPanorama.text.toString())
        }
        btnEastPreset5.setOnClickListener {
            tvUrlEast.text = EAST_PRESET5_URL
            sendRequest(EAST_PRESET5_URL, btnEastPreset5.text.toString())
        }
        btnEastPreset6.setOnClickListener {
            tvUrlEast.text = EAST_PRESET6_URL
            sendRequest(EAST_PRESET6_URL, btnEastPreset6.text.toString())
        }
        btnEastPreset7.setOnClickListener {
            tvUrlEast.text = EAST_PRESET7_URL
            sendRequest(EAST_PRESET7_URL, btnEastPreset7.text.toString())
        }
        btnEastRefresh.setOnClickListener {
            webViewEast.reload()
            Toast.makeText(context, "Refreshing East Camera...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendRequest(urlString: String, actionName: String) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.connectTimeout = 5000
                urlConnection.readTimeout = 5000

                val responseCode = urlConnection.responseCode
                val responseBody = if (responseCode == HttpURLConnection.HTTP_OK) {
                    urlConnection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    urlConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
                Log.d("PTZ_RESPONSE", "[$actionName] Code: $responseCode, Body: $responseBody")
                File(requireContext().filesDir, "camera_log.txt").appendText("[$actionName] Code: $responseCode, Body: $responseBody\n")

                activity?.runOnUiThread {
                    if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || (responseCode == HttpURLConnection.HTTP_OK && !responseBody.contains("Error"))) {
                        Toast.makeText(context, "$actionName: Success", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "$actionName: Failed (Code: $responseCode, $responseBody)", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("PTZ_RESPONSE", "[$actionName] Error: ${e.message}")
                File(requireContext().filesDir, "camera_log.txt").appendText("[$actionName] Error: ${e.message}\n")
                activity?.runOnUiThread {
                    Toast.makeText(context, "$actionName: Error (${e.message})", Toast.LENGTH_LONG).show()
                }
            } finally {
                urlConnection?.disconnect()
            }
        }
    }
}
