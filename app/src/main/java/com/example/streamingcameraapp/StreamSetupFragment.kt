package com.example.streamingcameraapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.Executors // Added this import

class StreamSetupFragment : Fragment() {

    private lateinit var etProclaimName: EditText
    private lateinit var btnStartStream: Button
    private lateinit var btnStopStream: Button
    private lateinit var pbVolumeMic1: ProgressBar
    private lateinit var pbVolumeMic2: ProgressBar
    private lateinit var wvObsPreview: WebView
    private lateinit var wvYouTubePreview: WebView
    private var obsWebSocket: WebSocket? = null
    private val OBS_WS_URL = "ws://192.168.88.[obs-pc-ip]:4455" // Replace with church PC IP
    private val PROCLAIM_API_URL = "https://api.logos.com/api/presentations" // Replace with exact endpoint
    private val YOUTUBE_STREAM_KEY = "your-youtube-stream-key" // From YouTube Studio
    private val YOUTUBE_STREAM_ID = "your-youtube-stream-id" // From YouTube Studio

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_stream_setup, container, false)

        etProclaimName = view.findViewById(R.id.et_proclaim_name)
        btnStartStream = view.findViewById(R.id.btn_start_stream)
        btnStopStream = view.findViewById(R.id.btn_stop_stream)
        pbVolumeMic1 = view.findViewById(R.id.pb_volume_mic1)
        pbVolumeMic2 = view.findViewById(R.id.pb_volume_mic2)
        wvObsPreview = view.findViewById(R.id.wv_obs_preview)
        wvYouTubePreview = view.findViewById(R.id.wv_youtube_preview)

        wvObsPreview.settings.javaScriptEnabled = true
        wvObsPreview.loadUrl("http://192.168.88.[obs-pc-ip]:[obs-web-port]") // Configure OBS web interface
        wvYouTubePreview.settings.javaScriptEnabled = true
        wvYouTubePreview.loadUrl("https://youtube.com/embed/$YOUTUBE_STREAM_ID")

        btnStartStream.setOnClickListener {
            fetchProclaimName { name ->
                etProclaimName.setText(name)
                startStream(name)
            }
        }

        btnStopStream.setOnClickListener {
            stopStream()
        }

        connectToOBS()

        return view
    }

    private fun connectToOBS() {
        val client = OkHttpClient()
        val request = Request.Builder().url(OBS_WS_URL).build()
        obsWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                val auth = JSONObject().put("op", 1).put("d", JSONObject().put("rpcVersion", 1).put("authentication", "your-obs-password"))
                webSocket.send(auth.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.has("op") && json.getInt("op") == 2) {
                    // Auth success, request volume levels
                    val getVolumes = JSONObject().put("op", 6).put("d", JSONObject().put("requestType", "GetSourceAudioLevels").put("requestId", "1").put("inputName", "Mic1"))
                    webSocket.send(getVolumes.toString())
                } else if (json.has("d") && json.getJSONObject("d").has("requestType") && json.getJSONObject("d").getString("requestType") == "GetSourceAudioLevels") {
                    val levels = json.getJSONObject("d").getJSONArray("inputAudioLevels")
                    activity?.runOnUiThread {
                        pbVolumeMic1.progress = levels.getInt(0) // Adjust based on OBS source names
                        pbVolumeMic2.progress = levels.getInt(1)
                        if (levels.getInt(0) < 20) Toast.makeText(context, "Low Audio Alert: Mic1", Toast.LENGTH_SHORT).show()
                        if (levels.getInt(1) < 20) Toast.makeText(context, "Low Audio Alert: Mic2", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "OBS Connection Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun fetchProclaimName(callback: (String) -> Unit) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            var client: OkHttpClient? = null
            try {
                client = OkHttpClient()
                val request = Request.Builder()
                    .url(PROCLAIM_API_URL)
                    .header("Authorization", Credentials.basic("your-proclaim-api-key", "")) // Get API key from Faithlife
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseCode = response.code
                    if (responseCode == 200) {
                        val jsonResponse = JSONObject(response.body?.string() ?: "")
                        val name = jsonResponse.getString("title") // Adjust based on actual API response
                        activity?.runOnUiThread {
                            callback(name)
                        }
                    } else {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Proclaim API Failed (Code: $responseCode)", Toast.LENGTH_LONG).show()
                            callback(etProclaimName.text.toString()) // Fallback to manual input
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(context, "Proclaim API Error: ${e.message}", Toast.LENGTH_LONG).show()
                    callback(etProclaimName.text.toString()) // Fallback
                }
            }
        }
    }

    private fun startStream(name: String) {
        val setTitle = JSONObject().put("op", 6).put("d", JSONObject().put("requestType", "SetStreamServiceSettings").put("requestId", "2").put("streamServiceType", "rtmp_custom").put("streamServiceSettings", JSONObject().put("server", "rtmp://a.rtmp.youtube.com/live2").put("key", YOUTUBE_STREAM_KEY).put("title", name)))
        obsWebSocket?.send(setTitle.toString())
        val startStream = JSONObject().put("op", 6).put("d", JSONObject().put("requestType", "StartStream").put("requestId", "3"))
        obsWebSocket?.send(startStream.toString())
        activity?.runOnUiThread {
            Toast.makeText(context, "Starting Stream: $name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopStream() {
        val stopStream = JSONObject().put("op", 6).put("d", JSONObject().put("requestType", "StopStream").put("requestId", "4"))
        obsWebSocket?.send(stopStream.toString())
        activity?.runOnUiThread {
            Toast.makeText(context, "Stopping Stream", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        obsWebSocket?.close(1000, "Fragment destroyed")
    }
}
