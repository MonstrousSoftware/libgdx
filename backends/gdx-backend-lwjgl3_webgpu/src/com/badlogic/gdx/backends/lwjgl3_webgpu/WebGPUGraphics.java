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

package com.badlogic.gdx.backends.lwjgl3_webgpu;

import com.badlogic.gdx.AbstractGraphics;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUGraphicsBase;
import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.webgpu.*;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Disposable;
import jnr.ffi.Pointer;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.system.Configuration;

import java.nio.IntBuffer;

public class WebGPUGraphics extends AbstractGraphics implements WebGPUGraphicsBase, Disposable {
	final WebGPUWindow window;
	final WebGPUApplication app;
	GL20 gl20;
	private GL30 gl30;
	private GL31 gl31;
	private GL32 gl32;
	private GLVersion glVersion;
	private volatile int backBufferWidth;
	private volatile int backBufferHeight;
	private volatile int logicalWidth;
	private volatile int logicalHeight;
	private volatile boolean isContinuous = true;
	private BufferFormat bufferFormat;
	private long lastFrameTime = -1;
	private float deltaTime;
	private boolean resetDeltaTime = false;
	private long frameId;
	private long frameCounterStart = 0;
	private int frames;
	private int fps;
	private int windowPosXBeforeFullscreen;
	private int windowPosYBeforeFullscreen;
	private int windowWidthBeforeFullscreen;
	private int windowHeightBeforeFullscreen;
	private DisplayMode displayModeBeforeFullscreen = null;
	private final WebGPU_JNI webGPU;
	public WebGPUDevice device;
	public WebGPUQueue queue;
	public Pointer surface;
	public WGPUTextureFormat surfaceFormat;
	public Pointer targetView;
	public WebGPUCommandEncoder commandEncoder;
	//public WGPUTextureFormat depthTextureFormat;
	public WebGPUTexture depthTexture;
	//public WebGPUTextureView depthTextureView;
	private WGPUSupportedLimits supportedLimits;
	private WebGPUTexture multiSamplingTexture;

	IntBuffer tmpBuffer = BufferUtils.createIntBuffer(1);
	IntBuffer tmpBuffer2 = BufferUtils.createIntBuffer(1);

	GLFWFramebufferSizeCallback resizeCallback = new GLFWFramebufferSizeCallback() {
		@Override
		public void invoke (long windowHandle, final int width, final int height) {
			if (!"glfw_async".equals(Configuration.GLFW_LIBRARY_NAME.get())) {
				updateFramebufferInfo();
				if (!window.isListenerInitialized()) {
					return;
				}
				window.makeCurrent();
				// gl20.glViewport(0, 0, backBufferWidth, backBufferHeight);
				window.getListener().resize(getWidth(), getHeight());
				update();
				onResize();
				//window.renderFrame();
				// window.getListener().render();
				// GLFW.glfwSwapBuffers(windowHandle);
				WebGPUApplication app = (WebGPUApplication) Gdx.app;
				if(app.getConfiguration().samples > 1 ) {
					if(multiSamplingTexture != null)
						multiSamplingTexture.dispose();
					multiSamplingTexture = new WebGPUTexture("multisampling", width, height, false, true, surfaceFormat, app.getConfiguration().samples);
				}
			} else {
				window.asyncResized = true;
			}
		}
	};

	public WebGPUGraphics (WebGPUWindow window, WebGPU_JNI webGPU, long win32handle) {
		this.window = window;
		this.webGPU = webGPU;

		this.gl20 = null;
		this.gl30 = null;
		this.gl31 = null;
		this.gl32 = null;
		updateFramebufferInfo();

		app = (WebGPUApplication) Gdx.app;
		Gdx.graphics = this;

		initWebGPU(win32handle, getWidth(), getHeight(), app.getConfiguration().samples, app.getConfiguration().vSyncEnabled);

		if (app.getConfiguration().samples > 1) {
			multiSamplingTexture = new WebGPUTexture("multisampling", getWidth(), getHeight(), false, true, surfaceFormat, app.getConfiguration().samples);
		}

		// initiateGL();

		GLFW.glfwSetFramebufferSizeCallback(window.getWindowHandle(), resizeCallback);
	}

	public WebGPU_JNI getWebGPU () {
		return webGPU;
	}

	@Override
	public WebGPUDevice getDevice() {
		return device;
	}

	@Override
	public WebGPUQueue getQueue() {
		return queue;
	}
	@Override
	public Pointer getSurface () {
		return surface;
	}
	@Override
	public WGPUTextureFormat getSurfaceFormat () {
		return surfaceFormat;
	}
	@Override
	public Pointer getTargetView () {
		return targetView;
	}
	@Override
	public WebGPUCommandEncoder getCommandEncoder () {
		return commandEncoder;
	}


