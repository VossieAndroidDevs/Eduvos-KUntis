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
import java.io.File
import java.io.StringReader
import kotlin.system.exitProcess

class KUntis(var secret: String, var email: String) {
    private interface loginDataclass {
        @Serializable
        data class Auth(val clientTime: Long, val user: String, val otp: String)

        @Serializable
        data class Params(val auth: Auth)

        @Serializable
        data class Payload(
            val id: Int = 0,
            val jsonrpc: String = "2.0",
            val method: String,
            val params: List<Params>
        )

    }

    private interface API {
        @Headers("Content-Type: application/json", "Accept: application/json")
        @POST("WebUntis/jsonrpc_intern.do")
        suspend fun login(@Body payload: KUntis.loginDataclass.Payload): Response<ResponseBody>

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

    private var service: API
    private var authToken: String? = null
    private var loginMaxRequests = 3

    init {
        val jsonCfg = Json { encodeDefaults = true; prettyPrint = false; explicitNulls = false }
        val contentType = "application/json; charset=utf-8".toMediaType()
        val rf = Retrofit.Builder()
            .baseUrl("https://eduvos-campus.webuntis.com/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(jsonCfg.asConverterFactory(contentType))
            .build()
        service = rf.create(API::class.java)
    }

    suspend fun login(_secret: String?, _email: String?): String? {
        if (_email != null) {
            email = _email
        }
        if (_secret != null) {
            secret = _secret
        }
        if (email == null || secret == null) throw Exception("An email and a secret are needed.")

        val time = System.currentTimeMillis()
        val otp = HTOP.generate(base32Decode(secret), time)

        val payload = loginDataclass.Payload(
            method = "getUserData2017",
            params = listOf(
                loginDataclass.Params(
                    auth = loginDataclass.Auth(
                        clientTime = time,
                        user = email,
                        otp = otp
                    )
                )
            )
        )

        val call = service.login(payload)
        if (call.isSuccessful) {
            var jsession: String? = null
            for ((name, value) in call.headers()) {
                if (name == "set-cookie") {
                    for (x in value.split(";")) {
                        if (x.trim().startsWith("JSESSIONID")) {
                            jsession = x
                        }
                    }
                }
                if (jsession != null) break
            }
            if (jsession == null) {
                throw kotlin.Exception("Error[2]: JSession ID not found.")
            }

            val accessCall = service.reqAccess(jsession)
            if (accessCall.isSuccessful) {
                val bodyData = accessCall.body()
                val data = bodyData?.string()
                if (this != null){
                    authToken = data
                }
                return data
            } else {
                throw kotlin.Exception("Error: ${call.code()} ${call.errorBody()?.string()}")
            }
        } else {
            throw kotlin.Exception("Error: ${call.code()} ${call.errorBody()?.string()}")
        }

        return null
    }

    suspend fun login(): String? = this.login(secret, email)


    suspend fun fetchID(authorization: String): Int? {
        val req = service.fetchID(authorization)
        if (req.isSuccessful) {
            val body = req.body()
            if (body != null) {
                val klaxon = Klaxon()
                val obj = klaxon.parseJsonObject(StringReader(body.string()))
                val preSelected = obj.obj("preSelected")
                val id = preSelected?.int("id")
                return id
            }
        } else {
            if (req.code() == 401) {
                if (loginMaxRequests == 0){
                    loginMaxRequests = 5
                    throw kotlin.Exception("Error: ${req.code()} ${req.errorBody()?.string()}")
                }

                loginMaxRequests--;
                this.login()
                return this.fetchID()
            } else{
                throw kotlin.Exception("Error: ${req.code()} ${req.errorBody()?.string()}")
            }
        }
        return null
    }
    suspend fun fetchID(): Int? {
        return authToken?.let { this.fetchID(it) }
    }

    companion object {
        suspend fun login(secret: String, email: String): String? {
            return this.login(secret, email)
        }

        suspend fun fetchID(authorization: String): Int? {
            return this.fetchID(authorization)
        }
    }
}

class Cache() {
    @Serializable
    data class Credentials(val secret: String, val email: String, val token: String)


    companion object {
        private fun getHomeFP(): File {
            val home = System.getProperty("user.home")
            val fp = File("$home/.kuntis_cache")
            return fp
        }

        fun write(secret: String, email: String, token: String) =
            getHomeFP().writeText(Json.encodeToString(Cache.Credentials("", "", "")))

        fun read(): Credentials? {
            try {
                val data = getHomeFP().readText()
                return Klaxon().parse<Credentials>(data)
            } catch (e: Exception) {
            }
            return null
        }
    }
}

suspend fun main() {
    val cachedValue = Cache.read()

    if (cachedValue == null) {
        print("Enter Secret: ")
        val secret = readln()
        print("Enter Email: ")
        val email = readln()
        println("Contacting Eduvos Servers, this may take some time...")
        val authToken = KUntis.login(secret, email)
        if (authToken != null) {
            Cache.write(secret, email, authToken)
        } else {
            throw Exception("Fatal Exception. Failed to get authorization")
        }
    }

    val authToken =
        "eyJraWQiOiI3MzIxNjk2MzYiLCJhbGciOiJSUzI1NiJ9.eyJ0ZW5hbnRfaWQiOiI5MTM4OTAwIiwic3ViIjoiRURVVjQ3NzcyMTlAdm9zc2llLm5ldCIsInJvbGVzIjoiU1RVREVOVCIsImlzcyI6IndlYnVudGlzIiwibG9jYWxlIjoiZW4iLCJzYyI6IiIsInVzZXJfdHlwZSI6IlVTRVIiLCJyb3V0ZSI6Imtvcy5pbnRlcm5hbC53ZWJ1bnRpcy5jb20iLCJ1c2VyX2lkIjozNzg2MzEsImhvc3QiOiJlZHV2b3MtY2FtcHVzLndlYnVudGlzLmNvbSIsInNuIjoiZWR1dm9zLWNhbXB1cyIsInNjb3BlcyI6IiIsImV4cCI6MTc4MTcyOTAyMSwicGVyIjpbXSwiaWF0IjoxNzgxNzI4MTIxLCJ1c2VybmFtZSI6IkVEVVY0Nzc3MjE5QHZvc3NpZS5uZXQiLCJzciI6IiIsInBlcnNvbl9pZCI6MzI5MDgzfQ.OOriSNVtomBXeR1jAltEFatu9MoC7Q48YWghgjdL6UdDJWJrCSEDsfHZo7CviXqwbDC_8pJp-2nVAftg7f7v9mASd3RkKocPMZJBIP0B0dgoYivAkjvHYem_1Pm6Womf6SyHZ_ASHqcdMDdrGLYhd6IfoD3bFuYLLcXZaaWALgZMShpKGLs4vn0LmmygbAWYNQmjKLIcu8cYzSjqwJA4ZfN9SIhSCdIE5hQZty5zt1a62aXzoHLPYSMocRw6mCX6yLqiR3TtFKQC94tNFB_ZoRzUdfKQj-4WVYTvgewc63eVUW-axkNQjCjfEhDLEQwoQHezAu0sqLUogLajTbkeLQ"
    if (authToken != null) {
        try {
            KUntis.fetchID("Bearer $authToken")
        } catch (e: Exception) {

        }
    }
    exitProcess(0)
}