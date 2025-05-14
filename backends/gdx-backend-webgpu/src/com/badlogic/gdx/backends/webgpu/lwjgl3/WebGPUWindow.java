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

package com.badlogic.gdx.backends.webgpu.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.webgpu.*;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Os;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import jnr.ffi.Pointer;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;

import java.nio.IntBuffer;

public class WebGPUWindow implements Disposable {
	private long windowHandle;
	final ApplicationListener listener;
	private final Array<LifecycleListener> lifecycleListeners;
	final WebGPUApplication application;
	private boolean listenerInitialized = false;
	WebGPUWindowListener windowListener;
	private WebGPUGraphics graphics;
	private WebGPUInput input;
	private final WebGPUApplicationConfiguration config;
	private final Array<Runnable> runnables = new Array<Runnable>();
	private final Array<Runnable> executedRunnables = new Array<Runnable>();
	private final IntBuffer tmpBuffer;
	private final IntBuffer tmpBuffer2;
	boolean iconified = false;
	boolean focused = false;
	boolean asyncResized = false;
	private boolean requestRendering = false;
	private WebGPU_JNI webGPU;
	public Pointer surface;
	public WGPUTextureFormat surfaceFormat;
	public WebGPUDevice device;
	public WebGPUQueue queue;
	public Pointer targetView;
	public WebGPUCommandEncoder commandEncoder;
	public WGPUTextureFormat depthTextureFormat;
	public WebGPUTexture depthTexture;
	public WebGPUTextureView depthTextureView;

	//private final WGPUBackendType backend = WGPUBackendType.Undefined;// .D3D12; // or Vulkan, etc.
	private final boolean vsyncEnabled = true;	// per window?? todo read from config

	private final GLFWWindowFocusCallback focusCallback = new GLFWWindowFocusCallback() {
		@Override
		public void invoke (long windowHandle, final boolean focused) {
			postRunnable(new Runnable() {
				@Override
				public void run () {
					if (focused) {
						if (config.pauseWhenLostFocus) {
							synchronized (lifecycleListeners) {
								for (LifecycleListener lifecycleListener : lifecycleListeners) {
									lifecycleListener.resume();
								}
							}
							listener.resume();
						}
						if (windowListener != null) {
							windowListener.focusGained();
						}
					} else {
						if (windowListener != null) {
							windowListener.focusLost();
						}
						if (config.pauseWhenLostFocus) {
							synchronized (lifecycleListeners) {
								for (LifecycleListener lifecycleListener : lifecycleListeners) {
									lifecycleListener.pause();
								}
							}
							listener.pause();
						}
					}
					WebGPUWindow.this.focused = focused;
				}
			});
		}
	};

	private final GLFWWindowIconifyCallback iconifyCallback = new GLFWWindowIconifyCallback() {
		@Override
		public void invoke (long windowHandle, final boolean iconified) {
			postRunnable(new Runnable() {
				@Override
				public void run () {
					if (windowListener != null) {
						windowListener.iconified(iconified);
					}
					WebGPUWindow.this.iconified = iconified;
					if (iconified) {
						if (config.pauseWhenMinimized) {
							synchronized (lifecycleListeners) {
								for (LifecycleListener lifecycleListener : lifecycleListeners) {
									lifecycleListener.pause();
								}
							}
							listener.pause();
						}
					} else {
						if (config.pauseWhenMinimized) {
							synchronized (lifecycleListeners) {
								for (LifecycleListener lifecycleListener : lifecycleListeners) {
									lifecycleListener.resume();
								}
							}
							listener.resume();
						}
					}
				}
			});
		}
	};

	private final GLFWWindowMaximizeCallback maximizeCallback = new GLFWWindowMaximizeCallback() {
		@Override
		public void invoke (long windowHandle, final boolean maximized) {
			postRunnable(new Runnable() {
				@Override
				public void run () {
					if (windowListener != null) {
						windowListener.maximized(maximized);
					}
				}
			});
		}

	};

