
package com.badlogic.gdx.webgpu.backends.lwjgl3;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.backends.lwjgl3.audio.Lwjgl3Audio;


public interface WebGPUApplicationBase extends Application {

	Lwjgl3Audio createAudio (WebGPUApplicationConfiguration config);

	WebGPUInput createInput (WebGPUWindow window);
}
