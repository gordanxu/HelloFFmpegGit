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
    int err_code=-1;
    char buf[1024];
    //获取到封装格式的上下文（注意参数需要的是二级指针，执行完下面一句 pFormatCtx 就不为空了）
    if (avformat_open_input(&pFormatCtx, input_str, NULL, NULL)!=0) {
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

    //获取到解码器
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

jint convertVideo(JNIEnv *env, jobject instance,jobjectArray cmdline)
{
    LOGI("convertVideo");
    av_log_set_callback(custom_log);

    //获取到数组的长度
    int argc=(*env)->GetArrayLength(env,cmdline);

    //声明Char数组
    char* argv[argc];
    int i=0;
    for ( i=0 ; i < argc; ++i) {
        //获取数组中的元素
        jstring js=(jstring)(*env)->GetObjectArrayElement(env,cmdline,i);
        //赋值
        argv[i]=(char *)(*env)->GetStringUTFChars(env,js,0);
    }
    int result=main(argc,argv);
    //LOGI("convertVideo===="+result);
    LOGI("convertVideo finished====");
    return result;
}


static const char *mClazzName = "com/gordan/helloffmpeg/util/FfmpegUtil";//加载动态库的那个JAVA类

static const JNINativeMethod method[] = {
        /*** Java方法     方法签名    JNI函数    *****/
        {"urlprotocolinfo",   "()Ljava/lang/String;", (void *) getProtocolInfo},
        {"avformatinfo",      "()Ljava/lang/String;", (void *) getAVFormatInfo},
        {"avcodecinfo",       "()Ljava/lang/String;", (void *) getAVCodecInfo},
        {"avfilterinfo",      "()Ljava/lang/String;", (void *) getAVFilterInfo},
        {"configurationinfo", "()Ljava/lang/String;", (void *) getProtocolInfo},
        {"convertVideoFormat", "([Ljava/lang/String;)I", (void *) convertVideo}
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

    return JNI_VERSION_1_6;
}