package ru.vsu.arembroidery.utils

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.google.android.material.slider.Slider

@BindingAdapter("android:value")
fun setSliderValue(slider: Slider, value: Float?) {
    value?.let {
        if (it != slider.value) {
            slider.value = it
        }
    }
}

@InverseBindingAdapter(attribute = "android:value", event = "android:valueAttrChanged")
fun getSliderValue(slider: Slider): Float {
    return slider.value
}

@BindingAdapter("android:valueAttrChanged")
fun setSliderValueListener(slider: Slider, listener: InverseBindingListener?) {
    if (listener == null) {
        slider.clearOnChangeListeners()
    } else {
        slider.addOnChangeListener { _, _, _ ->
            listener.onChange()
        }
    }
}
