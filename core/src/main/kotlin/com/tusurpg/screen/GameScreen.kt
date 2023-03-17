package com.tusurpg.screen

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.github.quillraven.fleks.World
import com.tusurpg.component.ImageComponent
import com.tusurpg.component.PhysicComponent
import com.tusurpg.event.MapChangeEvent
import com.tusurpg.event.fire
import com.tusurpg.system.*
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.box2d.createWorld
import ktx.log.logger
import ktx.math.vec2

class GameScreen : KtxScreen {

    //private val spriteBatch: Batch = SpriteBatch()
    private val stage: Stage = Stage(ExtendViewport(16f, 9f))
    private val textureAtlas = TextureAtlas("assets/graphics/player1.atlas") // импортированный атлас GDX
    private var currentMap: TiledMap? = null
    private val phWorld = createWorld(gravity = vec2()).apply{
        autoClearForces = false
    }



    //  private val playerTexture: Texture = Texture("assets/graphics/player.png") --- для импорта самого png
    //  private val slimeTexture: Texture = Texture("assets/graphics/slime.png")
    private val eWorld: World = World {
        inject(stage) //dependency injection like java
        inject(textureAtlas)
        inject(phWorld)

        componentListener<ImageComponent.Companion.ImageComponentListener>()
        componentListener<PhysicComponent.Companion.PhysicComponentListener>()

        system<EntitySpawnSystem>()
        system<PhysicSystem>()
        system<AnimationSystem>()
        system<RenderSystem>()
        system<DebugSystem>()
    }


    override fun show() {
        log.debug { "показываем GameScreen" }

        eWorld.systems.forEach { system ->
            if (system is EventListener){
                stage.addListener(system)
            }
        }

        currentMap = TmxMapLoader().load("assets/map/map1.tmx")
        stage.fire(MapChangeEvent(currentMap!!))

        //textureAtlas - посредством GDX "разбираем" png на спрайты
        }

        override fun resize(width: Int, height: Int) {
            stage.viewport.update(width, height, true)
        }

        override fun render(delta: Float) {

            eWorld.update(delta.coerceAtMost(0.25f))
        }

        override fun dispose() {
            stage.disposeSafely()
            textureAtlas.disposeSafely()
            eWorld.dispose()
            currentMap?.disposeSafely()
            phWorld.disposeSafely()
        }

        companion object {
            private val log = logger<GameScreen>()
        }
    }
