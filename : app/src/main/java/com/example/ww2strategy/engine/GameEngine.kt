package com.example.ww2strategy.engine

import com.example.ww2strategy.model.*
import java.util.UUID

object GameEngine {

    fun tick(state: GameState, now: Long = System.currentTimeMillis()): GameState {
        var s = state
        val minutesElapsed = (now - s.lastResourceTick) / 60_000L
        if (minutesElapsed >= 3) {
            val ticks = (minutesElapsed / 3).toInt()
            val income = calculateIncome(s) * ticks
            s = s.copy(
                resources = (s.resources + income).nonNegative(),
                lastResourceTick = s.lastResourceTick + ticks * 3 * 60_000L
            )
        }
        val (doneProd, pendingProd) = s.productionQueue.partition { it.endsAt <= now }
        if (doneProd.isNotEmpty()) {
            val provinces = s.provinces.toMutableList()
            doneProd.forEach { order ->
                val idx = provinces.indexOfFirst { it.id == order.provinceId }
                if (idx >= 0) {
                    val p = provinces[idx]
                    val newUnit = MilitaryUnit(UUID.randomUUID().toString(), order.type, order.type.baseHp)
                    provinces[idx] = p.copy(units = p.units + newUnit)
                }
            }
            s = s.copy(provinces = provinces, productionQueue = pendingProd)
        }
        val (doneRes, pendingRes) = s.researchQueue.partition { it.endsAt <= now }
        if (doneRes.isNotEmpty()) {
            s = s.copy(
                completedResearch = s.completedResearch + doneRes.map { it.researchId },
                researchQueue = pendingRes
            )
        }
        val (doneCon, pendingCon) = s.constructionQueue.partition { it.endsAt <= now }
        if (doneCon.isNotEmpty()) {
            val provinces = s.provinces.toMutableList()
            doneCon.forEach { order ->
                val idx = provinces.indexOfFirst { it.id == order.provinceId }
                if (idx >= 0) {
                    val p = provinces[idx]
                    val existing = p.buildings.firstOrNull { it.type == order.type }
                    val newBuildings = when {
                        existing == null -> p.buildings + Building(order.type, 1)
                        existing.level < order.type.maxLevel ->
                            p.buildings.map {
                                if (it.type == order.type) it.copy(level = it.level + 1, damaged = false)
                                else it
                            }
                        else -> p.buildings
                    }
                    provinces[idx] = p.copy(buildings = newBuildings)
                }
            }
            s = s.copy(provinces = provinces, constructionQueue = pendingCon)
        }
        val provinces = s.provinces.map { p ->
            if (p.irradiatedUntil <= now && p.morale < 100f)
                p.copy(morale = (p.morale + 0.01f).coerceAtMost(100f))
            else p
        }
        return s.copy(provinces = provinces)
    }

    data class Bonuses(val money: Float, val food: Float, val metal: Float, val oil: Float)

    fun buildingBonuses(p: Province): Bonuses {
        var m = 0f; var f = 0f; var me = 0f; var o = 0f
        p.buildings.filterNot { it.damaged }.forEach { b ->
            when (b.type) {
                BuildingType.MARKET  -> m  += b.type.productionBonus * b.level
                BuildingType.FARM    -> f  += b.type.productionBonus * b.level
                BuildingType.MINE    -> me += b.type.productionBonus * b.level
                BuildingType.OIL_RIG -> o  += b.type.productionBonus * b.level
                else -> {}
            }
        }
        return Bonuses(m, f, me, o)
    }

    fun calculateIncome(s: GameState): Resources {
        return s.provinces
            .filter { it.ownerCountryId == s.playerCountryId }
            .fold(Resources(0, 0, 0, 0)) { acc, p ->
                val moraleMult = 0.5f + (p.morale / 200f)
                val b = buildingBonuses(p)
                val y = Resources(
                    money = (p.baseYield.money * (1f + b.money) * moraleMult).toLong(),
                    food  = (p.baseYield.food  * (1f + b.food)  * moraleMult).toLong(),
                    metal = (p.baseYield.metal * (1f + b.metal) * moraleMult).toLong(),
                    oil   = (p.baseYield.oil   * (1f + b.oil)   * moraleMult).toLong()
                )
                acc + y
            }
    }

    fun startProduction(s: GameState, provinceId: String, type: UnitType, now: Long): GameState? {
        val p = s.provinces.firstOrNull { it.id == provinceId } ?: return null
        if (p.ownerCountryId != s.playerCountryId) return null
        if (type.requiresResearch != null && type.requiresResearch !in s.completedResearch) return null
        if (!s.resources.canAfford(type.cost)) return null
        if (p.buildings.none { it.type == BuildingType.BARRACKS }) return null
        val order = ProductionOrder(UUID.randomUUID().toString(), provinceId, type,
            now + type.productionMinutes * 60_000L)
        return s.copy(
            resources = (s.resources - type.cost).nonNegative(),
            productionQueue = s.productionQueue + order
        )
    }

