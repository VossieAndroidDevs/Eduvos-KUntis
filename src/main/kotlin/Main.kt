package org.neosahadeo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.warrenstrange.googleauth.GoogleAuthenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.Exception
import org.apache.commons.codec.binary.Base32

class KUntis(){
    @Serializable
    private data class Auth(val clientTime: Long, val user: String, val otp: String)

    @Serializable
    private data class Params(val auth: Auth)

    @Serializable
    private data class Payload(val id: Int = 0, val jsonrpc: String = "2.0", val method: String, val params: List<Params>)

    companion object {
        fun login(secretBase: String, email: String): String? {
            val time = System.currentTimeMillis()
            val base32 = Base32()
            val secret = base32.decode(secretBase)

            val authenticator = GoogleAuthenticator()
            val otpInt = authenticator.getTotpPassword(secret.toString())
            val otp = String.format("%06d", otpInt)

            val client = OkHttpClient()
            val payload = Payload(
                method = "getUserData2017",
                params = listOf(Params(auth = Auth(clientTime = time, user = email, otp = otp)))
            )

            val json = Json.encodeToString(payload)
            val mediaType = "application/json".toMediaType()
            val body = json.toRequestBody()

            val req = Request.Builder()
                .url("https://eduvos-campus.webuntis.com/WebUntis/jsonrpc_intern.do")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            var jsession = client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) throw Exception("HTTP Error ${res.code}")
                var found: String? = null
                for ((name, value) in res.headers) {
                    if (name == "set-cookie") {
                        for (x in value.split(";")) {
                            if (x.trim().startsWith("JSESSIONID")){
                                found = x
                            }
                        }
                    }
                    if (found != null) break
                }
                found
            }
            if (jsession == null)
                throw kotlin.Exception("JSession ID not found.")

            val accessReq = Request.Builder()
                .url("https://eduvos-campus.webuntis.com/WebUntis/api/token/new")
                .get()
                .addHeader("Accept", "application/json")
                .addHeader("Cookie", jsession)
                .build()

            println("----DATA----")
            client.newCall(accessReq).execute().use { res ->
                if (!res.isSuccessful) throw Exception("HTTP Error ${res.code}")
                println(res.body.string())
            }


            return null
        }
    }
}

fun main() {
    KUntis.login("", "")
}