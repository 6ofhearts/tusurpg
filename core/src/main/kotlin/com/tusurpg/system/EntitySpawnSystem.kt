package com.tusurpg.system

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.utils.Scaling
import com.github.quillraven.fleks.*
import com.tusurpg.actor.FlipImage
import com.tusurpg.component.*
import com.tusurpg.component.PhysicComponent.Companion.physicCmpFromImage
import com.tusurpg.event.MapChangeEvent
import com.tusurpg.tusurpg.Companion.UNIT_SCALE
import ktx.app.gdxError
import ktx.box2d.box
import ktx.math.vec2
import ktx.tiled.*
import kotlin.math.roundToInt

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
                    image = FlipImage().apply {
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


                physicCmpFromImage(phWorld, imageCmp.image, cfg.bodyType){
                   phCmp, width, height ->

                    val w = width*cfg.physicScaling.x
                    val h = height*cfg.physicScaling.y
                    phCmp.offset.set(cfg.physicOffset)
                    phCmp.size.set(w, h)

                    box(w, h, cfg.physicOffset){ //хит бокс
                        isSensor = false  //задание физики автоматически каждой сущности
                        userData = HIT_BOX_SENSOR
                    }
                }
                if(cfg.speedScaling>0f) {
                    add<MoveComponent>() { //рамка коллизии
                        speed = DEFAULT_SPEED * cfg.speedScaling
                    }
                }

                if(cfg.canAttack){
                    add<AttackComponent>{
                       maxDelay = cfg.attackDelay
                        damage = (DEFAULT_ATTACK_DAMAGE * cfg.attackScaling).roundToInt()
                        extraRange = cfg.attackExtraRange
                    }
                }

                if(cfg.lifeScaling > 0f){
                    add<LifeComponent>{
                        max = DEFAULT_LIFE*cfg.lifeScaling
                        life = max
                    }
                }

                if(type == "Player"){
                    add<PlayerComponent>()
                }

                if (cfg.lootable) {
                    add<LootComponent>()
                }

                if (cfg.bodyType != BodyDef.BodyType.StaticBody){
                    //создать или убрать сущности
                    add<CollisionComponent>()
                }
            }

        }
        world.remove(entity)
    }

    private fun spawnCfg(type:String): SpawnCfg = cachedCfgs.getOrPut(type){

        when (type) {
            "Player" -> SpawnCfg(
                AnimationModel.PLAYER,
                attackExtraRange = 0.6f,
                attackScaling = 1.25f,
                physicScaling = vec2(0.3f, 0.3f),
                physicOffset = vec2(0f, -10f* UNIT_SCALE)
            )
            "Slime" -> SpawnCfg(
                AnimationModel.SLIME,
                lifeScaling = 0.75f,
                physicScaling = vec2(0.3f, 0.3f),
                physicOffset = vec2(0f, -2f* UNIT_SCALE)
            )
            "Chest"-> SpawnCfg(
            AnimationModel.CHEST,
            speedScaling = 0f,
            bodyType = BodyDef.BodyType.StaticBody,
            lifeScaling = 0f,
                lootable = true,
                canAttack = false
        )
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

    companion object {
        const val HIT_BOX_SENSOR = "Hitbox"
    }

}
