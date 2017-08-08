package io.github.cawfree.libgdx;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

/** The Main Activity for the Application. */
public final class PhysicsActivity extends AndroidApplication {

	/** Handle creation of the Application. */
	@Override protected final void onCreate(final Bundle pSavedInstanceState) {
		// Implement the Parent.
		super.onCreate(pSavedInstanceState);
		// Declare the AndroidApplicationConfiguration. This prepares libgdx.
		final AndroidApplicationConfiguration lAndroidApplicationConfiguration = new AndroidApplicationConfiguration();
		// Initialize the Application using the AndroidApplicationConfiguration.
		this.initialize(new PhysicsWorld(), lAndroidApplicationConfiguration);
	}

}
