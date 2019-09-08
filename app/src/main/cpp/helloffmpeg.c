#include <jni.h>
#include <stdio.h>
#include <time.h>

#include "include/libavcodec/avcodec.h"
#include "include/libavformat/avformat.h"
#include "include/libswscale/swscale.h"
#include "include/libavutil/log.h"
#include "include/libavutil/frame.h"
#include "include/libavutil/imgutils.h"

#ifdef ANDROID

#include "ffmpeg.h"
#include <libavfilter/avfilter.h>
#include <libavcodec/jni.h>

//#define TAG "gordanxu"

//#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, TAG, format, ##__VA_ARGS__)
//#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  TAG, format, ##__VA_ARGS__)
#else
//#define LOGE(format, ...)  printf("(>_<) " format "\n", ##__VA_ARGS__)
//#define LOGI(format, ...)  printf("(^_^) " format "\n", ##__VA_ARGS__)
#endif


void custom_log(void *ptr, int level, const char *fmt, va_list vl) {
    FILE *fp = fopen("/storage/emulated/0/gordan_av_log.txt", "a+");
    if (fp) {
        vfprintf(fp, fmt, vl);
        fflush(fp);
        fclose(fp);
    }
}


/****
 * 抽取裸视频数据（YUV格式）的过程
 *
 * @param env
 * @param obj
 * @param input_jstr
 * @param output_jstr
 * @return
 */
JNIEXPORT jint JNICALL
Java_com_gordan_helloffmpeg_util_FfmpegUtil_decode(JNIEnv *env, jobject obj, jstring input_jstr,
                                                   jstring output_jstr) {
    //封装格式的上下文
    AVFormatContext *pFormatCtx;
    int i, videoindex;
    //解码器的上下文
    AVCodecContext *pCodecCtx;
    //解码器
    AVCodec *pCodec;
    //视频的数据帧
    AVFrame *pFrame, *pFrameYUV;
    uint8_t *out_buffer;
    //数据包信息
    AVPacket *packet;
    int y_size;
    int ret, got_picture;
    struct SwsContext *img_convert_ctx;
    FILE *fp_yuv;
    int frame_cnt;
    clock_t time_start, time_finish;
    double time_duration = 0.0;

    char input_str[500] = {0};
    char output_str[500] = {0};
    char info[1000] = {0};
    sprintf(input_str, "%s", (*env)->GetStringUTFChars(env, input_jstr, NULL));
    sprintf(output_str, "%s", (*env)->GetStringUTFChars(env, output_jstr, NULL));

    //FFmpeg库中的日志打印回调（不是系统库！） av_log() callback
    av_log_set_callback(custom_log);
    //会打印第0个流的信息到 上面指定的路径里（/storage/emulated/0/gordan_av_log.txt），第三个参数0表示输入 1表示输出
    //这里打印的结果和使用ffmpeg命令得到的结果是一致的
    //av_dump_format(pFormatCtx,0,"",0);

    av_register_all();
    avformat_network_init();
    pFormatCtx = avformat_alloc_context();
    int err_code = -1;
    char buf[1024];
    //获取到封装格式的上下文（注意参数需要的是二级指针，执行完下面一句 pFormatCtx 就不为空了）
    if (avformat_open_input(&pFormatCtx, input_str, NULL, NULL) != 0) {
        LOGE("Couldn't open input stream.\n");
        av_strerror(err_code, buf, 1024);
        LOGE("Couldn’t open file %s: %d(%s)", input_str, err_code, buf);
        return -1;
    }
    //获取视频流信息（参数需要的只是个指针）
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGE("Couldn't find stream information.\n");
        return -1;
    }

    //结构体AVStream中的codec成员已经被废弃 使用codecpar成员替代
    //参考：https://www.cnblogs.com/lgh1992314/p/5834634.html

    //找到数据流中的视频流（还包含音频流，字幕流）
    videoindex = -1;
    for (i = 0; i < pFormatCtx->nb_streams; i++)
        if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoindex = i;
            break;
        }
    if (videoindex == -1) {
        LOGE("Couldn't find a video stream.\n");
        return -1;
    }
    //pCodecCtx = pFormatCtx->streams[videoindex]->codecpar; 不推荐这样使用
    pCodecCtx = avcodec_alloc_context3(NULL);
    if (pCodecCtx == NULL) {
        printf("Could not allocate AVCodecContext\n");
        return -1;
    }
    avcodec_parameters_to_context(pCodecCtx, pFormatCtx->streams[videoindex]->codecpar);


    //获取到解码器(对于视频格式为h264的 先硬解 再软解)，所有视频格式都会优先使用硬解码??? 可是后面报错？
    //pCodec=avcodec_find_decoder_by_name("h264_mediacodec");
    pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
    if (pCodec == NULL) {
        LOGE("Couldn't find Codec.\n");
        return -1;
    }
    //打开解码器
    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGE("Couldn't open codec.\n");
        return -1;
    }

    pFrame = av_frame_alloc();
    pFrameYUV = av_frame_alloc();
    out_buffer = (unsigned char *) av_malloc(
            av_image_get_buffer_size(AV_PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height, 1));
    av_image_fill_arrays(pFrameYUV->data, pFrameYUV->linesize, out_buffer,
                         AV_PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height, 1);

    packet = (AVPacket *) av_malloc(sizeof(AVPacket));

    img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt,
                                     pCodecCtx->width, pCodecCtx->height, AV_PIX_FMT_YUV420P,
                                     SWS_BICUBIC, NULL, NULL, NULL);

    LOGI("[Input     ]%s", input_str);
    LOGI("[Output    ]%s", output_str);
    /*sprintf(info, "[Input     ]%s\n", input_str);
    sprintf(info, "%s[Output    ]%s\n", info, output_str);*/
    //视频的封装格式
    LOGI("Format:%s", pFormatCtx->iformat->name);
    //sprintf(info, "%s[Format    ]%s\n", info, pFormatCtx->iformat->name);
    //视频的编码格式
    LOGI("Codec:%s", pCodecCtx->codec->name);
    //sprintf(info, "%s[Codec     ]%s\n", info, pCodecCtx->codec->name);
    //视频的分辨率
    LOGI("Resolution:%d x %d", pCodecCtx->width, pCodecCtx->height);
    //sprintf(info, "%s[Resolution]%dx%d\n", info, pCodecCtx->width, pCodecCtx->height);

    //以可读可写打开文档如果不存在创建文档
    fp_yuv = fopen(output_str, "wb+");
    if (fp_yuv == NULL) {
        printf("Cannot open output file.\n");
        return -1;
    }

    frame_cnt = 0;
    time_start = clock();//获取到系统当前时间
    //读取视频流中的帧数据存入临时的数据包中
    while (av_read_frame(pFormatCtx, packet) >= 0) {
        if (packet->stream_index == videoindex) {
            ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
            if (ret < 0) {
                LOGE("Decode Error.\n");
                return -1;
            }
            if (got_picture) {
                //转码的关键代码 MP4转换为YUV格式的
                sws_scale(img_convert_ctx, (const uint8_t *const *) pFrame->data, pFrame->linesize,
                          0, pCodecCtx->height,
                          pFrameYUV->data, pFrameYUV->linesize);

                y_size = pCodecCtx->width * pCodecCtx->height;
                fwrite(pFrameYUV->data[0], 1, y_size, fp_yuv);    //Y
                fwrite(pFrameYUV->data[1], 1, y_size / 4, fp_yuv);  //U
                fwrite(pFrameYUV->data[2], 1, y_size / 4, fp_yuv);  //V
//Output info
                char pictype_str[10] = {0};
                switch (pFrame->pict_type) {
                    case AV_PICTURE_TYPE_I:
                        sprintf(pictype_str, "I");
                        break;
                    case AV_PICTURE_TYPE_P:
                        sprintf(pictype_str, "P");
                        break;
                    case AV_PICTURE_TYPE_B:
                        sprintf(pictype_str, "B");
                        break;
                    default:
                        sprintf(pictype_str, "Other");
                        break;
                }
                LOGI("Frame Index: %5d. Type:%s", frame_cnt, pictype_str);
                frame_cnt++;
            }
        }
        av_free_packet(packet);
    }
