package com.example.codeeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState: Bundle?)
        setContent {
            TerminalSimulation()
        }
    }
}

sealed class BuildResult {
    data class Success(val downloadUrl: String) : BuildResult()
    data class Error(val message: String) : BuildResult()
}

object BuildNetworkClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun requestCloudBuild(projectName: String, sourceCode: String): BuildResult = withContext(Dispatchers.IO) {
        try {
            val json = "{\"projectName\":\"$projectName\",\"sourceCode\":\"$sourceCode\"}"
            val body = json.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("https://your-backend.com/api/v1/build")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    BuildResult.Success(response.body?.string() ?: "")
                } else {
                    BuildResult.Error("فشلت العملية: ${response.code}")
                }
            }
        } catch (e: Exception) {
            BuildResult.Error(e.message ?: "خطأ غير معروف")
        }
    }
}

@Composable
fun TerminalSimulation() {
    val terminalOutput = remember { mutableStateListOf("مرحباً بك في طرفية", "...أندرويد الافتراضية") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = false
        ) {
            items(terminalOutput) { line ->
                Text(
                    text = line,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

