/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vsu.arembroidery

import android.view.View
import androidx.lifecycle.LifecycleOwner
import ru.vsu.arembroidery.common.helpers.SnackbarHelper

/**
 * Wraps [R.layout.activity_main] and controls lifecycle operations for [GLSurfaceView].
 */
class MainActivityView(activity: MainActivity, val renderer: AppRenderer) :
  androidx.lifecycle.DefaultLifecycleObserver {
  val root = View.inflate(
    activity,
    R.layout.activity_main,
    null
  )

  val snackbarHelper = SnackbarHelper().apply {
    setParentView(root.findViewById(R.id.coordinatorLayout))
    setMaxLines(6)
  }

  override fun onCreate(owner: LifecycleOwner) {
    super.onCreate(owner)
    renderer.startRendering()
  }

  fun post(action: java.lang.Runnable) = root.post(action)
}