    public WGPUSupportedLimits getSupportedLimits() {
        return supportedLimits;
    }

    public void setSupportedLimits(WGPUSupportedLimits supportedLimits) {
        this.supportedLimits = supportedLimits;
    }

	@Override
	public WebGPUTexture getDepthTexture () {
		return depthTexture;
	}
//	@Override
//	public WebGPUTextureView getDepthTextureView () {
//		return depthTextureView;
//	}
//	@Override
//	public WGPUTextureFormat getDepthTextureFormat () {
//		return depthTextureFormat;
//	}

	@Override
	public boolean getGPUtimingEnabled() {
		return app.getConfiguration().enableGPUtiming;
	}

	@Override
	public WGPUBackendType getRequestedBackendType() {
		return app.getConfiguration().backend;
	}

	@Override
	public int getSamples() {
		return app.getConfiguration().samples;
	}

	private void initWebGPU (long windowHandle, int width, int height, int samples, boolean vsync) {

		Pointer instance = webGPU.wgpuCreateInstance(null);

		surface = JavaWebGPU.getUtils().glfwGetWGPUSurface(instance, windowHandle);
		WebGPUAdapter adapter = new WebGPUAdapter(instance, surface);

		device = new WebGPUDevice(adapter);

		// Find out the preferred surface format of the window
		WGPUSurfaceCapabilities caps = WGPUSurfaceCapabilities.createDirect();
		webGPU.wgpuSurfaceGetCapabilities(surface, adapter.getHandle(), caps);
		Pointer formats = caps.getFormats();
		int format = formats.getInt(0);
		surfaceFormat = WGPUTextureFormat.values()[format];

		adapter.dispose();  // finished with adapter now that we have a device


		//device = initDevice(instance, surface);

		webGPU.wgpuInstanceRelease(instance); // we can release the instance now that we have the device

		queue = new WebGPUQueue(device);

		initSwapChain(width, height, vsync);
		initDepthBuffer(width, height, samples);
	}

	private void onResize(){
		terminateDepthBuffer();
		webGPU.wgpuSurfaceUnconfigure(surface);

		initSwapChain(getWidth(), getHeight(), app.getConfiguration().vSyncEnabled);
		initDepthBuffer(getWidth(), getHeight(), getSamples());
	}


	// todo handle resize
	// or call this from resize event rather than constructor
	private void initDepthBuffer(int width, int height, int samples){

		//depthTextureFormat = WGPUTextureFormat.Depth24Plus;

		depthTexture = new WebGPUTexture("depth texture", width, height, 1, WGPUTextureUsage.RenderAttachment,
				WGPUTextureFormat.Depth24Plus, samples, WGPUTextureFormat.Depth24Plus );

		// Create the view of the depth texture manipulated by the rasterizer
		//depthTextureView = new WebGPUTextureView(depthTexture, WGPUTextureAspect.DepthOnly, WGPUTextureViewDimension._2D,depthTextureFormat, 0, 1, 0, 1 );
	}

	private void terminateDepthBuffer(){
		// Destroy the depth texture
		if(depthTexture != null) {
			depthTexture.dispose();
		}
		depthTexture = null;
	}

	private void initSwapChain (int width, int height, boolean vsyncEnabled) {
		// configure the surface
		WGPUSurfaceConfiguration config = WGPUSurfaceConfiguration.createDirect();
		config.setNextInChain().setWidth(width).setHeight(height).setFormat(surfaceFormat).setViewFormatCount(0)
				.setViewFormats(JavaWebGPU.createNullPointer()).setUsage(WGPUTextureUsage.RenderAttachment).setDevice(device.getHandle())
				.setPresentMode(vsyncEnabled ? WGPUPresentMode.Fifo : WGPUPresentMode.Immediate)
				.setAlphaMode(WGPUCompositeAlphaMode.Auto);

		webGPU.wgpuSurfaceConfigure(surface, config);
	}

	private void exitWebGPU () {
		webGPU.wgpuSurfaceUnconfigure(surface);
		queue.dispose();
		device.dispose();
//
//		webGPU.wgpuQueueRelease(queue);
//		webGPU.wgpuDeviceRelease(device);
		webGPU.wgpuSurfaceRelease(surface);

		terminateDepthBuffer();
	}


	public WebGPUWindow getWindow () {
		return window;
	}

	public WebGPUTexture getMultiSamplingTexture() {
		return multiSamplingTexture;
	}

