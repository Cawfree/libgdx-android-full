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
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
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
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btConeShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;

/** @see https://xoppa.github.io/blog/using-the-libgdx-3d-physics-bullet-wrapper-part2/
 * @author Xoppa */

public final class PhysicsActivity implements ApplicationListener {

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

    class MyContactListener extends ContactListener {
        @Override
        public boolean onContactAdded (int userValue0, int partId0, int index0, boolean match0, int userValue1, int partId1,  int index1, boolean match1) {
            if (match0)
                ((ColorAttribute) mInstances.get(userValue0).materials.get(0).get(ColorAttribute.Diffuse)).color.set(Color.WHITE);
            if (match1)
                ((ColorAttribute) mInstances.get(userValue1).materials.get(0).get(ColorAttribute.Diffuse)).color.set(Color.WHITE);
            return true;
        }
    }

    static class MyMotionState extends btMotionState {
        Matrix4 transform;

        @Override
        public void getWorldTransform (Matrix4 worldTrans) {
            worldTrans.set(transform);
        }

        @Override
        public void setWorldTransform (Matrix4 worldTrans) {
            transform.set(worldTrans);
        }
    }

    static class GameObject extends ModelInstance implements Disposable {
        public final btRigidBody body;
        public final MyMotionState motionState;

        public GameObject (Model model, String node, btRigidBody.btRigidBodyConstructionInfo constructionInfo) {
            super(model, node);
            motionState = new MyMotionState();
            motionState.transform = transform;
            body = new btRigidBody(constructionInfo);
            body.setMotionState(motionState);
        }

        @Override
        public void dispose () {
            body.dispose();
            motionState.dispose();
        }

        static class Constructor implements Disposable {
            public final Model model;
            public final String node;
            public final btCollisionShape shape;
            public final btRigidBody.btRigidBodyConstructionInfo constructionInfo;
            private static Vector3 localInertia = new Vector3();

            public Constructor (Model model, String node, btCollisionShape shape, float mass) {
                this.model = model;
                this.node = node;
                this.shape = shape;
                if (mass > 0f)
                    shape.calculateLocalInertia(mass, localInertia);
                else
                    localInertia.set(0, 0, 0);
                this.constructionInfo = new btRigidBody.btRigidBodyConstructionInfo(mass, null, shape, localInertia);
            }

            public GameObject construct () {
                return new GameObject(model, node, constructionInfo);
            }

            @Override
            public void dispose () {
                shape.dispose();
                constructionInfo.dispose();
            }
        }
    }

    /* Member Variables. */
    private PerspectiveCamera                        mPerspectiveCamera;
    private CameraInputController                    mCameraController;
    private ModelBatch                               mModelBatch;
    private Environment                              mEnvironment;
    private Model                                    mModel;
    private Array<GameObject>                        mInstances;
    private ArrayMap<String, GameObject.Constructor> mConstructors;
    private float                                    mSpawnTimer;

    /* Bullet Physics Dependencies. */
    private btCollisionConfiguration mCollisionConfig;
    private btDispatcher             mDispatcher;
    private MyContactListener        mContactListener;
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
        // Allocate a PerspectiveCamera.
        this.mPerspectiveCamera = this.getPerspectiveCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        // Allocate the CameraController.
        this.mCameraController = new CameraInputController(this.getPerspectiveCamera());
        // Register the Input Processor.
        Gdx.input.setInputProcessor(this.getCameraController());

