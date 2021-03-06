package io.github.cawfree.libgdx;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;

import io.github.cawfree.libgdx.entity.PhysicsEntity;

/**
 * @url https://xoppa.github.io/blog/using-the-libgdx-3d-physics-bullet-wrapper-part2/
 * @author Xoppa
 **/

public final class PhysicsWorld implements ApplicationListener, InputProcessor {

    /* Configurations. */
    private static final boolean RENDER_DEBUG = false;

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
    public  static final String KEY_OBJECT_SHIP     = "ship";
    public  static final String KEY_OBJECT_SPINNER  = "spinner";

    /* Asset Definitions. */
    private static final String PATH_ASSET_SHIP     = "ship/ship.g3db";
    private static final String PATH_ASSET_SPINNER  = "fidget/fidget_spinner.g3db";
    private static final String PATH_ASSET_LOGO     = "texture/badlogic.jpg";

    /* Member Variables. */
    private PerspectiveCamera                       mPerspectiveCamera;
    private CameraInputController                   mCameraController;
    private ModelBatch                              mModelBatch;
    private SpriteBatch                             mSpriteBatch;
    private Environment                             mEnvironment;
    private Model                                   mModel;
    private Array<PhysicsEntity>                    mInstances;
    private ArrayMap<String, PhysicsEntity.Builder> mConstructors;
    private float                                   mSpawnTimer;
    private AssetManager                            mAssetManager;
    private DebugDrawer                             mDebugDrawer;

    /* Bullet Physics Dependencies. */
    private btCollisionConfiguration mCollisionConfig;
    private btDispatcher             mDispatcher;
    private btBroadphaseInterface    mBroadphaseInterface;
    private btDynamicsWorld          mDynamicsWorld;
    private btConstraintSolver       mConstraintsSolver;
    private ClosestRayResultCallback mClosestRayResultCallback;

    private Texture                  mTexture;

    /** Constructor. */
    public PhysicsWorld() { }