	void updateFramebufferInfo () {
		GLFW.glfwGetFramebufferSize(window.getWindowHandle(), tmpBuffer, tmpBuffer2);
		this.backBufferWidth = tmpBuffer.get(0);
		this.backBufferHeight = tmpBuffer2.get(0);
		GLFW.glfwGetWindowSize(window.getWindowHandle(), tmpBuffer, tmpBuffer2);
		WebGPUGraphics.this.logicalWidth = tmpBuffer.get(0);
		WebGPUGraphics.this.logicalHeight = tmpBuffer2.get(0);
		WebGPUApplicationConfiguration config = window.getConfig();
		bufferFormat = new BufferFormat(config.r, config.g, config.b, config.a, config.depth, config.stencil, config.samples,
			false);
	}

	void update () {
		long time = System.nanoTime();
		if (lastFrameTime == -1) lastFrameTime = time;
		if (resetDeltaTime) {
			resetDeltaTime = false;
			deltaTime = 0;
		} else
			deltaTime = (time - lastFrameTime) / 1000000000.0f;
		lastFrameTime = time;

		if (time - frameCounterStart >= 1000000000) {
			fps = frames;
			frames = 0;
			frameCounterStart = time;
		}
		frames++;
		frameId++;
	}

	@Override
	public boolean isGL30Available () {
		return gl30 != null;
	}

	@Override
	public boolean isGL31Available () {
		return gl31 != null;
	}

	@Override
	public boolean isGL32Available () {
		return gl32 != null;
	}

	@Override
	public GL20 getGL20 () {
		return gl20;
	}

	@Override
	public GL30 getGL30 () {
		return gl30;
	}

	@Override
	public GL31 getGL31 () {
		return gl31;
	}

	@Override
	public GL32 getGL32 () {
		return gl32;
	}

	@Override
	public void setGL20 (GL20 gl20) {
		this.gl20 = gl20;
	}

	@Override
	public void setGL30 (GL30 gl30) {
		this.gl30 = gl30;
	}

	@Override
	public void setGL31 (GL31 gl31) {
		this.gl31 = gl31;
	}

	@Override
	public void setGL32 (GL32 gl32) {
		this.gl32 = gl32;
	}

	@Override
	public int getWidth () {
		if (window.getConfig().hdpiMode == HdpiMode.Pixels) {
			return backBufferWidth;
		} else {
			return logicalWidth;
		}
	}

	@Override
	public int getHeight () {
		if (window.getConfig().hdpiMode == HdpiMode.Pixels) {
			return backBufferHeight;
		} else {
			return logicalHeight;
		}
	}

	@Override
	public int getBackBufferWidth () {
		return backBufferWidth;
	}

	@Override
	public int getBackBufferHeight () {
		return backBufferHeight;
	}

	public int getLogicalWidth () {
		return logicalWidth;
	}

	public int getLogicalHeight () {
		return logicalHeight;
	}

	@Override
	public long getFrameId () {
		return frameId;
	}

	@Override
	public float getDeltaTime () {
		return deltaTime;
	}

	public void resetDeltaTime () {
		resetDeltaTime = true;
	}

	@Override
	public int getFramesPerSecond () {
		return fps;
	}

	@Override
	public GraphicsType getType () {
		return GraphicsType.LWJGL3;
	}

	@Override
	public GLVersion getGLVersion () {
		return glVersion;
	}

	@Override
	public float getPpiX () {
		return getPpcX() * 2.54f;
	}

	@Override
	public float getPpiY () {
		return getPpcY() * 2.54f;
	}

	@Override
	public float getPpcX () {
		WebGPUMonitor monitor = (WebGPUMonitor)getMonitor();
		GLFW.glfwGetMonitorPhysicalSize(monitor.monitorHandle, tmpBuffer, tmpBuffer2);
		int sizeX = tmpBuffer.get(0);
		DisplayMode mode = getDisplayMode();
		return mode.width / (float)sizeX * 10;
	}

	@Override
	public float getPpcY () {
		WebGPUMonitor monitor = (WebGPUMonitor)getMonitor();
		GLFW.glfwGetMonitorPhysicalSize(monitor.monitorHandle, tmpBuffer, tmpBuffer2);
		int sizeY = tmpBuffer2.get(0);
		DisplayMode mode = getDisplayMode();
		return mode.height / (float)sizeY * 10;
	}

	@Override
	public boolean supportsDisplayModeChange () {
		return true;
	}

	@Override
	public Monitor getPrimaryMonitor () {
		return WebGPUApplicationConfiguration.toWebGPUMonitor(GLFW.glfwGetPrimaryMonitor());
	}

