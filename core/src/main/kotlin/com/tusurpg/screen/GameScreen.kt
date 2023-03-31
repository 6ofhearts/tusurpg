package com.tusurpg.screen

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.github.quillraven.fleks.World
import com.tusurpg.component.FloatingTextComponent
import com.tusurpg.component.ImageComponent
import com.tusurpg.component.PhysicComponent
import com.tusurpg.event.MapChangeEvent
import com.tusurpg.event.fire
import com.tusurpg.input.PlayerKeyboardInputProcessor
import com.tusurpg.system.*
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.box2d.createWorld
import ktx.log.logger
import ktx.math.vec2

class GameScreen : KtxScreen {

    private val gameStage: Stage = Stage(ExtendViewport(16f, 9f))
    private val uiStage: Stage = Stage(ExtendViewport(1280f, 720f))
    private val textureAtlas = TextureAtlas("assets/graphics/player1.atlas") // импортированный атлас GDX
    private var currentMap: TiledMap? = null
    private val phWorld = createWorld(gravity = vec2()).apply{
        autoClearForces = false
    }



    //  private val playerTexture: Texture = Texture("assets/graphics/player.png") --- для импорта самого png
    //  private val slimeTexture: Texture = Texture("assets/graphics/slime.png")
    private val eWorld: World = World {
        inject(gameStage) //dependency injection like java
        inject("uiStage",uiStage)
        inject(textureAtlas)
        inject(phWorld)

        componentListener<ImageComponent.Companion.ImageComponentListener>()
        componentListener<PhysicComponent.Companion.PhysicComponentListener>()
        componentListener<FloatingTextComponent.Companion.FloatingTextComponentListener>()

        system<EntitySpawnSystem>()
        system<CollisionSpawnSystem>()
        system<CollisionDespawnSystem>()
        system<MoveSystem>()
        system<AttackSystem>()
        system<LootSystem>()
        system<LifeSystem>()
        system<DeadSystem>()
        system<PhysicSystem>()
        system<AnimationSystem>()
        system<CameraSystem>()
        system<FloatingTextSystem>()
        system<RenderSystem>()
        system<DebugSystem>()
    }


    override fun show() {
        log.debug { "показываем GameScreen" }

        eWorld.systems.forEach { system ->
            if (system is EventListener){
                gameStage.addListener(system)
            }
        }

        currentMap = TmxMapLoader().load("assets/map/map1.tmx")
        gameStage.fire(MapChangeEvent(currentMap!!))

        PlayerKeyboardInputProcessor(eWorld)

        //textureAtlas - посредством GDX "разбираем" png на спрайты
        }

        override fun resize(width: Int, height: Int) {
            gameStage.viewport.update(width, height, true)
            uiStage.viewport.update(width, height, true)
        }

        override fun render(delta: Float) {

            eWorld.update(delta.coerceAtMost(0.25f))
        }

        override fun dispose() {
            gameStage.disposeSafely()
            uiStage.disposeSafely()
            textureAtlas.disposeSafely()
            eWorld.dispose()
            currentMap?.disposeSafely()
            phWorld.disposeSafely()
        }

        companion object {
            private val log = logger<GameScreen>()
        }
    }
