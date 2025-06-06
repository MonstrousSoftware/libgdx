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
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.webgpu.backends.lwjgl3.WebGPUApplication;
import com.badlogic.gdx.webgpu.backends.lwjgl3.WebGPUApplicationConfiguration;
import com.badlogic.gdx.webgpu.assets.WebGPUAssetManager;
import com.badlogic.gdx.webgpu.graphics.g2d.WebGPUBitmapFont;
import com.badlogic.gdx.webgpu.graphics.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.webgpu.graphics.g3d.WebGPUModelBatch;
import com.badlogic.gdx.webgpu.graphics.utils.WebGPUScreenUtils;
import com.badlogic.gdx.webgpu.graphics.viewport.WebGPUScreenViewport;
import com.badlogic.gdx.webgpu.scene2d.WebGPUSkin;
import com.badlogic.gdx.webgpu.scene2d.WebGPUStage;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.tests.utils.PerspectiveCamController;

/** Test model loading via asset manager for OBJ, G3DJ and G3DB formats */


public class LoadModelTest extends GdxTest {

	final static String[] fileNames = {  "data/g3d/ducky.obj", "data/g3d/head.g3db", "data/g3d/invaders.g3dj",
			"data/g3d/monkey.g3db", "data/g3d/skydome.g3db", "data/g3d/teapot.g3db", "data/g3d/cube.g3dj",
			"data/g3d/ship.obj"
	};

	WebGPUModelBatch modelBatch;
	PerspectiveCamera cam;
	PerspectiveCamController controller;
	Model model;
	ModelInstance instance;
	AssetManager assets;
	WebGPUScreenViewport viewport;
	WebGPUStage stage;
	WebGPUSkin skin;
	boolean loaded;
	WebGPUSpriteBatch batch;
	WebGPUBitmapFont font;



	// launcher
	public static void main (String[] argv) {

		WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
		config.setWindowedMode(640, 480);
		config.setTitle("WebGPUTest");

		new WebGPUApplication(new LoadModelTest(), config);
	}

	// application
	public void create () {
		batch = new WebGPUSpriteBatch();
		font = new WebGPUBitmapFont();

		modelBatch = new WebGPUModelBatch();
		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0, 2, 4);
		cam.lookAt(0,0,0);
		cam.near = 0.1f;
		cam.far = 1000f;		// extend far distance to avoid clipping the skybox


		//String modelFileName = "data/g3d/head.g3db";
//		FileHandle file = Gdx.files.internal(modelFileName);
//		model = new WebGPUG3dModelLoader(new JsonReader()).loadModel(file);

		// queue for asynchronous loading
		assets = new WebGPUAssetManager();
		loaded = false;
		for(String fileName : fileNames)
			assets.load(fileName, Model.class);

		controller = new PerspectiveCamController(cam);
		Gdx.input.setInputProcessor(controller);

		// Add some GUI
		//
		viewport = new WebGPUScreenViewport();
		stage = new WebGPUStage(viewport);
		//stage.setDebugAll(true);

		InputMultiplexer im = new InputMultiplexer();
		Gdx.input.setInputProcessor(im);
		im.addProcessor(stage);
		im.addProcessor(controller);

		skin = new WebGPUSkin(Gdx.files.internal("data/uiskin.json"));
		SelectBox<String> selectBox = new SelectBox<>(skin);
		// Add a listener to the button. ChangeListener is fired when the button's checked state changes, eg when clicked,
		// Button#setChecked() is called, via a key press, etc. If the event.cancel() is called, the checked state will be reverted.
		// ClickListener could have been used, but would only fire when clicked. Also, canceling a ClickListener event won't
		// revert the checked state.
		selectBox.addListener(new ChangeListener() {
			public void changed (ChangeEvent event, Actor actor) {
				System.out.println("Clicked! Is checked: " + selectBox.getSelected());
				if(loaded) {
					model = assets.get(selectBox.getSelected(), Model.class);
					instance = new ModelInstance(model);
				}
			}
		});

		selectBox.setItems(fileNames );

		Table screenTable = new Table();
		screenTable.setFillParent(true);
		Table controls = new Table();
		controls.add(new Label("File: ", skin));
		controls.add(selectBox).row();
		controls.debug();
		screenTable.add(controls).left().top().expand();


		stage.addActor(screenTable);

	}

	public void render () {
		float delta = Gdx.graphics.getDeltaTime();
		if(!loaded && assets.update()) {	// advance loading
			loaded = true;
			model = assets.get(fileNames[0], Model.class);
			instance = new ModelInstance(model);
		}


		if(loaded)
			instance.transform.rotate(Vector3.Y, 15f*delta);

		WebGPUScreenUtils.clear(Color.TEAL);

		cam.update();
		modelBatch.begin(cam);

		if(loaded)
			modelBatch.render(instance);

		modelBatch.end();


		stage.act();
		stage.draw();

		if(!loaded) {
			batch.begin();
			font.draw(batch, "Loading models from file...", 100, 100);
			batch.end();
		}

	}

	@Override
	public void resize(int width, int height) {
		cam.viewportWidth = width;
		cam.viewportHeight = height;
		cam.update();


	}

	@Override
	public void dispose () {
		modelBatch.dispose();
		skin.dispose();
		stage.dispose();
		assets.dispose();
		batch.dispose();
		font.dispose();
	}


}
