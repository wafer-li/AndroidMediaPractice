//
// Created by Wafer Li on 2019-08-27.
//

#include "LameWrapper.h"
#include "lame.h"

lame_t lameClient = nullptr;

extern "C" void Java_com_example_androidmediapractice_main_task2_LameWrapper_init(JNIEnv *jniEnv,
                                                                                  jobject jClass,
                                                                                  jint inSampleRate,
                                                                                  jint outSampleRate,
                                                                                  jint outChannels,
                                                                                  jint outBitRate,
                                                                                  jint quality) {
  lameClient = lame_init();
  lame_set_in_samplerate(lameClient, inSampleRate);
  lame_set_out_samplerate(lameClient, outSampleRate);
  lame_set_num_channels(lameClient, outChannels);
  lame_set_brate(lameClient, outBitRate / 1000);
  lame_set_quality(lameClient, quality);
  lame_init_params(lameClient);
}

extern "C" void Java_com_example_androidmediapractice_main_task2_LameWrapper_encode(JNIEnv *jniEnv,
                                                                                    jobject jClass,
                                                                                    jshortArray bufferLeft,
                                                                                    jshortArray bufferRight,
                                                                                    jint samples,
                                                                                    jbyteArray mp3Buffer) {
  jshort *buffer_l = jniEnv->GetShortArrayElements(bufferLeft, nullptr);
  jshort *buffer_r =
      bufferRight != nullptr ? jniEnv->GetShortArrayElements(bufferRight, nullptr) : nullptr;
  jbyte *mp3_buffer = jniEnv->GetByteArrayElements(mp3Buffer, nullptr);

  const jsize mp3_buffer_size = jniEnv->GetArrayLength(mp3Buffer);

  lame_encode_buffer(lameClient, buffer_l, buffer_r, samples,
                     reinterpret_cast<unsigned char *>(mp3_buffer), mp3_buffer_size);
  jniEnv->ReleaseShortArrayElements(bufferLeft, buffer_l, 0);
  if (buffer_r != nullptr) {
    jniEnv->ReleaseShortArrayElements(bufferRight, buffer_r, 0);
  }
  jniEnv->ReleaseByteArrayElements(mp3Buffer, mp3_buffer, 0);
}

extern "C" jint Java_com_example_androidmediapractice_main_task2_LameWrapper_flush(JNIEnv *jniEnv,
                                                                                   jobject jCliass,
                                                                                   jbyteArray mp3Buffer) {
  if (lameClient != nullptr) {
    jbyte *mp3_buffer = jniEnv->GetByteArrayElements(mp3Buffer, nullptr);
    jsize mp3_size = jniEnv->GetArrayLength(mp3Buffer);

    int result = lame_encode_flush(lameClient,
                                   reinterpret_cast<unsigned char *>(mp3_buffer), mp3_size);
    return result;
  } else {
    return -1;
  }
}

extern "C" void
Java_com_example_androidmediapractice_main_task2_LameWrapper_close(JNIEnv *jniEnv, jobject jClass) {
  lame_close(lameClient);
  lameClient = nullptr;
}


