package com.camerastreamingapp.data.api

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

private interface OnvifService {
    @Headers("Content-Type: application/soap+xml; charset=utf-8")
    @POST
    suspend fun sendCommand(
        @Url endpoint: String,
        @Body body: okhttp3.RequestBody,
        @Header("SOAPAction") soapAction: String
    ): Response<ResponseBody>
}

class ONVIFCommandManager(
    baseUrl: String,
    username: String? = null,
    password: String? = null
) {
    private val service: OnvifService

    init {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val parsed = runCatching { java.net.URI(normalizedBaseUrl) }.getOrNull()
        require(parsed != null && (parsed.scheme == "http" || parsed.scheme == "https") && !parsed.host.isNullOrBlank()) {
            "baseUrl must be an absolute http(s) URL"
        }
        val clientBuilder = OkHttpClient.Builder()
        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            val auth = Credentials.basic(username, password)
            clientBuilder.addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("Authorization", auth).build())
            }
        }

        service = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(clientBuilder.build())
            .build()
            .create(OnvifService::class.java)
    }

    suspend fun panLeft(endpoint: String, profileToken: String): Result<String> =
        sendContinuousMove(endpoint, profileToken, pan = -0.5f)

    suspend fun panRight(endpoint: String, profileToken: String): Result<String> =
        sendContinuousMove(endpoint, profileToken, pan = 0.5f)

    suspend fun tiltUp(endpoint: String, profileToken: String): Result<String> =
        sendContinuousMove(endpoint, profileToken, tilt = 0.5f)

    suspend fun tiltDown(endpoint: String, profileToken: String): Result<String> =
        sendContinuousMove(endpoint, profileToken, tilt = -0.5f)

    suspend fun zoomIn(endpoint: String, profileToken: String): Result<String> =
        sendContinuousMove(endpoint, profileToken, zoom = 0.5f)

    suspend fun zoomOut(endpoint: String, profileToken: String): Result<String> =
        sendContinuousMove(endpoint, profileToken, zoom = -0.5f)

    suspend fun gotoHome(endpoint: String, profileToken: String): Result<String> =
        sendSoap(
            endpoint,
            "http://www.onvif.org/ver20/ptz/wsdl/GotoHomePosition",
            """
                <tptz:GotoHomePosition>
                  <tptz:ProfileToken>${profileToken.xmlEscaped()}</tptz:ProfileToken>
                </tptz:GotoHomePosition>
            """.trimIndent()
        )

    suspend fun gotoPreset(endpoint: String, profileToken: String, presetToken: String): Result<String> =
        sendSoap(
            endpoint,
            "http://www.onvif.org/ver20/ptz/wsdl/GotoPreset",
            """
                <tptz:GotoPreset>
                  <tptz:ProfileToken>${profileToken.xmlEscaped()}</tptz:ProfileToken>
                  <tptz:PresetToken>${presetToken.xmlEscaped()}</tptz:PresetToken>
                </tptz:GotoPreset>
            """.trimIndent()
        )

    private suspend fun sendContinuousMove(
        endpoint: String,
        profileToken: String,
        pan: Float = 0f,
        tilt: Float = 0f,
        zoom: Float = 0f
    ): Result<String> {
        val body = """
            <tptz:ContinuousMove>
              <tptz:ProfileToken>${profileToken.xmlEscaped()}</tptz:ProfileToken>
              <tptz:Velocity>
                <tt:PanTilt x="$pan" y="$tilt" />
                <tt:Zoom x="$zoom" />
              </tptz:Velocity>
            </tptz:ContinuousMove>
        """.trimIndent()
        return sendSoap(endpoint, "http://www.onvif.org/ver20/ptz/wsdl/ContinuousMove", body)
    }

    private suspend fun sendSoap(endpoint: String, action: String, innerBody: String): Result<String> {
        val endpointUri = runCatching { java.net.URI(endpoint) }.getOrNull()
        if (endpointUri == null || (endpointUri.scheme != "http" && endpointUri.scheme != "https") || endpointUri.host.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("endpoint must be an absolute http(s) URL"))
        }

        val envelope = """
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl"
                xmlns:tt="http://www.onvif.org/ver10/schema">
              <s:Body>
                $innerBody
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        return runCatching {
            val requestBody = envelope.toRequestBody("application/soap+xml; charset=utf-8".toMediaType())
            val response = service.sendCommand(endpoint, requestBody, action)
            val body = response.body()
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                body?.close()
                throw IllegalStateException("ONVIF command failed: ${response.code()}")
            }
            body?.use { it.string() }.orEmpty()
        }
    }

    private fun String.xmlEscaped(): String {
        return this
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
