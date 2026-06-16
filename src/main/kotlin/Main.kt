package org.neosahadeo
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.ByteString
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST


class KUntis(){
    @Serializable
    private data class Auth(val clientTime: Long, val user: String, val otp: String)

    @Serializable
    private data class Params(val auth: Auth)

    @Serializable
    private data class Payload(val id: Int = 0, val jsonrpc: String = "2.0", val method: String, val params: List<Params>)

    private interface ApiService {
        @Headers("Content-Type: application/json", "Accept: application/json")
        @POST("WebUntis/jsonrpc_intern.do")
        suspend fun postPayload(@Body payload: Payload): Response<ResponseBody>

        @Headers("Content-Type: application/json", "Accept: application/json")
        @GET("WebUntis/api/token/new")
        suspend fun reqAccess(@Header("Cookie") cookie: String): Response<ResponseBody>
    }

    companion object {
        suspend fun login(secret: String, email: String): String? {
            val time = System.currentTimeMillis()
            val otp = HTOP.generate(base32Decode(secret),time )
            println("Key: $otp")

            val jsonCfg = Json { encodeDefaults = true; prettyPrint = false; explicitNulls = false }
            val contentType = "application/json; charset=utf-8".toMediaType()
            val rf = Retrofit.Builder()
                .baseUrl("https://eduvos-campus.webuntis.com/")
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(jsonCfg.asConverterFactory(contentType))
                .build()

            val service = rf.create(ApiService::class.java)

//            val client = OkHttpClient()
            val payload = Payload(
                method = "getUserData2017",
                params = listOf(Params(auth = Auth(clientTime = time, user = email, otp = otp)))
            )
            val call = service.postPayload(payload)
            if (call.isSuccessful) {
                var jsession: String? = null
                for ((name, value) in call.headers()) {
                    if (name == "set-cookie") {
                        for (x in value.split(";")) {
                            if (x.trim().startsWith("JSESSIONID")){
                                jsession = x
                            }
                        }
                    }
                    if (jsession != null) break
                }
            if (jsession == null){
                throw kotlin.Exception("JSession ID not found.")
            }

                val accessCall = service.reqAccess(jsession)
                if (accessCall.isSuccessful){
                    println("----DATA----")
                    val bodyData = accessCall.body()
                    println(bodyData?.string())
                } else {
                    println("Error: ${call.code()} ${call.errorBody()?.string()}")
                }

            } else {
                println("Error: ${call.code()} ${call.errorBody()?.string()}")
            }

            return null
        }
    }
}

suspend fun main() {
    KUntis.login("", "")
}