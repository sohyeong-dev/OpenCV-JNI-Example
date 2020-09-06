#include <jni.h>
#include <string>
#include <vector>
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
JNIEXPORT jobjectArray JNICALL
Java_com_example_opencvjniexample_MainActivity_detectPlaylistJNI(JNIEnv *env, jobject thiz,
                                                                 jstring path, jlong dst_mat) {   // jlong addr_mat
    // TODO: implement detectPlaylistJNI()

    jboolean isCopy;
    const char *imagePath = (env)->GetStringUTFChars(path, &isCopy);

//    Mat &src = *(Mat *) addr_mat;
    Mat &gray = *(Mat *) dst_mat;
    Mat src = imread(imagePath, IMREAD_COLOR);
    cvtColor(src, gray, COLOR_BGRA2GRAY);

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

    // --- morphology: 팽창 후 침식 ---

    morphologyEx(mask, mask, MORPH_CLOSE, kernel);

    // --- Edge Detection ---

    Mat edge;
    Canny(mask, edge, 50, 150, 3);

    // --- Find Rectangle ---

    vector<Vec2f> lines;
    HoughLines(edge, lines, 1, CV_PI / 180, 100); // 250

    cvtColor(gray, gray, COLOR_GRAY2BGR);

    vector<int> temp_x;

    for (size_t i = 0; i < lines.size(); i++) {
        float rho = lines[i][0], theta = lines[i][1];
        if (theta == 0.0) {
            float cos_t = cos(theta), sin_t = sin(theta);
            float x0 = rho * cos_t, y0 = rho * sin_t;
            float alpha = 1000;

            temp_x.push_back(cvRound(x0 - alpha * sin_t));

            Point pt1(cvRound(x0 - alpha * sin_t), cvRound(y0 + alpha * cos_t));
            Point pt2(cvRound(x0 + alpha * sin_t), cvRound(y0 - alpha * cos_t));
            line(gray, pt1, pt2, (0, 0, 255), 2, LINE_AA);
            putText(gray, to_string(theta), Point(x0, y0), FONT_HERSHEY_PLAIN, 1, Scalar(0, 0, 255));
        }
    }

    sort(temp_x.begin(), temp_x.end());
    int album_w = temp_x[1] - temp_x[0];

    // --- Find Contours ---

    vector<vector<Point>> contours;
    findContours(edge, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
/*
  Mat dst = src.clone();

  Scalar c(255, 0, 0);

  for (int i = 0; i < contours.size(); i++) {
    drawContours(dst, contours, i, c, 2);
  }
*/

/*
    Scalar c(255, 0, 0);

    for (int i = 0; i < contours.size(); i++) {
        drawContours(gray, contours, i, c, 2);
    }
*/
    int cnt = 0;

    vector<Mat> mats;

    for (vector<Point> pts : contours) {
        Rect rc = boundingRect(pts);
        double x = rc.x;
        double y = rc.y;
        double w = rc.width;
        double h = rc.height;
/*
        rectangle(gray, rc, Scalar(0, 0, 255), 1);
        putText(gray, to_string(w * h), Point(x, y), FONT_HERSHEY_PLAIN, 1, Scalar(0, 0, 255));
*/
        if (x > temp_x[0] - 3 && x < temp_x[1] + 3 && h > album_w - 3) {
//        if (w / h > 0.8 && w / h < 1.2 && w * h > 300) {
            cnt++;
            rectangle(gray, rc, Scalar(0, 0, 255), 1);
            // album image
            Mat abm = src(Range(y, y + h), Range(x, x + w));
            cvtColor(abm, abm, COLOR_BGR2RGB);
            mats.push_back(abm);
            // ocr image
            Mat roi = src(Range(y, y + h), Range(x + w, width));
            cvtColor(roi, roi, COLOR_BGR2RGB);
            mats.push_back(roi);
        }
    }

    // for ArrayList return
    jclass matClass = env->FindClass("org/opencv/core/Mat");
    jmethodID jMatCons = env->GetMethodID(matClass, "<init>", "()V");
    jmethodID getPtrMethod = env->GetMethodID(matClass, "getNativeObjAddr", "()J");
    jobjectArray matArray = env->NewObjectArray(cnt * 2, matClass, 0);

    for (int i = 0; i < mats.size(); ++i) {
        jobject jMat = env->NewObject(matClass, jMatCons);
        Mat& tempMat = *(Mat*)env->CallLongMethod(jMat, getPtrMethod);
        tempMat = mats[i];
        env->SetObjectArrayElement(matArray, i, jMat);
    }

    return matArray;
}