package com.maksimowiczm.foodyou.photo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.app.ui.food.diary.quickadd.QuickAddFormState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
actual fun PhotoAnalyzeButton(state: QuickAddFormState) {
    val analyzer: PhotoAnalyzer = koinInject()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val pickGallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            loading = true; errorMsg = null
            try {
                val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw Exception("No se pudo leer la imagen")
                val name = "food.jpg"
                val result = analyzer.analyze(bytes, name)
                result.onSuccess { r ->
                    if (r.error != null) { errorMsg = r.error; return@onSuccess }
                    if (r.alimento == "no es comida") { errorMsg = "No parece comida, intenta otra foto"; return@onSuccess }
                    // Rellenar formulario
                    state.name.textFieldState.edit { replace(0, length, r.alimento) }
                    state.proteins.textFieldState.edit { replace(0, length, r.proteinas.toString()) }
                    state.fats.textFieldState.edit { replace(0, length, r.grasas.toString()) }
                    state.carbohydrates.textFieldState.edit { replace(0, length, r.carbos.toString()) }
                    // kcal se autocalcula si autoCalculateEnergy=true; si no, forzar
                    if (!state.autoCalculateEnergy) {
                        state.energy.textFieldState.edit { replace(0, length, r.kcal.toString()) }
                    }
                }.onFailure { e -> errorMsg = e.message ?: "Error" }
            } catch (e: Exception) { errorMsg = e.message } 
            finally { loading = false }
        }
    }

    // Camara simple: usa TakePicture con file provider? Por simplicidad solo galeria + camara via GetContent
    // Para camara real usamos TakePicture con temp file; si no, fallback a galeria
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap == null) return@rememberLauncherForActivityResult
        scope.launch {
            loading = true; errorMsg = null
            try {
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
                val bytes = stream.toByteArray()
                val result = analyzer.analyze(bytes, "food.jpg")
                result.onSuccess { r ->
                    if (r.error != null) { errorMsg = r.error; return@onSuccess }
                    if (r.alimento == "no es comida") { errorMsg = "No parece comida, intenta otra foto"; return@onSuccess }
                    state.name.textFieldState.edit { replace(0, length, r.alimento) }
                    state.proteins.textFieldState.edit { replace(0, length, r.proteinas.toString()) }
                    state.fats.textFieldState.edit { replace(0, length, r.grasas.toString()) }
                    state.carbohydrates.textFieldState.edit { replace(0, length, r.carbos.toString()) }
                    if (!state.autoCalculateEnergy) {
                        state.energy.textFieldState.edit { replace(0, length, r.kcal.toString()) }
                    }
                }.onFailure { e -> errorMsg = e.message }
            } catch (e: Exception) { errorMsg = e.message }
            finally { loading = false }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { takePictureLauncher.launch(null) },
                enabled = !loading,
                modifier = Modifier.weight(1f)
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (loading) "Analizando..." else "Foto camara")
            }
            OutlinedButton(
                onClick = { pickGallery.launch("image/*") },
                enabled = !loading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Galeria")
            }
        }
        Text(
            "Sube foto de tu comida y Yui (tailscale) calcula kcal/proteinas/grasas/carbos",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
        errorMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
