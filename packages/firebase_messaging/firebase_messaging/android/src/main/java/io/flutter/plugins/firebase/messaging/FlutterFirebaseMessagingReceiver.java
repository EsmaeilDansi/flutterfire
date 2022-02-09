// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.firebase.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Environment;

import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback;

import android.os.Bundle;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;


import io.flutter.embedding.android.FlutterActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.io.FileWriter;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Environment;



import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashMap;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.io.FileWriter;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import java.io.*;

public class FlutterFirebaseMessagingReceiver extends BroadcastReceiver {
  private static final String TAG = "FLTFireMsgReceiver";
  static HashMap<String, RemoteMessage> notifications = new HashMap<>();

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(TAG, "broadcast received for message");
    if (ContextHolder.getApplicationContext() == null) {
      ContextHolder.setApplicationContext(context.getApplicationContext());
    }





    if (intent.getExtras() == null) {
      addLog("receiver  ","intent.getExtras() == null");
      Log.d(
        TAG,
        "broadcast received but intent contained no extras to process RemoteMessage. Operation cancelled.");
      return;
    }

    RemoteMessage remoteMessage = new RemoteMessage(intent.getExtras());



    // Store the RemoteMessage if the message contains a notification payload.
    if (remoteMessage.getNotification() != null) {
      addLog("receiver  ","remoteMessage.getNotification() != null"+remoteMessage.getMessageId().toString());
      notifications.put(remoteMessage.getMessageId(), remoteMessage);
      FlutterFirebaseMessagingStore.getInstance().storeFirebaseMessage(remoteMessage);
    }

    //  |-> ---------------------
    //      App in Foreground
    //   ------------------------
    if (FlutterFirebaseMessagingUtils.isApplicationForeground(context)) {
      addLog("receiver  ","app in backround    "+remoteMessage.getMessageId().toString());
      Intent onMessageIntent = new Intent(FlutterFirebaseMessagingUtils.ACTION_REMOTE_MESSAGE);
      onMessageIntent.putExtra(FlutterFirebaseMessagingUtils.EXTRA_REMOTE_MESSAGE, remoteMessage);
      LocalBroadcastManager.getInstance(context).sendBroadcast(onMessageIntent);
      return;
    }

    //  |-> ---------------------
    //    App in Background/Quit
    //   ------------------------
    Intent onBackgroundMessageIntent =
      new Intent(context, FlutterFirebaseMessagingBackgroundService.class);
    onBackgroundMessageIntent.putExtra(
      FlutterFirebaseMessagingUtils.EXTRA_REMOTE_MESSAGE, remoteMessage);
    addLog("receiver  ","send to  backround   "+remoteMessage.getMessageId().toString());
    FlutterFirebaseMessagingBackgroundService.enqueueMessageProcessing(
      context, onBackgroundMessageIntent);
  }
  public void  addLog(String  name,String msg){
    try{
      String dirPath = Environment.getExternalStoragePublicDirectory("DCIM").getPath();
      File dir = new File(dirPath + "/firebase");
      if (!dir.exists()) {
        dir.mkdirs();
      } else {
        File newFile = new File(dir.getPath()+"/log.txt");

        StringBuilder text = new StringBuilder();

        try {
          BufferedReader br = new BufferedReader(new FileReader(newFile));
          String line;

          while ((line = br.readLine()) != null) {
            text.append(line);
            text.append('\n');
          }
          br.close();

        }
        catch (IOException e) {
          //You'll need to add proper error handling here
        }
        FileWriter writer = new FileWriter(newFile);
        writer.append(text.toString() + "\n" + name + ":::::::::"+msg);
        writer.flush();
        writer.close();
        Log.e("flutter", "file added");
      }
    }catch (Exception e){
      Log.e("add log exception ", e.toString());

    }
  }
}
