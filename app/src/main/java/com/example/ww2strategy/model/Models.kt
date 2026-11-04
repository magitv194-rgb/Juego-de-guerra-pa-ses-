package com.example.ww2strategy.model

import kotlinx.serialization.Serializable

@Serializable
data class Resources(
    val money: Long = 1500L,
    val food: Long = 800L,
    val metal: Long = 800L,
    val oil: Long = 500L
) {
    operator fun plus(o: Resources) =
        Resources(money + o.money, food + o.food, metal + o.metal, oil + o.oil)
    operator fun minus(o: Resources) =
        Resources(money - o.money, food - o.food, metal - o.metal, oil - o.oil)
    operator fun times(n: Int) =
        Resources(money * n, food * n, metal * n, oil * n)
    fun canAfford(o: Resources) =
        money >= o.money && food >= o.food && metal >= o.metal && oil >= o.oil
    fun nonNegative() = Resources(
        money.coerceAtLeast(0), food.coerceAtLeast(0),
        metal.coerceAtLeast(0), oil.coerceAtLeast(0)
    )
}

@Serializable
enum class UnitType(
    val displayName: String,
    val baseHp: Int,
    val attack: Int,
    val defense: Int,
    val productionMinutes: Int,
    val cost: Resources,
    val requiresResearch: String? = null
) {
    SOLDIER      ("Soldado",            20,  4,  2, 10, Resources(50,  20,   0,   0), null),
    ARMORED_CAR  ("Vehículo blindado",  25,  6,  4, 20, Resources(150,  0,  80,  30), "tech_armored"),
    TANK         ("Tanque",             30, 10,  8, 30, Resources(250,  0, 150,  60), "tech_tank"),
    ARTILLERY    ("Artillería",         25, 12,  3, 35, Resources(200,  0, 100,  40), "tech_artillery"),
    AIRCRAFT     ("Avión",              35, 14,  5, 40, Resources(350,  0, 120, 100), "tech_aircraft"),
    SHIP         ("Barco",              50, 16, 10, 45, Resources(400,  0, 200,  80), "tech_ship")
}

@Serializable
enum class BuildingType(
    val displayName: String,
    val maxLevel: Int,
    val minutes: Int,
    val cost: Resources,
    val productionBonus: Float = 0f
) {
    MILITARY_BASE("Base militar",      4, 25, Resources(300, 100, 150,  50), 0f),
    BARRACKS    ("Cuartel",           5, 20, Resources(150,  50,  50,   0), 0f),
    FARM        ("Granja",            5, 20, Resources(100,   0,   0,   0), 0.20f),
    MINE        ("Mina",              5, 20, Resources(120,   0,  40,   0), 0.20f),
    OIL_RIG     ("Pozo de petróleo",  5, 25, Resources(150,   0,  60,   0), 0.20f),
    MARKET      ("Mercado",           5, 20, Resources(100,  20,   0,   0), 0.20f),
    FACTORY     ("Fábrica",           4,  2, Resources(250,   0, 120,  60), 0f),
    RESEARCH_LAB("Laboratorio",       3, 60, Resources(300,   0, 150, 100), 0f)
}

@Serializable
data class Building(val type: BuildingType, val level: Int = 1, val damaged: Boolean = false)

@Serializable
data class MilitaryUnit(val id: String, val type: UnitType, val hp: Int)

@Serializable
data class Country(val id: String, val name: String, val colorArgb: Long)

@Serializable
data class Province(
    val id: String,
    val name: String,
    val ownerCountryId: String,
    val neighbors: List<String>,
    val baseYield: Resources,
    val x: Int,
    val y: Int,
    val buildings: List<Building> = emptyList(),
    val units: List<MilitaryUnit> = emptyList(),
    val morale: Float = 70f,
    val irradiatedUntil: Long = 0L
)

@Serializable
data class ProductionOrder(val id: String, val provinceId: String, val type: UnitType, val endsAt: Long)