	private final GLFWWindowCloseCallback closeCallback = new GLFWWindowCloseCallback() {
		@Override
		public void invoke (final long windowHandle) {
			postRunnable(new Runnable() {
				@Override
				public void run () {
					if (windowListener != null) {
						if (!windowListener.closeRequested()) {
							GLFW.glfwSetWindowShouldClose(windowHandle, false);
						}
					}
				}
			});
		}
	};

	private final GLFWDropCallback dropCallback = new GLFWDropCallback() {
		@Override
		public void invoke (final long windowHandle, final int count, final long names) {
			final String[] files = new String[count];
			for (int i = 0; i < count; i++) {
				files[i] = getName(names, i);
			}
			postRunnable(new Runnable() {
				@Override
				public void run () {
					if (windowListener != null) {
						windowListener.filesDropped(files);
					}
				}
			});
		}
	};

	private final GLFWWindowRefreshCallback refreshCallback = new GLFWWindowRefreshCallback() {
		@Override
		public void invoke (long windowHandle) {
			postRunnable(new Runnable() {
				@Override
				public void run () {
					if (windowListener != null) {
						windowListener.refreshRequested();
					}
				}
			});
		}
	};

	WebGPUWindow (ApplicationListener listener, Array<LifecycleListener> lifecycleListeners, WebGPUApplicationConfiguration config,
		WebGPUApplication application) {
		this.listener = listener;
		this.lifecycleListeners = lifecycleListeners;
		this.windowListener = config.windowListener;
		this.config = config;
		this.application = application;
		this.tmpBuffer = BufferUtils.createIntBuffer(1);
		this.tmpBuffer2 = BufferUtils.createIntBuffer(1);

	}

	void create (long windowHandle) {
		this.windowHandle = windowHandle;
		this.input = application.createInput(this);
		this.graphics = new WebGPUGraphics(this);

		GLFW.glfwSetWindowFocusCallback(windowHandle, focusCallback);
		GLFW.glfwSetWindowIconifyCallback(windowHandle, iconifyCallback);
		GLFW.glfwSetWindowMaximizeCallback(windowHandle, maximizeCallback);
		GLFW.glfwSetWindowCloseCallback(windowHandle, closeCallback);
		GLFW.glfwSetDropCallback(windowHandle, dropCallback);
		GLFW.glfwSetWindowRefreshCallback(windowHandle, refreshCallback);

		long win32Handle = GLFWNativeWin32.glfwGetWin32Window(getWindowHandle());

		initWebGPU(win32Handle, getGraphics().getWidth(), getGraphics().getHeight());

		if (windowListener != null) {
			windowListener.created(this);
		}
	}

	private void initWebGPU (long windowHandle, int width, int height) {
		webGPU = application.getWebGPU();

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

		initSwapChain(width, height);
		initDepthBuffer(width, height);
	}

	// todo handle resize
	// or call this from resize event rather than constructor
	private void initDepthBuffer(int width, int height){

		depthTextureFormat = WGPUTextureFormat.Depth24Plus;

		depthTexture = new WebGPUTexture("depth texture", width, height, 1, WGPUTextureUsage.RenderAttachment,
				depthTextureFormat, config.samples, depthTextureFormat );

		// Create the view of the depth texture manipulated by the rasterizer
		depthTextureView = new WebGPUTextureView(depthTexture, WGPUTextureAspect.DepthOnly, WGPUTextureViewDimension._2D,depthTextureFormat, 0, 1, 0, 1 );
	}

