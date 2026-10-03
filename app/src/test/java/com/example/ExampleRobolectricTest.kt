package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WeatherNow", appName)
  }

  @Test
  fun `test weather condition mapping`() {
    assertEquals(WeatherCondition.SUNNY, WeatherCondition.fromWmoCode(0))
    assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherCondition.fromWmoCode(2))
    assertEquals(WeatherCondition.RAINY, WeatherCondition.fromWmoCode(61))
    assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.fromWmoCode(95))
    assertEquals(WeatherCondition.SNOWY, WeatherCondition.fromWmoCode(71))
  }
}
