package com.example

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mealoux", appName)
  }

  @Test
  fun `verify restaurant assets exist and can be loaded`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Test index.html
    context.assets.open("index.html").use { stream ->
      assertNotNull(stream)
      assertTrue(stream.available() > 0)
    }

    // Test restaurant logo
    context.assets.open("restaurants/the-wall-dely-brahim/logo.webp").use { stream ->
      assertNotNull(stream)
      assertTrue(stream.available() > 0)
    }

    // Test restaurant cover photo
    context.assets.open("restaurants/the-wall-dely-brahim/pdc.webp").use { stream ->
      assertNotNull(stream)
      assertTrue(stream.available() > 0)
    }
  }

  @Test
  fun `verify interceptUri returns valid WebResourceResponse with Content-Type`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val logoUri = Uri.parse("https://appassets.androidplatform.net/restaurants/the-wall-dely-brahim/logo.webp")
    val resp = interceptAssetUri(context, logoUri)
    assertNotNull("Response should not be null", resp)
    assertEquals("image/webp", resp?.mimeType)
    assertNotNull("Data stream should not be null", resp?.data)
    assertTrue("Data stream should have bytes", (resp?.data?.available() ?: 0) > 0)

    val pdcUri = Uri.parse("https://appassets.androidplatform.net/restaurants/the-wall-dely-brahim/pdc.webp")
    val pdcResp = interceptAssetUri(context, pdcUri)
    assertNotNull("Pdc response should not be null", pdcResp)
    assertEquals("image/webp", pdcResp?.mimeType)
    assertTrue("Pdc stream should have bytes", (pdcResp?.data?.available() ?: 0) > 0)
  }
}
