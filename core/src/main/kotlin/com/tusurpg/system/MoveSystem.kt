package com.tusurpg.system

import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.tusurpg.component.ImageComponent
import com.tusurpg.component.MoveComponent
import com.tusurpg.component.PhysicComponent
import ktx.math.component1
import ktx.math.component2

@AllOf([MoveComponent::class, PhysicComponent::class])
class MoveSystem (
    private val moveCmps: ComponentMapper<MoveComponent>,
    private val physicCmps: ComponentMapper<PhysicComponent>,
    private val imgCmps: ComponentMapper<ImageComponent>,
    ): IteratingSystem(){

    override fun onTickEntity(entity: Entity) {
        val moveCmp = moveCmps[entity]
        val physicCmp = physicCmps[entity]
        val mass = physicCmp.body.mass
        val (velX, velY) = physicCmp.body.linearVelocity

        if(moveCmp.cos == 0f && moveCmp.sin == 0f){
            //нет направления - остановить сущность

            physicCmp.impulse.set (
                mass * (0f - velX),
                mass * (0f - velY),
                )
            return
        }

        physicCmp.impulse.set(
            mass*(moveCmp.speed*moveCmp.cos - velX),
            mass*(moveCmp.speed*moveCmp.sin - velY),
        )

        imgCmps.getOrNull(entity)?.let {
            imageCmp ->
            if (moveCmp.cos != 0f){
                imageCmp.image.flipX = moveCmp.cos < 0
        }
        }
    }


}