    fun startResearch(s: GameState, researchId: String, now: Long): GameState? {
        val def = ResearchCatalog.firstOrNull { it.id == researchId } ?: return null
        if (researchId in s.completedResearch) return null
        if (s.researchQueue.any { it.researchId == researchId }) return null
        if (!s.resources.canAfford(def.cost)) return null
        val order = ResearchOrder(UUID.randomUUID().toString(), researchId,
            now + def.minutes * 60_000L)
        return s.copy(
            resources = (s.resources - def.cost).nonNegative(),
            researchQueue = s.researchQueue + order
        )
    }

    fun startConstruction(s: GameState, provinceId: String, type: BuildingType, now: Long): GameState? {
        val p = s.provinces.firstOrNull { it.id == provinceId } ?: return null
        if (p.ownerCountryId != s.playerCountryId) return null
        val existing = p.buildings.firstOrNull { it.type == type }
        if (existing != null && existing.level >= type.maxLevel) return null
        if (!s.resources.canAfford(type.cost)) return null
        val order = ConstructionOrder(UUID.randomUUID().toString(), provinceId, type,
            now + type.minutes * 60_000L)
        return s.copy(
            resources = (s.resources - type.cost).nonNegative(),
            constructionQueue = s.constructionQueue + order
        )
    }

    fun investMorale(s: GameState, provinceId: String): GameState? {
        val cost = Resources(100, 0, 0, 0)
        if (!s.resources.canAfford(cost)) return null
        val provinces = s.provinces.map {
            if (it.id == provinceId && it.ownerCountryId == s.playerCountryId)
                it.copy(morale = (it.morale + 5f).coerceAtMost(100f))
            else it
        }
        return s.copy(resources = (s.resources - cost).nonNegative(), provinces = provinces)
    }

    fun launchAttack(s: GameState, fromId: String, toId: String, now: Long): GameState? {
        val from = s.provinces.firstOrNull { it.id == fromId } ?: return null
        val to   = s.provinces.firstOrNull { it.id == toId }   ?: return null
        if (from.ownerCountryId != s.playerCountryId) return null
        if (to.ownerCountryId == s.playerCountryId) return null
        if (toId !in from.neighbors) return null
        if (to.irradiatedUntil > now) return null
        if (from.units.isEmpty()) return null
        
        // LÍNEA CORREGIDA AQUÍ
        val attackPower  = from.units.map { it.type.attack * (it.hp.toFloat() / it.type.baseHp) }.sum()
        val defensePower = to.units.map   { it.type.defense * (it.hp.toFloat() / it.type.baseHp) }.sum()
        
        val provinces = s.provinces.toMutableList()
        val fromIdx = provinces.indexOfFirst { it.id == fromId }
        val toIdx   = provinces.indexOfFirst { it.id == toId }
        if (attackPower > defensePower) {
            val survivors = from.units.take((from.units.size * 0.5f).toInt().coerceAtLeast(1))
            provinces[toIdx] = to.copy(
                ownerCountryId = s.playerCountryId,
                units = survivors,
                morale = (to.morale * 0.6f).coerceAtLeast(5f)
            )
            provinces[fromIdx] = from.copy(units = from.units.drop(survivors.size))
        } else {
            provinces[fromIdx] = from.copy(units = from.units.take((from.units.size * 0.3f).toInt()))
            provinces[toIdx]   = to.copy(units   = to.units.take((to.units.size * 0.6f).toInt()))
        }
        return s.copy(provinces = provinces)
    }

    fun launchNuclear(s: GameState, targetProvinceId: String, now: Long): GameState? {
        if ("tech_nuclear" !in s.completedResearch) return null
        val cost = Resources(800, 0, 500, 300)
        if (!s.resources.canAfford(cost)) return null
        val target = s.provinces.firstOrNull { it.id == targetProvinceId } ?: return null
        if (target.irradiatedUntil > now) return null
        val provinces = s.provinces.map { p ->
            when {
                p.id == targetProvinceId -> p.copy(
                    units = emptyList(),
                    morale = (p.morale - 21f).coerceAtLeast(0f),
                    irradiatedUntil = now + 2 * 60_000L,
                    buildings = p.buildings.map { it.copy(damaged = true) }
                )
                p.id in target.neighbors -> p.copy(
                    units = p.units.map { u -> u.copy(hp = (u.hp - 5).coerceAtLeast(1)) },
                    morale = (p.morale - 5f).coerceAtLeast(0f)
                )
                else -> p
            }
        }
        return s.copy(resources = (s.resources - cost).nonNegative(), provinces = provinces)
    }
}
