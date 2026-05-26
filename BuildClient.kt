package com.example.codeeditor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// تمثيل لحالات عملية البناء السحابي
sealed class BuildResult {
    data class Success(val downloadUrl: String) : BuildResult()
    data class Error(val message: String) : BuildResult()
}

object BuildNetworkClient {
    // إعداد عميل HTTP مع زيادة مهلة الانتظار لأن التجميع السحابي قد يستغرق بعض الوقت
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES) // مهلة قراءة طويلة لانتهاء الـ Build
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun requestCloudBuild(projectName: String, sourceCode: String): BuildResult = withContext(Dispatchers.IO) {
        val serverUrl = "https://your-cloud-backend.com/api/v1/build" // استبدله برابط خادمك السحابي

        // إنشاء حمولة البيانات (Payload)
        val jsonPayload = JSONObject().apply {
            put("projectName", projectName)
            put("sourceCode", sourceCode)
            put("targetPlatform", "android") // أو أي منصة مستهدفة
        }.toString()

        val request = Request.Builder()
            .url(serverUrl)
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext BuildResult.Error("خطأ من الخادم: رمز الاستجابة ${response.code}")
                }

                val responseData = response.body?.string() ?: return@withContext BuildResult.Error("استجابة فارغة من الخادم")
                val jsonResponse = JSONObject(responseData)
                
                val status = jsonResponse.optString("status")
                if (status == "success") {
                    val downloadUrl = jsonResponse.optString("downloadUrl")
                    BuildResult.Success(downloadUrl)
                } else {
                    val errorMessage = jsonResponse.optString("message", "حدث خطأ غير معروف أثناء البناء السحابي")
                    BuildResult.Error(errorMessage)
                }
            }
        } catch (e: Exception) {
            BuildResult.Error("فشل الاتصال بالخادم: ${e.localizedMessage}")
        }
    }
}
