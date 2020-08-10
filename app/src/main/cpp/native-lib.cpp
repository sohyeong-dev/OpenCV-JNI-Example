#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_opencvjniexample_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_opencvjniexample_MainActivity_detectPlaylistJNI(JNIEnv *env, jobject thiz,
                                                                 jlong addr_mat, jlong dst_mat) {
    // TODO: implement detectPlaylistJNI()

    Mat &src = *(Mat *) addr_mat;
    Mat &gray = *(Mat *) dst_mat;
    cvtColor(src, gray, COLOR_BGRA2GRAY);
}