	@Override
	public Monitor getMonitor () {
		Monitor[] monitors = getMonitors();
		Monitor result = monitors[0];

		GLFW.glfwGetWindowPos(window.getWindowHandle(), tmpBuffer, tmpBuffer2);
		int windowX = tmpBuffer.get(0);
		int windowY = tmpBuffer2.get(0);
		GLFW.glfwGetWindowSize(window.getWindowHandle(), tmpBuffer, tmpBuffer2);
		int windowWidth = tmpBuffer.get(0);
		int windowHeight = tmpBuffer2.get(0);
		int overlap;
		int bestOverlap = 0;

		for (Monitor monitor : monitors) {
			DisplayMode mode = getDisplayMode(monitor);

			overlap = Math.max(0,
				Math.min(windowX + windowWidth, monitor.virtualX + mode.width) - Math.max(windowX, monitor.virtualX))
				* Math.max(0, Math.min(windowY + windowHeight, monitor.virtualY + mode.height) - Math.max(windowY, monitor.virtualY));

			if (bestOverlap < overlap) {
				bestOverlap = overlap;
				result = monitor;
			}
		}
		return result;
	}

	@Override
	public Monitor[] getMonitors () {
		PointerBuffer glfwMonitors = GLFW.glfwGetMonitors();
		Monitor[] monitors = new Monitor[glfwMonitors.limit()];
		for (int i = 0; i < glfwMonitors.limit(); i++) {
			monitors[i] = WebGPUApplicationConfiguration.toWebGPUMonitor(glfwMonitors.get(i));
		}
		return monitors;
	}

	@Override
	public DisplayMode[] getDisplayModes () {
		return WebGPUApplicationConfiguration.getDisplayModes(getMonitor());
	}

	@Override
	public DisplayMode[] getDisplayModes (Monitor monitor) {
		return WebGPUApplicationConfiguration.getDisplayModes(monitor);
	}

	@Override
	public DisplayMode getDisplayMode () {
		return WebGPUApplicationConfiguration.getDisplayMode(getMonitor());
	}

	@Override
	public DisplayMode getDisplayMode (Monitor monitor) {
		return WebGPUApplicationConfiguration.getDisplayMode(monitor);
	}

	@Override
	public int getSafeInsetLeft () {
		return 0;
	}

	@Override
	public int getSafeInsetTop () {
		return 0;
	}

	@Override
	public int getSafeInsetBottom () {
		return 0;
	}

	@Override
	public int getSafeInsetRight () {
		return 0;
	}

	@Override
	public boolean setFullscreenMode (DisplayMode displayMode) {
		window.getInput().resetPollingStates();
		WebGPUDisplayMode newMode = (WebGPUDisplayMode)displayMode;
		if (isFullscreen()) {
			WebGPUDisplayMode currentMode = (WebGPUDisplayMode)getDisplayMode();
			if (currentMode.getMonitor() == newMode.getMonitor() && currentMode.refreshRate == newMode.refreshRate) {
				// same monitor and refresh rate
				GLFW.glfwSetWindowSize(window.getWindowHandle(), newMode.width, newMode.height);
			} else {
				// different monitor and/or refresh rate
				GLFW.glfwSetWindowMonitor(window.getWindowHandle(), newMode.getMonitor(), 0, 0, newMode.width, newMode.height,
					newMode.refreshRate);
			}
		} else {
			// store window position so we can restore it when switching from fullscreen to windowed later
			storeCurrentWindowPositionAndDisplayMode();

			// switch from windowed to fullscreen
			GLFW.glfwSetWindowMonitor(window.getWindowHandle(), newMode.getMonitor(), 0, 0, newMode.width, newMode.height,
				newMode.refreshRate);
		}
		updateFramebufferInfo();

		setVSync(window.getConfig().vSyncEnabled);

		return true;
	}

	private void storeCurrentWindowPositionAndDisplayMode () {
		windowPosXBeforeFullscreen = window.getPositionX();
		windowPosYBeforeFullscreen = window.getPositionY();
		windowWidthBeforeFullscreen = logicalWidth;
		windowHeightBeforeFullscreen = logicalHeight;
		displayModeBeforeFullscreen = getDisplayMode();
	}

