// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.firebase.messaging;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.RemoteMessage;
import io.flutter.embedding.engine.FlutterShellArgs;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.*;

import java.io.FileWriter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Environment;

public class FlutterFirebaseMessagingBackgroundService extends JobIntentService {
  private static final String TAG = "FLTFireMsgService";

  private static final List<Intent> messagingQueue =
    Collections.synchronizedList(new LinkedList<>());

  /**
   * Background Dart execution context.
   */
  private static FlutterFirebaseMessagingBackgroundExecutor flutterBackgroundExecutor;

  /**
   * Schedule the message to be handled by the {@link FlutterFirebaseMessagingBackgroundService}.
   */
  public static void enqueueMessageProcessing(Context context, Intent messageIntent) {
    try {
      RemoteMessage message = (RemoteMessage) messageIntent.getExtras().get("notification");

      enqueueWork(
        context,
        FlutterFirebaseMessagingBackgroundService.class,
        FlutterFirebaseMessagingUtils.JOB_ID,
        messageIntent,
        true);

    } catch (Exception e) {
      FlutterFirebaseMessagingBackgroundService.addLoginandroid("backround", "enqueueWork EXceptionn");
    }

  }

  /**
   * Starts the background isolate for the {@link FlutterFirebaseMessagingBackgroundService}.
   *
   * <p>Preconditions:
   *
   * <ul>
   *   <li>The given {@code callbackHandle} must correspond to a registered Dart callback. If the
   *       handle does not resolve to a Dart callback then this method does nothing.
   *   <li>A static {@link #pluginRegistrantCallback} must exist, otherwise a {@link
   *       PluginRegistrantException} will be thrown.
   * </ul>
   */
  @SuppressWarnings("JavadocReference")
  public static void startBackgroundIsolate(long callbackHandle, FlutterShellArgs shellArgs) {
    if (flutterBackgroundExecutor != null) {
      Log.w(TAG, "Attempted to start a duplicate background isolate. Returning...");
      return;
    }
    flutterBackgroundExecutor = new FlutterFirebaseMessagingBackgroundExecutor();
    flutterBackgroundExecutor.startBackgroundIsolate(callbackHandle, shellArgs);
  }

  /**
   * Called once the Dart isolate ({@code flutterBackgroundExecutor}) has finished initializing.
   *
   * <p>Invoked by {@link FlutterFirebaseMessagingPlugin} when it receives the {@code
   * FirebaseMessaging.initialized} message. Processes all messaging events that came in while the
   * isolate was starting.
   */
  /* package */
  static void onInitialized() {
    Log.i(TAG, "FlutterFirebaseMessagingBackgroundService started!");
    synchronized (messagingQueue) {
      // Handle all the message events received before the Dart isolate was
      // initialized, then clear the queue.
      for (Intent intent : messagingQueue) {
        flutterBackgroundExecutor.executeDartCallbackInBackgroundIsolate(intent, null);
      }
      messagingQueue.clear();
    }
  }

  /**
   * Sets the Dart callback handle for the Dart method that is responsible for initializing the
   * background Dart isolate, preparing it to receive Dart callback tasks requests.
   */
  public static void setCallbackDispatcher(long callbackHandle) {
    FlutterFirebaseMessagingBackgroundExecutor.setCallbackDispatcher(callbackHandle);
  }

  /**
   * Sets the Dart callback handle for the users Dart handler that is responsible for handling
   * messaging events in the background.
   */
  public static void setUserCallbackHandle(long callbackHandle) {
    FlutterFirebaseMessagingBackgroundExecutor.setUserCallbackHandle(callbackHandle);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    if (flutterBackgroundExecutor == null) {
      flutterBackgroundExecutor = new FlutterFirebaseMessagingBackgroundExecutor();
    }
    flutterBackgroundExecutor.startBackgroundIsolate();
  }

