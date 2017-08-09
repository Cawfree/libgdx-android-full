package io.github.cawfree.libgdx.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.CollisionJNI;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btConeShape;
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
import com.badlogic.gdx.physics.bullet.collision.btPolyhedralConvexShape;
import com.badlogic.gdx.physics.bullet.collision.btShapeHull;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Disposable;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/** Tracks the physical implementation of an Entity. */
public class PhysicsEntity extends ModelInstance implements Disposable {

    /** Creates the fastest kind of arbitrary shape from a model, by defining the smallest number of vertices that enclose the vertices. */
    public static final btConvexHullShape createConvexHullShape (final Model pModel, final boolean pIsOptimized) {
        // Fetch the Mesh.
        final Mesh              lMesh            = pModel.meshes.get(0);
        // Allocate the ConvexHullShape.
        final btConvexHullShape lConvexHullShape = new btConvexHullShape(lMesh.getVerticesBuffer(), lMesh.getNumVertices(), lMesh.getVertexSize());
        // Are we not performing the optimization step?
        if(!pIsOptimized) {
            // Return the ConvexHullShape.
            return lConvexHullShape;
        }
        // Otherwise, allocate a ShapeHull about the ConvexHullShape.
        final btShapeHull lShapeHull = new btShapeHull(lConvexHullShape);
        // Construct the Hull about the maximum extent of the shape.
        lShapeHull.buildHull(lConvexHullShape.getMargin());
        // Fetch the OptimizedShape.
        final btConvexHullShape lOptimizedShape = new btConvexHullShape(lShapeHull);
        // Dispose of the shapes that we're no longer using.
        lConvexHullShape.dispose();
        lShapeHull.dispose();
        // Return the OptimizedShape.
        return lOptimizedShape;
    }

    /** Applies the Factory pattern for constructing PhysicsEntities. */
    public static class Builder <T extends btCollisionShape> implements Disposable {

        /** Generic Model Builder. Note, that this applies physics wrapping the Object in a bounding box; this doesn't follow the contours of the mesh. */
        public static final class Generic extends Builder<btCollisionShape> {
            /* Member Variables. */
            private final Model mModel;
            /** Constructor. */
            public Generic(final String pNode, final Model pModel, final boolean pIsOptimized, final float pMass) {
                // Initialize the Parent.
                super(pNode, createConvexHullShape(pModel, pIsOptimized), pMass);
                // Initialize Member Variables.
                this.mModel = pModel;
            }
            /** Define collision model construction. */
            @Override public final Builder build(final ModelBuilder pModelBuilder) {
                // Implement the Parent.
                super.build(pModelBuilder);
                // Update the ModelBuilder.
                pModelBuilder.node(this.getNode(), this.getModel());
                // Return the Reference.
                return this;
            }
            /* Getters. */
            private final Model getModel() { return this.mModel; }
        }

        /** Cylinder Builder. */
        public static final class Cylinder extends Builder<btCylinderShape> {
            /* Member Variables. */
            private final Vector3 mVector3;
            private final Color   mColor;
            private final int     mDivisions;
            /** Constructor. */
            public Cylinder(final String pNode, final Vector3 pVector3, final int pDivisions, final Color pColor, final float pMass) {
                // Initialize the Parent.
                super(pNode, new btCylinderShape(pVector3), pMass);
                // Initialize Member Variables.
                this.mVector3   = pVector3;
                this.mColor     = pColor;
                this.mDivisions = pDivisions;
            }
            /** Define collision model construction. */
            @Override public final Builder build(final ModelBuilder pModelBuilder) {
                // Implement the Parent.
                super.build(pModelBuilder);
                // Update the ModelBuilder.
                pModelBuilder.part(this.getNode(), GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(this.getColor()))).cylinder(this.getVector3().x * 2, this.getVector3().y * 2, this.getVector3().z * 2, this.getDivisions());
                // Return the Reference.
                return this;
            }
            /* Getters. */
            private final Vector3   getVector3() { return this.mVector3;   }
            private final Color       getColor() { return this.mColor;     }
            private final int     getDivisions() { return this.mDivisions; }
        }