	private void terminateDepthBuffer(){
		// Destroy the depth texture and its view
		if(depthTextureView != null)
			depthTextureView.dispose();

		if(depthTexture != null) {
			depthTexture.dispose();
		}
		depthTextureView = null;
		depthTexture = null;
	}

//	private Pointer getAdapterSync (Pointer instance, WGPURequestAdapterOptions options) {
//
//		Pointer userBuf = JavaWebGPU.createLongArrayPointer(new long[1]);
//		WGPURequestAdapterCallback callback = (WGPURequestAdapterStatus status, Pointer adapter, String message,
//											   Pointer userdata) -> {
//			if (status == WGPURequestAdapterStatus.Success)
//				userdata.putPointer(0, adapter);
//			else
//				System.out.println("Could not get adapter: " + message);
//		};
//		webGPU.wgpuInstanceRequestAdapter(instance, options, callback, userBuf);
//		// on native implementations, we don't have to wait for asynchronous operation. It returns result immediately.
//		return userBuf.getPointer(0);
//	}

//	private Pointer getDeviceSync (Pointer adapter, WGPUDeviceDescriptor deviceDescriptor) {
//
//		Pointer userBuf = JavaWebGPU.createLongArrayPointer(new long[1]);
//		WGPURequestDeviceCallback callback = (WGPURequestDeviceStatus status, Pointer device, String message, Pointer userdata) -> {
//			if (status == WGPURequestDeviceStatus.Success)
//				userdata.putPointer(0, device);
//			else
//				System.out.println("Could not get device: " + message);
//		};
//		webGPU.wgpuAdapterRequestDevice(adapter, deviceDescriptor, callback, userBuf);
//		// on native implementations, we don't have to wait for asynchronous operation. It returns result immediately.
//		return userBuf.getPointer(0);
//	}

//	private Pointer initDevice (Pointer instance, Pointer surface) {
//
//		// Select an Adapter
//		//
//
//		WGPURequestAdapterOptions options = WGPURequestAdapterOptions.createDirect();
//		options.setNextInChain();
//		options.setCompatibleSurface(surface);
//		options.setBackendType(backend);
//		options.setPowerPreference(WGPUPowerPreference.HighPerformance);
//
//		// Get Adapter
//
//		Pointer adapter = getAdapterSync(instance, options);
//
//		// Get Adapter properties out of interest
//		WGPUAdapterProperties adapterProperties = WGPUAdapterProperties.createDirect();
//		adapterProperties.setNextInChain();
//
//		webGPU.wgpuAdapterGetProperties(adapter, adapterProperties);
//
//		System.out.println("VendorID: " + adapterProperties.getVendorID());
//		System.out.println("Vendor name: " + adapterProperties.getVendorName());
//		System.out.println("Device ID: " + adapterProperties.getDeviceID());
//		System.out.println("Back end: " + adapterProperties.getBackendType());
//		System.out.println("Description: " + adapterProperties.getDriverDescription());
//
//		WGPURequiredLimits requiredLimits = WGPURequiredLimits.createDirect();
//		setDefaultLimits(requiredLimits.getLimits());
//
//		// Get a Device
//		//
//		WebGPUDevice device = new WebGPUDevice(adapter);
////		WGPUDeviceDescriptor deviceDescriptor = WGPUDeviceDescriptor.createDirect();
////		deviceDescriptor.setNextInChain();
////		deviceDescriptor.setLabel("My Device");
////		deviceDescriptor.setRequiredLimits(requiredLimits);
////		deviceDescriptor.setRequiredFeatureCount(0);
////		deviceDescriptor.setRequiredFeatures(JavaWebGPU.createNullPointer());
////
////		Pointer device = getDeviceSync(adapter, deviceDescriptor);
//
//		// use a lambda expression to define a callback function
//		WGPUErrorCallback deviceCallback = (WGPUErrorType type, String message, Pointer userdata) -> {
//			System.out.println("*** Device error: " + type + " : " + message);
//			System.exit(-1);
//		};
//		webGPU.wgpuDeviceSetUncapturedErrorCallback(device, deviceCallback, null);
//
//		// Find out the preferred surface format of the window
//		WGPUSurfaceCapabilities caps = WGPUSurfaceCapabilities.createDirect();
//		webGPU.wgpuSurfaceGetCapabilities(surface, adapter, caps);
//		Pointer formats = caps.getFormats();
//		int format = formats.getInt(0);
//		surfaceFormat = WGPUTextureFormat.values()[format];
//
//		webGPU.wgpuAdapterRelease(adapter); // we can release our adapter as soon as we have a device
//		return device;
//	}
	private void initSwapChain (int width, int height) {
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

	/** @return the {@link ApplicationListener} associated with this window **/
	public ApplicationListener getListener () {
		return listener;
	}

	/** @return the {@link WebGPUWindowListener} set on this window **/
	public WebGPUWindowListener getWindowListener () {
		return windowListener;
	}

	public void setWindowListener (WebGPUWindowListener listener) {
		this.windowListener = listener;
	}

	/** Post a {@link Runnable} to this window's event queue. Use this if you access statics like {@link Gdx#graphics} in your
	 * runnable instead of {@link Application#postRunnable(Runnable)}. */
	public void postRunnable (Runnable runnable) {
		synchronized (runnables) {
			runnables.add(runnable);
		}
	}

	/** Sets the position of the window in logical coordinates. All monitors span a virtual surface together. The coordinates are
	 * relative to the first monitor in the virtual surface. **/
	public void setPosition (int x, int y) {
		if (GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND) return;
		GLFW.glfwSetWindowPos(windowHandle, x, y);
	}

	/** @return the window position in logical coordinates. All monitors span a virtual surface together. The coordinates are
	 *         relative to the first monitor in the virtual surface. **/
	public int getPositionX () {
		GLFW.glfwGetWindowPos(windowHandle, tmpBuffer, tmpBuffer2);
		return tmpBuffer.get(0);
	}

	/** @return the window position in logical coordinates. All monitors span a virtual surface together. The coordinates are
	 *         relative to the first monitor in the virtual surface. **/
	public int getPositionY () {
		GLFW.glfwGetWindowPos(windowHandle, tmpBuffer, tmpBuffer2);
		return tmpBuffer2.get(0);
	}

	/** Sets the visibility of the window. Invisible windows will still call their {@link ApplicationListener} */
	public void setVisible (boolean visible) {
		if (visible) {
			GLFW.glfwShowWindow(windowHandle);
		} else {
			GLFW.glfwHideWindow(windowHandle);
		}
	}

	/** Closes this window and pauses and disposes the associated {@link ApplicationListener}. */
	public void closeWindow () {
		GLFW.glfwSetWindowShouldClose(windowHandle, true);
	}

	/** Minimizes (iconifies) the window. Iconified windows do not call their {@link ApplicationListener} until the window is
	 * restored. */
	public void iconifyWindow () {
		GLFW.glfwIconifyWindow(windowHandle);
	}

	/** Whether the window is iconfieid */
	public boolean isIconified () {
		return iconified;
	}

	/** De-minimizes (de-iconifies) and de-maximizes the window. */
	public void restoreWindow () {
		GLFW.glfwRestoreWindow(windowHandle);
	}

	/** Maximizes the window. */
	public void maximizeWindow () {
		GLFW.glfwMaximizeWindow(windowHandle);
	}

	/** Brings the window to front and sets input focus. The window should already be visible and not iconified. */
	public void focusWindow () {
		GLFW.glfwFocusWindow(windowHandle);
	}

	public boolean isFocused () {
		return focused;
	}

	/** Sets the icon that will be used in the window's title bar. Has no effect in macOS, which doesn't use window icons.
	 * @param image One or more images. The one closest to the system's desired size will be scaled. Good sizes include 16x16,
	 *           32x32 and 48x48. Pixmap format {@link Pixmap.Format#RGBA8888 RGBA8888} is preferred so the images will not have to
	 *           be copied and converted. The chosen image is copied, and the provided Pixmaps are not disposed. */
	public void setIcon (Pixmap... image) {
		setIcon(windowHandle, image);
	}

	static void setIcon (long windowHandle, String[] imagePaths, Files.FileType imageFileType) {
		if (SharedLibraryLoader.os == Os.MacOsX) return;

		Pixmap[] pixmaps = new Pixmap[imagePaths.length];
		for (int i = 0; i < imagePaths.length; i++) {
			pixmaps[i] = new Pixmap(Gdx.files.getFileHandle(imagePaths[i], imageFileType));
		}

		setIcon(windowHandle, pixmaps);

		for (Pixmap pixmap : pixmaps) {
			pixmap.dispose();
		}
	}

	static void setIcon (long windowHandle, Pixmap[] images) {
		if (SharedLibraryLoader.os == Os.MacOsX) return;
		if (GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND) return;

		GLFWImage.Buffer buffer = GLFWImage.malloc(images.length);
		Pixmap[] tmpPixmaps = new Pixmap[images.length];

		for (int i = 0; i < images.length; i++) {
			Pixmap pixmap = images[i];

			if (pixmap.getFormat() != Pixmap.Format.RGBA8888) {
				Pixmap rgba = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGBA8888);
				rgba.setBlending(Pixmap.Blending.None);
				rgba.drawPixmap(pixmap, 0, 0);
				tmpPixmaps[i] = rgba;
				pixmap = rgba;
			}

			GLFWImage icon = GLFWImage.malloc();
			icon.set(pixmap.getWidth(), pixmap.getHeight(), pixmap.getPixels());
			buffer.put(icon);

			icon.free();
		}

		buffer.position(0);
		GLFW.glfwSetWindowIcon(windowHandle, buffer);

		buffer.free();
		for (Pixmap pixmap : tmpPixmaps) {
			if (pixmap != null) {
				pixmap.dispose();
			}
		}

	}

