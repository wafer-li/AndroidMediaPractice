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
      lame_set_mode(lame, MONO);
      lame_set_brate(lame, bitRates / 1000);
      lame_init_params(lame);
      result = 0;
    }
  }
  return result;
}
void Mp3Encoder::encode() {
  auto *buffer = new short[bufferSize / 2];
  auto *mp3Buffer = new unsigned char[bufferSize];

  while (int
      readBufferSize = fread(buffer, sizeof(short), static_cast<size_t>(bufferSize / 2), pcmFile)) {
    int code = lame_encode_buffer(lame,
                                  buffer,
                                  nullptr,
                                  readBufferSize,
                                  mp3Buffer,
                                  bufferSize);
    fwrite(mp3Buffer, sizeof(unsigned char), static_cast<size_t>(code), mp3File);
  }
  delete[] buffer;
  delete[] mp3Buffer;
}
void Mp3Encoder::destroy() {
  if (pcmFile != nullptr) {
    fclose(pcmFile);
  }
  if (mp3File != nullptr) {
    fclose(mp3File);
    lame_close(lame);
  }
}