//flush decoder
//FIX: Flush Frames remained in Codec
    //为啥在这里还得再写一次？
    while (1) {
        ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
        if (ret < 0)
            break;
        if (!got_picture)
            break;
        sws_scale(img_convert_ctx, (const uint8_t *const *) pFrame->data, pFrame->linesize, 0,
                  pCodecCtx->height,
                  pFrameYUV->data, pFrameYUV->linesize);
        int y_size = pCodecCtx->width * pCodecCtx->height;
        fwrite(pFrameYUV->data[0], 1, y_size, fp_yuv);    //Y
        fwrite(pFrameYUV->data[1], 1, y_size / 4, fp_yuv);  //U
        fwrite(pFrameYUV->data[2], 1, y_size / 4, fp_yuv);  //V
//Output info
        char pictype_str[10] = {0};
        switch (pFrame->pict_type) {
            case AV_PICTURE_TYPE_I:
                //独立的帧数据
                sprintf(pictype_str, "I");
                break;
            case AV_PICTURE_TYPE_P:
                //非独立的帧数据 需要依赖前面一帧数据才能正确解析到数据
                sprintf(pictype_str, "P");
                break;
            case AV_PICTURE_TYPE_B:
                //非独立的帧数据 需要依赖前面一帧和后面一帧的数据才能正确解析到数据
                sprintf(pictype_str, "B");
                break;
            default:
                sprintf(pictype_str, "Other");
                break;
        }
        LOGI("======2=====Frame Index: %5d. Type:%s", frame_cnt, pictype_str);
        frame_cnt++;
    }
    time_finish = clock();
    time_duration = (double) (time_finish - time_start);

    LOGI("[Time      ]%fms", time_duration);
    LOGI("[Count   ]%d", frame_cnt);

    /*sprintf(info, "%s[Time      ]%fms\n", info, time_duration);
    sprintf(info, "%s[Count     ]%d\n", info, frame_cnt);*/

    sws_freeContext(img_convert_ctx);

    fclose(fp_yuv);

    av_frame_free(&pFrameYUV);
    av_frame_free(&pFrame);
    avcodec_close(pCodecCtx);
    avformat_close_input(&pFormatCtx);

    return 0;
}

