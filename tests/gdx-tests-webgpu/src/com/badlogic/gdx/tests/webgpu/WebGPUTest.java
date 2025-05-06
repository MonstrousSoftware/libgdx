
package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.WebGPUApplicationConfiguration;
import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.webgpu.*;

import jnr.ffi.Pointer;

public class WebGPUTest {

	// launcher
	public static void main (String[] argv) {

		WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
		config.setWindowedMode(640, 480);
		config.setTitle("WebGPUTest");

		new WebGPUApplication(new TestApp(), config);
	}

	// application
	static class TestApp extends ApplicationAdapter {
		private WebGPUApplication app;
		private WebGPU_JNI webGPU;
		private Pointer queue;
		private Pointer pipeline;

		public void create () {
			app = (WebGPUApplication)Gdx.app;
			webGPU = app.getWebGPU();
			queue = app.getQueue();
			pipeline = initPipeline();
		}

		@Override
		public void render () {
// if (Gdx.input.justTouched()) {
//
// ApplicationListener listener = new TestApp();
//
// WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
// config.setWindowedMode(200, 200);
// config.setTitle("Child Window");
// app.newWindow(listener, config);
// }

			Pointer renderPass = prepareRenderPass(app.getCommandEncoder(), app.getTargetView());

			renderStuff(renderPass); // do some rendering in this render pass

			webGPU.wgpuRenderPassEncoderEnd(renderPass);
			webGPU.wgpuRenderPassEncoderRelease(renderPass);

		}

		private void renderStuff (Pointer renderPass) {
			// Select which render pipeline to use
			webGPU.wgpuRenderPassEncoderSetPipeline(renderPass, pipeline);

			// Draw 1 instance of a 3-vertices shape
			webGPU.wgpuRenderPassEncoderDraw(renderPass, 3, 1, 0, 0);
		}

		@Override
		public void resize (int width, int height) {
			Gdx.app.log("", "resize");
		}

		@Override
		public void dispose () {
			webGPU.wgpuRenderPipelineRelease(pipeline);
		}

		private Pointer prepareRenderPass (Pointer encoder, Pointer targetView) {

			WGPURenderPassColorAttachment renderPassColorAttachment = WGPURenderPassColorAttachment.createDirect();
			renderPassColorAttachment.setNextInChain();
			renderPassColorAttachment.setView(targetView);
			renderPassColorAttachment.setResolveTarget(JavaWebGPU.createNullPointer());
			renderPassColorAttachment.setLoadOp(WGPULoadOp.Clear);
			renderPassColorAttachment.setStoreOp(WGPUStoreOp.Store);

			// background colour
			renderPassColorAttachment.getClearValue().setR(0.9);
			renderPassColorAttachment.getClearValue().setG(0.1);
			renderPassColorAttachment.getClearValue().setB(0.2);
			renderPassColorAttachment.getClearValue().setA(1.0);

			renderPassColorAttachment.setDepthSlice(-1L); // undefined

			WGPURenderPassDescriptor renderPassDescriptor = WGPURenderPassDescriptor.createDirect();
			renderPassDescriptor.setNextInChain();

			renderPassDescriptor.setLabel("Main Render Pass");

			renderPassDescriptor.setColorAttachmentCount(1);
			renderPassDescriptor.setColorAttachments(renderPassColorAttachment);
			renderPassDescriptor.setOcclusionQuerySet(JavaWebGPU.createNullPointer());
			renderPassDescriptor.setDepthStencilAttachment(); // no depth buffer or stencil buffer

			return webGPU.wgpuCommandEncoderBeginRenderPass(encoder, renderPassDescriptor);
		}

