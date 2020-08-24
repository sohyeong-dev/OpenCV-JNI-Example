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

    const float LIMIT_PX = 1024;

    Mat &src = *(Mat *) addr_mat;
    Mat &gray = *(Mat *) dst_mat;
//    cvtColor(src, gray, COLOR_BGRA2GRAY);

    int height = src.rows;
    int width = src.cols;

    // --- color_equalize ---

    Mat src_ycrcb;
    cvtColor(src, src_ycrcb, COLOR_BGR2YCrCb);

    vector<Mat> ycrcb_planes;
    split(src_ycrcb, ycrcb_planes);

    equalizeHist(ycrcb_planes[0], ycrcb_planes[0]); // Y channel

    Mat equalize_ycrcb;
    merge(ycrcb_planes, equalize_ycrcb);

    Mat equalize;
    cvtColor(equalize_ycrcb, equalize, COLOR_YCrCb2BGR);

    // --- morphology: 팽창 후 침식 ---
    Mat kernel = Mat::ones(5, 3, CV_8UC1);  // unsigned char, 1-channel
    morphologyEx(equalize, equalize, MORPH_CLOSE, kernel);

    // --- Blurring ---

    Mat blurred;
    medianBlur(equalize, blurred, 7);

    // --- inrange_hue ---

    Mat src_hsv;
    cvtColor(blurred, src_hsv, COLOR_BGR2HSV);

    vector<Mat> hsv_planes;
    split(src_hsv, hsv_planes);

    int half = cvRound(width / 2);

    int h_counter[256] = {0, };
    int s_counter[256] = {0, };
    int v_counter[256] = {0, };

    for (int i = 0; i < height; i++) {
        // uchar* h_p = hsv_planes[0].ptr<uchar>(i);
        // uchar* s_p = hsv_planes[1].ptr<uchar>(i);
        // uchar* v_p = hsv_planes[2].ptr<uchar>(i);
        // h_counter[h_p[half]]++;
        // s_counter[s_p[half]]++;
        // v_counter[v_p[half]]++;
        h_counter[hsv_planes[0].at<uchar>(i, half)]++;
        s_counter[hsv_planes[1].at<uchar>(i, half)]++;
        v_counter[hsv_planes[2].at<uchar>(i, half)]++;
    }

    int h_mode = 0;
    int s_mode = 0;
    int v_mode = 0;

    for (int i = 0; i < 256; i++) {
        if (h_counter[i] > h_counter[h_mode]) {
            h_mode = i;
        }
        if (s_counter[i] > s_counter[s_mode]) {
            s_mode = i;
        }
        if (v_counter[i] > v_counter[v_mode]) {
            v_mode = i;
        }
    }

    Scalar mode(h_mode, s_mode, v_mode);

    Mat mask;
    inRange(src_hsv, mode, mode, mask);

    // --- 자르기 ---

    if (mask.at<uchar>(half, 0) == 0) {
        uchar* p = mask.ptr<uchar>(half);
        for (int i = 0; i < width; i++) {
            if (p[i] == 255) {
                Rect roi(i + 2, 0, width - (i + 2), height);
                mask = mask(roi);
                Mat resized = Mat(height, width, CV_8UC1, Scalar(255));
                mask.copyTo(resized(Rect(0, 0, mask.cols, mask.rows)));
                mask = resized.clone();
                break;
            }
        }
    }

    // --- morphology: 팽창 후 침식 ---

    morphologyEx(mask, mask, MORPH_CLOSE, kernel);

    // --- Edge Detection ---

    Mat edge;
    Canny(mask, gray, 50, 150, 3);
}