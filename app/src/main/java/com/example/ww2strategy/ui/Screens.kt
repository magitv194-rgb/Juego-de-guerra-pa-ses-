package com.example.ww2strategy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ww2strategy.model.*
import kotlin.math.roundToInt

@Composable
fun MapScreen(
    state: GameState,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onAttack: (String, String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1.2f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(state.provinces) { p ->
                ProvinceCell(p, state, p.id == selectedId) { onSelect(p.id) }
            }
        }
        Spacer(Modifier.height(8.dp))
        val selected = state.provinces.firstOrNull { it.id == selectedId }
        if (selected != null) {
            ProvinceDetail(selected, state, onAttack)
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Selecciona una provincia", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ProvinceCell(p: Province, state: GameState, selected: Boolean, onClick: () -> Unit) {
    val country = state.countries.firstOrNull { it.id == p.ownerCountryId }
    val color = Color(country?.colorArgb ?: 0xFF666666)
    val irradiated = p.irradiatedUntil > System.currentTimeMillis()
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = .3f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(color.copy(alpha = .25f))
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(p.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text("👥 ${p.units.size}", fontSize = 10.sp)
            Text(if (irradiated) "☢️ ${((p.irradiatedUntil - System.currentTimeMillis())/1000)}s" else "☢️ --", fontSize = 10.sp)
        }
    }
}

@Composable
private fun ProvinceDetail(p: Province, state: GameState, onAttack: (String, String) -> Unit) {
    val country = state.countries.firstOrNull { it.id == p.ownerCountryId }
    val isMine = p.ownerCountryId == state.playerCountryId
    val irradiated = p.irradiatedUntil > System.currentTimeMillis()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${p.name} — ${country?.name ?: "?"}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Moral: ${p.morale.roundToInt()}%   Unidades: ${p.units.size}", fontSize = 13.sp)
            Text("Edificios: ${p.buildings.size}", fontSize = 13.sp)
            if (irradiated) Text("☢️ IRRADIADA — no conquistable", color = Color.Red, fontSize = 12.sp)
            if (isMine) {
                Text("Vecinos enemigos:", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                val enemies = p.neighbors
                    .mapNotNull { nid -> state.provinces.firstOrNull { it.id == nid } }
                    .filter { it.ownerCountryId != state.playerCountryId }
                if (enemies.isEmpty()) {
                    Text("—", fontSize = 12.sp)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        enemies.forEach { enemy ->
                            Button(onClick = { onAttack(p.id, enemy.id) }) {
                                Text("Atacar ${enemy.name}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArmyScreen(state: GameState, selectedId: String?, onProduce: (UnitType) -> Unit) {
    val province = state.provinces.firstOrNull { it.id == selectedId }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Producción militar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(if (province == null)
                "Selecciona una provincia en el mapa primero."
            else
                "Provincia: ${province.name}",
                fontSize = 13.sp)
        }
        items(UnitType.entries.toList()) { type ->
            val unlocked = type.requiresResearch == null ||
                    type.requiresResearch in state.completedResearch
            val hasBarracks = province?.buildings?.any { it.type == BuildingType.BARRACKS } == true
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(type.displayName, fontWeight = FontWeight.SemiBold)
                    Text("HP ${type.baseHp} · ATQ ${type.attack} · DEF ${type.defense}", fontSize = 12.sp)
                    Text("⏱ ${type.productionMinutes} min · 💵 ${type.cost.money} · 🌾 ${type.cost.food} · ⚙️ ${type.cost.metal} · 🛢️ ${type.cost.oil}",
                        fontSize = 11.sp)
                    if (!unlocked) Text("🔒 Requiere investigación", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    if (province != null && !hasBarracks)
                        Text("Falta Cuartel en la provincia", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    Button(
                        onClick = { onProduce(type) },
                        enabled = unlocked && hasBarracks && state.resources.canAfford(type.cost) &&
                                (province != null) && province.ownerCountryId == state.playerCountryId,
                        modifier = Modifier.padding(top = 6.dp)
                    ) { Text("Producir", fontSize = 12.sp) }
                }
            }
        }
        item {
            Text("Cola de producción (${state.productionQueue.size})",
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            state.productionQueue.forEach { order ->
                val left = ((order.endsAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                Text("· ${order.type.displayName} — ${left}s restantes", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ResearchScreen(state: GameState, onResearch: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Investigación", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(ResearchCatalog) { def ->
            val done = def.id in state.completedResearch
            val inProgress = state.researchQueue.any { it.researchId == def.id }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(def.displayName, fontWeight = FontWeight.SemiBold)
                    Text("⏱ ${def.minutes} min · 💵 ${def.cost.money} · ⚙️ ${def.cost.metal} · 🛢️ ${def.cost.oil}",
                        fontSize = 12.sp)
                    when {
                        done -> Text("✅ Completado", color = Color(0xFF2E7D32), fontSize = 12.sp)
                        inProgress -> Text("⏳ En curso", fontSize = 12.sp)
                        else -> Button(
                            onClick = { onResearch(def.id) },
                            enabled = state.resources.canAfford(def.cost),
                            modifier = Modifier.padding(top = 6.dp)
                        ) { Text("Investigar", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun NationScreen(state: GameState, onNuke: (String) -> Unit, onReset: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Nación", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Ingreso bruto cada 3 min: " +
                    "${state.provinces.filter { it.ownerCountryId == state.playerCountryId }
                        .sumOf { it.baseYield.money }} 💵",
                fontSize = 13.sp)
        }
        item {
            Text("Armamento nuclear", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            val ready = "tech_nuclear" in state.completedResearch
            val nukeCost = Resources(800, 0, 500, 300)
            Text(if (ready) "Programa listo" else "Requiere investigación 'Programa nuclear'",
                fontSize = 12.sp)
            Text("Coste: 💵800 ⚙️500 🛢️300", fontSize = 12.sp)
            if (ready) {
                Text("Elige provincia objetivo:", fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp))
                state.provinces
                    .filter { it.ownerCountryId != state.playerCountryId &&
                            it.irradiatedUntil <= System.currentTimeMillis() }
                    .forEach { p ->
                        Button(
                            onClick = { onNuke(p.id) },
                            enabled = state.resources.canAfford(nukeCost),
                            modifier = Modifier.padding(end = 4.dp, top = 4.dp)
                        ) { Text("☢️ ${p.name}", fontSize = 11.sp) }
                    }
            }
        }
        item {
            Text("Reiniciar partida", fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp))
            Button(onClick = onReset) { Text("Borrar y empezar de nuevo") }
        }
    }
}
