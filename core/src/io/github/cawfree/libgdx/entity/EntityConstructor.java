package io.github.cawfree.libgdx.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Disposable;

/** A class which defines the configuration of an Entity. */
public class EntityConstructor implements Disposable {

    /* Member Variables. */
    private final String                                  mNode;
    private final btCollisionShape                        mCollisionShape;
    private final btRigidBody.btRigidBodyConstructionInfo mConstructionInfo;

    /** Constructor. */
    public EntityConstructor(final String pNode, final btCollisionShape pCollisionShape, final float pMass) {
        // Initialize Member Variables.
        this.mNode           = pNode;
        this.mCollisionShape = pCollisionShape;
        // Declare the LocalInertia.
        final Vector3 lLocalInertia = new Vector3(0.0f, 0.0f, 0.0f);
        // Is there a valid mass?
        if(pMass > 0f) {
            // Assign the LocalInteria.
            pCollisionShape.calculateLocalInertia(pMass, lLocalInertia);
        }
        // Assign the ConstructionInfo for this style of Object.
        this.mConstructionInfo = new btRigidBody.btRigidBodyConstructionInfo(pMass, null, pCollisionShape, lLocalInertia);
    }

    /** Define the disposal operation. */
    @Override public final void dispose () {
        // Dispose of dependencies.
        this.getCollisionShape().dispose();
        this.getConstructionInfo().dispose();
    }

    /** Creates a PhysicsEntity based on the configuration of the Constructor. */
    public final PhysicsEntity construct(final Model pModel) {
        // Allocate a PhysicsEntity.
        return new PhysicsEntity(pModel, this.getNode(), this.getConstructionInfo());
    }

    /* Getters. */
    private final String getNode() {
        return this.mNode;
    }

    private final btCollisionShape getCollisionShape() {
        return this.mCollisionShape;
    }

    private btRigidBody.btRigidBodyConstructionInfo getConstructionInfo() {
        return this.mConstructionInfo;
    }

}