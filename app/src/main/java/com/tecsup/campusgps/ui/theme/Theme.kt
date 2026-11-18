package com.tecsup.campusgps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val EsquemaCampusOscuro = darkColorScheme(
    primary = AzulNoche,
    onPrimary = Color.White,
    primaryContainer = AzulNocheContenedor,
    onPrimaryContainer = AzulNocheTexto,
    background = FondoNoche,
    onBackground = TextoNoche,
    surface = FondoNoche,
    onSurface = TextoNoche,
    surfaceVariant = TarjetaNoche,
    onSurfaceVariant = GrisNoche,
    outline = AzulNoche,
    error = RojoNoche,
    errorContainer = RojoNocheFondo,
    onErrorContainer = RojoNocheTexto
)

@Composable
fun CampusGPSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaCampusOscuro else EsquemaCampus,
        typography = Typography,
        content = content
    )
}