JNIEXPORT jstring JNICALL
Java_com_gordan_helloffmpeg_util_FfmpegUtil_cpuInfo(JNIEnv *env, jobject instance) {
#if defined(__arm__)
#if defined(__ARM_ARCH_7A__)
#if defined(__ARM_NEON__)
#if defined(__ARM_PCS_VFP)
#define ABI "armeabi-v7a/NEON (hard-float)"
#else
#define ABI "armeabi-v7a/NEON"
#endif
#else
#if defined(__ARM_PCS_VFP)
#define ABI "armeabi-v7a (hard-float)"
#else
#define ABI "armeabi-v7a"
#endif
#endif
#else
#define ABI "armeabi"
#endif
#elif defined(__i386__)
#define ABI "x86"
#elif defined(__x86_64__)
#define ABI "x86_64"
#elif defined(__mips64)  /* mips64el-* toolchain defines __mips__ too */
#define ABI "mips64"
#elif defined(__mips__)
#define ABI "mips"
#elif defined(__aarch64__)
#define ABI "arm64-v8a"
#else
#define ABI "unknown"
#endif

    return (*env)->NewStringUTF(env, "Hello from JNI !  Compiled with ABI " ABI ".");
}


/****
 * 复用的过程（独立的音频和视频文件合并为一个新的视频文件）
 *
 * @param env
 * @param obj
 * @param input_h264
 * @param input_aac
 * @param output_ts
 */
