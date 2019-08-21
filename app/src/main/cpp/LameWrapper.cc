//
// Created by Wafer Li on 2019-08-20.
//

#include "LameWrapper.h"
#include "Mp3Encoder.h"

Mp3Encoder *mp3Encoder;

extern "C" void Java_com_example_androidmediapractice_main_task2_LameWrapper_init(JNIEnv *jniEnv,
                                                                                  jobject jClass,
                                                                                  jstring pcmPath,
                                                                                  jint audioChannels,
                                                                                  jint bitRates,
                                                                                  jint sampleRate,
                                                                                  jstring mp3Path) {
  mp3Encoder = new Mp3Encoder();
  const char *pcmFilePath = jniEnv->GetStringUTFChars(pcmPath, nullptr);
  const char *mp3FilePath = jniEnv->GetStringUTFChars(mp3Path, nullptr);
  mp3Encoder->init(pcmFilePath, mp3FilePath, audioChannels, bitRates, sampleRate);
  jniEnv->ReleaseStringUTFChars(pcmPath, pcmFilePath);
  jniEnv->ReleaseStringUTFChars(mp3Path, mp3FilePath);
}

extern "C" void Java_com_example_androidmediapractice_main_task2_LameWrapper_encode(JNIEnv *jniEnv,
                                                                                    jobject jClass) {
  if (mp3Encoder != nullptr) {
    mp3Encoder->encode();
  }
}
extern "C" void Java_com_example_androidmediapractice_main_task2_LameWrapper_destroy(JNIEnv *jniEnv,
                                                                                     jobject jClass) {
  if (mp3Encoder != nullptr) {
    mp3Encoder->destroy();
  }
  mp3Encoder = nullptr;
}


