package com.esafirm.sample;

import android.app.Application;

//import leakcanary.LeakCanary;


public class SampleApplication extends Application {
  @Override public void onCreate() {
    super.onCreate();
/*    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return;
    }
    LeakCanary.install(this);*/
  }
}
