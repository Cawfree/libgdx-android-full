package io.github.cawfree.libgdx.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btConeShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Disposable;

import io.github.cawfree.libgdx.PhysicsWorld;

/** Tracks the physical implementation of an Entity. */
public class PhysicsEntity extends ModelInstance implements Disposable {

    /** Applies the Factory pattern for constructing PhysicsEntities. */
    public static class Builder <T extends btCollisionShape> implements Disposable {
        /** Cylinder Builder. */
        public static final class Cylinder extends Builder<btCylinderShape> {
            /* Member Variables. */
            private final Vector3 mVector3;
            private final Color   mColor;
            /** Constructor. */
            public Cylinder(final String pNode, final Vector3 pVector3, final Color pColor, final float pMass) {
                // Initialize the Parent.
                super(pNode, new btCylinderShape(pVector3), pMass);
                // Initialize Member Variables.
                this.mVector3 = pVector3;
                this.mColor   = pColor;
            }
            /** Define collision model construction. */
            @Override public final Builder build(final ModelBuilder pModelBuilder) {
                // Implement the Parent.
                super.build(pModelBuilder);
                // Update the ModelBuilder.
                pModelBuilder.part(this.getNode(), GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.MAGENTA))).cylinder(this.getVector3().x * 2, this.getVector3().y * 2, this.getVector3().z * 2, 10);
                // Return the Reference.
                return this;
            }
            /* Getters. */
            private final Vector3 getVector3() { return this.mVector3; }
            private final Color     getColor() { return this.mColor;   }
        }

        /** Capsule Builder. */
        public static final class Capsule extends Builder<btCapsuleShape> {
            /* Member Variables. */
            private final float mRadius;
            private final float mHeight;
            private final Color mColor;
            /** Constructor. */
            public Capsule(final String pNode, final float pRadius, final float pHeight, final Color pColor, final float pMass) {
                // Implement the Parent.
                super(pNode, new btCapsuleShape(pRadius, pHeight), pMass);
                // Initialize Member Variables.
                this.mRadius = pRadius;
                this.mHeight = pHeight;
                this.mColor  = pColor;
            }
            /** Define collision model construction. */
            @Override public final Builder build(final ModelBuilder pModelBuilder) {
                // Implement the Parent.
                super.build(pModelBuilder);
                // Update the ModelBuilder.
                pModelBuilder.part(this.getNode(), GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(this.getColor()))).capsule(this.getRadius(), this.getHeight() * 2, 10);
                // Return the reference.
                return this;
            }
            /* Getters. */
            private final float getRadius() { return this.mRadius; }
            private final float getHeight() { return this.mHeight; }
            private final Color  getColor() { return this.mColor;  }
        }

        /** Cone Builder. */
        public static final class Cone extends Builder<btConeShape> {
            /* Member Variables. */
            private final float mRadius;
            private final float mHeight;
            private final Color mColor;
            /** Constructor. */
            public Cone(final String pNode, final float pRadius, final float pHeight, final Color pColor, final float pMass) {
                // Implement the Parent.
                super(pNode, new btConeShape(pRadius, pHeight), pMass);
                // Initialize Member Variables.
                this.mRadius = pRadius;
                this.mHeight = pHeight;
                this.mColor  = pColor;
            }
            /** Define collision model construction. */
            @Override public final Builder build(final ModelBuilder pModelBuilder) {
                // Implement the Parent.
                super.build(pModelBuilder);
                // Prepare the Builder.
                pModelBuilder.part(this.getNode(), GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(this.getColor()))).cone(this.getRadius() * 2, this.getHeight(), this.getRadius() * 2, 10);
                // Return the reference.
                return this;
            }
            /* Getters. */
            private final float getRadius() { return this.mRadius; }
            private final float getHeight() { return this.mHeight; }
            private final Color  getColor() { return this.mColor;  }
        }

        /** Sphere Builder. */
        public static final class Sphere extends Builder<btSphereShape> {
            /* Member Variables. */
            private final float mRadius;
            private final Color mColor;
            /** Constructor. */
            public Sphere(final String pNode, final float pRadius, final Color pColor, final float pMass) {
                // Buffer the Characteristics.
                super(pNode, new btSphereShape(pRadius), pMass);
                // Initialize Member Variables.
                this.mRadius = pRadius;
                this.mColor  = pColor;
            }
            /** Define collision model construction. */
            @Override public final Builder build(final ModelBuilder pModelBuilder) {
                // Implement the Parent.
                super.build(pModelBuilder);
                // Prepare the Builder.
                pModelBuilder.part(this.getNode(), GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(this.getColor()))).sphere((this.getRadius() * 2), (this.getRadius() * 2), (this.getRadius() * 2), 10, 10);
                // Return the reference.
                return this;
            }
            /* Getters. */
            private final float getRadius() { return this.mRadius; }
            private final Color getColor()  { return this.mColor;  }
        }

        /** Cube Builder. */
        public static final class Cube extends Builder<btBoxShape> {
            /* Member Variables. */
            private final Vector3 mDim;
            private final Color   mColor;
            /**  Constructor. */
            public Cube(final String pNode, final Vector3 pDim, final Color pColor, final float pMass) {
                // Buffer the characteristics.
                super(pNode, new btBoxShape(pDim), pMass);
                // Initialize Member Variables.
                this.mDim   = pDim;
                this.mColor = pColor;
            }
            /** Define collision model construction. */
            @Override public final Builder build(final ModelBuilder pModelBuilder) {
                // Implement the Parent.
                super.build(pModelBuilder);
                // Prepare the Builder.
                pModelBuilder.part(this.getNode(), GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(this.getColor()))).box((this.getDim().x * 2), (this.getDim().y * 2), (this.getDim().z * 2));
                // Return the reference.
                return this;
            }
            /* Getters. */
            private final Vector3   getDim() { return this.mDim;   }
            private final Color   getColor() { return this.mColor; }
        }

        /* Member Variables. */
        private final String                                  mNode;
        private final T                                       mCollisionShape;
        private final btRigidBody.btRigidBodyConstructionInfo mConstructionInfo;

        /**  Constructor. */
        public Builder(final String pNode, final T pCollisionShape, final float pMass) {
            // Initialize Member Variables.
            this.mNode           = pNode;
            this.mCollisionShape = pCollisionShape;
            // Declare the LocalInertia.
            final Vector3 lLocalInertia = new Vector3(0.0f, 0.0f, 0.0f);
            // Is there a valid mass?
            if (pMass > 0f) {
                // Assign the LocalInteria.
                pCollisionShape.calculateLocalInertia(pMass, lLocalInertia);
            }
            // Assign the ConstructionInfo for this style of Object.
            this.mConstructionInfo = new btRigidBody.btRigidBodyConstructionInfo(pMass, null, pCollisionShape, lLocalInertia);
        }

        /** Configures the ModelBuilder for the Entity. */
        public Builder build(final ModelBuilder pModelBuilder) {
            // Update using the id.
            pModelBuilder.node().id = this.getNode();
            // Return the instance.
            return this;
        }

        /**  Define the disposal operation. */
        @Override public final void dispose() {
            // Dispose of dependencies.
            this.getCollisionShape().dispose();
            this.getConstructionInfo().dispose();
        }

        /** Creates a PhysicsEntity based on the configuration of the Constructor. */
        public final PhysicsEntity build(final Model pModel) {
            // Allocate a PhysicsEntity.
            return new PhysicsEntity(pModel, this.getNode(), this.getConstructionInfo());
        }

        /* Getters. */
        public final String getNode() {
            return this.mNode;
        }

        protected final T getCollisionShape() {
            return this.mCollisionShape;
        }

        private btRigidBody.btRigidBodyConstructionInfo getConstructionInfo() {
            return this.mConstructionInfo;
        }

    }

    /* Member Variables. */
    private final btRigidBody   mBody;
    private final btMotionState mMotionState;

    /** Builder. */
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