package io.github.cawfree.libgdx.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Disposable;

/** Tracks the physical implementation of an Entity. */
public class PhysicsEntity extends ModelInstance implements Disposable {

    /* Member Variables. */
    private final btRigidBody   mBody;
    private final btMotionState mMotionState;

    /** EntityConstructor. */
    public PhysicsEntity(final Model pModel, final String pNode, final btRigidBody.btRigidBodyConstructionInfo pConstructionInfo) {
        super(pModel, pNode);
        // Allocate Member Variables.
        this.mMotionState = new btMotionState() {
            /** Returns the World Transform for this Entity. */
            @Override public final void getWorldTransform(final Matrix4 worldTrans) { worldTrans.set(PhysicsEntity.this.transform); }
            /** Applies the World Transform for this Entity. */
            @Override public final void setWorldTransform(final Matrix4 worldTrans) { PhysicsEntity.this.transform.set(worldTrans); }
        };
        // Allocate the Body.
        this.mBody = new btRigidBody(pConstructionInfo);
        // Define the MotionState.
        this.getBody().setMotionState(this.getMotionState());
    }

    /** Define the disposal operations. */
    @Override public final void dispose () {
        // Dispose of the dependencies.
        this.getBody().dispose();
        this.getMotionState().dispose();
    }

    /* Getters. */
    public final btRigidBody getBody() {
        return this.mBody;
    }

    private final btMotionState getMotionState() {
        return this.mMotionState;
    }

}