package com.tusurpg.system

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.*
import com.tusurpg.component.ImageComponent
import com.tusurpg.component.PlayerComponent
import com.tusurpg.event.MapChangeEvent
import ktx.tiled.height
import ktx.tiled.width


@AllOf([PlayerComponent::class, ImageComponent::class])
class CameraSystem(

    private val imageCmps: ComponentMapper<ImageComponent>,
    private val physicCmps: ComponentMapper<ImageComponent>,
    stage: Stage,
) : EventListener, IteratingSystem(){

    private var maxWidth = 0f
    private var maxHeight = 0f
    private val camera = stage.camera
    override fun onTickEntity(entity: Entity) {
        with(imageCmps[entity]){
            val viewWidth = camera.viewportWidth*0.5f
            val viewHeight = camera.viewportHeight*0.5f

            camera.position.set(
                image.x.coerceIn(viewWidth, maxWidth - viewWidth),
                image.y.coerceIn(viewHeight, maxHeight - viewHeight),
                camera.position.z
            )
        }

    }

    override fun handle(event: Event?): Boolean {
        if(event is MapChangeEvent){

            maxWidth = event.map.width.toFloat()
            maxHeight = event.map.height.toFloat()
            return true
        }

        return false
    }



}
