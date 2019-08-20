//
// Created by Wafer Li on 2019-08-20.
//

#include "LameWrapper.h"

extern "C" jstring JNICALL
Java_com_example_androidmediapractice_main_task2_LameWrapper_init(JNIEnv *jniEnv,
                                                                  jobject jobject1) {
  return jniEnv->NewStringUTF("Hello,World");
}


