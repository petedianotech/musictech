#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_PerformanceEnhancer_getNativeOptimizationFlag(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "C++ Performance Optimizer Loaded (NDK Ready)";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_PerformanceEnhancer_calculateDurationOpt(JNIEnv *env, jobject thiz, jlong duration) {
    // Some fast native operation (e.g., bitwise scaling for fast processing simulated)
    return duration + 100;
}