void mergeVideoAudioToTs(JNIEnv *env, jobject obj, jstring input_h264, jstring input_aac,
                         jstring output_ts) {

    AVOutputFormat* ofmt = NULL;
//Input AVFormatContext and Output AVFormatContext
    AVFormatContext *ifmt_ctx_v = NULL, *ifmt_ctx_a = NULL, *ofmt_ctx = NULL;
    AVPacket pkt;
    int ret, i;
    int videoindex_v = -1, videoindex_out = -1;
    int audioindex_a = -1, audioindex_out = -1;
    int frame_index = 0;
    int64_t cur_pts_v = 0, cur_pts_a = 0;
//	AVDictionary* pAVDictionary = NULL;
//	AVInputFormat* ifmt = NULL;

    char input_h264_str[500] = {0};
    char input_aac_str[500] = {0};
    char output_str[500] = {0};

    //JAVA 字符串 转换为 C语言字符串
    sprintf(input_h264_str, "%s", (*env)->GetStringUTFChars(env, input_h264, NULL));
    sprintf(input_aac_str, "%s", (*env)->GetStringUTFChars(env, input_aac, NULL));
    sprintf(output_str, "%s", (*env)->GetStringUTFChars(env, output_ts, NULL));

    av_log_set_callback(custom_log);

    av_register_all();
    avformat_network_init();
//Input 输入的视频文件
    ifmt_ctx_v = avformat_alloc_context();
    int err = avformat_open_input(&ifmt_ctx_v, input_h264_str, NULL, NULL);
    if (err != 0) {

        LOGE("Call avformat_open_input function failed!");
        return;
    }

    if ((ret = avformat_find_stream_info(ifmt_ctx_v, 0)) < 0) {
        LOGE("Failed to retrieve input stream information");
        goto readH264ACC2TS_end;
    }
//输入的音频文件
    ifmt_ctx_a = avformat_alloc_context();
    err = avformat_open_input(&ifmt_ctx_a,input_aac_str, NULL, NULL);
    if (err != 0) {
        LOGE("Call avformat_open_input function failed!");
        return;
    }
    if ((ret = avformat_find_stream_info(ifmt_ctx_a, 0)) < 0) {
        LOGE("Failed to retrieve input stream information");
        goto readH264ACC2TS_end;
    }

    //判断输出的TS文件是否存在，不存在则创建
    FILE *fp_ts;
    fp_ts=fopen(output_str,"wb+");
    if(fp_ts==NULL)
    {
        LOGE("===output ts null so return=====");
        return;
    }

    LOGE("muxing packet avformat_alloc_output_context2  mpegts");

//Output 申请输出的TS文件的上下文
//xpzhi  因为我这里是直接写入到文件
//参考 https://blog.csdn.net/leixiaohua1020/article/details/41198929
    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, output_str);
    if (!ofmt_ctx) {
        LOGE("Could not create output context");
        ret = AVERROR_UNKNOWN;
        goto readH264ACC2TS_end;
    }
    ofmt=ofmt_ctx->oformat;

    LOGE(" muxing packet ifmt_ctx_v->nb_streams=%d", ifmt_ctx_v->nb_streams);

    for (i = 0; i < ifmt_ctx_v->nb_streams; i++) {
//Create output AVStream according to input AVStream 查找视频流
        if (ifmt_ctx_v->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            AVStream *in_stream = ifmt_ctx_v->streams[i];
            AVStream *out_stream = avformat_new_stream(ofmt_ctx, in_stream->codec->codec);
            videoindex_v = i;
            LOGE("=========videoindex_v:%d====",videoindex_v);
            if (!out_stream) {
                LOGE("Failed allocating output stream");
                ret = AVERROR_UNKNOWN;
                goto readH264ACC2TS_end;
            }
            videoindex_out = out_stream->index;
//Copy the settings of AVCodecContext
            if (avcodec_copy_context(out_stream->codec, in_stream->codec) < 0) {
                LOGE("Failed to copy context from input to output stream codec context");
                goto readH264ACC2TS_end;
            }
            out_stream->codec->codec_tag = 0;
            //xpzhi 这里找不到 CODEC_FLAG_GLOBAL_HEADER
            if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER) {
                out_stream->codec->flags |= (1 << 22);
            }
            break;
        }
    }

    LOGE(" muxing packet ifmt_ctx_a->nb_streams=%d", ifmt_ctx_a->nb_streams);
    for (i = 0; i < ifmt_ctx_a->nb_streams; i++) {
//Create output AVStream according to input AVStream 查找音频流
        if (ifmt_ctx_a->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            AVStream *in_stream = ifmt_ctx_a->streams[i];
            AVStream *out_stream = avformat_new_stream(ofmt_ctx, in_stream->codec->codec);
            audioindex_a = i;
            LOGE("=========audioindex_a:%d====",audioindex_a);
            if (!out_stream) {
                LOGE("Failed allocating output stream");
                ret = AVERROR_UNKNOWN;
                goto readH264ACC2TS_end;
            }
            audioindex_out = out_stream->index;
//Copy the settings of AVCodecContext
            if (avcodec_copy_context(out_stream->codec, in_stream->codec) < 0) {
                LOGE("Failed to copy context from input to output stream codec context");
                goto readH264ACC2TS_end;
            }
            out_stream->codec->codec_tag = 0;
            //xpzhi CODEC_FLAG_GLOBAL_HEADER
            if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER) {
                out_stream->codec->flags |= (1 << 22);
            }

            break;
        }
    }

    LOGE("====v:%p===a:%p====t:%p==",&ifmt_ctx_v,&ifmt_ctx_a,&ofmt_ctx);
    LOGE ( "======================Input Information=====================\n");
    av_dump_format ( ifmt_ctx_v, 0, 0, 0 );
    LOGE ( "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
    av_dump_format ( ifmt_ctx_a, 0, 0, 0 );
    LOGE ( "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
    av_dump_format ( ofmt_ctx, 0, 0, 1 );
    LOGE ( "============================================================\n");

    if (!(ofmt->flags & AVFMT_NOFILE)) {
        if (avio_open(&ofmt_ctx->pb, output_str, AVIO_FLAG_WRITE) < 0) {
            LOGE( "Could not open output file '%s'", output_str);
            goto readH264ACC2TS_end;
        }
    }

//Write file header 写入TS的头文件
    if (avformat_write_header(ofmt_ctx, NULL) < 0) {
        LOGE("Error occurred when write header");
        goto readH264ACC2TS_end;
    }
    LOGE(" muxing packet while");
    while (1) {
        LOGE(" muxing packet while 666 ");
        AVFormatContext *ifmt_ctx;
        int stream_index = 0;
        AVStream *in_stream, *out_stream;
//Get an AVPacket
        //音视频帧的数据不是乱序写入的，而是按照音频帧 和 视频帧的时间基 哪个时间基小 则先写入
        if (av_compare_ts(cur_pts_v, ifmt_ctx_v->streams[videoindex_v]->time_base, cur_pts_a,
                          ifmt_ctx_a->streams[audioindex_a]->time_base) <= 0) {
            //视频帧的时间基 小于 音频帧的时间基
            LOGE("ykai av_compare_ts ");
            ifmt_ctx = ifmt_ctx_v;
            stream_index = videoindex_out;
            if (av_read_frame(ifmt_ctx, &pkt) >= 0) {
                LOGE("ykai av_read_frame");
                do {
                    LOGE("ykai do while");
                    in_stream = ifmt_ctx->streams[pkt.stream_index];
                    out_stream = ofmt_ctx->streams[stream_index];
                    if (pkt.stream_index == videoindex_v) {
//FIX��No PTS (Example: Raw H.264)
//Simple Write PTS
                        LOGE("=======gordanxu===video===%lld======",pkt.pts);
                        if (pkt.pts == AV_NOPTS_VALUE) {
//Write PTS
                            AVRational time_base1 = in_stream->time_base;
//Duration between 2 frames (us)
                            int64_t calc_duration =
                                    (double) AV_TIME_BASE / av_q2d(in_stream->r_frame_rate);
//Parameters
                            pkt.pts = (double) (frame_index * calc_duration) /
                                      (double) (av_q2d(time_base1) * AV_TIME_BASE);
                            pkt.dts = pkt.pts;
                            pkt.duration = (double) calc_duration /
                                           (double) (av_q2d(time_base1) * AV_TIME_BASE);
                            LOGE("===video====num:%d===frame_index:%d====calc_duration:%lld==",time_base1.den,frame_index,calc_duration);
                            frame_index++;
                        }
                        cur_pts_v = pkt.pts;
                        break;
                    }
                } while (av_read_frame(ifmt_ctx, &pkt) >= 0);
            } else {

                LOGE("====video break====");
                break;
            }
        } else {
            LOGE("ykai else ");
            ifmt_ctx = ifmt_ctx_a;
            stream_index = audioindex_out;
            if (av_read_frame(ifmt_ctx, &pkt) >= 0) {
                LOGE("ykai else av_read_frame");
                do {
                    LOGE("ykai else do while");
                    in_stream = ifmt_ctx->streams[pkt.stream_index];
                    out_stream = ofmt_ctx->streams[stream_index];
                    if (pkt.stream_index == audioindex_a) {
//FIX��No PTS
//Simple Write PTS
                        LOGE("=======gordanxu======%lld======",pkt.pts);
                        if (pkt.pts == AV_NOPTS_VALUE) {
//Write PTS                 音频帧的时候 PTS为 0 修改过后这里其实还是没有执行 音频文件里面没有时间基？
                            LOGE("=======gordanxu====AV_NOPTS_VALUE========");
                            AVRational time_base1 = in_stream->time_base;
//Duration between 2 frames (us)
                            int64_t calc_duration =(int64_t)( AV_TIME_BASE / av_q2d(in_stream->r_frame_rate));
//Parameters
                            pkt.pts = (int64_t) ((frame_index * calc_duration) / (av_q2d(time_base1) * AV_TIME_BASE));
                            pkt.dts = pkt.pts;
                            pkt.duration = (int64_t)( calc_duration /(av_q2d(time_base1) * AV_TIME_BASE));
                            frame_index++;
                            LOGE("====num:%d===frame_index:%d====calc_duration:%lld==",time_base1.den,frame_index,calc_duration);
                        }
                        cur_pts_a = pkt.pts;
                        LOGE("=======gordanxu====pts========");
                        break;
                    }
                } while (av_read_frame(ifmt_ctx, &pkt) >= 0);
            } else {
                LOGE("====audio break====");
                break;
            }
        }
//Convert PTS/DTS
        pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base,
                                   AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX);
        pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base,
                                   AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX);
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;
        pkt.stream_index = stream_index;
        LOGE("Write 1 Packet. size:%5d\tpts:%lld\n",pkt.size,pkt.pts);
//Write
        //写入 音视频数据帧
        if (av_interleaved_write_frame(ofmt_ctx, &pkt) < 0) {
            LOGE("write frame Error");
            break;
        }
        av_free_packet(&pkt);
        LOGE("===free finished===");
    }
