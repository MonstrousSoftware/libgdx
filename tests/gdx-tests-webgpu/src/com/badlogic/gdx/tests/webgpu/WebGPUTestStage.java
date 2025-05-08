
package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.WebGPUApplicationConfiguration;
import com.badlogic.gdx.backends.webgpu.gdx.g2d.WebGPUBitmapFont;
import com.badlogic.gdx.backends.webgpu.gdx.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.backends.webgpu.gdx.g2d.WebGPUTextureAtlas;
import com.badlogic.gdx.backends.webgpu.gdx.scene2d.WebGPUSkin;
import com.badlogic.gdx.backends.webgpu.gdx.scene2d.WebGPUStage;
import com.badlogic.gdx.backends.webgpu.gdx.utils.WebGPUScalingViewport;
import com.badlogic.gdx.backends.webgpu.gdx.utils.WebGPUViewport;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUBlendFactor;
import com.badlogic.gdx.backends.webgpu.wrappers.WebGPUTexture;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;

// demonstrates the use of WebGPUSpriteBatch
// shows texture from file, texture from pixmap, texture region, sprite
//
public class WebGPUTestStage {

	// launcher
	public static void main (String[] argv) {

		WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
		config.setWindowedMode(640, 480);
		config.setTitle("WebGPUTest");

		new WebGPUApplication(new TestApp(), config);
	}

	// application
	static class TestApp extends ApplicationAdapter {
		private Batch batch;
		private WebGPUScalingViewport viewport;
		private Stage stage;
		private WebGPUSkin skin;
		private WebGPUTexture texture;

		public void create () {
			viewport = new WebGPUScalingViewport(Scaling.stretch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new OrthographicCamera());
			batch = new WebGPUSpriteBatch();

			stage = new Stage(viewport, batch);
			Gdx.input.setInputProcessor(stage);

			skin = new WebGPUSkin(Gdx.files.internal("data/uiskin.json"));

			Button button = new Button(skin);


			Label label = new Label("Label text", skin);
			TextButton textButton = new TextButton("Text Button", skin);
			texture = new WebGPUTexture(Gdx.files.internal("data/badlogic.jpg"));
			Image image = new Image(texture);
			Slider slider = new Slider(0, 100, 20, false, skin);

			Table table = new Table();
			table.setFillParent(true);

			table.add(label);
			table.row();
			table.add(button).width(100);
			table.row();
			table.add(textButton);
			table.row();
			table.add(image);
			table.row();
			table.add(slider);
			//table.debug();

			stage.addActor(table);


		}

		@Override
		public void render () {

			stage.act();
			stage.draw();
		}


		@Override
		public void resize (int width, int height) {
			Gdx.app.log("resize", "");
			stage.getViewport().update(width, height, true);

		}

		@Override
		public void dispose () {

			stage.dispose();
			texture.dispose();

		}


	}
}
