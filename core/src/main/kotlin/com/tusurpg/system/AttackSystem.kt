package com.tusurpg.system

import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.fleks.*
import com.tusurpg.component.*
import com.tusurpg.system.EntitySpawnSystem.Companion.HIT_BOX_SENSOR
import ktx.box2d.query
import ktx.math.component1
import ktx.math.component2



@AllOf([AttackComponent::class, PhysicComponent::class, ImageComponent::class])
class AttackSystem(
    private val attackCmps: ComponentMapper<AttackComponent>,
    private val physicCmps: ComponentMapper<PhysicComponent>,
    private val imageCmps: ComponentMapper<ImageComponent>,
    private val lifeCmps: ComponentMapper<LifeComponent>,
    private val playerCmps: ComponentMapper<PlayerComponent>,
    private val lootCmps: ComponentMapper<LootComponent>,
    private val phWorld: com.badlogic.gdx.physics.box2d.World,
) : IteratingSystem(){
    override fun onTickEntity(entity: Entity) {
        val attackCmp = attackCmps[entity]

        if(attackCmp.state == AttackState.READY && !attackCmp.doAttack){
            return
        }

        if(attackCmp.isReady && attackCmp.doAttack){
            attackCmp.doAttack = false
            attackCmp.state = AttackState.ATTACKING
            attackCmp.delay = attackCmp.maxDelay
            return
        }

        attackCmp.delay -= deltaTime
        if (attackCmp.delay<= 0f && attackCmp.isAttacking) {
            //нанести урон окружающим врагам
            attackCmp.state = AttackState.DEAL_DAMAGE

            val image = imageCmps[entity].image
            val attackLeft = image.flipX
            val physicCmp = physicCmps[entity]
            val (x, y) = physicCmp.body.position
            val (offX, offY) = physicCmp.offset
            val (w, h) = physicCmp.size
            val halfW = w * 0.5f
            val halfH = h * 0.5f


            if (attackLeft) { //атака слева
                AABB_RECT.set(
                    x + offX - halfW - attackCmp.extraRange,
                    y + offY - halfH,
                    x + offX + halfW,
                    y + offY + halfH
                )
            } else { //атака справа
                AABB_RECT.set(
                    x + offX - halfW,
                    y + offY - halfH,
                    x + offX + halfW + attackCmp.extraRange, //правый верхний угол
                    y + offY + halfH
                )
            }

            phWorld.query(AABB_RECT.x, AABB_RECT.y, AABB_RECT.width, AABB_RECT.height){
                fixture ->
                if(fixture.userData != HIT_BOX_SENSOR){
                    return@query true
                    }


                val fixtureEntity = fixture.entity
                if (fixtureEntity == entity){
                    //не атакуем себя
                    return@query true
                }



                configureEntity(fixtureEntity){
                    lifeCmps.getOrNull(it)?.let { lifeCmp ->
                        lifeCmp.takeDamage += attackCmp.damage + MathUtils.random(0.9f, 1.2f)
                    }

                    if (entity in playerCmps){
                        lootCmps.getOrNull(it)?.let{ lootCmp ->
                            lootCmp.interactEntity = entity
                        }
                    }

                }

                return@query true
            }

            attackCmp.state = AttackState.READY
        }
    }

    companion object {
        val AABB_RECT = com.badlogic.gdx.math.Rectangle()
    }
}
