package com.example.codeeditor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// حالات الواجهة الرسومية أثناء طلب التصدير
enum class ExportStatus {
    IDLE,       // خامل
    BUILDING,   // جاري التجميع في السحابة
    SUCCESS,    // تم التجميع بنجاح ورابط التحميل جاهز
    FAILED      // فشلت العملية
}

@Composable
fun ExportProjectDialog(
    projectName: String,
    currentCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var currentStatus by remember { mutableStateOf(ExportStatus.IDLE) }
    var downloadUrl by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (currentStatus != ExportStatus.BUILDING) onDismiss() },
        title = { Text(text = "تصدير وبناء المشروع سحابياً", fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentStatus) {
                    ExportStatus.IDLE -> {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("سيتم إرسال مشروعك الحالي إلى الخادم السحابي الآمن لتجميعه وتوليد ملف التطبيق النهائي.")
                    }
                    ExportStatus.BUILDING -> {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("جاري تجميع كود المشروع سحابياً وتوليد حزمة التطبيق... يرجى الانتظار.")
                    }
                    ExportStatus.SUCCESS -> {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Green)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("اكتملت عملية البناء بنجاح! يمكنك الآن تحميل الملف النهائي.")
                    }
                    ExportStatus.FAILED -> {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("فشلت عملية البناء: $errorMessage", color = Color.Red)
                    }
                }
            }
        },
        confirmButton = {
            when (currentStatus) {
                ExportStatus.IDLE -> {
                    Button(onClick = {
                        currentStatus = ExportStatus.BUILDING
                        coroutineScope.launch {
                            val result = BuildNetworkClient.requestCloudBuild(projectName, currentCode)
                            when (result) {
                                is BuildResult.Success -> {
                                    downloadUrl = result.downloadUrl
                                    currentStatus = ExportStatus.SUCCESS
                                }
                                is BuildResult.Error -> {
                                    errorMessage = result.message
                                    currentStatus = ExportStatus.FAILED
                                }
                            }
                        }
                    }) {
                        Text("بدء البناء السحابي")
                    }
                }
                ExportStatus.SUCCESS -> {
                    Button(onClick = {
                        // فتح المتصفح لتحميل التطبيق من الرابط المرتجع
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                        context.startActivity(intent)
                        onDismiss()
                    }) {
                        Text("تحميل الملف")
                    }
                }
                ExportStatus.FAILED -> {
                    Button(onClick = { currentStatus = ExportStatus.IDLE }) {
                        Text("إعادة المحاولة")
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            if (currentStatus != ExportStatus.BUILDING) {
                TextButton(onClick = onDismiss) {
                    Text("إغلاق")
                }
            }
        }
    )
}
