package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.ThemeMode
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsState()
    val primaryColorName by viewModel.primaryAppColor.collectAsState()
    val textSize by viewModel.textSizeMultiplier.collectAsState()
    val language by viewModel.appLanguage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apariencia", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandSurface,
                    titleContentColor = TextPrimary
                ),
                modifier = Modifier.testTag("appearance_top_bar")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BrandBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode Segment (Claro, Oscuro, Automático)
            item {
                Text(
                    "Tema visual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val themes = listOf(
                            Triple(ThemeMode.LIGHT, "Modo Claro", "Fondo claro con alto contraste"),
                            Triple(ThemeMode.DARK, "Modo Oscuro", "Fondo oscuro ideal para la noche"),
                            Triple(ThemeMode.AUTO, "Automático", "Adaptarse a la configuración del sistema")
                        )

                        themes.forEachIndexed { index, (mode, name, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.themeMode.value = mode 
                                        Toast.makeText(context, "Tema cambiado a $name (Modo de demostración)", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                RadioButton(
                                    selected = currentTheme == mode,
                                    onClick = { 
                                        viewModel.themeMode.value = mode
                                        Toast.makeText(context, "Tema cambiado a $name (Modo de demostración)", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = OnAccentBlue,
                                        unselectedColor = TextMuted
                                    )
                                )
                            }
                            if (index < themes.lastIndex) {
                                HorizontalDivider(color = BrandBorder)
                            }
                        }
                    }
                }
            }

            // Accent Brand Colors (Linear, Stripe, Revolut, Monzo inspired)
            item {
                Text(
                    "Color principal de la aplicación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val colorsList = listOf(
                            Triple("Azul Stripe", Color(0xFF4379FF), "El clásico azul premium y tecnológico"),
                            Triple("Verde Revolut", Color(0xFF00C6FF), "Un cian brillante y moderno"),
                            Triple("Slate Linear", Color(0xFF5E6AD2), "Un violeta metálico con mucha clase"),
                            Triple("Coral Monzo", Color(0xFFFE5F55), "Un coral vibrante y amigable")
                        )

                        colorsList.forEachIndexed { index, (name, color, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.primaryAppColor.value = name }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(color, CircleShape)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (primaryColorName == name) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                            if (index < colorsList.lastIndex) {
                                HorizontalDivider(color = BrandBorder)
                            }
                        }
                    }
                }
            }

            // Text Size Settings
            item {
                Text(
                    "Tamaño de texto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Chico (85%)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Text("Normal (100%)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("Grande (115%)", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Slider(
                            value = when (textSize) {
                                0.85f -> 0f
                                1.0f -> 1f
                                1.15f -> 2f
                                else -> 1f
                            },
                            onValueChange = { value ->
                                val target = when (value.toInt()) {
                                    0 -> 0.85f
                                    1 -> 1.0f
                                    2 -> 1.15f
                                    else -> 1.0f
                                }
                                viewModel.textSizeMultiplier.value = target
                            },
                            steps = 1,
                            valueRange = 0f..2f,
                            colors = SliderDefaults.colors(
                                thumbColor = OnAccentBlue,
                                activeTrackColor = OnAccentBlue,
                                inactiveTrackColor = BrandBorder
                            )
                        )
                    }
                }
            }

            // Language settings
            item {
                Text(
                    "Idioma de la aplicación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface)
                ) {
                    Column {
                        val languages = listOf("Español", "English", "Português")
                        languages.forEachIndexed { index, lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.appLanguage.value = lang }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(lang, fontWeight = FontWeight.Medium, color = TextPrimary)
                                if (language == lang) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = OnAccentBlue)
                                }
                            }
                            if (index < languages.lastIndex) {
                                HorizontalDivider(color = BrandBorder)
                            }
                        }
                    }
                }
            }
        }
    }
}
