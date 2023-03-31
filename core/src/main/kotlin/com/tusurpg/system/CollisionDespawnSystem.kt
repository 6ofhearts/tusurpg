package com.tusurpg.system

//import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.*
import com.tusurpg.component.TiledComponent
import com.tusurpg.event.CollisionDespawnEvent
import com.tusurpg.event.fire



@AllOf([TiledComponent::class])
class CollisionDespawnSystem(
   private val stage: Stage,
    private val tiledCmps: ComponentMapper<TiledComponent>,
) : IteratingSystem() {
    override fun onTickEntity(entity: Entity) {
        with(tiledCmps[entity]) {
            //для инициализированных коллизий проверяем есть ли сущности вокруг них -> есть ничего нет, избавляемся от коллизии
            if (nearbyEntities.isEmpty()) {
                stage.fire(CollisionDespawnEvent(cell))
                world.remove(entity)
            }
        }
    }
}
