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
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.tests.utils.PerspectiveCamController;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.UBJsonReader;
import com.badlogic.gdx.webgpu.backends.lwjgl3.WebGPUApplication;
import com.badlogic.gdx.webgpu.backends.lwjgl3.WebGPUApplicationConfiguration;
import com.badlogic.gdx.webgpu.graphics.g2d.WebGPUBitmapFont;
import com.badlogic.gdx.webgpu.graphics.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.webgpu.graphics.g3d.WebGPUModelBatch;
import com.badlogic.gdx.webgpu.graphics.g3d.loaders.WebGPUG3dModelLoader;
import com.badlogic.gdx.webgpu.graphics.g3d.loaders.WebGPUObjLoader;
import com.badlogic.gdx.webgpu.graphics.utils.WebGPUScreenUtils;
import com.badlogic.gdx.webgpu.graphics.viewport.WebGPUScreenViewport;
import com.badlogic.gdx.webgpu.scene2d.WebGPUSkin;
import com.badlogic.gdx.webgpu.scene2d.WebGPUStage;
import com.badlogic.gdx.webgpu.webgpu.WGPUBackendType;

/** Test renderable instancing - reducing the number of draw calls if renderables use the same mesh part.
 * */


public class InstancingTest extends GdxTest {

	WebGPUModelBatch modelBatch;
	PerspectiveCamera cam;
	PerspectiveCamController controller;
	WebGPUSpriteBatch batch;
	WebGPUBitmapFont font;
	Model model;
	Array<ModelInstance> instances;
	Environment environment;
	WebGPUScreenViewport viewport;
	WebGPUStage stage;
	WebGPUSkin skin;

	// launcher
	public static void main (String[] argv) {

		WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
		config.setWindowedMode(640, 480);
		config.setTitle("WebGPUTest");
		config.useVsync(false);
		config.backend = WGPUBackendType.Vulkan;

		new WebGPUApplication(new InstancingTest(), config);
	}

	// application
	public void create () {
		modelBatch = new WebGPUModelBatch();
		cam = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0, 1.f, 2.5f);
		cam.lookAt(0,0,0);
		cam.near = 0.1f;

		// create a model instance
		instances = new Array<>();
		//WebGPUG3dModelLoader loader = new WebGPUG3dModelLoader(new UBJsonReader());
		WebGPUObjLoader loader = new WebGPUObjLoader();
		model = loader.loadModel(Gdx.files.internal("data/g3d/ducky.obj"), true);
		ModelInstance instance = new ModelInstance(model, 0, -1, 0);

		instances.add(instance);

		for(float z = -3; z > -40; z-= 2) {
            for (float x = -25; x < 25; x += 1) {
                instance = new ModelInstance(model, x, -1, z);
				instance.transform.rotate(Vector3.Y, (float)Math.random() * 360f);
                instances.add(instance);

            }
		}

		// Create an environment with lights
		environment = new Environment();

		ColorAttribute ambient =  ColorAttribute.createAmbientLight(0.0f, 0f, 0f, 1f);
		environment.set(ambient);

		DirectionalLight dirLight1 = new DirectionalLight();
		dirLight1.setDirection(1f, -.2f, .2f);
		dirLight1.setColor(Color.BLUE);

		DirectionalLight dirLight2 = new DirectionalLight();
		dirLight2.setDirection(-1f, -.2f, .2f);
		dirLight2.setColor(Color.RED);

		DirectionalLight dirLight3 = new DirectionalLight();
		dirLight3.setDirection(-.2f, -.6f, -.2f);
		dirLight3.setColor(Color.GREEN);

		PointLight pointLight1 = new PointLight();
		pointLight1.setPosition(-1f, 2f, -1f);
		pointLight1.setColor(Color.PURPLE);
		pointLight1.setIntensity(1f);

		PointLight pointLight2 = new PointLight();
		pointLight2.setPosition(1f, 2f, 1f);
		pointLight2.setColor(Color.YELLOW);
		pointLight2.setIntensity(1f);