	public void setTitle (CharSequence title) {
		GLFW.glfwSetWindowTitle(windowHandle, title);
	}

	/** Sets minimum and maximum size limits for the window. If the window is full screen or not resizable, these limits are
	 * ignored. Use -1 to indicate an unrestricted dimension. */
	public void setSizeLimits (int minWidth, int minHeight, int maxWidth, int maxHeight) {
		setSizeLimits(windowHandle, minWidth, minHeight, maxWidth, maxHeight);
	}

	static void setSizeLimits (long windowHandle, int minWidth, int minHeight, int maxWidth, int maxHeight) {
		GLFW.glfwSetWindowSizeLimits(windowHandle, minWidth > -1 ? minWidth : GLFW.GLFW_DONT_CARE,
			minHeight > -1 ? minHeight : GLFW.GLFW_DONT_CARE, maxWidth > -1 ? maxWidth : GLFW.GLFW_DONT_CARE,
			maxHeight > -1 ? maxHeight : GLFW.GLFW_DONT_CARE);
	}

	WebGPUGraphics getGraphics () {
		return graphics;
	}

	WebGPUInput getInput () {
		return input;
	}

	public long getWindowHandle () {
		return windowHandle;
	}

	void windowHandleChanged (long windowHandle) {
		this.windowHandle = windowHandle;
		input.windowHandleChanged(windowHandle);
	}

