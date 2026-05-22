package com.example.servicesapp

import android.app.Application
import android.view.ViewGroup
import android.widget.LinearLayout
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.adsbase.StartAppSDK

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        StartAppSDK.init(this, "204314184", false)

        registerActivityLifecycleCallbacks(object :
            android.app.Application.ActivityLifecycleCallbacks {

            override fun onActivityResumed(activity: android.app.Activity) {

                val banner = Banner(activity)

                val root = activity.findViewById<ViewGroup>(android.R.id.content)

                root.addView(
                    banner,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }

            override fun onActivityCreated(a: android.app.Activity, b: android.os.Bundle?) {}
            override fun onActivityStarted(a: android.app.Activity) {}
            override fun onActivityPaused(a: android.app.Activity) {}
            override fun onActivityStopped(a: android.app.Activity) {}
            override fun onActivitySaveInstanceState(a: android.app.Activity, b: android.os.Bundle) {}
            override fun onActivityDestroyed(a: android.app.Activity) {}
        })
    }
}
