package com.example.codeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

// الكلمات المفتاحية الشائعة للغات البرمجة (Kotlin, Java, Python)
private val KEYWORDS_PATTERN = Pattern.compile(
    "\\b(fun|val|var|class|interface|import|package|return|if|else|while|for|in|when|def|print|println|import|as|from|try|except|true|false|null)\\b"
)
private val NUMBERS_PATTERN = Pattern.compile("\\b\\d+\\b")
private val STRINGS_PATTERN = Pattern.compile("\"(.*?)\"|'(.*?)'")
private val COMMENTS_PATTERN = Pattern.compile("//.*|#.*")

@Composable
fun AdvancedCodeEditor(
    modifier: Modifier = Modifier,
    initialCode: String = ""
) {
    var codeText by remember { mutableStateOf(initialCode) }
    var lineCount by remember { mutableStateOf(1) }
    
    // ربط التمرير الأفقي والعمودي لمزامنة حقل النص مع الأسطر
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // محاكاة سريعة لتحديث عدد الأسطر
    LaunchedEffect(codeText) {
        val lines = codeText.split("\n").size
        lineCount = if (lines > 0) lines else 1
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // خلفية داكنة للمحرر
    ) {
        // 1. عمود أرقام الأسطر (Line Numbers)
        Column(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight()
                .verticalScroll(verticalScrollState)
                .background(Color(0xFF151515)) // خلفية أغمق قليلاً للأسطر
                .padding(vertical = 16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.End
        ) {
            for (i in 1..lineCount) {
                Text(
                    text = "$i",
                    style = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp, bottom = 2.dp) // تطابق مسافات الأسطر في حقل الإدخال
                )
            }
        }

        // خط فاصل عمودي
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.Gray.copy(alpha = 0.2f))
        )

        // 2. حقل كتابة الأكواد الرئيسي
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            BasicTextField(
                value = codeText,
                onValueChange = { codeText = it },
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color(0xFFD4D4D4) // اللون الافتراضي للنص البرمجي
                ),
                // تمرير خوارزمية التلوين البصري المخصصة
                visualTransformation = CodeSyntaxHighlightTransformation(),
                modifier = Modifier.fillMaxSize(),
                onTextLayout = { _: TextLayoutResult ->
                    // يمكن استخدام مخرجات التخطيط هنا في حال تفعيل التفاف الأسطر مستقبلاً
                }
            )
        }
    }
}

// كلاس مخصص لتطبيق تلوين الكود بشكل ديناميكي وسريع
class CodeSyntaxHighlightTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            highlightCode(text.text),
            OffsetMapping.Identity // يحافظ على مؤشر الكتابة في مكانه دون إزاحة
        )
    }
}

// الخوارزمية المسؤولة عن تلوين النص بناءً على قواعد التعبيرات المنتظمة (Regex)
private fun highlightCode(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder(text)

    // تلوين الكلمات المفتاحية (البنفسجي/الأزرق الفاتح)
    val keywordsMatcher = KEYWORDS_PATTERN.matcher(text)
    while (keywordsMatcher.find()) {
        builder.addStyle(
            style = SpanStyle(color = Color(0xFF569CD6)), // لون أزرق برمجيات
            start = keywordsMatcher.start(),
            end = keywordsMatcher.end()
        )
    }

    // تلوين الأرقام (البرتقالي الخفيف)
    val numbersMatcher = NUMBERS_PATTERN.matcher(text)
    while (numbersMatcher.find()) {
        builder.addStyle(
            style = SpanStyle(color = Color(0xFFB5CEA8)), // لون أخضر مصفر للأرقام
            start = numbersMatcher.start(),
            end = numbersMatcher.end()
        )
    }

    // تلوين النصوص المحصورة بين اقتباسات (الأخضر البرمجي)
    val stringsMatcher = STRINGS_PATTERN.matcher(text)
    while (stringsMatcher.find()) {
        builder.addStyle(
            style = SpanStyle(color = Color(0xFFCE9178)), // بني فاتح/أحمر برتقالي للنصوص
            start = stringsMatcher.start(),
            end = stringsMatcher.end()
        )
    }

    // تلوين التعليقات (الأخضر الداكن)
    val commentsMatcher = COMMENTS_PATTERN.matcher(text)
    while (commentsMatcher.find()) {
        builder.addStyle(
            style = SpanStyle(color = Color(0xFF6A9955)), // أخضر تعليقات
            start = commentsMatcher.start(),
            end = commentsMatcher.end()
        )
    }

    return builder.toAnnotatedString()
}
