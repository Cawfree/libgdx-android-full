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
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;

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
    private static final float DELAY_RESPAWN_MS   = 1.5f;

    /* Object Definitions. */
    public  static final String KEY_OBJECT_GROUND   = "ground";
    public  static final String KEY_OBJECT_SPHERE   = "sphere";
    public  static final String KEY_OBJECT_BOX      = "box";
    public  static final String KEY_OBJECT_CONE     = "cone";
    public  static final String KEY_OBJECT_CYLINDER = "cylinder";
    public  static final String KEY_OBJECT_CAPSULE  = "capsule";

    /* Member Variables. */
    private PerspectiveCamera                       mPerspectiveCamera;
    private CameraInputController                   mCameraController;
    private ModelBatch                              mModelBatch;
    private Environment                             mEnvironment;
    private Model                                   mModel;
    private Array<PhysicsEntity>                    mInstances;
    private ArrayMap<String, PhysicsEntity.Builder> mConstructors;
    private float                                   mSpawnTimer;

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

        // Allocate the Constructors.
        this.mConstructors = new ArrayMap<String, PhysicsEntity.Builder>(String.class, PhysicsEntity.Builder.class);

        // Assert that we're beginning to build the Model.
        lModelBuilder.begin();

        // Initialize Builder Mapping.
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_GROUND,   (new PhysicsEntity.Builder.Cube(PhysicsWorld.KEY_OBJECT_GROUND, new Vector3(2.5f, 0.5f, 2.5f), Color.FOREST, 0.0f)).build(lModelBuilder));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_SPHERE,   (new PhysicsEntity.Builder.Sphere(PhysicsWorld.KEY_OBJECT_SPHERE, 0.5f, Color.CHARTREUSE, 1.0f).build(lModelBuilder)));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_BOX,      (new PhysicsEntity.Builder.Cube(PhysicsWorld.KEY_OBJECT_BOX, new Vector3(0.5f, 0.5f, 0.5f), Color.CORAL, 1.0f)).build(lModelBuilder));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_CONE,     (new PhysicsEntity.Builder.Cone(PhysicsWorld.KEY_OBJECT_CONE, 0.5f, 2.5f, Color.FIREBRICK, 1.0f).build(lModelBuilder)));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_CAPSULE,  (new PhysicsEntity.Builder.Capsule(PhysicsWorld.KEY_OBJECT_CAPSULE, 0.5f, 1.0f, Color.GOLDENROD, 1.0f)).build(lModelBuilder));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_CYLINDER, (new PhysicsEntity.Builder.Cylinder(PhysicsWorld.KEY_OBJECT_CYLINDER, new Vector3(0.5f, 1.0f, 0.5f), Color.SALMON, 1.0f)).build(lModelBuilder));

        // Build the Model. (This is a complete physical representation of the objects in our scene.)
        this.setModel(lModelBuilder.end());

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
        final PhysicsEntity lFloorObject = this.getConstructors().get(PhysicsWorld.KEY_OBJECT_GROUND).build(this.getModel());
        // Define the Collision Flags.
        lFloorObject.getBody().setCollisionFlags(lFloorObject.getBody().getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
        // Register the Floor as a 3D physics instance.
        this.getInstances().add(lFloorObject);
        // Register the Floor as a rigid mBody; it's a persistent entity.
        this.getDynamicsWorld().addRigidBody(lFloorObject.getBody());
        // Configure the Floor's Callbacks.
        lFloorObject.getBody().setContactCallbackFlag(PhysicsWorld.GROUND_FLAG);
        lFloorObject.getBody().setContactCallbackFilter(0);
        lFloorObject.getBody().setActivationState(Collision.DISABLE_DEACTIVATION);
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
        final PhysicsEntity lPhysicsEntity = this.getConstructors().values[1 + MathUtils.random(this.getConstructors().size - 2)].build(pModel);
        // Configure a random angle for the Object.
        lPhysicsEntity.transform.setFromEulerAngles(MathUtils.random(360f), MathUtils.random(360f), MathUtils.random(360f));
        lPhysicsEntity.transform.trn(MathUtils.random(-2.5f, 2.5f), 9f, MathUtils.random(-2.5f, 2.5f));
        lPhysicsEntity.getBody().proceedToTransform(lPhysicsEntity.transform);
        lPhysicsEntity.getBody().setUserValue(this.getInstances().size);
        lPhysicsEntity.getBody().setCollisionFlags(lPhysicsEntity.getBody().getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
        // Register the PhysicsEntity as an Instance.
        this.getInstances().add(lPhysicsEntity);
        // Add the PhysicsEntity's mBody as a Rigid Body.
        this.getDynamicsWorld().addRigidBody(lPhysicsEntity.getBody());
        // Configure the Callbacks; we want to detect collisions with the Floor.
        lPhysicsEntity.getBody().setContactCallbackFlag(PhysicsWorld.OBJECT_FLAG);
        lPhysicsEntity.getBody().setContactCallbackFilter(PhysicsWorld.GROUND_FLAG);
    }

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
        // Update the simulation.
        this.getDynamicsWorld().stepSimulation(lStep, 5, 1f / PhysicsWorld.FRAMES_PER_SECOND);

        if ((mSpawnTimer -= lStep) < 0) {
            spawn(this.getModel());
            mSpawnTimer = PhysicsWorld.DELAY_RESPAWN_MS;
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
        for(final PhysicsEntity.Builder lConstructor : this.getConstructors().values()) {
            // Dispose of the Builder.
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

    private final ArrayMap<String, PhysicsEntity.Builder> getConstructors() {
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

    private final void setModel(final Model pModel) {
        this.mModel = pModel;
    }

    private final Model getModel() {
        return this.mModel;
    }

    private final ModelBatch getModelBatch() {
        return this.mModelBatch;
    }

}