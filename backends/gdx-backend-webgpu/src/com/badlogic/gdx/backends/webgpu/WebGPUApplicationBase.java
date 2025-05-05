
package com.badlogic.gdx.backends.webgpu;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.backends.webgpu.audio.WebGPUAudio;

public interface WebGPUApplicationBase extends Application {

	WebGPUAudio createAudio (WebGPUApplicationConfiguration config);

	WebGPUInput createInput (WebGPUWindow window);
}
