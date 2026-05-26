package com.example.codeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun TerminalSimulation() {
    val coroutineScope = rememberCoroutineScope()
    var commandInput by remember { mutableStateOf("") }
    val terminalOutput = remember { mutableStateListOf<String>("مرحباً بك في طرفية أندرويد الافتراضية...") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        // عرض مخرجات الطرفية
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

        // حقل إدخال الأوامر
        OutlinedTextField(
            value = commandInput,
            onValueChange = { commandInput = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            placeholder = { Text("أدخل الأمر هنا... (مثال: ls, pwd)", color = Color.Gray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (commandInput.isNotBlank()) {
                    val cmd = commandInput
                    terminalOutput.add("$ $cmd")
                    commandInput = ""
                    
                    coroutineScope.launch {
                        val result = runShellCommand(cmd)
                        terminalOutput.addAll(result.split("\n"))
                    }
                }
            })
        )
    }
}

// دالة تنفيذ الأمر في الخلفية وقراءة المخرجات والأخطاء
suspend fun runShellCommand(command: String): String = withContext(Dispatchers.IO) {
    try {
        // دمج مخرجات الأخطاء ومخرجات التشغيل القياسية معاً (Standard & Error Outputs)
        val process = ProcessBuilder("sh")
            .redirectErrorStream(true)
            .start()

        val outputStream = process.outputStream.bufferedWriter()
        val inputStream = BufferedReader(InputStreamReader(process.inputStream))

        // إرسال الأمر متبوعاً بسطر جديد وتمريره للعملية
        outputStream.write(command + "\n")
        outputStream.flush()
        outputStream.close() // إغلاق الدفق لإنهاء قراءة المدخلات في الطرفية

        val result = StringBuilder()
        var line: String?
        while (inputStream.readLine().also { line = it } != null) {
            result.append(line).append("\n")
        }

        process.waitFor()
        result.toString().ifBlank { "أمر تم تنفيذه بلا مخرجات مقروءة." }
    } catch (e: Exception) {
        "خطأ أثناء التنفيذ: ${e.localizedMessage}"
    }
}
