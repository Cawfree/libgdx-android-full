package io.github.cawfree.libgdx;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btConeShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;

import io.github.cawfree.libgdx.entity.EntityConstructor;
import io.github.cawfree.libgdx.entity.PhysicsEntity;

/**
 * @url https://xoppa.github.io/blog/using-the-libgdx-3d-physics-bullet-wrapper-part2/
 * @author Xoppa
 **/

public final class PhysicsWorld implements ApplicationListener {

    /* Static Declarations. */
    private static final short GROUND_FLAG        = (1 << 8);
    private static final short OBJECT_FLAG        = (1 << 9);
    private static final float FRAMES_PER_SECOND  = 60.0f;

    /* Object Definitions. */
    private static final String KEY_OBJECT_GROUND   = "ground";
    private static final String KEY_OBJECT_SPHERE   = "sphere";
    private static final String KEY_OBJECT_BOX      = "box";
    private static final String KEY_OBJECT_CONE     = "cone";
    private static final String KEY_OBJECT_CYLINDER = "cylinder";
    private static final String KEY_OBJECT_CAPSULE  = "capsule";

    /* Member Variables. */
    private PerspectiveCamera             mPerspectiveCamera;
    private CameraInputController         mCameraController;
    private ModelBatch                    mModelBatch;
    private Environment                   mEnvironment;
    private Model                         mModel;
    private Array<PhysicsEntity>          mInstances;
    private ArrayMap<String, EntityConstructor> mConstructors;
    private float                         mSpawnTimer;

    /* Bullet Physics Dependencies. */
    private btCollisionConfiguration mCollisionConfig;
    private btDispatcher             mDispatcher;
    private btBroadphaseInterface    mBroadphaseInterface;
    private btDynamicsWorld          mDynamicsWorld;
    private btConstraintSolver       mConstraintsSolver;

