package com.example.ww2strategy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ww2strategy.GameViewModel
import com.example.ww2strategy.model.Resources

@Composable
fun GameApp(vm: GameViewModel) {
    val state by vm.state.collectAsState()
    val selectedId by vm.selectedProvince.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 },
                    icon = { Text("🗺️") }, label = { Text("Mapa") })
                NavigationBarItem(tab == 1, { tab = 1 },
                    icon = { Text("⚔️") }, label = { Text("Ejército") })
                NavigationBarItem(tab == 2, { tab = 2 },
                    icon = { Text("🔬") }, label = { Text("Investigación") })
                NavigationBarItem(tab == 3, { tab = 3 },
                    icon = { Text("🏛️") }, label = { Text("Nación") })
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ResourceBar(state.resources)
            when (tab) {
                0 -> MapScreen(
                    state = state,
                    selectedId = selectedId,
                    onSelect = { vm.selectProvince(it) },
                    onAttack = { from, to -> vm.attack(from, to) }
                )
                1 -> ArmyScreen(state, selectedId, onProduce = { vm.produce(it) })
                2 -> ResearchScreen(state, onResearch = { vm.research(it) })
                3 -> NationScreen(
                    state = state,
                    onNuke = { vm.nuke(it) },
                    onReset = { vm.resetGame() }
                )
            }
        }
    }
}

@Composable
fun ResourceBar(r: Resources) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ResChip("💵", r.money)
            ResChip("🌾", r.food)
            ResChip("⚙️", r.metal)
            ResChip("🛢️", r.oil)
        }
    }
}

@Composable
private fun ResChip(icon: String, value: Long) {
    Text("$icon $value", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}
