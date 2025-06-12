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
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.tests.utils.PerspectiveCamController;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.webgpu.backends.lwjgl3.WebGPUApplication;
import com.badlogic.gdx.webgpu.backends.lwjgl3.WebGPUApplicationConfiguration;
import com.badlogic.gdx.webgpu.graphics.g2d.WebGPUBitmapFont;
import com.badlogic.gdx.webgpu.graphics.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.webgpu.graphics.g3d.WebGPUModelBatch;
import com.badlogic.gdx.webgpu.graphics.g3d.model.WebGPUMeshPart;
import com.badlogic.gdx.webgpu.graphics.g3d.utils.WebGPUModelBuilder;
import com.badlogic.gdx.webgpu.graphics.utils.WebGPUMeshBuilder;
import com.badlogic.gdx.webgpu.graphics.utils.WebGPUScreenUtils;
import com.badlogic.gdx.webgpu.wrappers.WebGPUTexture;

/** Test renderables sorting order */


public class ModelBatchSortingTest extends GdxTest {

	WebGPUModelBatch modelBatch;
	PerspectiveCamera cam;
	PerspectiveCamController controller;
	WebGPUSpriteBatch batch;
	WebGPUBitmapFont font;
	Model model;
	Array<ModelInstance> instances;


	// launcher
	public static void main (String[] argv) {

		WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
		config.setWindowedMode(640, 480);
		config.setTitle("WebGPUTest");

		new WebGPUApplication(new ModelBatchSortingTest(), config);
	}

	// application
	public void create () {
		modelBatch = new WebGPUModelBatch();
		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0, 0, 2);
		cam.near = 0.1f;


		controller = new PerspectiveCamController(cam);
		Gdx.input.setInputProcessor(controller);
		batch = new WebGPUSpriteBatch();
		font = new WebGPUBitmapFont(Gdx.files.internal("data/lsans-15.fnt"), false);


		instances = new Array<>();

		ModelBuilder modelBuilder = new WebGPUModelBuilder();
		WebGPUTexture texture2 = new WebGPUTexture(Gdx.files.internal("data/badlogic.jpg"), true);
		texture2.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
		Material mat = new Material(TextureAttribute.createDiffuse(texture2));
		Material mat2 = new Material(ColorAttribute.createDiffuse(new Color(0,1, 0, 0.5f)));
		BlendingAttribute blendingAttribute = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		blendingAttribute.opacity = 0.25f;
		mat.set(blendingAttribute);
		long attribs = VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked;
		model = modelBuilder.createBox(1, 1, 1, mat, attribs);
		//model = modelBuilder.createCone(1, 1, 1, 12, mat, attribs);
		//model = modelBuilder.createSphere(1, 1, 1, 12, 12, mat, attribs);
		//model = modelBuilder.createXYZCoordinates(10, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked);
		//model = modelBuilder.createLineGrid(1, 1, 10, 10, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked);

		for(int x = -5; x < 5; x += 4){
			for(int z = -5; z > -20; z -= 4){
				instances.add(new ModelInstance(model, x, 0, z));
			}
		}



	}

	public void render () {


		WebGPUScreenUtils.clear(Color.TEAL);

		cam.update();
		modelBatch.begin(cam);

		modelBatch.render(instances);

		modelBatch.end();


		batch.begin();
		font.draw(batch, "fps: " + Gdx.graphics.getFramesPerSecond(), 0, 20);
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
