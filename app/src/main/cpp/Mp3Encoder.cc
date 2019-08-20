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

}
void Mp3Encoder::destroy() {

}