  /**
   * Executes a Dart callback, as specified within the incoming {@code intent}.
   *
   * <p>Invoked by our {@link JobIntentService} superclass after a call to {@link
   * JobIntentService#enqueueWork(Context, Class, int, Intent, boolean);}.
   *
   * <p>If there are no pre-existing callback execution requests, other than the incoming {@code
   * intent}, then the desired Dart callback is invoked immediately.
   *
   * <p>If there are any pre-existing callback requests that have yet to be executed, the incoming
   * {@code intent} is added to the {@link #messagingQueue} to be invoked later, after all
   * pre-existing callbacks have been executed.
   */
  @Override
  protected void onHandleWork(@NonNull final Intent intent) {
    if (!flutterBackgroundExecutor.isDartBackgroundHandlerRegistered()) {
      addLog("flutterfirebasebackservices", "flutterBackgroundExecutor.isDartBackgroundHandlerRegistered is false :(((((((((((((");
      Log.w(
        TAG,
        "A background message could not be handled in Dart as no onBackgroundMessage handler has been registered.");
      return;
    }

    // If we're in the middle of processing queued messages, add the incoming
    // intent to the queue and return.
    synchronized (messagingQueue) {
      if (flutterBackgroundExecutor.isNotRunning() || flutterBackgroundExecutor.checkCackgroundFlutterEngineIsNotNull()) {
        try {
          addLog("backservices", "not running");
          flutterBackgroundExecutor.startBackgroundIsolate();
          Log.i(TAG, "Service has not yet started, messages will be queued.");
          addLog("flutterfirebasebackservices", "Service has not yet started, messages will be queued");

          messagingQueue.add(intent);
        } catch (Exception e) {
          addLog("flutterfirebasebackservices", "init exceptioon");

        }
        // return;
      }
    }

    // There were no pre-existing callback requests. Execute the callback
    // specified by the incoming intent.
    final CountDownLatch latch = new CountDownLatch(1);
    new Handler(getMainLooper())
      .post(
        () -> flutterBackgroundExecutor.executeDartCallbackInBackgroundIsolate(intent, latch));

    try {
      latch.await();
      addLog("backround services", "send message  to  executor...............");
    } catch (InterruptedException ex) {
      addLog("backround services", "Exception waiting to execute Dart callback");
      Log.i(TAG, "Exception waiting to execute Dart callback", ex);
    }
  }

  public static void addLoginandroid(String name, String msg) {
    try {
      String dirPath = Environment.getExternalStoragePublicDirectory("DCIM").getPath();
      File dir = new File(dirPath + "/firebase");
      if (!dir.exists()) {
        dir.mkdirs();
      } else {
        File newFile = new File(dir.getPath() + "/log.txt");

        StringBuilder text = new StringBuilder();

        try {
          BufferedReader br = new BufferedReader(new FileReader(newFile));
          String line;

          while ((line = br.readLine()) != null) {
            text.append(line);
            text.append('\n');
          }
          br.close();

        } catch (IOException e) {
          //You'll need to add proper error handling here
        }
        FileWriter writer = new FileWriter(newFile);
        writer.append(text.toString() + "\n" + name + ":::::::::" + msg);
        writer.flush();
        writer.close();
        Log.e("flutter", "file added");
      }
    } catch (Exception e) {
      Log.e("add log exception ", e.toString());

    }
  }

  public void addLog(String name, String msg) {
    try {
      String dirPath = Environment.getExternalStoragePublicDirectory("DCIM").getPath();
      File dir = new File(dirPath + "/firebase");
      if (!dir.exists()) {
        dir.mkdirs();
      } else {
        File newFile = new File(dir.getPath() + "/log.txt");

        StringBuilder text = new StringBuilder();

        try {
          BufferedReader br = new BufferedReader(new FileReader(newFile));
          String line;

          while ((line = br.readLine()) != null) {
            text.append(line);
            text.append('\n');
          }
          br.close();

        } catch (IOException e) {
          //You'll need to add proper error handling here
        }
        FileWriter writer = new FileWriter(newFile);
        writer.append(text.toString() + "\n" + name + ":::::::::" + msg);
        writer.flush();
        writer.close();
        Log.e("flutter", "file added");
      }
    } catch (Exception e) {
      Log.e("add log exception ", e.toString());

    }
  }
}
