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
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.utils.WebGPUScreenUtils;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.utils.WebGPUShapeRenderer;
import com.badlogic.gdx.backends.webgpu.wrappers.WebGPUTexture;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.tests.utils.GdxTest;

public class ShapeRenderer2DTest extends GdxTest {

	WebGPUShapeRenderer renderer;
	Camera cam;
	WebGPUSpriteBatch batch;
	WebGPUTexture texture;

	// launcher
	public static void main (String[] argv) {

		WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
		config.setWindowedMode(640, 480);
		config.setTitle("WebGPUTest");

		new WebGPUApplication(new ShapeRenderer2DTest(), config);
	}

	public void create () {
		renderer = new WebGPUShapeRenderer();

		cam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		// important to set near and far for WebGPU clip space
		cam.near = -1;
		cam.far = 1;
		cam.update();

		System.out.println(cam.combined.toString());

		batch = new WebGPUSpriteBatch();
		texture = new WebGPUTexture(Gdx.files.internal("data/badlogicsmall.jpg"));
	}

	public void render () {

		WebGPUScreenUtils.clear(Color.TEAL);

		renderer.setProjectionMatrix(cam.combined);

		renderer.begin(WebGPUShapeRenderer.ShapeType.Filled);
		renderer.setColor(1, 0, 1, 0.5f);
		// note: shape renderer has (0,0) at centre screen, unlike SpriteBatch
		renderer.circle(0, 0, 50);
		renderer.end();

		renderer.begin(WebGPUShapeRenderer.ShapeType.Line);
		renderer.setColor(Color.RED);
		// note: shape renderer has (0,0) at centre screen, unlike SpriteBatch
		renderer.circle(0, 0, 75);
		renderer.end();


		batch.setProjectionMatrix(cam.combined);
		batch.begin();
		batch.draw(texture, 100, 100);
		batch.end();
	}

	@Override
	public void dispose () {
		renderer.dispose();
		batch.dispose();
		texture.dispose();
	}
}
