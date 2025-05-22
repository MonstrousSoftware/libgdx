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

package com.badlogic.gdx.backends.webgpu.gdx.scene2d;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.utils.WebGPUShapeRenderer;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.viewport.WebGPUScalingViewport;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Version of Stage for WebGPU.
 * Just makes sure we use a WebGPUSpriteBatch and a WebGPUViewport.
 */
public class WebGPUStage extends Stage {

	private boolean debugEnabled = true;
	private WebGPUShapeRenderer debugShapes;
	private final Vector2 tmpCoords = new Vector2();

	/** Creates a stage with a {@link ScalingViewport} set to {@link Scaling#stretch}. The stage will use its own {@link Batch}
	 * which will be disposed when the stage is disposed. */
	public WebGPUStage() {
		super(new WebGPUScalingViewport(Scaling.stretch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new OrthographicCamera()),
			new WebGPUSpriteBatch());
	}

	/** Creates a stage with the specified viewport. The stage will use its own {@link Batch} which will be disposed when the stage
	 * is disposed. */
	public WebGPUStage(Viewport viewport) {
		super(viewport, new WebGPUSpriteBatch());
	}

	/** Override to call an alternative debug drawer */
	@Override
	public void draw () {


		if (!getRoot().isVisible()) return;

		getViewport().apply(); // Apply viewport changes (updates camera viewport dimensions)
		Camera camera = getViewport().getCamera();
		camera.update();

		Batch batch = this.getBatch();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		getRoot().draw(batch, 1);
		batch.end();

		// There is a static boolean debug in Stage that indicates if any actor has ever needed debug draw, to avoid calling drawDebug() unless needed.
		// However we cannot access this package private static member. We add stage.enableDebug() to block debug drawing including the recursive descent.
		if(debugEnabled)
			drawDebug(camera);
	}

	/** Allow debug drawing (default is enabled).
	 * Set false to disable any debug lines, for a small performance boost
	 * */
	public void enableDebug(boolean enable){
		debugEnabled = enable;
	}

	private void drawDebug(Camera camera){

		// there is a complicated logic with debug under mouse etc. that is not replicated here.

		if(debugShapes == null){
			Gdx.app.log("WebGPUStage", "create shape renderer");
			debugShapes = new WebGPUShapeRenderer();
		}

		debugShapes.setProjectionMatrix(camera.combined);
		debugShapes.begin(WebGPUShapeRenderer.ShapeType.Line);
		drawRecursiveDebug(getRoot(), debugShapes);
		debugShapes.end();
	}

	private void drawRecursiveDebug(Actor actor, WebGPUShapeRenderer debugShapes){
		drawActorDebug(actor, debugShapes);

		// Recurse for groups
		if (actor instanceof Group) {
			SnapshotArray<Actor> children = ((Group) actor).getChildren();
			for (int i = 0; i < children.size; i++) {
				drawRecursiveDebug(children.get(i), debugShapes);
			}
		}
	}

	private void drawActorDebug(Actor actor, WebGPUShapeRenderer debugShapes){
		if(actor == null) return;

		if(!isDebugAll() && !actor.getDebug()) return;

		// Calculate bottom-left position in STAGE coordinates
		tmpCoords.set(0, 0);
		actor.localToStageCoordinates(tmpCoords);
		float stageX = tmpCoords.x;
		float stageY = tmpCoords.y;

		float width = actor.getWidth();
		float height = actor.getHeight();

		debugShapes.setColor(getDebugColor());
		debugShapes.rect(stageX, stageY, actor.getOriginX(), actor.getOriginY(), width, height, actor.getScaleX(), actor.getScaleY(), actor.getRotation());
	}

}
