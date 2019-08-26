#ifndef __FFPLAYER_LOG_H_
#define __FFPLAYER_LOG_H_

#include <stdio.h>
#include <stdlib.h>
#include <chrono>


//std::time_t getTimeStamp() ;

#ifndef TAG
#define TAG "Holography"
#endif

#ifndef TAG2
#define TAG2 "CTest"
#endif

#ifdef  __x86_64

//#define LOGV(x,...) (void)0

#define LOGV(x, ...)    fprintf(stdout,"%s:  " x "\n",TAG, ##__VA_ARGS__);
#define LOGD(x, ...)    fprintf(stdout,"%s:  " x "\n", TAG, ##__VA_ARGS__);
#define LOGI(x, ...)    fprintf(stderr,"%s:  " x "\n", TAG, ##__VA_ARGS__);
#define LOGW(x, ...)    fprintf(stderr,"%s:  " x "\n", TAG, ##__VA_ARGS__);
#define LOGE(x, ...)    fprintf(stderr,"%s:  " x "\n",TAG,  ##__VA_ARGS__);

#else

#include <android/log.h>
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO ,   TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN ,   TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR ,  TAG, __VA_ARGS__)


#define LOGIT(...) __android_log_print(ANDROID_LOG_INFO ,   TAG2, __VA_ARGS__)

#endif


#define XWL 1
#define FI 1


#if XWL
#define LOGXWL(x, ...) LOGI("%s(%d)  " x,  __FUNCTION__,__LINE__, ##__VA_ARGS__)
#else
#define LOGXWL(x,...) (void)0
#endif

#if FI
#define LOGFI(x, ...) LOGI("%s(%d)  " x,  __FUNCTION__,__LINE__, ##__VA_ARGS__)
#else
#define LOGFI(x,...) (void)0
#endif




#endif
