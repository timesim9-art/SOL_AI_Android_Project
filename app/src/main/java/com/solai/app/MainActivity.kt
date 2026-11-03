package com.solai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

private val Bg = Color(0xFF090B10)
private val Panel = Color(0xFF151A24)
private val Accent = Color(0xFF8AB4FF)
private val Muted = Color(0xFF9AA7BC)

data class ChatMessage(val text: String, val user: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SolApp() }
    }
}

@Composable
fun SolApp() {
    var screen by remember { mutableStateOf("chat") }
    var apiKey by remember { mutableStateOf("") }
    var falKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("gpt-4o-mini") }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Готов к работе") }
    val messages = remember { mutableStateListOf(ChatMessage("Привет! Я SOL AI. Задай вопрос или попроси меня написать код.", false)) }
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = darkColorScheme(
        background = Bg, surface = Panel, primary = Accent,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(containerColor = Bg, topBar = {
            Row(Modifier.fillMaxWidth().background(Bg).padding(horizontal=18.dp, vertical=14.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Color(0xFF202B42), RoundedCornerShape(14.dp)), contentAlignment=Alignment.Center) {
                    Text("S", color=Accent, fontSize=25.sp, fontWeight=FontWeight.Bold)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("SOL AI", fontSize=21.sp, fontWeight=FontWeight.Bold)
                    Text(status, fontSize=11.sp, color=Muted)
                }
                IconButton(onClick={ screen = if(screen=="settings") "chat" else "settings" }) {
                    Icon(Icons.Default.Settings, contentDescription="Настройки", tint=Accent)
                }
            }
        }, bottomBar = {
            if (screen=="chat") {
                Row(Modifier.fillMaxWidth().background(Bg).padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
                    OutlinedTextField(value=input, onValueChange={input=it}, modifier=Modifier.weight(1f),
                        placeholder={Text("Напиши сообщение…", color=Muted)}, shape=RoundedCornerShape(18.dp),
                        colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Accent, unfocusedBorderColor=Color(0xFF303849)))
                    Spacer(Modifier.width(8.dp))
                    IconButton(enabled=!busy && input.isNotBlank(), onClick={
                        val prompt=input.trim(); input=""
                        messages.add(ChatMessage(prompt,true)); busy=true; status="Думаю…"
                        scope.launch {
                            val answer = AiClient.chat(apiKey, model, messages.dropLast(1).map { it })
                            messages.add(ChatMessage(answer,false)); busy=false; status="Готов к работе"
                        }
                    }, modifier=Modifier.size(52.dp).background(Accent, RoundedCornerShape(16.dp))) {
                        Icon(Icons.Default.Send, contentDescription="Отправить", tint=Bg)
                    }
                }
            }
        }) { pad ->
            if (screen=="settings") {
                LazyColumn(Modifier.fillMaxSize().padding(pad).padding(18.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    item { Text("Настройки SOL AI", fontSize=24.sp, fontWeight=FontWeight.Bold) }
                    item { Text("AI-чат · OpenAI-совместимый API", color=Muted) }
                    item { OutlinedTextField(apiKey,{apiKey=it}, modifier=Modifier.fillMaxWidth(), label={Text("API-ключ чата")}, visualTransformation=PasswordVisualTransformation()) }
                    item { OutlinedTextField(model,{model=it}, modifier=Modifier.fillMaxWidth(), label={Text("Модель")}) }
                    item { Divider(color=Color(0xFF303849)) }
                    item { Text("Изображения и видео · fal.ai", color=Muted) }
                    item { OutlinedTextField(falKey,{falKey=it}, modifier=Modifier.fillMaxWidth(), label={Text("Ключ fal.ai")}, visualTransformation=PasswordVisualTransformation()) }
                    item { Text("Ключи хранятся только в памяти приложения и исчезнут после его закрытия. Для публикации приложения нужен защищённый сервер-посредник.", color=Muted, fontSize=12.sp) }
                    item { Button(onClick={screen="chat"}, modifier=Modifier.fillMaxWidth()) { Text("Сохранить и вернуться") } }
                    item { Text("Генерация изображений/видео: добавь ключ fal.ai. Поддержка конкретных моделей и результатов генерации требует настройки endpoint под выбранную модель.", color=Muted, fontSize=12.sp) }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(pad)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=14.dp, vertical=8.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick={status="Чат"}, label={Text("Чат")}, leadingIcon={Icon(Icons.Default.Chat, null)})
                        AssistChip(onClick={screen="settings"}, label={Text("Модели / API")}, leadingIcon={Icon(Icons.Default.Tune, null)})
                        AssistChip(onClick={status="Генерация: подключи fal.ai"}, label={Text("Создать")}, leadingIcon={Icon(Icons.Default.AutoAwesome, null)})
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal=14.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        items(messages) { msg ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement=if(msg.user) Arrangement.End else Arrangement.Start) {
                                Surface(color=if(msg.user) Color(0xFF263B5B) else Panel, shape=RoundedCornerShape(18.dp), modifier=Modifier.widthIn(max=320.dp)) {
                                    Text(msg.text, Modifier.padding(14.dp), color=Color.White, fontSize=15.sp)
                                }
                            }
                        }
                        if(busy) item { Text("SOL AI печатает…", color=Accent, modifier=Modifier.padding(8.dp)) }
                    }
                }
            }
        }
    }
}

object AiClient {
    suspend fun chat(key: String, model: String, history: List<ChatMessage>): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (key.isBlank()) return@withContext "Добавь API-ключ в Настройки → API-ключ чата."

            try {
                val client = okhttp3.OkHttpClient()
                val input = org.json.JSONArray()

                history.forEach { m ->
                    input.put(
                        org.json.JSONObject()
                            .put("role", if (m.user) "user" else "assistant")
                            .put("content", m.text)
                    )
                }

                val body = org.json.JSONObject()
                    .put("model", model)
                    .put("input", input)

                val request = okhttp3.Request.Builder()
                    .url("https://api.openai.com/v1/responses")
                    .addHeader("Authorization", "Bearer $key")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        "Ошибка API ${response.code}: $raw"
                    } else {
                        val json = org.json.JSONObject(raw)
                        val output = json.getJSONArray("output")
                        val result = StringBuilder()

                        for (i in 0 until output.length()) {
                            val item = output.getJSONObject(i)
                            if (item.optString("type") == "message") {
                                val content = item.optJSONArray("content") ?: continue
                                for (j in 0 until content.length()) {
                                    val part = content.getJSONObject(j)
                                    if (part.optString("type") == "output_text") {
                                        result.append(part.optString("text"))
                                    }
                                }
                            }
                        }

                        result.toString().ifBlank { "Модель не вернула текстовый ответ." }
                    }
                }
            } catch (e: Exception) {
                "Не удалось подключиться: ${e.message}"
            }
        }
}