    /** Called when the 3D scene first undergoes construction. */
    @Override public final void create () {
        // Assert that we want to use Bullet Physics.
        Bullet.init();
        // Initialize Member Variables.
        this.mModelBatch   = new ModelBatch();
        this.mSpriteBatch  = new SpriteBatch();
        this.mEnvironment  = new Environment();
        this.mAssetManager = new AssetManager();
        this.mInstances    = new Array<PhysicsEntity>();
        this.mConstructors = new ArrayMap<String, PhysicsEntity.Builder>(String.class, PhysicsEntity.Builder.class);
        this.mDebugDrawer  = new DebugDrawer();
        // Configure the DebugDrawer.
        this.getDebugDrawer().setDebugMode(btIDebugDraw.DebugDrawModes.DBG_MAX_DEBUG_DRAW_MODE);
        // Initialize the Environment.
        this.getEnvironment().set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        this.getEnvironment().add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        // Configure the AssetManager.
        this.getAssetManager().load(PhysicsWorld.PATH_ASSET_SHIP,    Model.class);
        this.getAssetManager().load(PhysicsWorld.PATH_ASSET_SPINNER, Model.class);
        // Fetch the Texture.
        this.mTexture         = new Texture(PhysicsWorld.PATH_ASSET_LOGO);
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
        // Allocate the ClosestRayResultCallback.
        this.mClosestRayResultCallback = new ClosestRayResultCallback(Vector3.Zero, Vector3.Z);
        // Configure the direction of Gravity in this world.
        this.getDynamicsWorld().setGravity(new Vector3(0, -9.81f, 0));
//        this.getDynamicsWorld().setGravity(new Vector3(0, 0.0f, 0));
        // Register this class as the ContactListener. For some reason, there's some `static` style configuration going on.
        new ContactListener() { @Override public final boolean onContactAdded(final int pUserValue0, final int pPartId0, final int pIndex0, final boolean pIsMatch0, final int pUserValue1, final int pPartId1, final int pIndex1, final boolean pIsMatch1) {
            // Use this class' implementation.
            return PhysicsWorld.this.onContactAdded(pUserValue0, pPartId0, pIndex0, pIsMatch0, pUserValue1, pPartId1, pIndex1, pIsMatch1);
        } };
        // Update the Assets.
        this.getAssetManager().update();
        // Wait until all Assets have loaded.
        this.getAssetManager().finishLoading();

        // Fetch the Spinner Model, and remove the transforms that were generated from the Blender model. (This ensures consistency between the physics object and the graphical instances.)
        final Model lModel = PhysicsEntity.unblend(this.getAssetManager().get(PhysicsWorld.PATH_ASSET_SPINNER, Model.class));
        // Declare the ModelBuilder.
        final ModelBuilder lModelBuilder = new ModelBuilder();
        // Assert that we're beginning to build the Model.
        lModelBuilder.begin();
        // Initialize Builder Mapping.
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_GROUND,   (new PhysicsEntity.Builder.Cube(PhysicsWorld.KEY_OBJECT_GROUND, new Vector3(2.5f, 0.5f, 2.5f), Color.FOREST, 0.0f)).build(lModelBuilder));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_SPHERE,   (new PhysicsEntity.Builder.Sphere(PhysicsWorld.KEY_OBJECT_SPHERE, 1f, 30, Color.CHARTREUSE, 1.0f).build(lModelBuilder)));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_BOX,      (new PhysicsEntity.Builder.Cube(PhysicsWorld.KEY_OBJECT_BOX, new Vector3(0.5f, 0.5f, 0.5f), Color.CORAL, 1.0f)).build(lModelBuilder));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_CONE,     (new PhysicsEntity.Builder.Cone(PhysicsWorld.KEY_OBJECT_CONE, 0.5f, 2.5f, 10, Color.FIREBRICK, 1.0f).build(lModelBuilder)));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_CAPSULE,  (new PhysicsEntity.Builder.Capsule(PhysicsWorld.KEY_OBJECT_CAPSULE, 0.5f, 1.0f, 10, Color.GOLDENROD, 1.0f)).build(lModelBuilder));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_CYLINDER, (new PhysicsEntity.Builder.Cylinder(PhysicsWorld.KEY_OBJECT_CYLINDER, new Vector3(0.5f, 1.0f, 0.5f), 10, Color.SALMON, 1.0f)).build(lModelBuilder));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_SHIP,     (new PhysicsEntity.Builder.Generic(PhysicsWorld.KEY_OBJECT_SHIP, this.getAssetManager().get(PhysicsWorld.PATH_ASSET_SHIP, Model.class), true, 1.0f).build(lModelBuilder)));
        this.getConstructors().put(PhysicsWorld.KEY_OBJECT_SPINNER,  (new PhysicsEntity.Builder.Generic(PhysicsWorld.KEY_OBJECT_SPINNER, lModel, true, 1.0f).build(lModelBuilder)));
        // Build the Model. (This is a complete physical representation of the objects in our scene.)
        this.setModel(lModelBuilder.end());
        // Allocate the Floor.
        final PhysicsEntity lFloorObject = this.getConstructors().get(PhysicsWorld.KEY_OBJECT_GROUND).build(this.getModel());
        // Define the Collision Flags.
        lFloorObject.getBody().setCollisionFlags(lFloorObject.getBody().getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
        // Register the Floor as a 3D physics instance.
        this.getInstances().add(lFloorObject);
        // Register the Floor as a rigid mBody; it's a persistent entity.
        this.getDynamicsWorld().addRigidBody(lFloorObject.getBody());
        // Assign the DynamicsWorld the DebugDrawer.
        this.getDynamicsWorld().setDebugDrawer(this.getDebugDrawer());
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
    private final PhysicsEntity onSpawn(final Model pModel) {
        // Determine the index of the random item to generate. (Offset by 1 so we don't duplicate the floor.)
        final int lIndex = (1 + MathUtils.random(this.getConstructors().size - 2));
        // Allocate a new PhysicsEntity.
        final PhysicsEntity lPhysicsEntity = this.getConstructors().values[lIndex].build(pModel);
        // Rotate the entity.
        lPhysicsEntity.transform.setFromEulerAngles(MathUtils.random(360f), MathUtils.random(360f), MathUtils.random(360f));
        // Configure a random position for the Object.
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
        // Return the PhysicsEntity.
        return lPhysicsEntity;
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
        this.getDynamicsWorld().stepSimulation(lStep, 5, 1.0f / PhysicsWorld.FRAMES_PER_SECOND);
        // Update the CameraController.
        this.getCameraController().update();
        // Assert the Background Color.
        Gdx.gl.glClearColor(0.3f, 0.3f, 0.3f, 1.f);
        // Clear the screen in preparation for re-rendering.
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        /** @Cawfree: Spawn timing operation. */
        if ((mSpawnTimer -= lStep) < 0) {
            this.onSpawn(this.getModel());
            mSpawnTimer = PhysicsWorld.DELAY_RESPAWN_MS;
        }

        // Begin Rendering the Model Batch. (Batch drawing greatly increases the speed of rendering.)
        this.getModelBatch().begin(this.getPerspectiveCamera());
        // Render the Instances.
        this.getModelBatch().render(this.getInstances(), this.getEnvironment());
        // Assert that we've finished rendering using the ModelBatch.
        this.getModelBatch().end();

        // Perform Debug Rendering?
        if(PhysicsWorld.RENDER_DEBUG) {
            // Prepare Debug Drawing.
            this.getDebugDrawer().begin(this.getPerspectiveCamera());
            // Render the Debugging Information.
            this.getDynamicsWorld().debugDrawWorld();
            // Finish Debug Drawing.
            this.getDebugDrawer().end();
        }

        // Begin rendering Sprites.
        this.getSpriteBatch().begin();
        // Draw the Texture.
        this.getSpriteBatch().draw(this.getTexture(), 0, 0);
        // Finish ending Sprites.
        this.getSpriteBatch().end();
    }

    private static final float RAY_DISTANCE_MAX = 50.0f;

    /** Handle when the screen is pressed down on. */
    @Override public final boolean touchDown(final int pScreenX, final int pScreenY, final int pPointer, final int pButton) {
        // Allocate the Ray.
        final Ray lRay = this.getPerspectiveCamera().getPickRay(pScreenX, pScreenY);
        // Define calculation dependencies.
        final Vector3 lFromRay = new Vector3();
        final Vector3 lToRay   = new Vector3();
        // Define the Origin of the FromRay.
        lFromRay.set(lRay.origin);
        // Define the Destination of the ToRay.
        lToRay.set(lRay.direction).scl(PhysicsWorld.RAY_DISTANCE_MAX).add(lFromRay);
        // Re-initiailize the ClosestRayResultCallback, since it is re-used.
        this.getClosestRayResultCallback().setCollisionObject(null);
        this.getClosestRayResultCallback().setClosestHitFraction(1f);
        // Update the Ray Params.
        this.getClosestRayResultCallback().setRayFromWorld(lFromRay);
        this.getClosestRayResultCallback().setRayToWorld(lToRay);
        // Perform the RayTest.
        this.getDynamicsWorld().rayTest(lFromRay, lToRay, this.getClosestRayResultCallback());
        // Has the Ray hit an Object?
        if(this.getClosestRayResultCallback().hasHit()) {
            // Fetch the CollisionObject.
            final btCollisionObject lCollisionObject = this.getClosestRayResultCallback().getCollisionObject();
            System.out.println("Found collision.");
        }
        // Consume the Event.
        return true;
    }

//    /** Performs a raycast operation on the scene. */
//    public final PhysicsEntity cast(final Ray pRay) {
//        // Define the Distance.
//              float         lDistance      = -1;
//        // Define the BoundingBox.
//        final BoundingBox   lBoundingBox   = new BoundingBox();
//        // Define the Vector we use for computing the RayCast within a PhysicsEntity's domain.
//        final Vector3       lCenter        = new Vector3();
//        // Declare the return reference.
//              PhysicsEntity lPhysicsEntity = null;
//        // Iterate the PhysicsEntities.
//        for(int i = 0; i < this.getInstances().size; i++) {
//            // Fetch the next PhysicsEntity we'll be computing intersection for.
//            final PhysicsEntity lIntersection = this.getInstances().get(i);
//            // Fetch the PhysicsEntity's Translation.
//            lIntersection.transform.getTranslation(lCenter);
//            // Calculate the Bounding Box.
//            lIntersection.calculateBoundingBox(lBoundingBox);
//            // Apply the BoundingBox's offset to the Centre.
//            lCenter.add(lBoundingBox.getCenter(new Vector3()));
//            // Compute the distance between the Ray and the point of intersection.
//            float lDistance2 = pRay.origin.dst2(lCenter);
//            // Does the Raycast intersect with the PhysicsEntity?
//            if(lDistance >= 0f && lDistance2 > lDistance) {
//                // Ignore the rest of this iteration.
//                continue;
//            }
//            // Does the instance intersect?
//            if(lIntersection.isIntersectingWith(pRay, lCenter, lBoundingBox)) {
//                // Track the Intersection.
//                lPhysicsEntity = lIntersection;
//                // Overwrite the Distance.
//                lDistance = lDistance2;
//            }
//        }
//        // Return the PhysicsEntity.
//        return lPhysicsEntity;
//    }

    /** Handle when the screen is resized. (Useful for changes in screen orientation on Android.) */
    @Override public final void resize(final int pWidth, final int pHeight) {
        // Reassign the PerspectiveCamera.
        this.setPerpectiveCamera(PhysicsWorld.getPerspectiveCamera(pWidth, pHeight));
        // Update the CameraController.
        this.setCameraController(new CameraInputController(this.getPerspectiveCamera()));
        // Configure Input Multiplexing.
        Gdx.input.setInputProcessor(new InputMultiplexer(this, this.getCameraController()));
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
        this.getTexture().dispose();
        this.getSpriteBatch().dispose();
        this.getAssetManager().dispose();
        this.getClosestRayResultCallback().dispose();
    }

    /* Unused Overrides. */
    @Override public final boolean      keyDown(final int pKeyCode) { return false; }
    @Override public final boolean        keyUp(final int pKeyCode) { return false; }
    @Override public final boolean     keyTyped(final char pCharacter) { return false; }
    @Override public final boolean      touchUp(final int pScreenX, final int pScreenY, final int pPointer, final int pButton) { return false; }
    @Override public final boolean touchDragged(final int pScreenX, final int pScreenY, final int pPointer) { return false; }
    @Override public final boolean   mouseMoved(final int pScreenX, final int pScreenY) { return false; }
    @Override public final boolean     scrolled(final int pAmount) { return false; }
    @Override public final void           pause() { }
    @Override public final void          resume() { }

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

    private final AssetManager getAssetManager() {
        return this.mAssetManager;
    }

    private final Texture getTexture() {
        return this.mTexture;
    }

    private final SpriteBatch getSpriteBatch() {
        return this.mSpriteBatch;
    }

    private final DebugDrawer getDebugDrawer() {
        return this.mDebugDrawer;
    }

    private final ClosestRayResultCallback getClosestRayResultCallback() {
        return this.mClosestRayResultCallback;
    }

}