	@Override
	public boolean setWindowedMode (int width, int height) {
		window.getInput().resetPollingStates();
		if (!isFullscreen()) {
			GridPoint2 newPos = null;
			boolean centerWindow = false;
			if (width != logicalWidth || height != logicalHeight) {
				centerWindow = true; // recenter the window since its size changed
				newPos = WebGPUApplicationConfiguration.calculateCenteredWindowPosition((WebGPUMonitor)getMonitor(), width, height);
			}
			GLFW.glfwSetWindowSize(window.getWindowHandle(), width, height);
			if (centerWindow) {
				window.setPosition(newPos.x, newPos.y); // on macOS the centering has to happen _after_ the new window size was set
			}
		} else { // if we were in fullscreen mode, we should consider restoring a previous display mode
			if (displayModeBeforeFullscreen == null) {
				storeCurrentWindowPositionAndDisplayMode();
			}
			if (width != windowWidthBeforeFullscreen || height != windowHeightBeforeFullscreen) { // center the window since its size
				// changed
				GridPoint2 newPos = WebGPUApplicationConfiguration.calculateCenteredWindowPosition((WebGPUMonitor)getMonitor(), width,
					height);
				GLFW.glfwSetWindowMonitor(window.getWindowHandle(), 0, newPos.x, newPos.y, width, height,
					displayModeBeforeFullscreen.refreshRate);
			} else { // restore previous position
				GLFW.glfwSetWindowMonitor(window.getWindowHandle(), 0, windowPosXBeforeFullscreen, windowPosYBeforeFullscreen, width,
					height, displayModeBeforeFullscreen.refreshRate);
			}
		}
		updateFramebufferInfo();
		return true;
	}

	@Override
	public void setTitle (String title) {
		if (title == null) {
			title = "";
		}
		GLFW.glfwSetWindowTitle(window.getWindowHandle(), title);
	}

	@Override
	public void setUndecorated (boolean undecorated) {
		getWindow().getConfig().setDecorated(!undecorated);
		GLFW.glfwSetWindowAttrib(window.getWindowHandle(), GLFW.GLFW_DECORATED, undecorated ? GLFW.GLFW_FALSE : GLFW.GLFW_TRUE);
	}

	@Override
	public void setResizable (boolean resizable) {
		getWindow().getConfig().setResizable(resizable);
		GLFW.glfwSetWindowAttrib(window.getWindowHandle(), GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
	}

	@Override
	public void setVSync (boolean vsync) {
		getWindow().getConfig().vSyncEnabled = vsync;
		GLFW.glfwSwapInterval(vsync ? 1 : 0);
	}

	/** Sets the target framerate for the application, when using continuous rendering. Must be positive. The cpu sleeps as needed.
	 * Use 0 to never sleep. If there are multiple windows, the value for the first window created is used for all. Default is 0.
	 *
	 * @param fps fps */
	@Override
	public void setForegroundFPS (int fps) {
		getWindow().getConfig().foregroundFPS = fps;
	}

	@Override
	public BufferFormat getBufferFormat () {
		return bufferFormat;
	}

	@Override
	public boolean supportsExtension (String extension) {
		return GLFW.glfwExtensionSupported(extension);
	}

	@Override
	public void setContinuousRendering (boolean isContinuous) {
		this.isContinuous = isContinuous;
	}

	@Override
	public boolean isContinuousRendering () {
		return isContinuous;
	}

	@Override
	public void requestRendering () {
		window.requestRendering();
	}

	@Override
	public boolean isFullscreen () {
		return GLFW.glfwGetWindowMonitor(window.getWindowHandle()) != 0;
	}

	@Override
	public Cursor newCursor (Pixmap pixmap, int xHotspot, int yHotspot) {
		return new WebGPUCursor(getWindow(), pixmap, xHotspot, yHotspot);
	}

	@Override
	public void setCursor (Cursor cursor) {
		GLFW.glfwSetCursor(getWindow().getWindowHandle(), ((WebGPUCursor)cursor).glfwCursor);
	}

	@Override
	public void setSystemCursor (SystemCursor systemCursor) {
		WebGPUCursor.setSystemCursor(getWindow().getWindowHandle(), systemCursor);
	}

	@Override
	public void dispose () {
		this.resizeCallback.free();
		exitWebGPU();
	}

	public static class WebGPUDisplayMode extends DisplayMode {
		final long monitorHandle;

		WebGPUDisplayMode (long monitor, int width, int height, int refreshRate, int bitsPerPixel) {
			super(width, height, refreshRate, bitsPerPixel);
			this.monitorHandle = monitor;
		}

		public long getMonitor () {
			return monitorHandle;
		}
	}

	public static class WebGPUMonitor extends Monitor {
		final long monitorHandle;

		WebGPUMonitor (long monitor, int virtualX, int virtualY, String name) {
			super(virtualX, virtualY, name);
			this.monitorHandle = monitor;
		}

		public long getMonitorHandle () {
			return monitorHandle;
		}
	}
}
