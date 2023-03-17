package com.tusurpg.system

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.*
import com.github.quillraven.fleks.collection.compareEntity
import com.tusurpg.component.ImageComponent
import com.tusurpg.event.MapChangeEvent
import com.tusurpg.tusurpg.Companion.UNIT_SCALE
import ktx.graphics.use
import ktx.tiled.forEachLayer

@AllOf([ImageComponent::class])
//@NoneOf()
//@AnyOf()

class RenderSystem (
    private val stage: Stage,
    private val imageCmps: ComponentMapper<ImageComponent>
    ): EventListener, IteratingSystem(
    comparator = compareEntity { e1, e2 ->  imageCmps[e1].compareTo(imageCmps[e2])}
    ) {

    private val bgdLayers = mutableListOf<TiledMapTileLayer>()
    private val fgdLayers = mutableListOf<TiledMapTileLayer>()
    private val mapRenderer = OrthogonalTiledMapRenderer(null, UNIT_SCALE, stage.batch)
    private val orthoCam = stage.camera as OrthographicCamera

    override fun onTick() {
        super.onTick()

        with(stage) {
            viewport.apply()

            AnimatedTiledMapTile.updateAnimationBaseTime()
            mapRenderer.setView(orthoCam)
           // mapRenderer.render()

            if (bgdLayers.isNotEmpty()){
                stage.batch.use(orthoCam.combined) {
                    bgdLayers.forEach{mapRenderer.renderTileLayer(it)}
                }
            }

            act(deltaTime)
            draw()
            if (fgdLayers.isNotEmpty()){
                stage.batch.use(orthoCam.combined) {
                    fgdLayers.forEach{ mapRenderer.renderTileLayer(it)}
                }
            }
        }
    }

    override fun onTickEntity(entity: Entity) {
        imageCmps[entity].image.toFront()
    }

    override fun handle(event: Event): Boolean {
        when {
            event is MapChangeEvent -> {
                bgdLayers.clear()
                fgdLayers.clear()

                event.map.forEachLayer<TiledMapTileLayer> { layer ->
                    if (layer.name.startsWith("fgd_")){
                        fgdLayers.add(layer)
                    }
                    else {
                        bgdLayers.add(layer)

                    }
                }
                return true
            }
        }
        return false
    }
}
