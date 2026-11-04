package com.example.ww2strategy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
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
    onAttack: (String, String) -> Unit,
    onConstruct: (BuildingType) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val features = remember {
        val raw = context.assets.open("argentina_provinces.json").bufferedReader().use { it.readText() }
        val root = kotlinx.serialization.json.Json.parseToJsonElement(raw).jsonObject
        root["features"]!!.jsonArray.map { item ->
            val obj = item.jsonObject
            val center = obj["center"]!!.jsonArray
            val points = obj["points"]!!.jsonArray.map { pair ->
                val values = pair.jsonArray
                androidx.compose.ui.geometry.Offset(values[0].jsonPrimitive.float, values[1].jsonPrimitive.float)
            }
            ArgentinaFeature(
                id = obj["id"]!!.jsonPrimitive.content,
                name = obj["name"]!!.jsonPrimitive.content,
                centerLon = center[0].jsonPrimitive.float,
                centerLat = center[1].jsonPrimitive.float,
                points = points
            )
        }
    }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    Column(Modifier.fillMaxSize().padding(6.dp)) {
        Text("ARGENTINA · 23 PROVINCIAS Y CABA", fontSize = 12.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE7EDE8))) {
            Canvas(Modifier.fillMaxSize()
                .pointerInput(features, zoom, pan) {
                    detectTransformGestures { centroid, panChange, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(1f, 5f)
                        pan += panChange
                    }
                }
                .pointerInput(features, zoom, pan) {
                    detectTapGestures { tap ->
                        val projection = ArgentinaProjection(size.width.toFloat(), size.height.toFloat(), zoom, pan)
                        val geo = projection.toGeo(tap)
                        val hit = features.firstOrNull { pointInPolygon(geo.x, geo.y, it.points) }
                        if (hit != null && state.provinces.any { it.id == hit.id }) onSelect(hit.id)
                    }
                }) {
                val projection = ArgentinaProjection(size.width, size.height, zoom, pan)
                features.forEach { feature ->
                    val province = state.provinces.firstOrNull { it.id == feature.id } ?: return@forEach
                    val path = Path()
                    feature.points.forEachIndexed { index, point ->
                        val screen = projection.toScreen(point.x, point.y)
                        if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
                    }
                    path.close()
                    drawPath(path, Color(0xFF55A85E))
                    drawPath(path, if (province.id == selectedId) Color(0xFFFFC107) else Color(0xFF205C32),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = if (province.id == selectedId) 3.5f else 1.2f
                        ))
                    val label = projection.toScreen(feature.centerLon, feature.centerLat)
                    drawContext.canvas.nativeCanvas.drawText(
                        feature.name,
                        label.x,
                        label.y,
                        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.BLACK
                            textSize = if (feature.name.length > 17) 8.5f else 10.5f
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                        }
                    )
                }
            }
            Column(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Text("Pellizca para acercar · Arrastra para mover",
                    modifier = Modifier.background(Color.White.copy(alpha=.85f), RoundedCornerShape(6.dp))
                        .padding(6.dp), fontSize = 9.sp)
                TextButton(onClick = { zoom = 1f; pan = androidx.compose.ui.geometry.Offset.Zero }) {
                    Text("Restablecer", fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        val selected = state.provinces.firstOrNull { it.id == selectedId }
        if (selected != null) {
            Box(Modifier.weight(0.9f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                ProvinceDetail(selected, state, onAttack, onConstruct)
            }
        } else {
            Text("Toca una provincia para ver sus edificios y unidades.",
                modifier = Modifier.padding(8.dp), fontSize = 12.sp)
        }
    }
}

private data class ArgentinaFeature(
    val id: String, val name: String, val centerLon: Float, val centerLat: Float,
    val points: List<androidx.compose.ui.geometry.Offset>
)

private class ArgentinaProjection(
    width: Float, height: Float, private val zoom: Float,
    private val pan: androidx.compose.ui.geometry.Offset
) {
    private val minLon = -73f
    private val maxLon = -53f
    private val minLat = -56f
    private val maxLat = -21f
    private val scale = minOf(width / (maxLon-minLon), height / (maxLat-minLat)) * .94f
    private val left = (width - (maxLon-minLon)*scale)/2f
    private val top = (height - (maxLat-minLat)*scale)/2f
    fun base(lon: Float, lat: Float) = androidx.compose.ui.geometry.Offset(
        left+(lon-minLon)*scale, top+(maxLat-lat)*scale
    )
    fun toScreen(lon: Float, lat: Float): androidx.compose.ui.geometry.Offset {
        val p=base(lon,lat)
        return androidx.compose.ui.geometry.Offset(
            (p.x-widthCenter())*zoom+widthCenter()+pan.x,
            (p.y-heightCenter())*zoom+heightCenter()+pan.y
        )
    }
    fun toGeo(screen: androidx.compose.ui.geometry.Offset): androidx.compose.ui.geometry.Offset {
        val x=(screen.x-pan.x-widthCenter())/zoom+widthCenter()
        val y=(screen.y-pan.y-heightCenter())/zoom+heightCenter()
        return androidx.compose.ui.geometry.Offset(
            minLon+(x-left)/scale, maxLat-(y-top)/scale
        )
    }
    private fun widthCenter() = (left*2f + (maxLon-minLon)*scale)/2f
    private fun heightCenter() = (top*2f + (maxLat-minLat)*scale)/2f
}

private fun pointInPolygon(x: Float, y: Float, points: List<androidx.compose.ui.geometry.Offset>): Boolean {
    var inside = false
    var j = points.lastIndex
    for (i in points.indices) {
        val a = points[i]; val b = points[j]
        if ((a.y > y) != (b.y > y) &&
            x < (b.x-a.x)*(y-a.y)/(b.y-a.y) + a.x) inside = !inside
        j = i
    }
    return inside
}

@Composable
private fun ProvinceDetail(
    p: Province,
    state: GameState,
    onAttack: (String, String) -> Unit,
    onConstruct: (BuildingType) -> Unit
) {
    val country = state.countries.firstOrNull { it.id == p.ownerCountryId }
    val isMine = p.ownerCountryId == state.playerCountryId
    val irradiated = p.irradiatedUntil > System.currentTimeMillis()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${p.name} — ${country?.name ?: "?"}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Moral: ${p.morale.roundToInt()}%   Unidades: ${p.units.size}", fontSize = 13.sp)
            Text("Edificios:", fontSize = 13.sp)
            if (p.buildings.isEmpty()) Text("Ninguno", fontSize = 12.sp)
            p.buildings.forEach { building ->
                val percentage = when (building.type) {
                    BuildingType.FACTORY -> when (building.level) {
                        1 -> "20%"; 2 -> "50%"; 3 -> "100%"; else -> "150%"
                    }
                    else -> ""
                }
                Text("• ${building.type.displayName} nivel ${building.level}" +
                    if (percentage.isNotEmpty()) " — producción $percentage cada 2 min" else "",
                    fontSize = 12.sp)
            }
            if (irradiated) Text("☢️ IRRADIADA — no conquistable", color = Color.Red, fontSize = 12.sp)
            if (isMine) {
                Text("Construir o mejorar edificios:", fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp))
                BuildingType.entries.forEach { type ->
                    val current = p.buildings.firstOrNull { it.type == type }
                    val queued = state.constructionQueue.any {
                        it.provinceId == p.id && it.type == type
                    }
                    val atMax = current != null && current.level >= type.maxLevel
                    val action = if (current == null) "Construir" else "Mejorar"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${type.displayName} " +
                                (current?.let { "Nv. ${it.level} → ${it.level + 1}" } ?: "Nv. 1"),
                                fontSize = 12.sp)
                            Text("💵 ${type.cost.money} · ⚙️ ${type.cost.metal} · ⏱ ${type.minutes} min",
                                fontSize = 10.sp)
                        }
                        Button(
                            onClick = { onConstruct(type) },
                            enabled = !queued && !atMax && state.resources.canAfford(type.cost)
                        ) {
                            Text(when {
                                queued -> "En cola"
                                atMax -> "Máximo"
                                else -> action
                            }, fontSize = 10.sp)
                        }
                    }
                }
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
