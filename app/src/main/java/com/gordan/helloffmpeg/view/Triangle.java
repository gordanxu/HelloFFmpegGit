package com.gordan.helloffmpeg.view;

import android.graphics.Shader;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Triangle {

    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private final int mProgram;

    private int positionHandle;
    private int colorHandle;

    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex


    private FloatBuffer vertexBuffer;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float triangleCoords[] = {   // in counterclockwise order:
            0.0f, 0.622008459f, 0.0f, // top
            -0.5f, -0.311004243f, 0.0f, // bottom left
            0.5f, -0.311004243f, 0.0f  // bottom right
    };

    // Set color with red, green, blue and alpha (opacity) values
    float color[] = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

    public Triangle() {

        /*******
         * 在堆上开辟缓存空间（缓存对于长距离的读写操作可以提高效率 单次读写的效率提高）
         * allocateDirect() 不在JAVA虚拟机上开辟空间 而直接在操作系统上开辟
         * allocate() 在JAVA虚拟机的堆上开辟空间
         *
         * MappedByteBuffer 大文件映射的类 在读书APK里用过的
         *
         *https://blog.csdn.net/xialong_927/article/details/81044759
         * *****/

        //声明 4 倍长度的 三角形空间
        ByteBuffer byteBuffer=ByteBuffer.allocateDirect(triangleCoords.length*4);
        byteBuffer.order(ByteOrder.nativeOrder());

        vertexBuffer=byteBuffer.asFloatBuffer();

        vertexBuffer.put(triangleCoords);

        vertexBuffer.position(0);

        //加载顶点着色器
        int vertexShader=GordanRender.loadShader(GLES20.GL_VERTEX_SHADER,vertexShaderCode);

        //加载片元着色器
        int fragmentShader=GordanRender.loadShader(GLES20.GL_FRAGMENT_SHADER,fragmentShaderCode);

        //创建程序
        mProgram = GLES20.glCreateProgram();//构造方法在类实例化的时候就会创建

        //注册顶点着色器
        GLES20.glAttachShader(mProgram,vertexShader);
        //注册片元着色器
        GLES20.glAttachShader(mProgram,fragmentShader);
        //链接程序
        GLES20.glLinkProgram(mProgram);
    }


    public void draw() {


        //使用程序
        GLES20.glUseProgram(mProgram);

        //获取程序属性
        positionHandle=GLES20.glGetAttribLocation(mProgram,"vPosition");

        //启用顶点着色器的属性(着色器的值默认是禁用的)
        GLES20.glEnableVertexAttribArray(positionHandle);

        //向GPU传递顶点属性
        GLES20.glVertexAttribPointer(positionHandle,COORDS_PER_VERTEX,GLES20.GL_FLOAT,false,vertexStride,vertexBuffer);

        colorHandle=GLES20.glGetUniformLocation(mProgram,"vColor");


        //改变colorHandle的值
        //https://blog.csdn.net/wangyuchun_799/article/details/7742787

        GLES20.glUniform4fv(colorHandle,1,color,0);

        //绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertexCount);


        //禁用顶点着色器的属性
        GLES20.glDisableVertexAttribArray(positionHandle);


    }


}
