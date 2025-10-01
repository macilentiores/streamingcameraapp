package com.example.streamingcameraapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.Executors

class StreamSetupFragment : Fragment() {

    private lateinit var etProclaimName: EditText
    private lateinit var btnStartStream: Button
    private lateinit var btnStopStream: Button
    private lateinit var btnSetupStream: Button
    private lateinit var tvStreamStatus: TextView
    private lateinit var pbVolumeMic1: ProgressBar
    private lateinit var pbVolumeMic2: ProgressBar
    private lateinit var wvObsPreview: WebView
    private lateinit var wvYouTubePreview: WebView
    private var obsWebSocket: WebSocket? = null
    private val OBS_WS_URL = "ws://172.16.1.153:4455" // Replace with church PC IP
    private val PROCLAIM_API_URL = "https://api.logos.com/api/presentations" // Replace with exact endpoint
    private val YOUTUBE_STREAM_KEY = "your-youtube-stream-key" // From YouTube Studio
    private val YOUTUBE_STREAM_ID = "your-youtube-stream-id" // From YouTube Studio

    // OBS Request IDs
    private val REQUEST_ID_GET_MIC_LEVELS = "getMicLevels_${UUID.randomUUID()}"
    private val REQUEST_ID_CONFIGURE_STREAM_SETTINGS = "configureStreamSettings_${UUID.randomUUID()}"
    private val REQUEST_ID_START_STREAMING = "startStreaming_${UUID.randomUUID()}"
    private val REQUEST_ID_STOP_STREAMING = "stopStreaming_${UUID.randomUUID()}"


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_stream_setup, container, false)

        etProclaimName = view.findViewById(R.id.et_proclaim_name)
        btnStartStream = view.findViewById(R.id.btn_start_stream)
        btnStopStream = view.findViewById(R.id.btn_stop_stream)
        btnSetupStream = view.findViewById(R.id.btn_setup_stream)
        tvStreamStatus = view.findViewById(R.id.tv_stream_status)
        pbVolumeMic1 = view.findViewById(R.id.pb_volume_mic1)
        pbVolumeMic2 = view.findViewById(R.id.pb_volume_mic2)
        wvObsPreview = view.findViewById(R.id.wv_obs_preview)
        wvYouTubePreview = view.findViewById(R.id.wv_youtube_preview)

        wvObsPreview.settings.javaScriptEnabled = true
        wvObsPreview.loadUrl("http://172.16.1.153:8080")
        wvYouTubePreview.settings.javaScriptEnabled = true
        wvYouTubePreview.loadUrl("https://youtube.com/embed/$YOUTUBE_STREAM_ID")

        btnSetupStream.setOnClickListener {
            Toast.makeText(context, "Setting up stream...", Toast.LENGTH_SHORT).show()
            tvStreamStatus.text = "Stream Status: Setting up..."
            fetchProclaimName { name ->
                etProclaimName.setText(name)
                configureOBSStreamSettings(name) // Configure OBS with the fetched name
                activity?.runOnUiThread {
                     tvStreamStatus.text = "Stream Ready: $name"
                }
            }
        }

        btnStartStream.setOnClickListener {
            // Assumes stream has been configured by "Setup Stream" button
            sendStartStreamingCommandToOBS()
        }

        btnStopStream.setOnClickListener {
            sendStopStreamingCommandToOBS()
        }

        connectToOBS()

        return view
    }

    private fun configureOBSStreamSettings(streamTitle: String) {
        if (obsWebSocket == null) {
            Toast.makeText(context, "OBS not connected. Cannot configure stream.", Toast.LENGTH_LONG).show()
            activity?.runOnUiThread { tvStreamStatus.text = "Stream Status: OBS Disconnected" }
            return
        }
        val streamServiceSettings = JSONObject()
            .put("server", "rtmp://a.rtmp.youtube.com/live2") // Standard YouTube RTMP server
            .put("key", YOUTUBE_STREAM_KEY)
            .put("title", streamTitle) // Set the stream title

        val requestPayload = JSONObject()
            .put("requestType", "SetStreamServiceSettings")
            .put("requestId", REQUEST_ID_CONFIGURE_STREAM_SETTINGS)
            .put("requestData", JSONObject()
                .put("streamServiceType", "rtmp_custom")
                .put("streamServiceSettings", streamServiceSettings)
            )

        val obsRequest = JSONObject()
            .put("op", 6) // Request
            .put("d", requestPayload)

        obsWebSocket?.send(obsRequest.toString())
        activity?.runOnUiThread {
             Toast.makeText(context, "Stream settings sent to OBS with title: $streamTitle", Toast.LENGTH_SHORT).show()
        }
    }


    private fun connectToOBS() {
        val client = OkHttpClient()
        val request = Request.Builder().url(OBS_WS_URL).build()
        obsWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                // Using d.authentication requires rpcVersion >= 1 (OBS WS v5+)
                val authPayload = JSONObject()
                    .put("rpcVersion", 1)
                    // Add authentication string if OBS WebSocket server has authentication enabled
                    // .put("authentication", "your-obs-password") // Uncomment and set password if needed

                val identifyRequest = JSONObject()
                    .put("op", 1) // Identify
                    .put("d", authPayload)
                webSocket.send(identifyRequest.toString())
                activity?.runOnUiThread { tvStreamStatus.text = "Stream Status: OBS Connecting..." }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                val opCode = json.optInt("op", -1)

                when (opCode) {
                    0 -> { // Hello (Server -> Client)
                        // Contains OBS WebSocket version, rpcVersion, and authentication details if enabled
                        // If authentication is required, client should send Identify (op: 1)
                        // If rpcVersion from Hello is not supported, client should disconnect.
                        // For now, we assume auth is not required or handled by Identify if needed.
                        // We can now request initial data, like volume levels
                        val getVolumesRequest = JSONObject()
                            .put("op", 6) // Request
                            .put("d", JSONObject()
                                .put("requestType", "GetInputAudioTracks") // More robust for v5
                                .put("requestId", REQUEST_ID_GET_MIC_LEVELS)
                                .put("requestData", JSONObject().put("inputName", "Mic/Aux")) // Example, use your actual source name
                            )
                        // webSocket.send(getVolumesRequest.toString()) // Example: how to get audio levels
                        activity?.runOnUiThread { tvStreamStatus.text = "Stream Status: OBS Connected" }

                    }
                    2 -> { // Identified (Server -> Client) - Success after Identify (op: 1)
                        activity?.runOnUiThread { tvStreamStatus.text = "Stream Status: OBS Identified" }
                        // Now it's safe to send other requests
                        // Example: Start requesting volume for a specific source
                        // This part needs adjustment based on GetInputAudioTracks response or if you use GetInputVolume
                         val getInputVolume = JSONObject()
                            .put("op", 6)
                            .put("d", JSONObject()
                                .put("requestType", "GetInputVolume")
                                .put("requestId", REQUEST_ID_GET_MIC_LEVELS) // Reusing for simplicity, or use a new one
                                .put("requestData", JSONObject().put("inputName", "Mic/Aux")) // Replace with your actual OBS Mic/Aux source name
                            )
                        webSocket.send(getInputVolume.toString())
                    }

                    5 -> { // Event (Server -> Client)
                        val eventData = json.optJSONObject("d")?.optJSONObject("eventData")
                        val eventType = json.optJSONObject("d")?.optString("eventType")

                        if (eventType == "StreamStateChanged" && eventData != null) {
                            val outputState = eventData.optString("outputState")
                            activity?.runOnUiThread {
                                when (outputState) {
                                    "OBS_WEBSOCKET_OUTPUT_STARTED" -> tvStreamStatus.text = "Stream Status: Live"
                                    "OBS_WEBSOCKET_OUTPUT_STOPPED" -> tvStreamStatus.text = "Stream Status: Offline"
                                    "OBS_WEBSOCKET_OUTPUT_STARTING" -> tvStreamStatus.text = "Stream Status: Starting..."
                                    "OBS_WEBSOCKET_OUTPUT_STOPPING" -> tvStreamStatus.text = "Stream Status: Stopping..."
                                    "OBS_WEBSOCKET_OUTPUT_RECONNECTING" -> tvStreamStatus.text = "Stream Status: Reconnecting..."
                                    else -> tvStreamStatus.text = "Stream Status: $outputState"
                                }
                            }
                        }
                         // Example: Handling InputVolumeChanged event for volume meters
                        if (eventType == "InputVolumeChanged" && eventData != null) {
                            val inputName = eventData.optString("inputName")
                            // Assuming you have two progress bars and two specific input names you're listening for
                            if (inputName == "Mic/Aux") { // Replace with your actual OBS Mic/Aux source name for pbVolumeMic1
                                val inputVolumeDb = eventData.optDouble("inputVolumeDb", -100.0)
                                val volumePercent = ((inputVolumeDb + 60) / 60 * 100).coerceIn(0.0, 100.0) // Example conversion dB to %
                                activity?.runOnUiThread {
                                    pbVolumeMic1.progress = volumePercent.toInt()
                                    if (volumePercent < 20) Toast.makeText(context, "Low Audio: Mic1 ($volumePercent%)", Toast.LENGTH_SHORT).show()
                                }
                            }
                            // Add similar block for pbVolumeMic2 if monitoring another source
                        }
                    }
                    7 -> { // RequestResponse (Server -> Client)
                        val requestResponseData = json.optJSONObject("d")
                        val requestId = requestResponseData?.optString("requestId")
                        val requestStatus = requestResponseData?.optJSONObject("requestStatus")
                        val requestResult = requestStatus?.optBoolean("result", false) == true

                        if (requestId == REQUEST_ID_GET_MIC_LEVELS && requestStatus != null) {
                            if (requestResult) {
                                val responseData = requestResponseData.optJSONObject("responseData")
                                if (responseData != null) {
                                    val volumeDb = responseData.optDouble("inputVolumeDb", -100.0)
                                    // Convert dB to percentage (example, needs tuning based on your preferred dB range)
                                    // -60dB to 0dB range to 0-100%
                                    val volumePercent = ((volumeDb + 60) / 60 * 100).coerceIn(0.0, 100.0)
                                    activity?.runOnUiThread {
                                        pbVolumeMic1.progress = volumePercent.toInt()
                                        // You might want to periodically call GetInputVolume if InputVolumeChanged events aren't sufficient
                                    }
                                }
                            } else {
                                activity?.runOnUiThread { Toast.makeText(context, "Failed to get Mic/Aux volume", Toast.LENGTH_SHORT).show()}
                            }
                        }
                        // Add handlers for other request IDs if needed (e.g., to confirm settings were applied)
                        if (requestId == REQUEST_ID_CONFIGURE_STREAM_SETTINGS && requestStatus != null) {
                            if (requestResult) {
                                activity?.runOnUiThread { Toast.makeText(context, "OBS stream settings configured.", Toast.LENGTH_SHORT).show() }
                            } else {
                                activity?.runOnUiThread { Toast.makeText(context, "Failed to configure OBS stream settings.", Toast.LENGTH_LONG).show() }
                            }
                        }
                         if (requestId == REQUEST_ID_START_STREAMING && requestStatus != null && !requestResult) {
                            activity?.runOnUiThread { Toast.makeText(context, "OBS failed to start stream.", Toast.LENGTH_LONG).show() }
                        }
                        if (requestId == REQUEST_ID_STOP_STREAMING && requestStatus != null && !requestResult) {
                            activity?.runOnUiThread { Toast.makeText(context, "OBS failed to stop stream.", Toast.LENGTH_LONG).show() }
                        }
                    }
                }
            }


            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "OBS Connection Error: ${t.message}", Toast.LENGTH_LONG).show()
                    tvStreamStatus.text = "Stream Status: OBS Disconnected"
                }
            }
        })
    }

    private fun fetchProclaimName(callback: (String) -> Unit) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(PROCLAIM_API_URL)
                    .header("Authorization", Credentials.basic("your-proclaim-api-key", "")) // Get API key from Faithlife
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseCode = response.code
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(response.body?.string() ?: "")
                        val name = jsonResponse.optString("title", etProclaimName.text.toString()) // Adjust based on actual API response
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

    private fun sendStartStreamingCommandToOBS() {
        if (obsWebSocket == null) {
            Toast.makeText(context, "OBS not connected. Cannot start stream.", Toast.LENGTH_LONG).show()
            activity?.runOnUiThread { tvStreamStatus.text = "Stream Status: OBS Disconnected" }
            return
        }
        val requestPayload = JSONObject()
            .put("requestType", "StartStream")
            .put("requestId", REQUEST_ID_START_STREAMING)

        val obsRequest = JSONObject()
            .put("op", 6) // Request
            .put("d", requestPayload)

        obsWebSocket?.send(obsRequest.toString())
        // Stream status will be updated by "StreamStateChanged" event from OBS
        activity?.runOnUiThread {
             Toast.makeText(context, "Start stream command sent to OBS.", Toast.LENGTH_SHORT).show()
             // tvStreamStatus.text = "Stream Status: Starting..." // Set by event now
        }
    }

    private fun sendStopStreamingCommandToOBS() {
        if (obsWebSocket == null) {
            Toast.makeText(context, "OBS not connected. Cannot stop stream.", Toast.LENGTH_LONG).show()
            activity?.runOnUiThread { tvStreamStatus.text = "Stream Status: OBS Disconnected" }
            return
        }
         val requestPayload = JSONObject()
            .put("requestType", "StopStream")
            .put("requestId", REQUEST_ID_STOP_STREAMING)

        val obsRequest = JSONObject()
            .put("op", 6) // Request
            .put("d", requestPayload)

        obsWebSocket?.send(obsRequest.toString())
        // Stream status will be updated by "StreamStateChanged" event from OBS
        activity?.runOnUiThread {
            Toast.makeText(context, "Stop stream command sent to OBS.", Toast.LENGTH_SHORT).show()
            // tvStreamStatus.text = "Stream Status: Stopping..." // Set by event now
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        obsWebSocket?.close(1000, "Fragment destroyed")
    }
}
