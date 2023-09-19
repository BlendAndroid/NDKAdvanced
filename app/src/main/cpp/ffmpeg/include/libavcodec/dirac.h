#ifndef AVCODEC_DIRAC_H
#define AVCODEC_DIRAC_H

/**
 * @file
 * Interface to Dirac Decoder/Encoder
 */

#include "avcodec.h"

/**
 * The spec limits the number of wavelet decompositions to 4 for both
 * level 1 (VC-2) and 128 (long-gop default).
 * 5 decompositions is the maximum before >16-bit buffers are needed.
 * Schroedinger allows this for DD 9,7 and 13,7 wavelets only, limiting
 * the others to 4 decompositions (or 3 for the fidelity filter).
 *
 * We use this instead of MAX_DECOMPOSITIONS to save some memory.
 */
#define MAX_DWT_LEVELS 5

/**
 * Parse code values:
 *
 * Dirac Specification ->
 * 9.6.1  Table 9.1
 *
 * VC-2 Specification  ->
 * 10.4.1 Table 10.1
 */

enum DiracParseCodes {
    DIRAC_PCODE_SEQ_HEADER      = 0x00,
    DIRAC_PCODE_END_SEQ         = 0x10,
    DIRAC_PCODE_AUX             = 0x20,
    DIRAC_PCODE_PAD             = 0x30,
    DIRAC_PCODE_PICTURE_CODED   = 0x08,
    DIRAC_PCODE_PICTURE_RAW     = 0x48,
    DIRAC_PCODE_PICTURE_LOW_DEL = 0xC8,
    DIRAC_PCODE_PICTURE_HQ      = 0xE8,
    DIRAC_PCODE_INTER_NOREF_CO1 = 0x0A,
    DIRAC_PCODE_INTER_NOREF_CO2 = 0x09,
    DIRAC_PCODE_INTER_REF_CO1   = 0x0D,
    DIRAC_PCODE_INTER_REF_CO2   = 0x0E,
    DIRAC_PCODE_INTRA_REF_CO    = 0x0C,
    DIRAC_PCODE_INTRA_REF_RAW   = 0x4C,
    DIRAC_PCODE_INTRA_REF_PICT  = 0xCC,
    DIRAC_PCODE_MAGIC           = 0x42424344,
};

typedef struct DiracVersionInfo {
    int major;
    int minor;
} DiracVersionInfo;

typedef struct AVDiracSeqHeader {
    unsigned width;
    unsigned height;
    uint8_t chroma_format;          ///< 0: 444  1: 422  2: 420

    uint8_t interlaced;
    uint8_t top_field_first;

    uint8_t frame_rate_index;       ///< index into dirac_frame_rate[]
    uint8_t aspect_ratio_index;     ///< index into dirac_aspect_ratio[]

    uint16_t clean_width;
    uint16_t clean_height;
    uint16_t clean_left_offset;
    uint16_t clean_right_offset;

    uint8_t pixel_range_index;      ///< index into dirac_pixel_range_presets[]
    uint8_t color_spec_index;       ///< index into dirac_color_spec_presets[]

    int profile;
    int level;

    AVRational framerate;
    AVRational sample_aspect_ratio;

    enum AVPixelFormat pix_fmt;
    enum AVColorRange color_range;
    enum AVColorPrimaries color_primaries;
    enum AVColorTransferCharacteristic color_trc;
    enum AVColorSpace colorspace;

    DiracVersionInfo version;
    int bit_depth;
} AVDiracSeqHeader;

/**
 * Parse a Dirac sequence header.
 *
 * @param dsh this function will allocate and fill an AVDiracSeqHeader struct
 *            and write it into this pointer. The caller must free it with
 *            av_free().
 * @param buf the data buffer
 * @param buf_size the size of the data buffer in bytes
 * @param log_ctx if non-NULL, this function will log errors here
 * @return 0 on success, a negative AVERROR code on failure
 */
int av_dirac_parse_sequence_header(AVDiracSeqHeader **dsh,
                                   const uint8_t *buf, size_t buf_size,
                                   void *log_ctx);

#endif /* AVCODEC_DIRAC_H */
