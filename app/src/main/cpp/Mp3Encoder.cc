//
// Created by Wafer Li on 2019-08-20.
//

#include "Mp3Encoder.h"
int Mp3Encoder::init(const char *pcmPath,
                     const char *mp3Path,
                     int audioChannels,
                     int bitRates,
                     int sampleRates) {
  int result = -1;
  if (pcmPath == nullptr || pcmPath[0] == '\0' || mp3Path == nullptr || mp3Path[0] == '\0') {
    return result;
  }
  pcmFile = fopen(pcmPath, "rbe");
  if (pcmFile != nullptr) {
    mp3File = fopen(mp3Path, "wbe");
    if (mp3File != nullptr) {
      lame = lame_init();
      lame_set_in_samplerate(lame, sampleRates);
      lame_set_out_samplerate(lame, sampleRates);
      lame_set_num_channels(lame, audioChannels);
      lame_set_brate(lame, bitRates);
      lame_init_params(lame);
      result = 0;
    }
  }
  return result;
}
void Mp3Encoder::encode() {
  auto *buffer = new short[bufferSize / 2];
  auto *leftBuffer = new short[bufferSize / 4];
  auto *rightBuffer = new short[bufferSize / 4];
  auto mp3Buffer = new unsigned char[bufferSize];

  while (int readBufferSize = fread(buffer, 2, static_cast<size_t>(bufferSize / 2), pcmFile)) {
    for (int i = 0; i < readBufferSize; i++) {
      if (i % 2 == 0) {
        leftBuffer[i / 2] = buffer[i];
      } else {
        rightBuffer[i / 2] = buffer[i];
      }
    }
    int code = lame_encode_buffer(lame,
                                  leftBuffer,
                                  rightBuffer,
                                  readBufferSize / 2,
                                  mp3Buffer,
                                  bufferSize);
    if (code == 0) {
      fwrite(mp3Buffer, 1, static_cast<size_t>(bufferSize), mp3File);
    }
  }
  delete[] buffer;
  delete[] leftBuffer;
  delete[] rightBuffer;
  delete[] mp3Buffer;
}
void Mp3Encoder::destroy() {
  if (mp3File != nullptr) {
    fclose(mp3File);
  }
  if (pcmFile != nullptr) {
    fclose(pcmFile);
  }
}
