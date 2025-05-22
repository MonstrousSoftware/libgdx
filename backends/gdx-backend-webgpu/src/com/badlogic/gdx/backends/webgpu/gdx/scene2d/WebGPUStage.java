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
import com.badlogic.gdx.backends.webgpu.gdx.graphics.viewport.WebGPUScalingViewport;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Version of Stage for WebGPU.
 * Just makes sure we use a WebGPUSpriteBatch and a WebGPUViewport.
 */
public class WebGPUStage extends Stage {

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

}