		private Pointer initPipeline () {

			Pointer shaderModule = makeShaderModule();

			WGPURenderPipelineDescriptor pipelineDesc = WGPURenderPipelineDescriptor.createDirect();
			pipelineDesc.setNextInChain();
			pipelineDesc.setLabel("my pipeline");

			pipelineDesc.getVertex().setBufferCount(0); // no vertex buffer, because we define it in the shader
			pipelineDesc.getVertex().setBuffers();

			pipelineDesc.getVertex().setModule(shaderModule);
			pipelineDesc.getVertex().setEntryPoint("vs_main");
			pipelineDesc.getVertex().setConstantCount(0);
			pipelineDesc.getVertex().setConstants();

			pipelineDesc.getPrimitive().setTopology(WGPUPrimitiveTopology.TriangleList);
			pipelineDesc.getPrimitive().setStripIndexFormat(WGPUIndexFormat.Undefined);
			pipelineDesc.getPrimitive().setFrontFace(WGPUFrontFace.CCW);
			pipelineDesc.getPrimitive().setCullMode(WGPUCullMode.None);

			WGPUFragmentState fragmentState = WGPUFragmentState.createDirect();
			fragmentState.setNextInChain();
			fragmentState.setModule(shaderModule);
			fragmentState.setEntryPoint("fs_main");
			fragmentState.setConstantCount(0);
			fragmentState.setConstants();

			// blending
			WGPUBlendState blendState = WGPUBlendState.createDirect();
			blendState.getColor().setSrcFactor(WGPUBlendFactor.SrcAlpha);
			blendState.getColor().setDstFactor(WGPUBlendFactor.OneMinusSrcAlpha);
			blendState.getColor().setOperation(WGPUBlendOperation.Add);
			blendState.getAlpha().setSrcFactor(WGPUBlendFactor.One);
			blendState.getAlpha().setDstFactor(WGPUBlendFactor.Zero);
			blendState.getAlpha().setOperation(WGPUBlendOperation.Add);

			WGPUColorTargetState colorTarget = WGPUColorTargetState.createDirect();

			colorTarget.setFormat(app.getSurfaceFormat()); // match output surface
			colorTarget.setBlend(blendState);
			colorTarget.setWriteMask(WGPUColorWriteMask.All);

			fragmentState.setTargetCount(1);
			fragmentState.setTargets(colorTarget);

			pipelineDesc.setFragment(fragmentState);

			pipelineDesc.setDepthStencil(); // no depth or stencil buffer

			pipelineDesc.getMultisample().setCount(1);
			pipelineDesc.getMultisample().setMask(-1L);
			pipelineDesc.getMultisample().setAlphaToCoverageEnabled(0);

			pipelineDesc.setLayout(JavaWebGPU.createNullPointer());
			pipeline = webGPU.wgpuDeviceCreateRenderPipeline(app.getDevice(), pipelineDesc);
			if (pipeline.address() == 0) throw new RuntimeException("Pipeline creation failed");

			// We no longer need to access the shader module
			webGPU.wgpuShaderModuleRelease(shaderModule);
			return pipeline;
		}

		private String readShaderSource () {

			return "// triangleShader.wgsl\n" + "\n" + "@vertex\n"
				+ "fn vs_main(@builtin(vertex_index) in_vertex_index: u32) -> @builtin(position) vec4f {\n"
				+ "    var p = vec2f(0.0, 0.0);\n" + "    if (in_vertex_index == 0u) {\n" + "        p = vec2f(-0.5, -0.5);\n"
				+ "    } else if (in_vertex_index == 1u) {\n" + "        p = vec2f(0.5, -0.5);\n" + "    } else {\n"
				+ "        p = vec2f(0.0, 0.5);\n" + "    }\n" + "    return vec4f(p, 0.0, 1.0);\n" + "}\n" + "\n" + "@fragment\n"
				+ "fn fs_main() -> @location(0) vec4f {\n" + "    return vec4f(0.0, 0.4, 1.0, 1.0);\n" + "}";
		}

		private Pointer makeShaderModule () {

			String shaderSource = readShaderSource();

			// Create Shader Module
			WGPUShaderModuleDescriptor shaderDesc = WGPUShaderModuleDescriptor.createDirect();
			shaderDesc.setLabel("triangle shader");

			WGPUShaderModuleWGSLDescriptor shaderCodeDesc = WGPUShaderModuleWGSLDescriptor.createDirect();
			shaderCodeDesc.getChain().setNext();
			shaderCodeDesc.getChain().setSType(WGPUSType.ShaderModuleWGSLDescriptor);
			shaderCodeDesc.setCode(shaderSource);

			shaderDesc.getNextInChain().set(shaderCodeDesc.getPointerTo());

			Pointer shaderModule = webGPU.wgpuDeviceCreateShaderModule(app.getDevice(), shaderDesc);
			if (shaderModule.address() == 0) throw new RuntimeException("ShaderModule: compile failed.");
			return shaderModule;
		}
	}
}
