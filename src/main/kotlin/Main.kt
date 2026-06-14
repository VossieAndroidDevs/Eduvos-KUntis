package org.neosahadeo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.warrenstrange.googleauth.GoogleAuthenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.Exception
import java.util.Calendar

@Serializable
data class Auth(val clientTime: Long, val user: String, val otp: Int)

@Serializable
data class Params(val auth: Auth)

@Serializable
data class Payload(val id: Int = 0, val jsonrpc: String = "2.0", val method: String, val params: List<Params>)

fun main() {
    val auth = GoogleAuthenticator()
    val code: Int = auth.getTotpPassword("")

    val time = Calendar.getInstance().timeInMillis
    val client = OkHttpClient()
    val payload = Payload(
        method = "getUserData2017",
        params = listOf(Params(auth = Auth(clientTime = time, user = "EDUV4777219@vossie.net", otp = code)))
    )

    val json = Json.encodeToString(payload)
    val mediaType = "application/json".toMediaType()
    val body = json.toRequestBody(mediaType)

    val req = Request.Builder()
        .url("https://eduvos-campus.webuntis.com/WebUntis/jsonrpc.do")
        .post(body)
        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.3")
        .addHeader("Content-Type", "application/json")
        .build()

    client.newCall(req).execute().use {
        res -> if (!res.isSuccessful) throw Exception("HTTP Error ${res.code}")
        for ((name, value) in res.headers) {
            println("$name: $value")
        }
    }
}