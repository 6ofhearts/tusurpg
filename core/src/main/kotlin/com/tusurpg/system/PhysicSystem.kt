package com.tusurpg.system

//import com.tusurpg.system.EntitySpawnSystem.Companion.ACTION_SENSOR
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.fleks.*
import com.tusurpg.component.*
import com.tusurpg.component.ImageComponent
import com.tusurpg.component.PhysicComponent
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

val Fixture.entity: Entity
    get() = this.body.userData as Entity

@AllOf([PhysicComponent::class, ImageComponent::class])
class PhysicSystem(
    private val phWorld: World,
    private val imageCmps: ComponentMapper<ImageComponent>,
    private val physicCmps: ComponentMapper<PhysicComponent>,
    private val tiledCmps: ComponentMapper<TiledComponent>,
    private val collisionCmps: ComponentMapper<CollisionComponent>
) : ContactListener, IteratingSystem(interval = Fixed(1/60f)) {

    init {
        phWorld.setContactListener(this)
    }

    override fun onUpdate() {
        if(phWorld.autoClearForces){
            log.error{"autoClearForces должен иметь значение false для корректной симуляции физики"}
            phWorld.autoClearForces = false
        }
        super.onUpdate()
        phWorld.clearForces()
    }

    override fun onTick() {
        super.onTick()
        phWorld.step(deltaTime, 0, 2)
    }

    override fun onTickEntity(entity: Entity) {
        val physicCmp = physicCmps[entity]

        physicCmp.prevPos.set(physicCmp.body.position)

        if(!physicCmp.impulse.isZero){
            physicCmp.body.applyLinearImpulse(physicCmp.impulse,physicCmp.body.worldCenter, true)
            physicCmp.impulse.setZero()
        }

    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        val physicCmp = physicCmps[entity]
        val imageCmp = imageCmps[entity]

        val (prevX, prevY) = physicCmp.prevPos
        val (bodyX, bodyY) = physicCmp.body.position
        imageCmp.image.run {
            setPosition(
                MathUtils.lerp(prevX, bodyX, alpha)- width * 0.5f,
                MathUtils.lerp(prevY, bodyY, alpha) - height*0.5f
            )
        }
    }

    companion object {
        private val log = logger<PhysicSystem>()
    }

    private val Contact.isSensorA: Boolean
        get() = this.fixtureA.isSensor

    private val Contact.isSensorB: Boolean
        get() = this.fixtureB.isSensor

    override fun beginContact(contact: Contact) {
        val entityA = contact.fixtureA.entity
        val entityB = contact.fixtureB.entity
        val isEntityATiledCollisionSensor = entityA in tiledCmps && contact.fixtureA.isSensor
        val isEntityBCollisionFixture = entityB in collisionCmps && !contact.fixtureB.isSensor
        val isEntityBTiledCollisionSensor = entityB in tiledCmps && contact.fixtureA.isSensor
        val isEntityACollisionFixture = entityA in collisionCmps && !contact.fixtureA.isSensor

        when {
            isEntityATiledCollisionSensor && isEntityBCollisionFixture->{
                tiledCmps[entityA].nearbyEntities += entityB
            }
            isEntityBTiledCollisionSensor && isEntityACollisionFixture-> {
                tiledCmps[entityB].nearbyEntities += entityA
            }

        }
    }

    override fun endContact(contact: Contact) {
        val entityA = contact.fixtureA.entity
        val entityB = contact.fixtureB.entity
        val isEntityATiledCollisionSensor = entityA in tiledCmps && contact.fixtureA.isSensor
        val isEntityBTiledCollisionSensor = entityB in tiledCmps && contact.fixtureA.isSensor


        when {
            isEntityATiledCollisionSensor && !contact.fixtureB.isSensor -> {
                tiledCmps[entityA].nearbyEntities -= entityB
            }
            isEntityBTiledCollisionSensor && !contact.fixtureB.isSensor -> {
                tiledCmps[entityB].nearbyEntities -= entityA
            }
        }
    }

   // private fun Fixture.isStaticBody() = this.body.type == BodyDef.BodyType.StaticBody
  //  private fun Fixture.isDynamicBody() = this.body.type == BodyDef.BodyType.DynamicBody

    override fun preSolve(contact: Contact, oldManifold: Manifold) {
        //задаем столкновения только со статичными объектами
        contact.isEnabled = (contact.fixtureA.body.type == BodyDef.BodyType.StaticBody && contact.fixtureB.body.type == BodyDef.BodyType.DynamicBody) ||
            (contact.fixtureB.body.type == BodyDef.BodyType.StaticBody && contact.fixtureA.body.type == BodyDef.BodyType.DynamicBody)
    }



    override fun postSolve(contact: Contact?, impulse: ContactImpulse?) = Unit //не будет использоваться в приложении
}