        // Declare the ModelBuilder.
        final ModelBuilder lModelBuilder = new ModelBuilder();
        lModelBuilder.begin();
        lModelBuilder.node().id = PhysicsActivity.KEY_OBJECT_GROUND;
        lModelBuilder.part(PhysicsActivity.KEY_OBJECT_GROUND,   GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.RED))).box(5f, 1f, 5f);
        lModelBuilder.node().id = PhysicsActivity.KEY_OBJECT_SPHERE;
        lModelBuilder.part(PhysicsActivity.KEY_OBJECT_SPHERE,   GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.GREEN))).sphere(1f, 1f, 1f, 10, 10);
        lModelBuilder.node().id = PhysicsActivity.KEY_OBJECT_BOX;
        lModelBuilder.part(PhysicsActivity.KEY_OBJECT_BOX,      GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.BLUE))).box(1f, 1f, 1f);
        lModelBuilder.node().id = PhysicsActivity.KEY_OBJECT_CONE;
        lModelBuilder.part(PhysicsActivity.KEY_OBJECT_CONE,     GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.YELLOW))).cone(1f, 2f, 1f, 10);
        lModelBuilder.node().id = PhysicsActivity.KEY_OBJECT_CAPSULE;
        lModelBuilder.part(PhysicsActivity.KEY_OBJECT_CAPSULE,  GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.CYAN))).capsule(0.5f, 2f, 10);
        lModelBuilder.node().id = PhysicsActivity.KEY_OBJECT_CYLINDER;
        lModelBuilder.part(PhysicsActivity.KEY_OBJECT_CYLINDER, GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.MAGENTA))).cylinder(1f, 2f, 1f, 10);

        // Build the Model. (This is a complete physical representation of the objects in our scene.)
        this.mModel        = lModelBuilder.end();
        // Allocate the Constructors.
        this.mConstructors = new ArrayMap<String, GameObject.Constructor>(String.class, GameObject.Constructor.class);

        // Initialize Constructor Mapping.
        this.getConstructors().put(PhysicsActivity.KEY_OBJECT_GROUND,   new GameObject.Constructor(mModel, PhysicsActivity.KEY_OBJECT_GROUND,   new btBoxShape(new Vector3(2.5f, 0.5f, 2.5f)), 0f));
        this.getConstructors().put(PhysicsActivity.KEY_OBJECT_SPHERE,   new GameObject.Constructor(mModel, PhysicsActivity.KEY_OBJECT_SPHERE,   new btSphereShape(0.5f), 1f));
        this.getConstructors().put(PhysicsActivity.KEY_OBJECT_BOX,      new GameObject.Constructor(mModel, PhysicsActivity.KEY_OBJECT_BOX,      new btBoxShape(new Vector3(0.5f, 0.5f, 0.5f)), 1f));
        this.getConstructors().put(PhysicsActivity.KEY_OBJECT_CONE,     new GameObject.Constructor(mModel, PhysicsActivity.KEY_OBJECT_CONE,     new btConeShape(0.5f, 2f), 1f));
        this.getConstructors().put(PhysicsActivity.KEY_OBJECT_CAPSULE,  new GameObject.Constructor(mModel, PhysicsActivity.KEY_OBJECT_CAPSULE,  new btCapsuleShape(.5f, 1f), 1f));
        this.getConstructors().put(PhysicsActivity.KEY_OBJECT_CYLINDER, new GameObject.Constructor(mModel, PhysicsActivity.KEY_OBJECT_CYLINDER, new btCylinderShape(new Vector3(.5f, 1f, .5f)),  1f));

        // Allocate the CollisionConfig; defines how to handle collisions within the scene.
        this.mCollisionConfig = new btDefaultCollisionConfiguration();
        // Allocate a CollisionDispatcher; this propagates collision events across the scene. We maintain a reference to ensure we may manually dispose of it later.
        this.mDispatcher = new btCollisionDispatcher(this.getCollisionConfig());
        // Allocate a BroadphaseInterface.
        this.mBroadphaseInterface = new btDbvtBroadphase();
        mConstraintsSolver = new btSequentialImpulseConstraintSolver();
        mDynamicsWorld = new btDiscreteDynamicsWorld(this.getDispatcher(), mBroadphaseInterface, mConstraintsSolver, mCollisionConfig);
        mDynamicsWorld.setGravity(new Vector3(0, -10f, 0));
        mContactListener = new MyContactListener();

        mInstances = new Array<GameObject>();
        final GameObject lGameObject = this.getConstructors().get(PhysicsActivity.KEY_OBJECT_GROUND).construct();
        lGameObject.body.setCollisionFlags(lGameObject.body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
        this.getInstances().add(lGameObject);
        mDynamicsWorld.addRigidBody(lGameObject.body);
        lGameObject.body.setContactCallbackFlag(GROUND_FLAG);
        lGameObject.body.setContactCallbackFilter(0);
        lGameObject.body.setActivationState(Collision.DISABLE_DEACTIVATION);
    }

    /** Spawns a random shape within the 3D scene. */
    private final void spawn () {
        // Allocate a new GameObject.
        final GameObject lGameObject = this.getConstructors().values[1 + MathUtils.random(this.getConstructors().size - 2)].construct();
        lGameObject.transform.setFromEulerAngles(MathUtils.random(360f), MathUtils.random(360f), MathUtils.random(360f));
        lGameObject.transform.trn(MathUtils.random(-2.5f, 2.5f), 9f, MathUtils.random(-2.5f, 2.5f));
        lGameObject.body.proceedToTransform(lGameObject.transform);
        lGameObject.body.setUserValue(this.getInstances().size);
        lGameObject.body.setCollisionFlags(lGameObject.body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
        this.getInstances().add(lGameObject);
        mDynamicsWorld.addRigidBody(lGameObject.body);
        lGameObject.body.setContactCallbackFlag(OBJECT_FLAG);
        lGameObject.body.setContactCallbackFilter(GROUND_FLAG);
    }

    float angle, speed = 90f;

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

        angle = (angle + lStep * speed) % 360f;
        this.getInstances().get(0).transform.setTranslation(0, MathUtils.sinDeg(angle) * 2.5f, 0f);

        mDynamicsWorld.stepSimulation(lStep, 5, 1f / 60f);

        if ((mSpawnTimer -= lStep) < 0) {
            spawn();
            mSpawnTimer = 1.5f;
        }

        // Update the CameraController.
        this.getCameraController().update();

        Gdx.gl.glClearColor(0.3f, 0.3f, 0.3f, 1.f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        mModelBatch.begin(this.getPerspectiveCamera());
        mModelBatch.render(this.getInstances(), this.getEnvironment());
        mModelBatch.end();
    }

    /** Handle when the screen is resized. (Useful for changes in screen orientation on Android.) */
    @Override public final void resize(final int pWidth, final int pHeight) {
        // Reassign the PerspectiveCamera.
        this.setPerpectiveCamera(PhysicsActivity.getPerspectiveCamera(pWidth, pHeight));
        // Update the CameraController.
        this.setCameraController(new CameraInputController(this.getPerspectiveCamera()));
    }

    /** Handles destruction of the 3D scene. */
    @Override public final void dispose () {
        // Iterate the Instances.
        for(final GameObject lGameObject : this.getInstances()) {
            // Dispose of the GameObject.
            lGameObject.dispose();
        }
        // Clear the Instances.
        this.getInstances().clear();

        // Iterate the Constructors.
        for(final GameObject.Constructor lConstructor : this.getConstructors().values()) {
            // Dispose of the Constructor.
            lConstructor.dispose();
        }
        // Empty the Constructors.
        this.getConstructors().clear();

        mDynamicsWorld.dispose();
        mConstraintsSolver.dispose();
        mBroadphaseInterface.dispose();
        // Dispose of the Dispatcher.
        this.getDispatcher().dispose();
        // Dispose of the CollisionConfig.
        this.getCollisionConfig().dispose();

        mContactListener.dispose();

        mModelBatch.dispose();
        mModel.dispose();
    }

    /* Unused Overrides. */
    @Override public final void  pause() { /* Do nothing. */ }
    @Override public final void resume() { /* Do nothing. */ }

    /** Computes the elapsed time in the scene; render either the animation step or the time that has elapsed to ensure smooth display. */
    private final float getSimulationStep() {
        // Return the minimum step to apply, between either the Frames Per Second or the time that's elapsed.
        return Math.min(1.0f / PhysicsActivity.FRAMES_PER_SECOND, Gdx.graphics.getDeltaTime());
    }

    /* Getters. */
    private final void setCameraController(final CameraInputController pCameraInputController) {
        this.mCameraController = pCameraInputController;
    }

    private final CameraInputController getCameraController() {
        return this.mCameraController;
    }

    private final ArrayMap<String, GameObject.Constructor> getConstructors() {
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

    private final Array<GameObject> getInstances() {
        return this.mInstances;
    }

    private final btDispatcher getDispatcher() {
        return this.mDispatcher;
    }

}