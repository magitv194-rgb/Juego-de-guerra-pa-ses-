package com.example.ww2strategy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val betaCountries = listOf(
    "🇦🇷 Argentina", "🇩🇪 Alemania", "🇬🇧 Reino Unido", "🇫🇷 Francia",
    "🇮🇹 Italia", "🇯🇵 Japón", "🇺🇸 Estados Unidos", "🇷🇺 Unión Soviética",
    "🇵🇱 Polonia", "🇪🇸 España", "🇹🇷 Turquía", "🇸🇪 Suecia",
    "🇨🇳 China", "🇧🇷 Brasil", "🇨🇦 Canadá", "🇦🇺 Australia"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetaMenuScreen(onOpenGame: () -> Unit) {
    var page by remember { mutableStateOf("inicio") }
    var playerCount by remember { mutableIntStateOf(2) }
    var selectedCountry by remember { mutableStateOf(betaCountries.first()) }
    var selectedMap by remember { mutableStateOf("Europa 1942") }
    var searchId by remember { mutableStateOf("") }
    var soundEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GUERRA MUNDIAL · BETA", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (page != "inicio") {
                        TextButton(onClick = { page = "inicio" }) { Text("‹ Volver") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (page) {
                "inicio" -> {
                    Text("Buscar partida por ID", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = searchId,
                        onValueChange = { searchId = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("ID de partida") },
                        placeholder = { Text("Disponible cuando se habilite el modo online") }
                    )
                    Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text("Buscar partida (desactivado en la beta)")
                    }
                    MenuCard("🗺️", "Mapas", "Ver partidas y mapas guardados", onClick = { page = "mapas" })
                    MenuCard("➕", "Crear partida", "Preparar un mapa de 2 a 20 países", onClick = { page = "crear" })
                    MenuCard("👤", "Perfil y ajustes", "Perfil local y preferencias del juego", onClick = { page = "perfil" })
                    Text(
                        "BETA SIN INTERNET · Las partidas online y los avisos al celular están desactivados.",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                }
                "mapas" -> {
                    Text("Mapas y partidas", style = MaterialTheme.typography.headlineSmall)
                    MenuCard("⚔️", "Partida local de prueba", "Abrir el mapa y las funciones actuales del juego", onClick = onOpenGame)
                    Text("Las partidas compartidas y la búsqueda por ID llegarán cuando se habilite Internet.", fontSize = 13.sp)
                }
                "crear" -> {
                    Text("Crear partida", style = MaterialTheme.typography.headlineSmall)
                    Text("Cantidad de jugadores")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 5, 10, 20).forEach { count ->
                            FilterChip(
                                selected = playerCount == count,
                                onClick = { playerCount = count },
                                label = { Text("$count") }
                            )
                        }
                    }
                    Text("Mapa histórico")
                    listOf("Europa 1942", "Mundo 1942").forEach { map ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedMap == map, onClick = { selectedMap = map })
                            Text(map)
                        }
                    }
                    Text("Elige tu país")
                    LazyColumn(Modifier.weight(1f)) {
                        items(betaCountries) { country ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedCountry == country,
                                    onClick = { selectedCountry = country }
                                )
                                Text(country)
                            }
                        }
                    }
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("Configuración elegida", fontWeight = FontWeight.Bold)
                            Text("$selectedMap · $playerCount jugadores")
                            Text("País: $selectedCountry")
                            Text("Esta selección es una vista previa: el generador de mapas y países reales todavía debe conectarse al motor.")
                        }
                    }
                }
                "perfil" -> {
                    Text("Perfil y ajustes", style = MaterialTheme.typography.headlineSmall)
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("Comandante", fontWeight = FontWeight.Bold)
                            Text("Perfil local de la beta")
                            Text("Las cuentas y perfiles online aún no están habilitados.")
                        }
                    }
                    SettingSwitch("Sonido", soundEnabled) { soundEnabled = it }
                    SettingSwitch("Notificaciones locales", notificationsEnabled) { notificationsEnabled = it }
                    Text("Las notificaciones de ataques y mensajes requieren el sistema de partidas online, que permanece apagado.", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MenuCard(icon: String, title: String, description: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(icon, fontSize = 28.sp)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }
            Text("›", fontSize = 24.sp)
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
