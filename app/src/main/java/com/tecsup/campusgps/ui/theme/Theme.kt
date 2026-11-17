package com.tecsup.campusgps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaCampus = lightColorScheme(
    primary = AzulCampus,
    onPrimary = Color.White,
    primaryContainer = AzulCampusClaro,
    onPrimaryContainer = AzulCampusOscuro,
    background = FondoCampus,
    onBackground = TextoCampus,
    surface = FondoCampus,
    onSurface = TextoCampus,
    surfaceVariant = TarjetaCampus,
    onSurfaceVariant = GrisCampus,
    outline = AzulCampus,
    error = RojoError,
    errorContainer = RojoErrorFondo,
    onErrorContainer = RojoErrorTexto
)

@Composable
fun CampusGPSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaCampus,
        typography = Typography,
        content = content
    )
}
