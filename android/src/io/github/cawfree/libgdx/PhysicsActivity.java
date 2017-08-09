package io.github.cawfree.libgdx;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
