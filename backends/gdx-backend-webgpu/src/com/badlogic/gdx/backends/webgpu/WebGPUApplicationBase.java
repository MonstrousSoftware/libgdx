
package com.badlogic.gdx.backends.webgpu;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.backends.lwjgl3.audio.Lwjgl3Audio;

public interface WebGPUApplicationBase extends Application {

	Lwjgl3Audio createAudio (WebGPUApplicationConfiguration config);

	WebGPUInput createInput (WebGPUWindow window);
}