//Write file trailer 写入TS的尾部
    av_write_trailer(ofmt_ctx);
    readH264ACC2TS_end:
    LOGE("goto module:Error muxing packet");
    avformat_close_input(&ifmt_ctx_v);
    avformat_close_input(&ifmt_ctx_a);
    /* close output */
    if (ofmt_ctx && !(ofmt->flags& AVFMT_NOFILE)) {
        avio_close(ofmt_ctx->pb);
    }

    avformat_free_context(ofmt_ctx);
    if (ret < 0 && ret != AVERROR_EOF) {
        LOGE("goto module:Error occurred");
    }
}

/****
 * 解复用的过程（音频和视频分离，前提是得知道视频和音频的编码格式）
 *
 * @param env
 * @param obj
 * @param input_ts
 */
void separateVideoAndAudio(JNIEnv *env, jobject obj,jstring input_ts,jstring output_h264,jstring output_aac)
{
    //const char* out_filename_v="/storage/emulated/0/douyin.h264";
    //const char* out_filename_a="/storage/emulated/0/douyin.aac";

    char in_filename[500] = {0};
    char out_filename_v[500] ={0};
    char out_filename_a[500]={0};
    //JAVA 字符串 转换为 C语言字符串
    sprintf(in_filename, "%s", (*env)->GetStringUTFChars(env, input_ts, NULL));
    sprintf(out_filename_v, "%s", (*env)->GetStringUTFChars(env, output_h264, NULL));
    sprintf(out_filename_a, "%s", (*env)->GetStringUTFChars(env, output_aac, NULL));

    FILE* file_v;
    FILE* file_a;

    file_v=fopen(out_filename_v,"wb+");
    if(file_v==NULL)
    {
        LOGE("====video file is null so return====");
        return;
    }

    file_a=fopen(out_filename_a,"wb+");
    if(file_a==NULL)
    {
        LOGE("====audio file is null so return====");
        return;
    }
    av_log_set_callback(custom_log);
    av_register_all();

    AVOutputFormat *ofmt_a=NULL,*ofmt_v=NULL;
    AVFormatContext *ifmt_ctx=NULL,*ofmt_ctx_a=NULL,*ofmt_ctx_v=NULL;
    AVPacket pck;
    int ret,i;
    int videoindex=-1,audioindex=-1;
    int frameindex=0;

    if((ret=avformat_open_input(&ifmt_ctx,in_filename,NULL,NULL))<0)
    {
        //打开输入文件失败
        LOGE("=====could not open input file so return=====");
        goto end;
    }

    if((ret=avformat_find_stream_info(ifmt_ctx,NULL))<0)
    {
        //查找音视频流信息失败
        LOGE("====failed to retrieve stream info so return====");
        goto end;
    }

    avformat_alloc_output_context2(&ofmt_ctx_v,NULL,NULL,out_filename_v);
    if(!ofmt_ctx_v)
    {
        //获取输出视频文件上下文失败
        LOGE("===open output video file failed so return====");
        goto end;
    }
    ofmt_v=ofmt_ctx_v->oformat;

    avformat_alloc_output_context2(&ofmt_ctx_a,NULL,NULL,out_filename_a);
    if(!ofmt_ctx_a)
    {
        //获取输出音频文件上下文失败
        LOGE("===open output audio file failed so return====");
        goto end;
    }
    ofmt_a=ofmt_ctx_a->oformat;

    for ( i = 0; i < ifmt_ctx->nb_streams; i++)
    {
        AVFormatContext *ofmt_ctx;
        AVStream *in_stream=ifmt_ctx->streams[i];
        AVStream *out_stream=NULL;

        if(ifmt_ctx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO)
        {
            videoindex=i;
            out_stream=avformat_new_stream(ofmt_ctx_v,in_stream->codec->codec);
            ofmt_ctx=ofmt_ctx_v;
            LOGE("===videoindex:%d===",videoindex);
        }
        else if(ifmt_ctx->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO)
        {
            audioindex=i;
            out_stream=avformat_new_stream(ofmt_ctx_a,in_stream->codec->codec);
            ofmt_ctx=ofmt_ctx_a;
            LOGE("===audioindex:%d===",audioindex);
        } else
        {
            break;
        }

        if(!out_stream)
        {
            LOGE("=====allocate output stream failed so return=====");
            goto end;
        }

        //复制音视频相关的解码参数
        if(avcodec_copy_context(out_stream->codec,in_stream->codec)<0)
        {
            LOGE("===failed to copy codec so return===");
            goto end;
        }

        out_stream->codec->codec_tag=0;

        if(ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
        {
            out_stream->codec->flags |= (1<<22);
        }
    }

    //LOGCAT中看不到打印 杨凯的串口可以看见打印
    printf("\n==============Input Video=============\n");
    av_dump_format(ifmt_ctx, 0, in_filename, 0);
    printf("\n==============Output Video============\n");
    av_dump_format(ofmt_ctx_v, 0, out_filename_v, 1);
    printf("\n==============Output Audio============\n");
    av_dump_format(ofmt_ctx_a, 0, out_filename_a, 1);
    printf("\n======================================\n");

    if(!(ofmt_v->flags & AVFMT_NOFILE))
    {
        //打开输出的视频流
        if(avio_open(&ofmt_ctx_v->pb,out_filename_v,AVIO_FLAG_WRITE)<0)
        {
            LOGE("could not open output file %s",out_filename_v);
            goto end;
        }
    }

    if(!(ofmt_a->flags & AVFMT_NOFILE))
    {
        //打开输出的音频流
        if(avio_open(&ofmt_ctx_a->pb,out_filename_a,AVIO_FLAG_WRITE)<0)
        {
            LOGE("could not open output file %s",out_filename_a);
            goto end;
        }
    }
    //写入视频文件的头信息
    if(avformat_write_header(ofmt_ctx_v,NULL)<0)
    {
        LOGE("====write video header failed so return====");
        goto end;
    }
    //写入音频文件的头信息
    if(avformat_write_header(ofmt_ctx_a,NULL)<0)
    {
        LOGE("====write audio header failed so return====");
        goto end;
    }

    //AVBitStreamFilterContext* h264bsfc = av_bitstream_filter_init("h264_mp4toannexb");

    while (1)
    {
        AVFormatContext *ofmt_ctx;
        AVStream *in_stream,*out_stream;
        //读取一帧音/视频数据（读取出错则跳出）
        if(av_read_frame(ifmt_ctx,&pck)<0)
        {
            LOGE("===read frame failed so break====");
            break;
        }

        in_stream=ifmt_ctx->streams[pck.stream_index];
        if(pck.stream_index==videoindex)
        {
            out_stream=ofmt_ctx_v->streams[0];
            ofmt_ctx=ofmt_ctx_v;
            //av_bitstream_filter_filter(h264bsfc, in_stream->codec, NULL, &pck.data, &pck.size, pck.data, pck.size, 0);
            LOGE("===write video packet size:%d pts:%lld====",pck.size,pck.pts);
        }
        else if(pck.stream_index == audioindex)
        {
            out_stream=ofmt_ctx_a->streams[0];
            ofmt_ctx=ofmt_ctx_a;
            LOGE("===write audio packet size:%d pts:%lld====",pck.size,pck.pts);
        }
        else
        {
            continue;
        }

        //设置相关的时间基参数
        pck.pts=av_rescale_q_rnd(pck.pts,in_stream->time_base,out_stream->time_base,(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
        pck.dts=av_rescale_q_rnd(pck.pts,in_stream->time_base,out_stream->time_base,(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
        pck.duration=av_rescale_q(pck.duration,in_stream->time_base,out_stream->time_base);
        pck.pos=-1;
        pck.stream_index=0;
        //将一帧音/视频数据写入输出文件
        if(av_interleaved_write_frame(ofmt_ctx,&pck)<0)
        {
            LOGE("===write frame error so return===");
            break;
        }
        av_free_packet(&pck);
        frameindex++;
        LOGE("=========frameindex:%d======",frameindex);
    }

    //av_bitstream_filter_close(h264bsfc);

    //写入音视频文件的尾
    av_write_trailer(ofmt_ctx_a);
    av_write_trailer(ofmt_ctx_v);

    end:
    //关闭输入流
    avformat_close_input(&ifmt_ctx);

    //关闭音频输出流
    if(ofmt_ctx_a && !(ofmt_a->flags & AVFMT_NOFILE))
    {
        avio_close(ofmt_ctx_a->pb);
    }

    //关闭视频输出流
    if(ofmt_ctx_v && !(ofmt_v->flags & AVFMT_NOFILE))
    {
        avio_close(ofmt_ctx_v->pb);
    }

    avformat_free_context(ofmt_ctx_a);
    avformat_free_context(ofmt_ctx_v);
}

/****
 *
 * 封装格式转换
 *
 * @param env
 * @param obj
 * @param input
 * @param output
 */
void convertMediaFormat(JNIEnv *env, jobject obj,jstring input,jstring output)
{
    char in_filename[500] = {0};
    char out_filename[500] = {0};

    sprintf(in_filename,"%s",(*env)->GetStringUTFChars(env,input,NULL));
    sprintf(out_filename,"%s",(*env)->GetStringUTFChars(env,output,NULL));

    FILE *file_in;
    FILE *file_out;

    file_in=fopen(in_filename,"ab");
    if(file_in == NULL)
    {
        LOGE("=====input file is null so return======");
        return;
    }
    file_out=fopen(out_filename,"wb+");
    if(file_out == NULL)
    {
        LOGE("=====output file is null so return======");
        return;
    }

    AVOutputFormat *ofmt=NULL;
    AVFormatContext *ifmt_ctx=NULL,*ofmt_ctx=NULL;
    AVPacket pkt;
    int ret,i;
    LOGE("=====register all======");
    av_register_all();
    //打开输入文件
    if((ret=avformat_open_input(&ifmt_ctx,in_filename,0,0))<0)
    {
        LOGE("====open input file failed so return=====");
        goto end;
    }
    //查找输入文件中的音视频流信息
    if((ret=avformat_find_stream_info(ifmt_ctx,0))<0)
    {
        LOGE("====find stream info failed so return=====");
        goto end;
    }

    //收集输入文件的流媒体信息（无法打印到LOGCAT平台 不过串口似乎可以看到）
    av_dump_format(ifmt_ctx,0,in_filename,0);

    //获取输出文件的上下文
    avformat_alloc_output_context2(&ofmt_ctx,NULL,NULL,out_filename);

    if(!ofmt_ctx)
    {
        LOGE("can not allocate output context");
        goto end;
    }
    //复制输出文件的格式信息（注意格式是根据文件名的后缀来区分，为后面添加对应格式的头和尾信息）
    ofmt=ofmt_ctx->oformat;

    for (i = 0; i <ifmt_ctx->nb_streams ; i++) {
        AVStream *in_stream=ifmt_ctx->streams[i];
        AVStream *out_stream=avformat_new_stream(ofmt_ctx,in_stream->codec->codec);

        if(!out_stream)
        {
            LOGE("alloct out_stream failed so return");
            goto end;
        }

        //将输入流的解码器复制到输出流？
        ret=avcodec_copy_context(out_stream->codec,in_stream->codec);
        if(ret<0)
        {
            LOGE("failed copy codec so return");
            goto end;
        }

        out_stream->codec->codec_tag=0;
        if(ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
        {
            out_stream->codec->flags |=(1<<22);
        }
    }

    av_dump_format(ofmt_ctx,0,out_filename,1);

    if(!(ofmt->flags & AVFMT_NOFILE))
    {
        ret=avio_open(&ofmt_ctx->pb,out_filename,AVIO_FLAG_WRITE);
        if(ret<0)
        {
            LOGE("can not open output file!");
            goto end;
        }
    }
    //写入输出文件的头
    ret=avformat_write_header(ofmt_ctx,NULL);
    if(ret<0)
    {
        LOGE("write header error so return");
        goto end;
    }

    int frame_index=0;
    while(1)
    {
        AVStream *in_stream,*out_stream;
        //读取音视频帧数据
        ret=av_read_frame(ifmt_ctx,&pkt);
        if(ret<0)
        {
            LOGE("read frame failed so return");
            break;
        }

        in_stream=ifmt_ctx->streams[pkt.stream_index];
        out_stream=ofmt_ctx->streams[pkt.stream_index];
        //时间基的相互转化
        pkt.pts=av_rescale_q_rnd(pkt.pts,in_stream->time_base,out_stream->time_base,(AV_ROUND_INF|AV_ROUND_PASS_MINMAX));
        pkt.dts=av_rescale_q_rnd(pkt.dts,in_stream->time_base,out_stream->time_base,(AV_ROUND_INF|AV_ROUND_PASS_MINMAX));
        pkt.duration=av_rescale_q(pkt.duration,in_stream->time_base,out_stream->time_base);
        pkt.pos=-1;
        //写入音视频帧数据
        ret=av_write_frame(ofmt_ctx,&pkt);
        if(ret<0)
        {
            LOGE("===write frame failed so return===");
            break;
        }
        LOGE("===write frameindex:%d===",frame_index);
        av_free_packet(&pkt);
        frame_index++;
    }
    //写入输出文件的尾
    av_write_trailer(ofmt_ctx);

    end:

    LOGE("====end module===");
    //关闭输入流
    avformat_close_input(&ifmt_ctx);
    //关闭输出流
    if(ofmt_ctx && !(ofmt->flags&AVFMT_NOFILE))
    {
        avio_close(ofmt_ctx->pb);
    }
    avformat_free_context(ofmt_ctx);


}

jstring getProtocolInfo(JNIEnv *env, jobject instance) {
    char info[40000] = {0};
    av_register_all();

    struct URLProtocol *pup = NULL;
    //Input
    struct URLProtocol **p_temp = &pup;
    avio_enum_protocols((void **) p_temp, 0);
    while ((*p_temp) != NULL) {
        sprintf(info, "%s[In ][%10s]\n", info, avio_enum_protocols((void **) p_temp, 0));
    }
    pup = NULL;
    //Output
    avio_enum_protocols((void **) p_temp, 1);
    while ((*p_temp) != NULL) {
        sprintf(info, "%s[Out][%10s]\n", info, avio_enum_protocols((void **) p_temp, 1));
    }

    LOGE("%s", info);
    return (*env)->NewStringUTF(env, info);
}

jstring getAVFormatInfo(JNIEnv *env, jobject instance) {
    char info[40000] = {0};

    av_register_all();

    AVInputFormat *if_temp = av_iformat_next(NULL);
    AVOutputFormat *of_temp = av_oformat_next(NULL);
    //Input
    while (if_temp != NULL) {
        sprintf(info, "%s[In ][%10s]\n", info, if_temp->name);
        if_temp = if_temp->next;
    }
    //Output
    while (of_temp != NULL) {
        sprintf(info, "%s[Out][%10s]\n", info, of_temp->name);
        of_temp = of_temp->next;
    }
    LOGE("%s", info);
    return (*env)->NewStringUTF(env, info);
}

jstring getAVCodecInfo(JNIEnv *env, jobject instance) {
    char info[40000] = {0};

    av_register_all();

    AVCodec *c_temp = av_codec_next(NULL);

    while (c_temp != NULL) {
        if (c_temp->decode != NULL) {
            sprintf(info, "%s[Dec]", info);
        } else {
            sprintf(info, "%s[Enc]", info);
        }
        switch (c_temp->type) {
            case AVMEDIA_TYPE_VIDEO:
                sprintf(info, "%s[Video]", info);
                break;
            case AVMEDIA_TYPE_AUDIO:
                sprintf(info, "%s[Audio]", info);
                break;
            default:
                sprintf(info, "%s[Other]", info);
                break;
        }
        sprintf(info, "%s[%10s]\n", info, c_temp->name);


        c_temp = c_temp->next;
    }
    LOGE("%s", info);

    return (*env)->NewStringUTF(env, info);
}

jstring getAVFilterInfo(JNIEnv *env, jobject instance) {
    char info[40000] = {0};
    avfilter_register_all();
    AVFilter *f_temp = (AVFilter *) avfilter_next(NULL);
    while (f_temp != NULL) {
        sprintf(info, "%s[%10s]\n", info, f_temp->name);
        f_temp = f_temp->next;
    }
    LOGE("%s", info);
    return (*env)->NewStringUTF(env, info);
}

jstring getConfigInfo(JNIEnv *env, jobject instance) {
    char info[10000] = {0};
    av_register_all();

    sprintf(info, "%s\n", avcodec_configuration());

    LOGE("%s", info);
    return (*env)->NewStringUTF(env, info);
}

jint convertVideo(JNIEnv *env, jobject instance, jobjectArray cmdline) {
    LOGI("convertVideo");
    av_log_set_callback(custom_log);

    //获取到数组的长度
    int argc = (*env)->GetArrayLength(env, cmdline);

    //声明Char数组
    char *argv[argc];
    int i = 0;
    for (i = 0; i < argc; ++i) {
        //获取数组中的元素
        jstring js = (jstring) (*env)->GetObjectArrayElement(env, cmdline, i);
        //赋值
        argv[i] = (char *) (*env)->GetStringUTFChars(env, js, 0);
    }
    int result = main(argc, argv);
    //LOGI("convertVideo===="+result);
    LOGI("convertVideo finished====");
    return result;
}

static const char *mClazzName = "com/gordan/helloffmpeg/util/FfmpegUtil";//加载动态库的那个JAVA类

static const JNINativeMethod method[] = {
        /*** Java方法     方法签名    JNI函数    *****/
        {"urlprotocolinfo",    "()Ljava/lang/String;",   (void *) getProtocolInfo},
        {"avformatinfo",       "()Ljava/lang/String;",   (void *) getAVFormatInfo},
        {"avcodecinfo",        "()Ljava/lang/String;",   (void *) getAVCodecInfo},
        {"avfilterinfo",       "()Ljava/lang/String;",   (void *) getAVFilterInfo},
        {"configurationinfo",  "()Ljava/lang/String;",   (void *) getProtocolInfo},
        {"convertVideoFormat", "([Ljava/lang/String;)I", (void *) convertVideo},
        {"mergeVideoAndAudio","(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",(void *)mergeVideoAudioToTs},
        {"separateVideoAndAudio","(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",(void *)separateVideoAndAudio},
        {"convertMediaFormat","(Ljava/lang/String;Ljava/lang/String;)V",(void*)convertMediaFormat}
};


int JNI_OnLoad(JavaVM *jvm, void *reserved) {
    LOGI("======JNI_OnLoad=======");
    JNIEnv *env = 0;
    jint resId = (*jvm)->GetEnv(jvm, (void **) (&env), JNI_VERSION_1_6);//执行完该代码后JNIEnv就被赋值了

    if (resId != JNI_OK) {
        LOGE("=====obtain jni error!=====");
        return -1;
    }

    jclass clazz = (*env)->FindClass(env, mClazzName);

    //动态注册JNI方法
    (*env)->RegisterNatives(env, clazz, method, sizeof(method) / sizeof(JNINativeMethod));

    LOGI("======JNI_OnLoad finished=======");
    //ffmpeg使用硬解码的设置（需要获取JAVA 虚拟机）
    av_jni_set_java_vm(jvm, reserved);

    return JNI_VERSION_1_6;
}