	boolean update () {
		this.webGPU = application.getWebGPU(); // not yet available in Window constructor
		this.surface = application.getSurface();

		if (!listenerInitialized) {
			initializeListener();
		}
		synchronized (runnables) {
			executedRunnables.addAll(runnables);
			runnables.clear();
		}
		for (Runnable runnable : executedRunnables) {
			runnable.run();
		}
		boolean shouldRender = executedRunnables.size > 0 || graphics.isContinuousRendering();
		executedRunnables.clear();

		if (!iconified) input.update();

		synchronized (this) {
			shouldRender |= requestRendering && !iconified;
			requestRendering = false;
		}

		// In case glfw_async is used, we need to resize outside the GLFW
		if (asyncResized) {
			asyncResized = false;
			graphics.updateFramebufferInfo();
			// graphics.gl20.glViewport(0, 0, graphics.getBackBufferWidth(), graphics.getBackBufferHeight());
			listener.resize(graphics.getWidth(), graphics.getHeight());
			graphics.update();
			// listener.render();
			// GLFW.glfwSwapBuffers(windowHandle);
			//renderFrame();
			return true;
		}

		if (shouldRender) {
			graphics.update();
			// listener.render();
			renderFrame();

			// GLFW.glfwSwapBuffers(windowHandle);
		}

		if (!iconified) input.prepareNext();

		return shouldRender;
	}

