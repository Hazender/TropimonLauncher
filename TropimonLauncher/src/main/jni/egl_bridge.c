#include <jni.h>
#include <assert.h>
#include <dlfcn.h>

#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>

#include <EGL/egl.h>
#include <GL/osmesa.h>
#include "ctxbridges/egl_loader.h"
#include "ctxbridges/osmesa_loader.h"
#include "ctxbridges/renderer_config.h"
#include "driver_helper/nsbypass.h"

#ifdef GLES_TEST
#include <GLES2/gl2.h>
#endif

#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/rect.h>
#include <string.h>
#include <environ/environ.h>
#include <android/dlext.h>
#include <time.h>
#include "utils.h"
#include "ctxbridges/bridge_tbl.h"
#include "ctxbridges/osm_bridge.h"

#define GLFW_CLIENT_API 0x22001
/* Consider GLFW_NO_API as Vulkan API */
#define GLFW_NO_API 0
#define GLFW_OPENGL_API 0x30001

// This means that the function is an external API and that it will be used
#define EXTERNAL_API __attribute__((used))
// This means that you are forced to have this function/variable for ABI compatibility
#define ABI_COMPAT __attribute__((unused))

EGLConfig config;

void calculateFPS();

EXTERNAL_API void pojavTerminate() {
    printf("EGLBridge: Terminating\n");

    const char *renderer = getenv("POJAV_RENDERER");
    if (renderer && !strncmp("opengles", renderer, 8)) {
        eglReleaseThread_p();
    }
}

JNIEXPORT void JNICALL Java_com_hazender_tropimonlauncher_bridge_ZLBridge_setupBridgeWindow(JNIEnv* env, ABI_COMPAT jclass clazz, jobject surface) {
    pojav_environ->pojavWindow = ANativeWindow_fromSurface(env, surface);
    if (br_setup_window) br_setup_window();
}

JNIEXPORT void JNICALL
Java_com_hazender_tropimonlauncher_bridge_ZLBridge_releaseBridgeWindow(ABI_COMPAT JNIEnv *env, ABI_COMPAT jclass clazz) {
    ANativeWindow_release(pojav_environ->pojavWindow);
}

EXTERNAL_API void* pojavGetCurrentContext() {
    return br_get_current();
}

int pojavInitOpenGL() {
    const char *renderer = getenv("POJAV_RENDERER");

    if (renderer && !strncmp("opengles", renderer, 8)) {
        set_gl_bridge_tbl();
    }

    if (br_init()) br_setup_window();

    return 0;
}

EXTERNAL_API int pojavInit() {
    ANativeWindow_acquire(pojav_environ->pojavWindow);
    pojav_environ->savedWidth = ANativeWindow_getWidth(pojav_environ->pojavWindow);
    pojav_environ->savedHeight = ANativeWindow_getHeight(pojav_environ->pojavWindow);
    ANativeWindow_setBuffersGeometry(pojav_environ->pojavWindow,pojav_environ->savedWidth,pojav_environ->savedHeight,AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM);
    pojavInitOpenGL();
    return 1;
}

EXTERNAL_API void pojavSetWindowHint(int hint, int value) {
    if (hint != GLFW_CLIENT_API) return;
    if (value == GLFW_OPENGL_API) {
        // Nothing to do: initialization is called in pojavCreateContext
    } else {
        printf("GLFW: Unimplemented API 0x%x\n", value);
        abort();
    }
}

EXTERNAL_API void* pojavCreateContext(void* contextSrc) {
    return br_init_context((basic_render_window_t*)contextSrc);
}

EXTERNAL_API void pojavSwapBuffers() {
    calculateFPS();
    br_swap_buffers();
}

EXTERNAL_API void pojavMakeCurrent(void* window) {
    br_make_current((basic_render_window_t*)window);
}

EXTERNAL_API void pojavSwapInterval(int interval) {
    br_swap_interval(interval);
}

static int frameCount = 0;
static time_t lastTime = 0;

void calculateFPS() {
    frameCount++;
    time_t currentTime = time(NULL);

    if (currentTime != lastTime) {
        lastTime = currentTime;

        if (!pojav_environ->dalvikJavaVMPtr ||!pojav_environ->class_ZLInvoker ||!pojav_environ->method_PutFpsValue) {
            return;
        }

        JNIEnv *dalvikEnv;
        (*pojav_environ->dalvikJavaVMPtr)->AttachCurrentThread(pojav_environ->dalvikJavaVMPtr,&dalvikEnv,NULL);
        (*dalvikEnv)->CallStaticVoidMethod(dalvikEnv,pojav_environ->class_ZLInvoker,pojav_environ->method_PutFpsValue,(jint) frameCount);
        (*pojav_environ->dalvikJavaVMPtr)->DetachCurrentThread(pojav_environ->dalvikJavaVMPtr);

        frameCount = 0;
    }
}
