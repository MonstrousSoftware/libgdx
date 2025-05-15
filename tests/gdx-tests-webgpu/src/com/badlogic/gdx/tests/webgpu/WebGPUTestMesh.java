
package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.backends.lwjgl3_webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.lwjgl3_webgpu.WebGPUApplicationConfiguration;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUGraphicsBase;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUMesh;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.model.WebGPUMeshPart;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.graphics.*;

// Basic test of Mesh and MeshPart
// Renders a rectangle (or part of it).


public class WebGPUTestMesh {

	// launcher
	public static void main (String[] argv) {

		WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
		config.setWindowedMode(640, 480);
		config.setTitle("WebGPUTest");
		//config.backend = WGPUBackendType.D3D12;
		config.enableGPUtiming = false;

		new WebGPUApplication(new TestApp(), config);
	}

	// application
	static class TestApp extends ApplicationAdapter {
		private WebGPUMesh mesh;
		private VertexAttributes vattr;
		private WebGPUPipeline pipeline;
		private WebGPUMeshPart meshPart;

		public void create () {
			vattr = new VertexAttributes(VertexAttribute.Position(),  VertexAttribute.TexCoords(0), VertexAttribute.ColorUnpacked());

			PipelineSpecification pipelineSpec = new PipelineSpecification(vattr, getDefaultShaderSource());
			pipelineSpec.name = "pipeline";
			pipeline = new WebGPUPipeline((WebGPUPipelineLayout) null, pipelineSpec);


			mesh = new WebGPUMesh(true, 4, 6, vattr);
			mesh.setVertices(new float[]{
				-0.5f, -0.5f, 0, 	0, 1, 	1,0,1,1,
				0.5f, -0.5f, 0, 	1,1,	0,1,1,1,
				0.5f, 0.5f, 0, 		1,0,	1,1,0,1,
				-0.5f, 0.5f, 0, 	0,0,	0,1,0,1,
			});

			mesh.setIndices(new short[] {0, 1, 2, 	2, 3, 0});

			int offset = 3;	// offset in the indices array, since the mesh is indexed
			int size = 3;	// nr of indices, since the mesh is indexed
			int type = GL20.GL_TRIANGLES;	// primitive type using GL constant
			meshPart = new WebGPUMeshPart("part", mesh, offset, size, type);
		}

		@Override
		public void render () {

			// create a render pass
			WebGPURenderPass pass = RenderPassBuilder.create( Color.SKY );

			pass.setPipeline(pipeline);

			//mesh.render(pass, 0, 0, 6);

			meshPart.render(pass);

			// end the render pass
			pass.end();
		}


		@Override
		public void resize (int width, int height) {
			Gdx.app.log("", "resize");
		}

		@Override
		public void dispose () {
			pipeline.dispose();
			mesh.dispose();
		}

		private String getDefaultShaderSource() {
			return "\n" +
					"\n" +
					"\n" +
					"struct VertexInput {\n" +
					"    @location(0) position: vec3f,\n" +
					"    @location(5) color: vec4f,\n" +
					"    @location(1) uv: vec2f,\n" +

					"};\n" +
					"\n" +
					"struct VertexOutput {\n" +
					"    @builtin(position) position: vec4f,\n" +
					"    @location(0) uv : vec2f,\n" +
					"    @location(1) color: vec4f,\n" +
					"};\n" +
					"\n" +
					"\n" +
					"@vertex\n" +
					"fn vs_main(in: VertexInput) -> VertexOutput {\n" +
					"   var out: VertexOutput;\n" +
					"\n" +
					"   var pos =  vec4f(in.position,  1.0);\n" +
					"   out.position = pos;\n" +
					"   out.uv = vec2f(0,0);\n" +
					"   let color:vec4f = in.color;\n" +
					"   out.color = color;\n" +
					"\n" +
					"   return out;\n" +
					"}\n" +
					"\n" +
					"@fragment\n" +
					"fn fs_main(in : VertexOutput) -> @location(0) vec4f {\n" +
					"\n" +
					"    let color = in.color;\n" +
					"    return vec4f(color);\n" +
					"}";
		}

	}
}
