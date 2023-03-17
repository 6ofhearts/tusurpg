package com.tusurpg.system

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Scaling
import com.github.quillraven.fleks.*
import com.tusurpg.component.*
import com.tusurpg.component.PhysicComponent.Companion.physicCmpFromImage
import com.tusurpg.event.MapChangeEvent
import com.tusurpg.tusurpg.Companion.UNIT_SCALE
import ktx.app.gdxError
import ktx.box2d.box
import ktx.math.vec2
import ktx.tiled.*

@AllOf([SpawnComponent:: class])
class EntitySpawnSystem (
    private val phWorld: World,
    private val atlas: TextureAtlas,
    private val spawnCmps: ComponentMapper<SpawnComponent>,
    ) : EventListener, IteratingSystem() {


    private val cachedCfgs = mutableMapOf<String, SpawnCfg>()
    private val cachedSizes = mutableMapOf<AnimationModel, Vector2>()

    override fun onTickEntity(entity: Entity) {
        with(spawnCmps[entity]){
            val cfg = spawnCfg(type)
            val relativeSize = size(cfg.model)

            world.entity{
                add<ImageComponent>{
                    image = Image().apply {
                        setPosition(location.x, location.y)
                        setSize(relativeSize.x,relativeSize.y)
                        setScaling(Scaling.fill)
                    }
                }
                add<AnimationComponent>{
                    nextAnimation(cfg.model, AnimationType.IDLE)
                }


                val imageCmp = add<ImageComponent> {
                    image.apply {
                        setScaling(Scaling.fill)
                        setPosition(location.x, location.y)
                        setSize(relativeSize.x, relativeSize.y)
                    }
                }


                physicCmpFromImage(phWorld, imageCmp.image, BodyDef.BodyType.DynamicBody){
                    phCmp, width, height ->
                    box(width, height){
                        isSensor = false  //задание физики автоматически каждой сущности
                    }
                }
            }

        }
        world.remove(entity)
    }

    private fun spawnCfg(type:String): SpawnCfg = cachedCfgs.getOrPut(type){

        when (type) {
            "Player" -> SpawnCfg(AnimationModel.PLAYER)
            "Slime" -> SpawnCfg(AnimationModel.SLIME)
            else -> gdxError("Тип $type не предусмотрен в SpawnCfg")
        }
      //  SpawnCfg(AnimationModel.PLAYER)
    }

    private fun size(model: AnimationModel) = cachedSizes.getOrPut(model) { //устанавливаем соответствие тайлов объектов (персонажа и противника) с размером карты
        val regions = atlas.findRegions("${model.atlasKey}/${AnimationType.IDLE.atlasKey}")
        if(regions.isEmpty){
            gdxError("Не найдены regions для анимации IDLE к модели $model")
        }
        val firstFrame = regions.first()
        vec2(firstFrame.originalWidth* UNIT_SCALE, firstFrame.originalHeight* UNIT_SCALE)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangeEvent -> {
                val entityLayer = event.map.layer("entities")
                entityLayer.objects.forEach {
                    mapObj ->
                    val type = mapObj.type?: gdxError("Объект Карты (Map Object) $mapObj не имеет типа")
                    world.entity{
                        add<SpawnComponent>{
                            this.type = type
                            this.location.set(mapObj.x*UNIT_SCALE, mapObj.y*UNIT_SCALE)
                        }
                    }
                }
                return true
            }
        }
        return false
    }

}
