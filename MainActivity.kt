package com.example.codeeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { // مظهر داكن يناسب محرري الأكواد
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IDEHomeScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IDEHomeScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    
    // حالة النص داخل محرر الأكواد
    var codeText by remember { mutableStateOf("// اكتب كودك هنا...\nfun main() {\n    println(\"Hello World\")\n}") }
    
    // قائمة ملفات وهمية لإدارة الملفات في القائمة الجانبية
    val filesList = listOf("MainActivity.kt", "styles.xml", "build.gradle", "AndroidManifest.xml")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "إدارة الملفات",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Divider()
                LazyColumn {
                    items(filesList) { fileName ->
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Code, contentDescription = null) },
                            label = { Text(text = fileName) },
                            selected = false,
                            onClick = {
                                // هنا يمكن معالجة فتح الملف المحدد
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        // BottomSheetScaffold يدمج اللوحة السفلية (Terminal) مع مساحة العمل الرئيسية
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 80.dp, // الارتفاع الظاهر للطرفية وهي مغلقة جزئياً
            sheetContainerColor = Color(0xFF1E1E1E), // لون داكن خاص بالطرفية
            sheetContent = {
                TerminalPanel()
            },
            topBar = {
                TopAppBar(
                    title = { Text("محرر الأكواد", fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "القائمة الجانبية")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* إجراء التشغيل */ }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = Color.Green)
                        }
                        IconButton(onClick = { /* إجراء الحفظ */ }) {
                            Icon(Icons.Default.Save, contentDescription = "حفظ")
                        }
                        IconButton(onClick = { /* إجراء الإعدادات */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            }
        ) { paddingValues ->
            // مساحة كتابة الكود الرئيسية
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF121212)) // خلفية داكنة للمحرر
            ) {
                BasicTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace, // خط أحادي المسافة مناسب للأكواد
                        fontSize = 14.sp,
                        color = Color.LightGray
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    decorationBox = { innerTextField ->
                        if (codeText.isEmpty()) {
                            Text("اكتب كودك هنا...", color = Color.Gray)
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
fun TerminalPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp) // الارتفاع الأقصى للوحة عند سحبها للأعلى
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.KeyboardArrowUp, 
                    contentDescription = null, 
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "الطرفية (Terminal)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
            }
            Text(
                text = "sh",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Green
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color.Gray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))
        
        // محاكاة لمخرجات الطرفية
        Text(
            text = "user@android-ide:~$ ./run_project.sh\nCompilation successful.\nRunning...\nHello World\n\nProcess finished with exit code 0",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFF00FF00), // لون أخضر كلاسيكي للطرفية
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(8.dp)
        )
    }
}
