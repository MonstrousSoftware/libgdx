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

package com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.model;

import com.badlogic.gdx.backends.webgpu.gdx.graphics.WebGPUMesh;
import com.badlogic.gdx.backends.webgpu.wrappers.WebGPURenderPass;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;



public class WebGPUMeshPart extends MeshPart {

	// mesh must be a WebGPUMesh
	// note primitive type is a GL constant, e.g. GL_TRIANGLES
	public WebGPUMeshPart (final String id, final Mesh mesh, final int offset, final int size, final int type) {
		if (!(mesh instanceof WebGPUMesh))
			throw new RuntimeException("WebGPUMeshPart supports only WebGPUMesh");
		set(id, mesh, offset, size, type);
	}

	public WebGPUMeshPart() {
		super();
	}

	// catch invalid use
	@Override
	public void render (ShaderProgram shader, boolean autoBind) {
		throw new IllegalArgumentException("WebGPUMeshPart: call render with a render pass");
	}

	// catch invalid use
	@Override
	public void render (ShaderProgram shader) {
		throw new IllegalArgumentException("WebGPUMeshPart: call render with a render pass");
	}

	// this is new w.r.t. MeshPart
	public void render (WebGPURenderPass renderPass) {
		if (!(mesh instanceof WebGPUMesh))
			throw new RuntimeException("WebGPUMeshPart supports only WebGPUMesh");
		((WebGPUMesh)mesh).render(renderPass, primitiveType, offset, size);
	}
}
