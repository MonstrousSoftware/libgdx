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

package com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d;

import com.badlogic.gdx.backends.webgpu.gdx.WebGPUMesh;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.model.WebGPUMeshPart;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.graphics.g3d.model.data.*;
import com.badlogic.gdx.utils.*;

import java.nio.Buffer;
import java.nio.ShortBuffer;


public class WebGPUModel extends Model {

	@Override
	protected void convertMesh (ModelMesh modelMesh) {
		int numIndices = 0;
		for (ModelMeshPart part : modelMesh.parts) {
			numIndices += part.indices.length;
		}
		boolean hasIndices = numIndices > 0;
		VertexAttributes attributes = new VertexAttributes(modelMesh.attributes);
		int numVertices = modelMesh.vertices.length / (attributes.vertexSize / 4);

		Mesh mesh = new WebGPUMesh(true, numVertices, numIndices, attributes);
		meshes.add(mesh);
		disposables.add(mesh);

		BufferUtils.copy(modelMesh.vertices, mesh.getVerticesBuffer(true), modelMesh.vertices.length, 0);
		int offset = 0;
		ShortBuffer indicesBuffer = mesh.getIndicesBuffer(true);
		((Buffer)indicesBuffer).clear();
		for (ModelMeshPart part : modelMesh.parts) {
			MeshPart meshPart = new WebGPUMeshPart();
			meshPart.id = part.id;
			meshPart.primitiveType = part.primitiveType;
			meshPart.offset = offset;
			meshPart.size = hasIndices ? part.indices.length : numVertices;
			meshPart.mesh = mesh;
			if (hasIndices) {
				indicesBuffer.put(part.indices);
			}
			offset += meshPart.size;
			meshParts.add(meshPart);
		}
		((Buffer)indicesBuffer).position(0);
		for (MeshPart part : meshParts)
			part.update();
	}
}
