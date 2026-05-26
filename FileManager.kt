package com.example.codeeditor

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.io.OutputStream

// تمثيل بيانات الملف أو المجلد في الشجرة
data class FileItem(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val extension: String = "",
    var isExpanded: Boolean = false,
    val children: List<FileItem> = emptyList()
)

@Composable
fun FileManagerScreen(
    onFileSelected: (Uri, String) -> Unit // استدعاء عند فتح ملف لقراءة محتواه
) {
    val context = LocalContext.current
    var rootDirectoryUri by remember { mutableStateOf<Uri?>(null) }
    var fileTree by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    
    // لفتح نافذة حوار لإنشاء ملف/مجلد جديد
    var showCreateDialog by remember { mutableStateOf(false) }
    var isCreatingDirectory by remember { mutableStateOf(false) }
    var selectedParentUri by remember { mutableStateOf<Uri?>(null) }

    // لاقط الصلاحيات لاختيار المجلد الرئيسي
    val directoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            rootDirectoryUri = uri
            // حفظ الصلاحيات الدائمة للمجلد المختار
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            fileTree = buildFileTree(context, uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (rootDirectoryUri == null) {
            // واجهة تطلب من المستخدم اختيار مجلد للعمل عليه
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { directoryLauncher.launch(null) }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اختر مجلد العمل (Workspace)")
                }
            }
        } else {
            // شريط تحكم سريع لإدارة المجلد الرئيسي
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المستكشف", style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = {
                        isCreatingDirectory = false
                        selectedParentUri = rootDirectoryUri
                        showCreateDialog = true
                    }) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "ملف جديد")
                    }
                    IconButton(onClick = {
                        isCreatingDirectory = true
                        selectedParentUri = rootDirectoryUri
                        showCreateDialog = true
                    }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "مجلد جديد")
                    }
                    IconButton(onClick = {
                        rootDirectoryUri?.let { fileTree = buildFileTree(context, it) }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث")
                    }
                }
            }

            Divider()

            // عرض شجرة الملفات
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(fileTree) { fileItem ->
                    FileTreeNode(
                        item = fileItem,
                        depth = 0,
                        onFileClick = { clickedItem ->
                            if (clickedItem.isDirectory) {
                                // تصفح مجلد فرعي أو تحديث حالته (تبسيطاً، يتم التحديث بطلب البناء)
                            } else {
                                val content = readFileContent(context, clickedItem.uri)
                                onFileSelected(clickedItem.uri, content)
                            }
                        },
                        onDeleteClick = { clickedItem ->
                            deleteFile(context, clickedItem.uri)
                            rootDirectoryUri?.let { fileTree = buildFileTree(context, it) } // إعادة تحميل الشجرة
                        },
                        onCreateInDirectory = { parentUri, isDir ->
                            selectedParentUri = parentUri
                            isCreatingDirectory = isDir
                            showCreateDialog = true
                        }
                    )
                }
            }
        }
    }

    // نافذة إنشاء عنصر جديد (ملف أو مجلد)
    if (showCreateDialog && selectedParentUri != null) {
        var newItemName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (isCreatingDirectory) "مجلد جديد" else "ملف جديد") },
            text = {
                TextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    placeholder = { Text("أدخل الاسم هنا") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newItemName.isNotBlank()) {
                        createFileOrDirectory(
                            context = context,
                            parentUri = selectedParentUri!!,
                            name = newItemName,
                            isDirectory = isCreatingDirectory
                        )
                        rootDirectoryUri?.let { fileTree = buildFileTree(context, it) }
                    }
                    showCreateDialog = false
                    newItemName = ""
                }) {
                    Text("إنشاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// عرض عناصر شجرة الملفات بشكل هرمي متداخل
@Composable
fun FileTreeNode(
    item: FileItem,
    depth: Int,
    onFileClick: (FileItem) -> Unit,
    onDeleteClick: (FileItem) -> Unit,
    onCreateInDirectory: (Uri, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(item.isExpanded) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (item.isDirectory) {
                        expanded = !expanded
                        item.isExpanded = expanded
                    }
                    onFileClick(item)
                }
                .padding(start = (depth * 16 + 8).dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // تحديد الأيقونة واللون بناءً على اللاحقة أو النوع
                val (icon, iconColor) = getFileIconAndColor(item)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = item.name, fontSize = 14.sp)
            }

            // أزرار التحكم الجانبية لكل ملف/مجلد
            Row {
                if (item.isDirectory) {
                    IconButton(
                        modifier = Modifier.size(24.dp),
                        onClick = { onCreateInDirectory(item.uri, false) }
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "أضف ملفاً هنا", modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = { onDeleteClick(item) }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }

        // عرض العناصر الفرعية بشكل عودي (Recursive) إذا كان المجلد مفتوحاً
        if (item.isDirectory && expanded) {
            item.children.forEach { child ->
                FileTreeNode(
                    item = child,
                    depth = depth + 1,
                    onFileClick = onFileClick,
                    onDeleteClick = onDeleteClick,
                    onCreateInDirectory = onCreateInDirectory
                )
            }
        }
    }
}

// دالة لتحديد الأيقونة واللون بناءً على صيغة الملف
private fun getFileIconAndColor(item: FileItem): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    return if (item.isDirectory) {
        Pair(Icons.Default.Folder, Color(0xFFFFCA28)) // لون أصفر للمجلدات
    } else {
        when (item.extension.lowercase()) {
            "py" -> Pair(Icons.Default.Terminal, Color(0xFF3776AB))    // بايثون (أزرق مائل للرمادي)
            "kt", "kts" -> Pair(Icons.Default.Code, Color(0xFF7F52FF)) // كوتلن (بنفسجي)
            "java" -> Pair(Icons.Default.Code, Color(0xFFF89820))      // جافا (برتقالي)
            "html", "htm" -> Pair(Icons.Default.Html, Color(0xFFE34F26)) // ويب (أحمر برتقالي)
            "json" -> Pair(Icons.Default.Settings, Color(0xFF8BC34A))   // ملفات إعدادات (أخضر)
            "txt" -> Pair(Icons.Default.Description, Color.LightGray)  // نص عادي
            else -> Pair(Icons.Default.InsertDriveFile, Color.Gray)     // ملف عام
        }
    }
}

// بناء شجرة الملفات من Uri المجلد المحدد
private fun buildFileTree(context: Context, directoryUri: Uri): List<FileItem> {
    val directory = DocumentFile.fromTreeUri(context, directoryUri) ?: return emptyList()
    return listFilesRecursive(directory)
}

private fun listFilesRecursive(directory: DocumentFile): List<FileItem> {
    val fileList = mutableListOf<FileItem>()
    val files = directory.listFiles()
    
    // فرز المجلدات أولاً ثم الملفات لتسهيل الرؤية
    val sortedFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name }))

    for (file in sortedFiles) {
        val name = file.name ?: continue
        val isDirectory = file.isDirectory
        val extension = if (!isDirectory) name.substringAfterLast('.', "") else ""
        
        fileList.add(
            FileItem(
                name = name,
                uri = file.uri,
                isDirectory = isDirectory,
                extension = extension,
                children = if (isDirectory) listFilesRecursive(file) else emptyList()
            )
        )
    }
    return fileList
}

// قراءة محتويات ملف نصي
private fun readFileContent(context: Context, uri: Uri): String {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.bufferedReader()?.use { it.readText() } ?: ""
    } catch (e: Exception) {
        "خطأ أثناء قراءة الملف."
    }
}

// إنشاء ملف أو مجلد جديد
private fun createFileOrDirectory(context: Context, parentUri: Uri, name: String, isDirectory: Boolean) {
    try {
        val parentDoc = DocumentFile.fromTreeUri(context, parentUri) ?: return
        if (isDirectory) {
            parentDoc.createDirectory(name)
        } else {
            // نستخدم text/plain كنوع افتراضي للملفات النصية للمحرر
            parentDoc.createFile("text/plain", name)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// حذف ملف أو مجلد
private fun deleteFile(context: Context, uri: Uri) {
    try {
        val fileDoc = DocumentFile.fromSingleUri(context, uri)
        if (fileDoc != null && fileDoc.exists()) {
            fileDoc.delete()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