		controller = new PerspectiveCamController(cam);
		Gdx.input.setInputProcessor(controller);
		batch = new WebGPUSpriteBatch();
		font = new WebGPUBitmapFont(Gdx.files.internal("data/lsans-15.fnt"), false);

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
		CheckBox checkBox1 = new CheckBox("blue directional light", skin);
		checkBox1.setChecked(true);
		environment.add(dirLight1);
		checkBox1.addListener(new ChangeListener() {
			public void changed (ChangeEvent event, Actor actor) {
				System.out.println("Clicked! Is checked: " + checkBox1.isChecked());
				if(checkBox1.isChecked())
					environment.add(dirLight1);
				else
					environment.remove(dirLight1);
			}
		});
		CheckBox checkBox2 = new CheckBox("red directional light", skin);
		checkBox2.setChecked(true);
		environment.add(dirLight2);
		checkBox2.addListener(new ChangeListener() {
			public void changed (ChangeEvent event, Actor actor) {
				System.out.println("Clicked! Is checked: " + checkBox2.isChecked());
				if(checkBox2.isChecked())
					environment.add(dirLight2);
				else
					environment.remove(dirLight2);
			}
		});
		CheckBox checkBox3 = new CheckBox("green directional light", skin);
		checkBox3.setChecked(true);
		environment.add(dirLight3);
		checkBox3.addListener(new ChangeListener() {
			public void changed (ChangeEvent event, Actor actor) {
				System.out.println("Clicked! Is checked: " + checkBox3.isChecked());
				if(checkBox3.isChecked())
					environment.add(dirLight3);
				else
					environment.remove(dirLight3);
			}
		});


		Slider ambientSlider = new Slider(0.0f, 1.0f, 0.01f, false, skin);
		ambientSlider.addListener(new ChangeListener() {
			public void changed (ChangeEvent event, Actor actor) {
				System.out.println("Ambient level: " + ambientSlider.getValue());
				float v = ambientSlider.getValue();
				ambient.color.set(v, v, v, 1.0f);
			}
		});


		CheckBox checkBox4 = new CheckBox("purple point light", skin);
		checkBox4.addListener(new ChangeListener() {
			public void changed (ChangeEvent event, Actor actor) {
				System.out.println("Point light 1: " + checkBox4.isChecked());
				if(checkBox4.isChecked())
					environment.add(pointLight1);
				else
					environment.remove(pointLight1);
			}
		});

		CheckBox checkBox5 = new CheckBox("yellow point light", skin);
		checkBox5.addListener(new ChangeListener() {
			public void changed (ChangeEvent event, Actor actor) {
				System.out.println("Point light 2: " + checkBox5.isChecked());
				if(checkBox5.isChecked())
					environment.add(pointLight2);
				else
					environment.remove(pointLight2);
			}
		});

		Slider intensitySlider = new Slider(0.0f, 10.0f, 0.01f, false, skin);
		intensitySlider.setValue(1f);
		intensitySlider.addListener(new ChangeListener() {
			public void changed (ChangeEvent event, Actor actor) {
				System.out.println("Intensity level: " + intensitySlider.getValue());
				float v = intensitySlider.getValue();
				pointLight1.intensity = v;
				pointLight2.intensity = v;
			}
		});

		Table screenTable = new Table();
		screenTable.setFillParent(true);
		Table controls = new Table();
		controls.add(checkBox1).align(Align.left).row();
		controls.add(checkBox2).align(Align.left).row();
		controls.add(checkBox3).align(Align.left).row();
		controls.add(new Label("ambient:", skin)).align(Align.left).row();
		controls.add(ambientSlider).align(Align.left).row();
		controls.add(checkBox4).align(Align.left).row();
		controls.add(checkBox5).align(Align.left).row();
		controls.add(new Label("point lights intensity:", skin)).align(Align.left).row();
		controls.add(intensitySlider).align(Align.left).row();
		screenTable.add(controls).left().top().expand();


		stage.addActor(screenTable);
	}



	public void render () {
		float delta = Gdx.graphics.getDeltaTime();
		for(ModelInstance instance : instances)
			instance.transform.rotate(Vector3.Y, 15f*delta);

		WebGPUScreenUtils.clear(Color.TEAL);

		cam.update();
		modelBatch.begin(cam);

		modelBatch.render(instances, environment);

		modelBatch.end();


		batch.begin();
		int y = 120;
		font.draw(batch, "Draw calls: "+modelBatch.drawCalls+" shader switches: "+modelBatch.shaderSwitches,0, y -= 20);
		font.draw(batch, "numRenderables: "+modelBatch.numRenderables ,0, y -= 20);
		font.draw(batch, "Materials: "+modelBatch.numMaterials ,0, y -= 20);
		font.draw(batch, "FPS: "+Gdx.graphics.getFramesPerSecond() ,0, y -= 20);
		batch.end();

		stage.act();
		stage.draw();
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
		stage.dispose();
		skin.dispose();
	}


}
