//
// Created by Wafer Li on 2019-08-20.
//

#ifndef ANDROIDMEDIAPRACTICE_MP3ENCODER_H
#define ANDROIDMEDIAPRACTICE_MP3ENCODER_H


#include <stdio.h>
#include <lame.h>
class Mp3Encoder {
 private:
  FILE *pcmFile{};
  FILE *mp3File{};
  lame_t lame{};
  int bufferSize = 1024 * 256;

 public:
  int init(const char *pcmPath,
           const char *mp3Path,
           int audioChannels, int bitRates, int sampleRates);
  void encode();
  void destroy();
};


#endif //ANDROIDMEDIAPRACTICE_MP3ENCODER_H