@Serializable
data class ResearchOrder(val id: String, val researchId: String, val endsAt: Long)

@Serializable
data class ConstructionOrder(val id: String, val provinceId: String, val type: BuildingType, val endsAt: Long)

@Serializable
data class ResearchDef(
    val id: String,
    val displayName: String,
    val minutes: Int,
    val cost: Resources,
    val unlocksUnit: UnitType?
)

val ResearchCatalog: List<ResearchDef> = listOf(
    ResearchDef("tech_armored",   "Blindados",         15, Resources(200, 0, 100, 20),  UnitType.ARMORED_CAR),
    ResearchDef("tech_tank",      "Tanques",           30, Resources(300, 0, 200, 50),  UnitType.TANK),
    ResearchDef("tech_artillery", "Artillería",        25, Resources(250, 0, 150, 30),  UnitType.ARTILLERY),
    ResearchDef("tech_aircraft",  "Aviación",          45, Resources(400, 0, 150, 150), UnitType.AIRCRAFT),
    ResearchDef("tech_ship",      "Marina",            40, Resources(350, 0, 250, 100), UnitType.SHIP),
    ResearchDef("tech_nuclear",   "Programa nuclear", 120, Resources(1000, 0, 800, 500), null)
)

@Serializable
data class GameState(
    val playerCountryId: String = "player",
    val countries: List<Country> = defaultCountries(),
    val provinces: List<Province> = defaultProvinces(),
    val resources: Resources = Resources(),
    val completedResearch: Set<String> = emptySet(),
    val productionQueue: List<ProductionOrder> = emptyList(),
    val researchQueue: List<ResearchOrder> = emptyList(),
    val constructionQueue: List<ConstructionOrder> = emptyList(),
    val lastResourceTick: Long = System.currentTimeMillis(),
    val lastFactoryTick: Long = System.currentTimeMillis(),
    val gameStartTime: Long = System.currentTimeMillis()
)

fun defaultCountries(): List<Country> = listOf(
    Country("player", "Argentina", 0xFF278A45)
)

private val provinceNames = listOf(
    "Ciudad Autónoma de Buenos Aires", "Buenos Aires", "Catamarca", "Chaco",
    "Chubut", "Córdoba", "Corrientes", "Entre Ríos", "Formosa", "Jujuy",
    "La Pampa", "La Rioja", "Mendoza", "Misiones", "Neuquén", "Río Negro",
    "Salta", "San Juan", "San Luis", "Santa Cruz", "Santa Fe",
    "Santiago del Estero", "Tierra del Fuego", "Tucumán"
)

fun defaultProvinces(): List<Province> = provinceNames.mapIndexed { index, name ->
    val isCapital = index == 0
    Province(
        id = listOf("caba","buenos-aires","catamarca","chaco","chubut","cordoba",
            "corrientes","entre-rios","formosa","jujuy","la-pampa","la-rioja",
            "mendoza","misiones","neuquen","rio-negro","salta","san-juan",
            "san-luis","santa-cruz","santa-fe","santiago-del-estero",
            "tierra-del-fuego","tucuman")[index],
        name = name,
        ownerCountryId = "player",
        neighbors = emptyList(),
        baseYield = Resources(
            money = if (index == 1) 100 else if (index == 0) 70 else 35,
            food = if (index == 1) 65 else 25,
            metal = if (index == 1) 45 else 20,
            oil = if (index == 1) 30 else 12
        ),
        x = 0,
        y = 0,
        buildings = if (isCapital) listOf(
            Building(BuildingType.MILITARY_BASE),
            Building(BuildingType.BARRACKS),
            Building(BuildingType.FACTORY)
        ) else emptyList(),
        units = if (isCapital) List(20) {
            MilitaryUnit("arg-start-$it", UnitType.SOLDIER, UnitType.SOLDIER.baseHp)
        } else emptyList()
    )
}