    /** Called when the 3D scene first undergoes construction. */
    @Override public final void create () {
        // Assert that we want to use Bullet Physics.
        Bullet.init();
        // Initialize Member Variables.
        this.mModelBatch  = new ModelBatch();
        this.mEnvironment = new Environment();
        // Initialize the Environment.
        this.getEnvironment().set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        this.getEnvironment().add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Declare the ModelBuilder.
        final ModelBuilder lModelBuilder = new ModelBuilder();
        lModelBuilder.begin();
        lModelBuilder.node().id = PhysicsWorld.KEY_OBJECT_GROUND;
        lModelBuilder.part(PhysicsWorld.KEY_OBJECT_GROUND,   GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.RED))).box(5f, 1f, 5f);
        lModelBuilder.node().id = PhysicsWorld.KEY_OBJECT_SPHERE;
        lModelBuilder.part(PhysicsWorld.KEY_OBJECT_SPHERE,   GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.GREEN))).sphere(1f, 1f, 1f, 10, 10);
        lModelBuilder.node().id = PhysicsWorld.KEY_OBJECT_BOX;
        lModelBuilder.part(PhysicsWorld.KEY_OBJECT_BOX,      GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.BLUE))).box(1f, 1f, 1f);
        lModelBuilder.node().id = PhysicsWorld.KEY_OBJECT_CONE;
        lModelBuilder.part(PhysicsWorld.KEY_OBJECT_CONE,     GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.YELLOW))).cone(1f, 2f, 1f, 10);
        lModelBuilder.node().id = PhysicsWorld.KEY_OBJECT_CAPSULE;
        lModelBuilder.part(PhysicsWorld.KEY_OBJECT_CAPSULE,  GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.CYAN))).capsule(0.5f, 2f, 10);
        lModelBuilder.node().id = PhysicsWorld.KEY_OBJECT_CYLINDER;
        lModelBuilder.part(PhysicsWorld.KEY_OBJECT_CYLINDER, GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.MAGENTA))).cylinder(1f, 2f, 1f, 10);

        // Build the Model. (This is a complete physical representation of the objects in our scene.)
        this.mModel        = lModelBuilder.end();
        // Allocate the Constructors.
        this.mConstructors = new ArrayMap<String, EntityConstructor>(String.class, EntityConstructor.class);

        // Initialize EntityConstructor Mapping.
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_GROUND,   new EntityConstructor(PhysicsWorld.KEY_OBJECT_GROUND,   new btBoxShape(new Vector3(2.5f, 0.5f, 2.5f)), 0f));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_SPHERE,   new EntityConstructor(PhysicsWorld.KEY_OBJECT_SPHERE,   new btSphereShape(0.5f), 1f));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_BOX,      new EntityConstructor(PhysicsWorld.KEY_OBJECT_BOX,      new btBoxShape(new Vector3(0.5f, 0.5f, 0.5f)), 1f));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_CONE,     new EntityConstructor(PhysicsWorld.KEY_OBJECT_CONE,     new btConeShape(0.5f, 2f), 1f));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_CAPSULE,  new EntityConstructor(PhysicsWorld.KEY_OBJECT_CAPSULE,  new btCapsuleShape(.5f, 1f), 1f));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_CYLINDER, new EntityConstructor(PhysicsWorld.KEY_OBJECT_CYLINDER, new btCylinderShape(new Vector3(.5f, 1f, .5f)),  1f));

        // Allocate the CollisionConfig; defines how to handle collisions within the scene.
        this.mCollisionConfig = new btDefaultCollisionConfiguration();
        // Allocate a CollisionDispatcher; this propagates collision events across the scene. We maintain a reference to ensure we may manually dispose of it later.
        this.mDispatcher = new btCollisionDispatcher(this.getCollisionConfig());
        // Allocate a BroadphaseInterface.
        this.mBroadphaseInterface = new btDbvtBroadphase();
        // Allocate the ConstraintSolver.
        this.mConstraintsSolver = new btSequentialImpulseConstraintSolver();
        // Declare the DynamicsWorld based upon the declared components.
        this.mDynamicsWorld = new btDiscreteDynamicsWorld(this.getDispatcher(), this.getBroadphaseInterface(), this.getConstraintSolver(), this.getCollisionConfig());
        // Configure the direction of Gravity in this world.
        this.getDynamicsWorld().setGravity(new Vector3(0, -9.81f, 0));
        // Register this class as the ContactListener. For some reason, there's some `static` style configuration going on.
        new ContactListener() { @Override public final boolean onContactAdded(final int pUserValue0, final int pPartId0, final int pIndex0, final boolean pIsMatch0, final int pUserValue1, final int pPartId1, final int pIndex1, final boolean pIsMatch1) {
            // Use this class' implementation.
            return PhysicsWorld.this.onContactAdded(pUserValue0, pPartId0, pIndex0, pIsMatch0, pUserValue1, pPartId1, pIndex1, pIsMatch1);
        } };

        // Allocate the Instances that will populate the 3D world.
        this.mInstances = new Array<PhysicsEntity>();
        // Allocate the Floor.
        final PhysicsEntity lFloorObject = this.getConstructors().get(PhysicsWorld.KEY_OBJECT_GROUND).construct(this.getModel());
        // Define the Collision Flags.
        lFloorObject.mBody.setCollisionFlags(lFloorObject.mBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
        // Register the Floor as a 3D physics instance.
        this.getInstances().add(lFloorObject);
        // Register the Floor as a rigid mBody; it's a persistent entity.
        this.getDynamicsWorld().addRigidBody(lFloorObject.mBody);
        // Configure the Floor's Callbacks.
        lFloorObject.mBody.setContactCallbackFlag(PhysicsWorld.GROUND_FLAG);
        lFloorObject.mBody.setContactCallbackFilter(0);
        lFloorObject.mBody.setActivationState(Collision.DISABLE_DEACTIVATION);
    }

    /** Called when Contact has been detected. */
    public final boolean onContactAdded(final int pUserValue0, final int pPartId0, final int pIndex0, final boolean pIsMatch0, final int pUserValue1, final int pPartId1, final int pIndex1, final boolean pIsMatch1) {
        // Are we matching on 0?
        if(pIsMatch0) {
            // Update the Color.
            ((ColorAttribute)this.getInstances().get(pUserValue0).materials.get(0).get(ColorAttribute.Diffuse)).color.set(Color.WHITE);
        }
        // Are we matching on 1?
        if (pIsMatch1) {
            // Update the Color.
            ((ColorAttribute)this.getInstances().get(pUserValue1).materials.get(0).get(ColorAttribute.Diffuse)).color.set(Color.WHITE);
        }
        // Assert that we've handled the Contact.
        return true;
    }

    /** Spawns a random shape within the 3D scene. */
    private final void spawn (final Model pModel) {
        // Allocate a new PhysicsEntity.
        final PhysicsEntity lPhysicsEntity = this.getConstructors().values[1 + MathUtils.random(this.getConstructors().size - 2)].construct(pModel);
        // Configure a random angle for the Object.
        lPhysicsEntity.transform.setFromEulerAngles(MathUtils.random(360f), MathUtils.random(360f), MathUtils.random(360f));
        lPhysicsEntity.transform.trn(MathUtils.random(-2.5f, 2.5f), 9f, MathUtils.random(-2.5f, 2.5f));
        lPhysicsEntity.mBody.proceedToTransform(lPhysicsEntity.transform);
        lPhysicsEntity.mBody.setUserValue(this.getInstances().size);
        lPhysicsEntity.mBody.setCollisionFlags(lPhysicsEntity.mBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
        this.getInstances().add(lPhysicsEntity);
        // Add the PhysicsEntity's mBody as a Rigid Body.
        this.getDynamicsWorld().addRigidBody(lPhysicsEntity.mBody);
        // Configure the Callbacks.
        lPhysicsEntity.mBody.setContactCallbackFlag(PhysicsWorld.OBJECT_FLAG);
        lPhysicsEntity.mBody.setContactCallbackFilter(PhysicsWorld.GROUND_FLAG);
    }

//    float angle, speed = 90f;

    /** Creates a PerspectiveCamera for the Scene. */
    private static final PerspectiveCamera getPerspectiveCamera(final int pWidth, final int pHeight) {
        // Allocate a PerspectiveCamera.
        final PerspectiveCamera lPerspectiveCamera = new PerspectiveCamera(67, pWidth, pHeight);
        // Configure the Parameters for the PerspectiveCamera.
        lPerspectiveCamera.position.set(3f, 7f, 10f);
        lPerspectiveCamera.lookAt(0, 4f, 0);
        // How close an Object can get before obscurity.
        lPerspectiveCamera.near = 1f;
        // How far an Object may stray from the camera position before clipping.
        lPerspectiveCamera.far = 300f;
        // Update the Camera.
        lPerspectiveCamera.update();
        // Return the PerspectiveCamera.
        return lPerspectiveCamera;
    }

    /** Handle rendering. */
    @Override public final void render () {
        // Compute how much to elapse the simulation by.
        final float lStep = this.getSimulationStep();

//        angle = (angle + lStep * speed) % 360f;
//        this.getInstances().get(0).transform.setTranslation(0, MathUtils.sinDeg(angle) * 2.5f, 0f);

        this.getDynamicsWorld().stepSimulation(lStep, 5, 1f / 60f);

        if ((mSpawnTimer -= lStep) < 0) {
            spawn(this.getModel());
            mSpawnTimer = 0.5f;
        }

        // Update the CameraController.
        this.getCameraController().update();

        // Assert the Background Color.
        Gdx.gl.glClearColor(0.3f, 0.3f, 0.3f, 1.f);
        // Clear the screen in preparation for re-rendering.
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        // Begin Rendering the Model Batch. (Batch drawing greatly increases the speed of rendering.)
        this.getModelBatch().begin(this.getPerspectiveCamera());
        // Render the Instances.
        this.getModelBatch().render(this.getInstances(), this.getEnvironment());
        // Assert that we've finished rendering using the ModelBatch.
        this.getModelBatch().end();
    }

    /** Handle when the screen is resized. (Useful for changes in screen orientation on Android.) */
    @Override public final void resize(final int pWidth, final int pHeight) {
        // Reassign the PerspectiveCamera.
        this.setPerpectiveCamera(PhysicsWorld.getPerspectiveCamera(pWidth, pHeight));
        // Update the CameraController.
        this.setCameraController(new CameraInputController(this.getPerspectiveCamera()));
        // Register the Input Processor.
        Gdx.input.setInputProcessor(this.getCameraController());
    }

    /** Handles destruction of the 3D scene. */
    @Override public final void dispose () {
        // Iterate the Instances.
        for(final PhysicsEntity lPhysicsEntity : this.getInstances()) {
            // Dispose of the PhysicsEntity.
            lPhysicsEntity.dispose();
        }

        // Iterate the Constructors.
        for(final EntityConstructor lConstructor : this.getConstructors().values()) {
            // Dispose of the EntityConstructor.
            lConstructor.dispose();
        }

        // Empty the Constructors.
        this.getInstances().clear();
        this.getConstructors().clear();

        // Dispose of dependencies.
        this.getDynamicsWorld().dispose();
        this.getConstraintSolver().dispose();
        this.getBroadphaseInterface().dispose();
        this.getDispatcher().dispose();
        this.getCollisionConfig().dispose();
        this.getConstraintSolver().dispose();
        this.getModelBatch().dispose();
        this.getModel().dispose();
    }

    /* Unused Overrides. */
    @Override public final void  pause() { /* Do nothing. */ }
    @Override public final void resume() { /* Do nothing. */ }

    /** Computes the elapsed time in the scene; render either the animation step or the time that has elapsed to ensure smooth display. */
    private final float getSimulationStep() {
        // Return the minimum step to apply, between either the Frames Per Second or the time that's elapsed.
        return Math.min(1.0f / PhysicsWorld.FRAMES_PER_SECOND, Gdx.graphics.getDeltaTime());
    }

    /* Getters. */
    private final void setCameraController(final CameraInputController pCameraInputController) {
        this.mCameraController = pCameraInputController;
    }

    private final CameraInputController getCameraController() {
        return this.mCameraController;
    }

    private final ArrayMap<String, EntityConstructor> getConstructors() {
        return this.mConstructors;
    }

    private final Environment getEnvironment() {
        return this.mEnvironment;
    }

    private final void setPerpectiveCamera(final PerspectiveCamera pPerspectiveCamera) {
        this.mPerspectiveCamera = pPerspectiveCamera;
    }

    private final PerspectiveCamera getPerspectiveCamera() {
        return this.mPerspectiveCamera;
    }

    private final btCollisionConfiguration getCollisionConfig() {
        return this.mCollisionConfig;
    }

    private final Array<PhysicsEntity> getInstances() {
        return this.mInstances;
    }

    private final btDispatcher getDispatcher() {
        return this.mDispatcher;
    }

    private final btBroadphaseInterface getBroadphaseInterface() {
        return this.mBroadphaseInterface;
    }

    private final btConstraintSolver getConstraintSolver() {
        return this.mConstraintsSolver;
    }

    private final btDynamicsWorld getDynamicsWorld() {
        return this.mDynamicsWorld;
    }

    private final Model getModel() {
        return this.mModel;
    }

    private final ModelBatch getModelBatch() {
        return this.mModelBatch;
    }

}