
package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.WebGPUApplicationConfiguration;
import com.badlogic.gdx.backends.webgpu.gdx.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.backends.webgpu.gdx.scene2d.WebGPUSkin;
import com.badlogic.gdx.backends.webgpu.gdx.scene2d.WebGPUStage;
import com.badlogic.gdx.backends.webgpu.gdx.utils.WebGPUScreenViewport;
import com.badlogic.gdx.backends.webgpu.wrappers.WebGPUTexture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.tests.utils.GdxTest;

// demonstrates the use of WebGPUSpriteBatch
// shows texture from file, texture from pixmap, texture region, sprite
//
public class StageTest extends GdxTest {
		private WebGPUSpriteBatch batch;
		private WebGPUScreenViewport viewport;
		private WebGPUStage stage;
		private WebGPUSkin skin;
		private WebGPUTexture texture;

		public void create () {
			viewport = new WebGPUScreenViewport();
			batch = new WebGPUSpriteBatch();

			stage = new WebGPUStage(viewport);
			Gdx.input.setInputProcessor(stage);

			skin = new WebGPUSkin(Gdx.files.internal("data/uiskin.json"));

			Button button = new Button(skin);


			Label label = new Label("Label text", skin);
			TextButton textButton = new TextButton("Text Button", skin);
			// Add a listener to the button. ChangeListener is fired when the button's checked state changes, eg when clicked,
			// Button#setChecked() is called, via a key press, etc. If the event.cancel() is called, the checked state will be reverted.
			// ClickListener could have been used, but would only fire when clicked. Also, canceling a ClickListener event won't
			// revert the checked state.
			textButton.addListener(new ChangeListener() {
				public void changed (ChangeEvent event, Actor actor) {
					System.out.println("Clicked! Is checked: " + button.isChecked());
					textButton.setText("Good job!");
				}
			});
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