	public void renderFrame () {

		targetView = getNextSurfaceTextureView();
		if (targetView.address() == 0) {
			System.out.println("*** Invalid target view");
			return;
		}

		//commandEncoder = prepareEncoder();

		commandEncoder = new WebGPUCommandEncoder(device);

		if(application.getCommandEncoder() != commandEncoder){
			System.out.println("******* currentWindow not pointing to this window ******");
		}


		listener.render();	// call user code

		// finish command encoder to get a command buffer
		WebGPUCommandBuffer commandBuffer = commandEncoder.finish();
		commandEncoder.dispose();
		commandEncoder = null;
		queue.submit(commandBuffer);	// submit command buffer
		commandBuffer.dispose();

		// At the end of the frame
		webGPU.wgpuTextureViewRelease(targetView);
		webGPU.wgpuSurfacePresent(surface);
		targetView = null;
		device.tick();

	}

	private Pointer getNextSurfaceTextureView () {
		// [...] Get the next surface texture
		WGPUSurfaceTexture surfaceTexture = WGPUSurfaceTexture.createDirect();
		webGPU.wgpuSurfaceGetCurrentTexture(surface, surfaceTexture);
		// System.out.println("get current texture: "+surfaceTexture.status.get());
		if (surfaceTexture.getStatus() != WGPUSurfaceGetCurrentTextureStatus.Success) {
			System.out.println("*** No current texture");
			return JavaWebGPU.createNullPointer();
		}
		// [...] Create surface texture view
		WGPUTextureViewDescriptor viewDescriptor = WGPUTextureViewDescriptor.createDirect();
		viewDescriptor.setNextInChain();
		viewDescriptor.setLabel("Surface texture view");
		Pointer tex = surfaceTexture.getTexture();
		WGPUTextureFormat format = webGPU.wgpuTextureGetFormat(tex);
		// System.out.println("Set format "+format);
		viewDescriptor.setFormat(format);
		viewDescriptor.setDimension(WGPUTextureViewDimension._2D);
		viewDescriptor.setBaseMipLevel(0);
		viewDescriptor.setMipLevelCount(1);
		viewDescriptor.setBaseArrayLayer(0);
		viewDescriptor.setArrayLayerCount(1);
		viewDescriptor.setAspect(WGPUTextureAspect.All);
		Pointer view = webGPU.wgpuTextureCreateView(surfaceTexture.getTexture(), viewDescriptor);

		// we can release the texture now as the texture view now has its own reference to it
		webGPU.wgpuTextureRelease(surfaceTexture.getTexture());
		return view;
	}



	void requestRendering () {
		synchronized (this) {
			this.requestRendering = true;
		}
	}

	boolean shouldClose () {
		return GLFW.glfwWindowShouldClose(windowHandle);
	}

	WebGPUApplicationConfiguration getConfig () {
		return config;
	}

	boolean isListenerInitialized () {
		return listenerInitialized;
	}

	void initializeListener () {
		if (!listenerInitialized) {
			listener.create();
			listener.resize(graphics.getWidth(), graphics.getHeight());
			listenerInitialized = true;
		}
	}

	void makeCurrent () {
		Gdx.graphics = graphics;	// holds window size for example
		Gdx.input = input;
	}

	@Override
	public void dispose () {
		listener.pause();
		listener.dispose();
		WebGPUCursor.dispose(this);
		graphics.dispose();
		input.dispose();
		GLFW.glfwSetWindowFocusCallback(windowHandle, null);
		GLFW.glfwSetWindowIconifyCallback(windowHandle, null);
		GLFW.glfwSetWindowCloseCallback(windowHandle, null);
		GLFW.glfwSetDropCallback(windowHandle, null);
		GLFW.glfwDestroyWindow(windowHandle);

		focusCallback.free();
		iconifyCallback.free();
		maximizeCallback.free();
		closeCallback.free();
		dropCallback.free();
		refreshCallback.free();

		exitWebGPU();
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int)(windowHandle ^ (windowHandle >>> 32));
		return result;
	}

	@Override
	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		WebGPUWindow other = (WebGPUWindow)obj;
		if (windowHandle != other.windowHandle) return false;
		return true;
	}

	public void flash () {
		GLFW.glfwRequestWindowAttention(windowHandle);
	}
}
