package com.blend.ndkadvanced.hello;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityHelloWorldBinding;

/**
 * JNI方法的注册:
 * 静态注册: 当Java层调用navtie函数时，会在JNI库中根据函数名查找对应的JNI函数。如果没找到，会报错。
 * 如果找到了，则会在native函数与JNI函数之间建立关联关系，其实就是保存JNI函数的函数指针。
 * <p>
 * <p>
 * 动态注册: 通过提供一个函数映射表，注册给JVM虚拟机，这样JVM就可以用函数映射表来调用相应的函数，
 * 就不必通过函数名来查找需要调用的函数。
 * <p>
 * <p>
 * 数据类型转换：分为基本数据类型转换和引用数据类型转换。
 * 除了Class、String、Throwable和基本数据类型的数组外，其余所有Java对象的数据类型在JNI中都用jobject
 * 表示。Java中的String也是引用类型，但是由于使用频率较高，所以在JNI中单独创建了一个jstring类型。
 * <p>
 * <p>
 * JNI函数签名信息：(参数1类型字符…)返回值类型字符
 * 数组以"["开头；
 * 引用类型为"L包名/类名;"，最后要有;结尾
 * 比如：long fun(int i, Class c)的函数签名是(ILjava/lang/Class)J
 * <p>
 * <p>
 * JNIEnv介绍:JNIEnv是一个线程相关的结构体, 该结构体代表了 Java 在本线程的运行环境。通过JNIEnv可以调用
 * 到一系列JNI系统函数。每个线程中都有一个 JNIEnv 指针。JNIEnv只在其所在线程有效, 它不能在线程之间进行传递。
 * 作用：访问Java成员变量和成员方法；调用Java构造方法创建Java对象等。
 */
public class HelloWorldActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private ActivityHelloWorldBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityHelloWorldBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // Example of a call to a native method
        TextView tv = mBinding.sampleText;
        tv.setText(stringFromJNI());

        mBinding.fromJNIDynamic.setOnClickListener(v -> tv.setText(stringFromJNIDynamic()));

    }


    public native String stringFromJNI();

    // 动态注册，这里爆红没有关系
    public native String stringFromJNIDynamic();
}