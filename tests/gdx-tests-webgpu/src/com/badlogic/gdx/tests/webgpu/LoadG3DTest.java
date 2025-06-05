/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3_webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.lwjgl3_webgpu.WebGPUApplicationConfiguration;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g2d.WebGPUBitmapFont;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.WebGPUModelBatch;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.loaders.WebGPUG3dModelLoader;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.loaders.WebGPUObjLoader;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.utils.WebGPUScreenUtils;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.tests.utils.PerspectiveCamController;
import com.badlogic.gdx.utils.JsonReader;

/** Test OBJ loading and ModelInstance rendering */


public class LoadG3DTest extends GdxTest {

	WebGPUModelBatch modelBatch;
	PerspectiveCamera cam;
	PerspectiveCamController controller;
	WebGPUSpriteBatch batch;
	WebGPUBitmapFont font;
	Model model;
	ModelInstance instance;


	// launcher
	public static void main (String[] argv) {

		WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
		config.setWindowedMode(640, 480);
		config.setTitle("WebGPUTest");

		new WebGPUApplication(new LoadG3DTest(), config);
	}

	// application
	public void create () {
		modelBatch = new WebGPUModelBatch();
		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0, 2, 4);
		cam.lookAt(0,0,0);
		cam.near = 0.1f;
		cam.far = 1000f;

		String modelFileName = "data/g3d/invaders.g3dj";
		FileHandle file = Gdx.files.internal(modelFileName);
		model = new WebGPUG3dModelLoader(new JsonReader()).loadModel(file);
		instance = new ModelInstance(model);

//		// these assets need to be put in the class path...
//		model = loader.loadModel(Gdx.files.internal("data/g3d/monkey.g3db"), true);
//		instance = new ModelInstance(model);


		controller = new PerspectiveCamController(cam);
		Gdx.input.setInputProcessor(controller);
		batch = new WebGPUSpriteBatch();
		font = new WebGPUBitmapFont(Gdx.files.internal("data/lsans-15.fnt"), false);


	}

	public void render () {
		float delta = Gdx.graphics.getDeltaTime();
		instance.transform.rotate(Vector3.Y, 15f*delta);

		WebGPUScreenUtils.clear(Color.TEAL);

		cam.update();
		modelBatch.begin(cam);

		modelBatch.render(instance);

		modelBatch.end();


		batch.begin();
		font.draw(batch, "Model loaded from G3DJ file" , 0, 20);
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		cam.viewportWidth = width;
		cam.viewportHeight = height;
		cam.update();

	}

	@Override
	public void dispose () {
		batch.dispose();
		font.dispose();
		modelBatch.dispose();
		model.dispose();
	}


}
