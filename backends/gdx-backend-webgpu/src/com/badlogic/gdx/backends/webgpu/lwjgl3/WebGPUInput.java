
package com.badlogic.gdx.backends.webgpu.lwjgl3;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Disposable;

public interface WebGPUInput extends Input, Disposable {

	void windowHandleChanged (long windowHandle);

	void update ();

	void prepareNext ();

	void resetPollingStates ();
}
