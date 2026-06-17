package org.neosahadeo
import com.beust.klaxon.Klaxon
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.io.StringReader
import kotlin.system.exitProcess


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

        @Headers("Content-Type: application/json", "Accept: application/json")
        @GET("WebUntis/api/rest/view/v1/timetable/entries?start={startDate}&end={endDate}")
        suspend fun getTimetable(@Header("Authorization") authorization: String): Response<ResponseBody>

        @Headers("Content-Type: application/json", "Accept: application/json")
        @GET("WebUntis/api/rest/view/v1/timetable/filter?resourceType=STUDENT")
        suspend fun fetchID(@Header("Authorization") authorization: String): Response<ResponseBody>
    }

    companion object {
        /**
         * Generate the Retrofit default API service
         * interface.
         * @return {KUntis.ApiService}
         * */
        private fun genService(): ApiService{
            val jsonCfg = Json { encodeDefaults = true; prettyPrint = false; explicitNulls = false }
            val contentType = "application/json; charset=utf-8".toMediaType()
            val rf = Retrofit.Builder()
                .baseUrl("https://eduvos-campus.webuntis.com/")
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(jsonCfg.asConverterFactory(contentType))
                .build()

            val service = rf.create(ApiService::class.java)
            return service
        }

        /**
         * Log in to the Eduvos Untis site.
         * This will throw and error if a request fails.
         * Returns an authorization token.
         * @param {String} secret; should be base32.
         * @param {String} email; Eduvos email.
         * @return {String?} Authorization Token.
         * */
        suspend fun login(secret: String, email: String): String? {
            val time = System.currentTimeMillis()
            val otp = HTOP.generate(base32Decode(secret),time )

            val service = genService()

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
                throw kotlin.Exception("Error[2]: JSession ID not found.")
            }

                val accessCall = service.reqAccess(jsession)
                if (accessCall.isSuccessful){
                    val bodyData = accessCall.body()
                    return bodyData?.string()
                } else {
                    throw kotlin.Exception("Error[1]: ${call.code()} ${call.errorBody()?.string()}")
                }
            } else {
                throw kotlin.Exception("Error[0]: ${call.code()} ${call.errorBody()?.string()}")
            }

            return null
        }

        /**
         * Fetches the internal ID for a student.
         * This ID is used to help locate the correct timetable.
         * @param {String} authorization bearer token.
         * @return {Int?} returns an ID
         * */
        suspend fun fetchID(authorization: String): Int?{
            val service = genService()
            val req = service.fetchID(authorization)
            if (req.isSuccessful){
                val body = req.body()
                if (body!=null){
                    val klaxon = Klaxon()
                    val obj = klaxon.parseJsonObject(StringReader(body.string()))
                    val preSelected = obj.obj("preSelected")
                    val id = preSelected?.int("id")
                    return id
                }
            } else {
                throw kotlin.Exception("Error: ${req.code()} ${req.errorBody()?.string()}")
            }
            return null
        }
    }
}

suspend fun main() {
    print("Enter Secret: ")
    val secret = readln()
    print("Enter Email: ")
    val email = readln()
    println("Contacting Eduvos Servers, this may take some time...")
    println(KUntis.login(secret, email))
    exitProcess(0)
}