package com.tusurpg.system

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.*
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.tusurpg.component.CollisionComponent
import com.tusurpg.component.PhysicComponent
import com.tusurpg.component.PhysicComponent.Companion.physicCmpFromShape2D
import com.tusurpg.component.TiledComponent
import com.tusurpg.event.CollisionDespawnEvent
import com.tusurpg.event.MapChangeEvent
import ktx.box2d.body
import ktx.box2d.loop
import ktx.collections.GdxArray
import ktx.math.component1
import ktx.math.component2
import ktx.math.vec2
import ktx.tiled.*


@AllOf([PhysicComponent::class, CollisionComponent::class])
class CollisionSpawnSystem(
    private val phWorld: World,
    private val physicCmps: ComponentMapper<PhysicComponent>,
) : EventListener, IteratingSystem() {

    private val tiledLayers = GdxArray<TiledMapTileLayer>()
    private val processedCells = mutableSetOf<TiledMapTileLayer.Cell>()


    private fun TiledMapTileLayer.forEachCell(
        startX: Int,
        startY: Int,
        size: Int,
        action: (TiledMapTileLayer.Cell, Int, Int) -> Unit
    ) {
        for (x in startX - size..startX + size) {
            for (y in startY - size until startY + size) {
                this.getCell(x, y)?.let { action(it, x, y) }
            }
        }
    }

    override fun onTickEntity(entity: Entity) {
        // for collision entities we will spawn the collision objects around them that are not spawned yet
        val (entityX, entityY) = physicCmps[entity].body.position

        tiledLayers.forEach { layer ->
            layer.forEachCell(entityX.toInt(), entityY.toInt(), SPAWN_AREA_SIZE) { cell, x, y ->
                if (cell.tile.objects.isEmpty()) {
                    // tileCell не связана с объектами коллизии -> пропускаем
                    return@forEachCell
                }
                if (cell in processedCells) {
                    // cell обработана -> запоминаем её, пропускаем (чтобы избежать бесконечной генерации коллизий)
                    return@forEachCell
                }

                processedCells.add(cell)
                cell.tile.objects.forEach { mapObj ->
                    world.entity {
                        physicCmpFromShape2D(phWorld, x, y, mapObj.shape)
                        add<TiledComponent> {
                            this.cell = cell
                            // add entity immediately here, otherwise the newly created
                            // collision entity might get removed by the CollisionDespawnSystem because
                            // the physic collision event will come later in the PhysicSystem when
                            // the physic world gets updated
                            nearbyEntities.add(entity)
                        }
                    }
                }
            }
        }
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangeEvent -> {

                processedCells.clear()
                event.map.layers.getByType(TiledMapTileLayer::class.java, tiledLayers)

                // создание коллизии для границ карты
                world.entity {
                    val w = event.map.width.toFloat()
                    val h = event.map.height.toFloat()
                    add<PhysicComponent> {
                        body = phWorld.body(StaticBody) {
                            position.set(0f, 0f)
                            fixedRotation = true
                            allowSleep = false
                            loop(
                                //цикл для избежания случаев "призрачных стен" и неверного определения коллизий при использовании box2d напрямую
                                vec2(0f, 0f),
                                vec2(w, 0f),
                                vec2(w, h),
                                vec2(0f, h),
                            )
                        }
                    }
                }
                return true
            }
            is CollisionDespawnEvent -> {
                processedCells.remove(event.cell)
                return true
        }
        else -> return false
    }
}

    companion object {
        const val SPAWN_AREA_SIZE = 3
    }
}
