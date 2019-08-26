#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include "DCR.h"
#include "FaceFinder.h"

#ifdef __cplusplus
extern "C" {
#endif

#define ALIGN32(d) ((~0x03)&(d+0x03))

#define FACT_P16 (1.f/(1<<16))
static float from_s16p16(s16p16 value) {return FACT_P16*value;}

struct FD {
    btk_HSDK sdk;
    btk_HDCR dcr;
    btk_HFaceFinder fd;
    btk_Rect face;  //last face rect
    unsigned int rotation;  //src rotation
    unsigned int width;     //dst width
    unsigned int height;    //dst height
    void* y8;   //dst buffer
};

static void CHECK(btk_Error s, const char* e) {
    if(btk_STATUS_OK!=s)
        __android_log_print(ANDROID_LOG_ERROR, "STATUS", "%s %i\n", e, s);
}

static btk_FaceFinderCreateParam fdParam = {0};

JNIEXPORT JNICALL jboolean Java_com_tdim_qas_FaceDetectorBTK_isInitialized(JNIEnv* env, jclass self) {
    return NULL!=fdParam.pModuleParam;
}

JNIEXPORT JNICALL void Java_com_tdim_qas_FaceDetectorBTK_globalFini(JNIEnv* env, jclass self) {
    free(fdParam.pModuleParam);
    memset(&fdParam, 0, sizeof(btk_FaceFinderCreateParam));
}
JNIEXPORT JNICALL jint Java_com_tdim_qas_FaceDetectorBTK_globalInit(JNIEnv* env, jclass self, jbyteArray data, jint maxFaces) {
    Java_com_tdim_qas_FaceDetectorBTK_globalFini(env, self);
    if(NULL==data||1>maxFaces)
        return -1;
    fdParam = btk_FaceFinder_defaultParam();
	fdParam.maxDetectableFaces = maxFaces;
	fdParam.moduleParamSize = (*env)->GetArrayLength(env, data);
	fdParam.pModuleParam = malloc(fdParam.moduleParamSize);
    jbyte* raw = (*env)->GetByteArrayElements(env, data, NULL);
    memcpy(fdParam.pModuleParam, raw, fdParam.moduleParamSize);
    (*env)->ReleaseByteArrayElements(env, data, raw, 0);
    return fdParam.moduleParamSize;
}

JNIEXPORT JNICALL void Java_com_tdim_qas_FaceDetectorBTK_setEyeDistance(JNIEnv* env, jclass self, jlong pDetector, jint min, jint max) {
    struct FD* detector = (struct FD*)pDetector;
    if(NULL==detector)
        return;
    btk_FaceFinder_setRange(detector->fd, min, max);
}

static void detector_fini(struct FD* detector) {
    btk_FaceFinder_close(detector->fd);
    btk_DCR_close(detector->dcr);
    btk_SDK_close(detector->sdk);
    free(detector->y8);
}
JNIEXPORT JNICALL void Java_com_tdim_qas_FaceDetectorBTK_destroy(JNIEnv* env, jclass self, jlong pDetector) {
    struct FD* detector = (struct FD*) pDetector;
    if(NULL==detector)
        return;
    detector_fini(detector);
    free(detector);
}

static btk_Status detector_init(struct FD* detector, jint width, jint height, jint rotation) {
    btk_Status status = NULL==detector?btk_STATUS_INVALID_HANDLE:btk_STATUS_OK;

    if(btk_STATUS_OK==status) {
        memset(detector, 0, sizeof(struct FD));
        if(0<width&&0<height)
            detector->y8 = malloc(ALIGN32(width)*ALIGN32(height));
        else
            status = btk_STATUS_ERROR;
    }

    if(btk_STATUS_OK==status) {
        btk_SDKCreateParam sdkParam = btk_SDK_defaultParam();
        sdkParam.fpMalloc = malloc;
        sdkParam.fpFree = free;
        detector->rotation = 0x03&rotation;
        if(0==(0x01&detector->rotation)) {
            sdkParam.maxImageWidth =detector->width = width;
            sdkParam.maxImageHeight=detector->height= height;
        } else {
            sdkParam.maxImageWidth =detector->width = height;
            sdkParam.maxImageHeight=detector->height= width;
        }
        detector->face.xMin=detector->face.yMin = 0;
        detector->face.xMax = detector->width<<16;
        detector->face.yMax = detector->height<<16;
        status = btk_SDK_create(&sdkParam, &detector->sdk);
	}

    if(btk_STATUS_OK==status) {
        btk_DCRCreateParam dcrParam = btk_DCR_defaultParam();
        status = btk_DCR_create(detector->sdk, &dcrParam, &detector->dcr);
    }

    if(btk_STATUS_OK==status)
	    status = btk_FaceFinder_create(detector->sdk, &fdParam, &detector->fd);

    return status;
}
JNIEXPORT JNICALL jlong Java_com_tdim_qas_FaceDetectorBTK_create(JNIEnv* env, jclass self, jint width, jint height, jint rotation) {
    btk_Status status = btk_STATUS_OK;
	struct FD* detector = NULL;

    if(Java_com_tdim_qas_FaceDetectorBTK_isInitialized(env, self)) {
        detector = malloc(sizeof(struct FD));
        if(btk_STATUS_OK!=detector_init(detector, width, height, rotation)) {
            Java_com_tdim_qas_FaceDetectorBTK_destroy(env, self, (jlong)detector);
            detector = NULL;
        }
    }

	return (jlong)detector;
}

JNIEXPORT JNICALL void Java_com_tdim_qas_FaceDetectorBTK_setRotation(JNIEnv* env, jclass self, jlong pDetector, jint rotation) {
    struct FD* detector = (struct FD*)pDetector;
    if(NULL==detector)
        return;
    if((0x01&detector->rotation)==(0x01&rotation))//orientation reversed
        detector->rotation = 0x03&rotation;
    /*else {
        detector_fini(detector);
        if(0==(0x01&detector->rotation))//from landscape to portrait
            detector_init(detector, detector->width, detector->height, rotation);
        else//from portrait to landscape
            detector_init(detector, detector->height, detector->width, rotation);
    }*/
}

#define MIN(a, b) (((a)<(b))?(a):(b))
#define MAX(a, b) (((a)>(b))?(a):(b))
JNIEXPORT JNICALL jint Java_com_tdim_qas_FaceDetectorBTK_detect(JNIEnv* env, jclass self, jlong pDetector, jbyteArray data, jfloatArray posLR) {
    btk_Status status = btk_STATUS_OK;
    struct FD* detector = (struct FD*)pDetector;
    int faces = 0;
    jsize lenData = NULL==data?0:(*env)->GetArrayLength(env, data);
    jsize lenPosLR = NULL==posLR?0:(*env)->GetArrayLength(env, posLR);

    if(NULL==detector||detector->width*detector->height>lenData||4*fdParam.maxDetectableFaces>lenPosLR)
        return -1;

    void* raw = (*env)->GetPrimitiveArrayCritical(env, data, NULL);
    switch(detector->rotation) {
    default:
    case 0://0 deg
        memcpy(detector->y8, raw, detector->width*detector->height);
        break;
    case 2://180 deg
        {
        const unsigned int* src = (const unsigned int*)raw;
        unsigned int* dst = (unsigned int*)detector->y8;
        int w = (detector->width+0x03)>>2;
        int last = w*detector->height-1;
        for(int y=0;y<detector->height;y++)
            for(int x=0;x<w;x++) {
                int off = y*w+x;
                unsigned int b = src[last-off];
                dst[off] = ((0xFF000000&b)>>24)|((0x00FF0000&b)>>8)|((0x0000FF00&b)<<8)|((0x000000FF&b)<<24);
            }
        }
        break;
    case 3://90 deg
        {
        int w = (detector->width+0x03)>>2;
        int h = (detector->height+0x03)>>2;
        int b4[4];
        for(int y=0;y<h;y++) //every block of 4 lines
            for(int x=0;x<w;x++) {//every block of 4 columns
                const unsigned int* src = ((const unsigned int*)raw)+y+x*4*h;//T->B, L->R
                unsigned int* dst = ((unsigned int*)detector->y8)+x+(h-1-y)*4*w;//L->R, B->T
                for(int l=0;l<4;l++)
                    b4[l] = src[l*h];
                dst[0*w] = ((0x000000FF&b4[0])>> 0)|((0x000000FF&b4[1])<< 8)|((0x000000FF&b4[2])<<16)|((0x000000FF&b4[3])<<24);
                dst[1*w] = ((0x0000FF00&b4[0])>> 8)|((0x0000FF00&b4[1])<< 0)|((0x0000FF00&b4[2])<< 8)|((0x0000FF00&b4[3])<<16);
                dst[2*w] = ((0x00FF0000&b4[0])>>16)|((0x00FF0000&b4[1])>> 8)|((0x00FF0000&b4[2])>> 0)|((0x00FF0000&b4[3])<< 8);
                dst[3*w] = ((0xFF000000&b4[0])>>24)|((0xFF000000&b4[1])>>16)|((0xFF000000&b4[2])>> 8)|((0xFF000000&b4[3])<< 0);
            }
        }
        break;
    case 1://270 deg
        {
        int w = (detector->width+0x03)>>2;
        int h = (detector->height+0x03)>>2;
        int b4[4];
        for(int y=0;y<h;y++) //every block of 4 lines
            for(int x=0;x<w;x++) {//every block of 4 columns
                const unsigned int* src = ((const unsigned int*)raw)+y+x*4*h;//T->B, L->R
                unsigned int* dst = ((unsigned int*)detector->y8)+(w-1-x)+y*4*w;//R->L, T->B
                for(int l=0;l<4;l++)
                    b4[l] = src[l*h];
                dst[0*w] = ((0xFF000000&b4[0])>>24)|((0xFF000000&b4[1])>>16)|((0xFF000000&b4[2])>> 8)|((0xFF000000&b4[3])<< 0);
                dst[1*w] = ((0x00FF0000&b4[0])>>16)|((0x00FF0000&b4[1])>> 8)|((0x00FF0000&b4[2])>> 0)|((0x00FF0000&b4[3])<< 8);
                dst[2*w] = ((0x0000FF00&b4[0])>> 8)|((0x0000FF00&b4[1])<< 0)|((0x0000FF00&b4[2])<< 8)|((0x0000FF00&b4[3])<<16);
                dst[3*w] = ((0x000000FF&b4[0])>> 0)|((0x000000FF&b4[1])<< 8)|((0x000000FF&b4[2])<<16)|((0x000000FF&b4[3])<<24);
            }
        }
        break;
    }
    (*env)->ReleasePrimitiveArrayCritical(env, data, raw, 0);

    //status = btk_DCR_assignImage(detector->dcr, detector->y8, detector->width, detector->height);
    status = btk_DCR_assignImageROI(detector->dcr, detector->y8, detector->width, detector->height, &detector->face);
    CHECK(status, "btk_DCR_assignImage");
    status = btk_FaceFinder_putDCR(detector->fd, detector->dcr);
    CHECK(status, "btk_FaceFinder_putDCR");
    faces = btk_FaceFinder_faces(detector->fd);

    if(0<faces) {
        btk_Rect rect = {0};
        btk_Node node = {0};
        btk_Node left, right;
        left.id=right.id = -1;
        status = btk_FaceFinder_getDCR(detector->fd, detector->dcr);
        CHECK(status, "btk_FaceFinder_getDCR");

        status = btk_DCR_getRect(detector->dcr, &rect);
        CHECK(status, "btk_DCR_getRect");
        s16p16 x = (rect.xMax+rect.xMin)>>1;
        s16p16 y = (rect.yMax+rect.yMin)>>1;
        s16p16 w_2 = (s16p16)(1.2f*((rect.xMax-rect.xMin)>>1));
        s16p16 h_2 = (s16p16)(1.2f*((rect.yMax-rect.yMin)>>1));
        //detector->face.xMin = MAX(x-w_2, 0);
        //detector->face.yMin = MAX(y-h_2, 0);
        //detector->face.xMax = MIN(x+w_2, detector->width);
        //detector->face.yMax = MIN(y+h_2, detector->height);

        for(int i=0;i<btk_DCR_nodeCount(detector->dcr);i++)
            if(btk_STATUS_OK==(status=btk_DCR_getNode(detector->dcr, i, &node))) {
                if(0==node.id)
                    left = node;
                else if(1==node.id)
                    right = node;
            }

        if(0>left.id||0>right.id)
            faces = 0;
        else {
            jfloat* coord = (*env)->GetFloatArrayElements(env, posLR, NULL);
            coord[0] = from_s16p16(left.x);
            coord[1] = from_s16p16(left.y);
            coord[2] = from_s16p16(right.x);
            coord[3] = from_s16p16(right.y);
            (*env)->ReleaseFloatArrayElements(env, posLR, coord, 0);
        }
    } else {
        detector->face.xMin=detector->face.yMin = 0;
        detector->face.xMax = detector->width<<16;
        detector->face.yMax = detector->height<<16;
    }
    return faces;
}

#ifdef __cplusplus
}
#endif
