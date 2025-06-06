
package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3_webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.lwjgl3_webgpu.WebGPUApplicationConfiguration;
import com.badlogic.gdx.webgpu.graphics.g2d.WebGPUBitmapFont;
import com.badlogic.gdx.webgpu.graphics.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.webgpu.graphics.viewport.WebGPUScreenViewport;
import com.badlogic.gdx.webgpu.scene2d.WebGPUSkin;
import com.badlogic.gdx.webgpu.scene2d.WebGPUStage;
import com.badlogic.gdx.webgpu.wrappers.WebGPUTexture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.utils.Align;

/** Test of texture wrap and filter methods.
 * todo: select box has a visual issue.
 */
public class WrapAndFilterTest extends GdxTest {

	// launcher
	public static void main (String[] argv) {

		WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
		config.setWindowedMode(640, 480);
		config.setTitle("WebGPUTest");

		new WebGPUApplication(new TestApp(), config);
	}

	// application
	static class TestApp extends ApplicationAdapter {
		private WebGPUSpriteBatch batch;
		private WebGPUTexture texture;
		private TextureRegion region;

		private WebGPUBitmapFont font;
		private WebGPUScreenViewport viewport;
		private WebGPUStage stage;
		private WebGPUSkin skin;
		private Texture.TextureFilter minFilter = Texture.TextureFilter.Nearest;
		private Texture.TextureFilter magFilter = Texture.TextureFilter.Nearest;


		public void create () {
			batch = new WebGPUSpriteBatch();

			texture = new WebGPUTexture(Gdx.files.internal("data/badlogic.jpg"));

//			texture.setWrap(Texture.TextureWrap.MirroredRepeat, Texture.TextureWrap.MirroredRepeat);
//			texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);


			region = new TextureRegion(texture, 0f,0f,2f,2f);		// uv outside 1.0 range to show off wrap mode

			font = new WebGPUBitmapFont();

			// Add some GUI
			//
			viewport = new WebGPUScreenViewport();
			stage = new WebGPUStage(viewport);
			stage.setDebugAll(true);
			Gdx.input.setInputProcessor(stage);

			skin = new WebGPUSkin(Gdx.files.internal("data/uiskin.json"));
			SelectBox<String> selectBox = new SelectBox<>(skin);
			// Add a listener to the button. ChangeListener is fired when the button's checked state changes, eg when clicked,
			// Button#setChecked() is called, via a key press, etc. If the event.cancel() is called, the checked state will be reverted.
			// ClickListener could have been used, but would only fire when clicked. Also, canceling a ClickListener event won't
			// revert the checked state.
			selectBox.addListener(new ChangeListener() {
				public void changed (ChangeEvent event, Actor actor) {
					System.out.println("Clicked! Is checked: " + selectBox.getSelected());
					handleWrap(selectBox.getSelected());
				}
			});

			selectBox.setItems(new String[]{"Repeat", "ClampToEdge", "MirroredRepeat"} );

			SelectBox<String> filterBox = new SelectBox<>(skin);
			// Add a listener to the button. ChangeListener is fired when the button's checked state changes, eg when clicked,
			// Button#setChecked() is called, via a key press, etc. If the event.cancel() is called, the checked state will be reverted.
			// ClickListener could have been used, but would only fire when clicked. Also, canceling a ClickListener event won't
			// revert the checked state.
			filterBox.addListener(new ChangeListener() {
				public void changed (ChangeEvent event, Actor actor) {
					System.out.println("Clicked! Is checked: " + filterBox.getSelected());
					handleMinFilter(filterBox.getSelected());
					texture.setFilter(minFilter, magFilter);
				}
			});

			filterBox.setItems(new String[]{"Nearest", "Linear"} );

			SelectBox<String> filterMagBox = new SelectBox<>(skin);
			// Add a listener to the button. ChangeListener is fired when the button's checked state changes, eg when clicked,
			// Button#setChecked() is called, via a key press, etc. If the event.cancel() is called, the checked state will be reverted.
			// ClickListener could have been used, but would only fire when clicked. Also, canceling a ClickListener event won't
			// revert the checked state.
			filterMagBox.addListener(new ChangeListener() {
				public void changed (ChangeEvent event, Actor actor) {
					System.out.println("Clicked! Is checked: " + filterMagBox.getSelected());
					handleMagFilter(filterMagBox.getSelected());
					texture.setFilter(minFilter, magFilter);
				}
			});

			filterMagBox.setItems(new String[]{"Nearest", "Linear"} );


			Table table = new Table();
			table.setFillParent(true);
			Table controls = new Table();
			controls.add(new Label("Wrap: ", skin));
			controls.add(selectBox).row();
			controls.add(new Label("Min Filter: ", skin));
			controls.add(filterBox).row();
			controls.add(new Label("Mag Filter: ", skin));
			controls.add(filterMagBox).row();
			table.add(controls).align(Align.topLeft).expand();
			table.debug();

			stage.addActor(table);


		}

		private void handleWrap(String wrap){
			if(wrap.contentEquals("Repeat"))
				texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
			else if(wrap.contentEquals("ClampToEdge"))
				texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
			else if(wrap.contentEquals("MirroredRepeat"))
				texture.setWrap(Texture.TextureWrap.MirroredRepeat, Texture.TextureWrap.MirroredRepeat);
		}

		private void handleMinFilter(String filter){
			if(filter.contentEquals("Nearest"))
				minFilter = Texture.TextureFilter.Nearest;
			else if(filter.contentEquals("Linear"))
				minFilter = Texture.TextureFilter.Linear;

		}

		private void handleMagFilter(String filter){
			if(filter.contentEquals("Nearest"))
				magFilter = Texture.TextureFilter.Nearest;
			else if(filter.contentEquals("Linear"))
				magFilter = Texture.TextureFilter.Linear;

		}

		@Override
		public void render () {

			batch.begin(Color.FOREST);
			batch.draw(region, 100, 100, 200, 200);

			batch.draw(texture, 300, 0, 64, 64);
			batch.draw(texture, 325, 0, 128, 128);
			batch.draw(texture, 350, 0, 256, 256);
			batch.draw(texture, 400, 0, 512, 512);
			batch.draw(texture, 500, 0, 1024, 1024);

			font.draw(batch, "Use the controls to test texture wrap and filter modes", 20, 330);

			batch.end();

			stage.act();
			stage.draw();
		}


		@Override
		public void resize (int width, int height) {
			Gdx.app.log("resize", "");
			batch.getProjectionMatrix().setToOrtho2D(0,0,Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		}

		@Override
		public void dispose () {
			batch.dispose();
			texture.dispose();
			stage.dispose();
		}


	}
}
