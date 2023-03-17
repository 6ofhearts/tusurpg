package com.tusurpg.component

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.ComponentListener

class ImageComponent: Comparable<ImageComponent>{
    lateinit var image: Image

    override fun compareTo(other: ImageComponent): Int { //при пересечении изображений объектов или акторов в 2d
       val yDiff = other.image.y.compareTo(image.y)      //необходимо выполнить сравнение "местоположения", чтобы показать актора перед объектом или за ним
        return if(yDiff != 0) {
            yDiff
        }
        else{
            other.image.x.compareTo(image.x)
        }
    }

    companion object {
        class ImageComponentListener(
            private val stage: Stage
        ): ComponentListener<ImageComponent> {


            override fun onComponentAdded(entity: com.github.quillraven.fleks.Entity, component: ImageComponent) {
                stage.addActor(component.image)
            }

            override fun onComponentRemoved(entity: com.github.quillraven.fleks.Entity, component: ImageComponent) {
                stage.root.removeActor(component.image)
            }
        }
    }

}