        /** Capsule Builder. */
        public static final class Capsule extends Builder<btCapsuleShape> {
            /* Member Variables. */
            private final float mRadius;
            private final float mHeight;
            private final int   mDivisions;
            private final Color mColor;
            /** Constructor. */
            public Capsule(final String pNode, final float pRadius, final float pHeight, final int pDivisions, final Color pColor, final float pMass) {
                // Implement the Parent.
                super(pNode, new btCapsuleShape(pRadius, pHeight), pMass);
                // Initialize Member Variables.
                this.mRadius    = pRadius;
                this.mHeight    = pHeight;
                this.mDivisions = pDivisions;
                this.mColor     = pColor;
            }
            /** Define collision model construction. */
            @Override public final Builder build(final ModelBuilder pModelBuilder) {
                // Implement the Parent.
                super.build(pModelBuilder);
                // Update the ModelBuilder.
                pModelBuilder.part(this.getNode(), GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(this.getColor()))).capsule(this.getRadius(), this.getHeight() * 2, this.getDivisions());
                // Return the reference.
                return this;
            }
            /* Getters. */
            private final float    getRadius() { return this.mRadius;    }
            private final float    getHeight() { return this.mHeight;    }
            private final Color     getColor() { return this.mColor;     }
            private final int   getDivisions() { return this.mDivisions; }
        }

        /** Cone Builder. */
        public static final class Cone extends Builder<btConeShape> {
            /* Member Variables. */
            private final float mRadius;
            private final float mHeight;
            private final int   mDivisions;
            private final Color mColor;
            /** Constructor. */
            public Cone(final String pNode, final float pRadius, final float pHeight, final int pDivisions, final Color pColor, final float pMass) {
                // Implement the Parent.
                super(pNode, new btConeShape(pRadius, pHeight), pMass);
                // Initialize Member Variables.
                this.mRadius    = pRadius;
                this.mHeight    = pHeight;
                this.mDivisions = pDivisions;
                this.mColor     = pColor;
            }
            /** Define collision model construction. */
            @Override public final Builder build(final ModelBuilder pModelBuilder) {
                // Implement the Parent.
                super.build(pModelBuilder);
                // Prepare the Builder.
                pModelBuilder.part(this.getNode(), GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(this.getColor()))).cone(this.getRadius() * 2, this.getHeight(), this.getRadius() * 2, this.getDivisions());
                // Return the reference.
                return this;
            }
            /* Getters. */
            private final float    getRadius() { return this.mRadius;    }
            private final float    getHeight() { return this.mHeight;    }
            private final Color     getColor() { return this.mColor;     }
            private final int   getDivisions() { return this.mDivisions; }
        }

        /** Sphere Builder. */
        public static final class Sphere extends Builder<btSphereShape> {
            /* Member Variables. */
            private final float mRadius;
            private final Color mColor;
            private final int   mDivisions;
            /** Constructor. */
            public Sphere(final String pNode, final float pRadius, final int pDivisions, final Color pColor, final float pMass) {
                // Buffer the Characteristics.
                super(pNode, new btSphereShape(pRadius), pMass);
                // Initialize Member Variables.
                this.mRadius    = pRadius;
                this.mDivisions = pDivisions;
                this.mColor     = pColor;
            }
            /** Define collision model construction. */
            @Override public final Builder build(final ModelBuilder pModelBuilder) {
                // Implement the Parent.
                super.build(pModelBuilder);
                // Prepare the Builder.
                pModelBuilder.part(this.getNode(), GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(this.getColor()))).sphere((this.getRadius() * 2), (this.getRadius() * 2), (this.getRadius() * 2), this.getDivisions(), this.getDivisions());
                // Return the reference.
                return this;
            }
            /* Getters. */
            private final float    getRadius() { return this.mRadius;    }
            private final Color     getColor() { return this.mColor;     }
            private final int   getDivisions() { return this.mDivisions; }
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