package com.example.bluetoothcontroller;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

//Держит подключение приложения и выполняет некоторые его функции в фоне
public class BluetoothService extends Service {

    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    public static final String ACTION = "BTACTION";

    ThreadConnectBTdevice myThreadConnectBTdevice;
    BluetoothAdapter bluetoothAdapter;
    DataBaseHelper dataBaseHelper;
    SQLiteDatabase db;
    Cursor userCursor;
    String MAC; //Хранит MAC адрес подключенного устройства
    PendingIntent pendingIntent;

    private StringBuilder sb = new StringBuilder();

    Intent intentAction = new Intent(Settings.ACTION);

    boolean user_close_connect = false; //Сообщает о том, что подключение закрыл пользователь
    boolean user_connect = false; //Сообщает о том, что подключение открыл пользователь

    ThreadConnected myThreadConnected;

    Notification notification;

    BluetoothDevice device2;

    private UUID myUUID;
    final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";

    public BluetoothService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    PendingIntent piDisconnet;
    PendingIntent piSettings;
    PendingIntent piStop;
    PendingIntent piCloseNotif;

    @Override
    public void onCreate() {
        super.onCreate();

        //Работа с БД
        dataBaseHelper = new DataBaseHelper(BluetoothService.this);
        db = dataBaseHelper.getWritableDatabase(); //Открываем базу данных для чтения

        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Intent iDisconnect = new Intent(ACTION); //Отслеживает нажатие кнопок в уведомлениии
        iDisconnect.putExtra("NOTIF_BUTTON", "disconnect");
        piDisconnet = PendingIntent.getBroadcast(this, 1, iDisconnect, 0);

        Intent iSettings = new Intent(ACTION);
        iSettings.putExtra("NOTIF_BUTTON", "settings");
        piSettings = PendingIntent.getBroadcast(this, 2, iSettings, 0);

        Intent iStop = new Intent(ACTION);
        iStop.putExtra("NOTIF_BUTTON", "stop");
        piStop = PendingIntent.getBroadcast(this, 3, iStop, 0);

        Intent iCloseNotif = new Intent(ACTION);
        iCloseNotif.putExtra("NOTIF_BUTTON", "close_notif");
        piCloseNotif = PendingIntent.getBroadcast(this, 4, iCloseNotif, 0);

        if(DatabaseUtils.longForQuery(db, "SELECT COUNT(" + dataBaseHelper.DEVICE_MAC + ") FROM " + dataBaseHelper.TABLE_CONNECT_DEVICE, null) > 0){ //Подключаемся автоматически, есди в базе есть устройство

            userCursor = db.query(dataBaseHelper.TABLE_APP_SETTINGS, new String[]{dataBaseHelper.AUTO_CONNECT},
                    null, null, null, null, null);
            userCursor.moveToFirst();

            if (userCursor.getInt(userCursor.getColumnIndex(dataBaseHelper.AUTO_CONNECT)) == 1){ //Если разрешено автоматическое подключение

                createNotificationChannel(); //Тут создаём уведомление для постоянной работы приложения в фоне
                Intent notificationIntent = new Intent(this, MainActivity.class);
                pendingIntent = PendingIntent.getActivity(this,
                        0, notificationIntent, 0);

                notification = new NotificationCompat.Builder(this,CHANNEL_ID)
                        .setContentTitle("Поиск устройства")
                        .addAction(R.mipmap.ic_launcher_round, "Стоп", piStop)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentIntent(pendingIntent)
                        .build();

                startForeground(1, notification);

            connect_to_device(); //Подключаемся к устройству
            }
        }


        BroadcastReceiver br = new BroadcastReceiver() { //Ловит команды с Activity
            // действия при получении сообщений
            @SuppressLint("WrongConstant")
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onReceive(Context context, Intent intent) {

                if(intent.getStringExtra("Отключить") != null) {
                    myThreadConnectBTdevice.cancel();
                    user_close_connect = true; //Говорим о том, что подключение закрыл пользователь
                }

                if(intent.getStringExtra("NOTIF_BUTTON") != null) { //Отслеживаем нажатие кнопок в уведомлении
                    String value = intent.getStringExtra("NOTIF_BUTTON");
                    switch (value){
                        case "disconnect":
                        myThreadConnectBTdevice.cancel();
                        user_close_connect = true; //Говорим о том, что подключение закрыл пользователь
                        break;

                        case "settings":
                        Intent activity_intent = new Intent(BluetoothService.this, Settings.class);
                        startActivity(activity_intent);
                        break;

                        case "close_notif":
                            stopForeground(1);
                            break;

                        case "stop":
                            user_close_connect = true;
                            myThreadConnectBTdevice.cancel();
                            user_close_connect = true;
                            stopForeground(1);
                            break;

                    }

                }

                if(intent.getStringExtra("TASK") != null) {
                    String task = intent.getStringExtra("TASK");
                    switch (task) {
                        case "connect_to_device": //Подключение к устройству
                            intentAction.putExtra("LIST_STATUS", true); //Обновляем статус элемента списка устройств в настройках, если они открыты
                            sendBroadcast(intentAction);
                            connect_to_device();
                            break;
                        case "close_connection": //Закрываем подключение
                            myThreadConnectBTdevice.cancel();
                            break;
                    }
                }
                if(intent.getStringExtra("DATA") != null){ //Если есть данные для отправки по Bluetooth, отправляем
                    if(myThreadConnected != null){
                    myThreadConnected.write(intent.getStringExtra("DATA").getBytes());
                    }
                }

            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intFilt = new IntentFilter(ACTION);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(br, intFilt);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        userCursor = db.query(dataBaseHelper.TABLE_APP_SETTINGS, new String[]{dataBaseHelper.IS_BACKGROUND},
                null, null, null, null, null);
        userCursor.moveToFirst();

        if(userCursor.getInt(userCursor.getColumnIndex(dataBaseHelper.IS_BACKGROUND)) == 1) { //Если работа в фоне разрешена
            return Service.START_STICKY; //Заставляем перезапускаться службу, если она была остановлена
        }else{
            return Service.START_NOT_STICKY; //Не перезапускаем службу
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(myThreadConnectBTdevice!=null) myThreadConnectBTdevice.cancel();

    }

    private void connect_to_device(){ //Функция подключения к устройству

        userCursor = db.query(dataBaseHelper.TABLE_CONNECT_DEVICE, new String[]{dataBaseHelper.DEVICE_MAC},
                null, null, null, null, null);
        userCursor.moveToFirst();

        MAC = userCursor.getString(userCursor.getColumnIndex(dataBaseHelper.DEVICE_MAC));

        device2 = bluetoothAdapter.getRemoteDevice(MAC); //Получаем MAC устройства из базы и подклчюаемся к нему

            myThreadConnectBTdevice = new BluetoothService.ThreadConnectBTdevice(device2);
            myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth


    }

    private void createNotificationChannel() { //Создание канала уведомления
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Текущее подключение подключение",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }





    //ПОТОКИ

    public class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth

        private BluetoothSocket bluetoothSocket = null;

        private ThreadConnectBTdevice(BluetoothDevice device) {

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            }

            catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void connect_close(){
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() { // Коннект

            Looper.prepare();

            boolean success = false;

            try {
                bluetoothSocket.connect();
                success = true;
            }

            catch (IOException e) {
                e.printStackTrace();

                if(!user_close_connect || !user_connect){ //Переподключаемся снова, если подключение было закрыто не пользователем
                    connect_to_device();
                }

                intentAction.putExtra("LIST_STATUS", false); //Обновляем статус элемента списка устройств в настройках, если они открыты
                sendBroadcast(intentAction);
                Toast.makeText(BluetoothService.this, "не удалось подключиться к устройству!", Toast.LENGTH_LONG).show();
                /*runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Нет коннекта, проверьте Bluetooth-устройство с которым хотите соединица!", Toast.LENGTH_LONG).show();
                        listViewPairedDevice.setVisibility(View.VISIBLE);
                    }
                });*/

                try {
                    bluetoothSocket.close();
                }

                catch (IOException e1) {

                    e1.printStackTrace();
                }
            }

            if(success) {  // Если законнектились, тогда открываем панель с кнопками и запускаем поток приёма и отправки данных

                userCursor = db.query(dataBaseHelper.TABLE_CONNECT_DEVICE, new String[]{dataBaseHelper.DEVICE_NAME},
                        null, null, null, null, null);
                userCursor.moveToFirst();

                notification = new NotificationCompat.Builder(BluetoothService.this,CHANNEL_ID)
                        .setContentTitle("Подключено к " + userCursor.getString(userCursor.getColumnIndex(dataBaseHelper.DEVICE_NAME)))
                        .addAction(R.mipmap.ic_launcher_round, "Отключить", piDisconnet)
                        .addAction(R.mipmap.ic_launcher_round, "Настройки", piSettings)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentIntent(pendingIntent)
                        .build();

                startForeground(1, notification);

                /*runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        ButPanel.setVisibility(View.VISIBLE); // открываем панель с кнопками
                    }
                });*/
                myThreadConnected = new ThreadConnected(bluetoothSocket);
                myThreadConnected.start(); // запуск потока приёма и отправки данных
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @SuppressLint("WrongConstant")
        public void cancel() {

            userCursor = db.query(dataBaseHelper.TABLE_CONNECT_DEVICE, new String[]{dataBaseHelper.DEVICE_NAME},
                    null, null, null, null, null);
            userCursor.moveToFirst();

            stopForeground(1); //Убираем уведомление о подключении

            Toast.makeText(getApplicationContext(), "Отключено от " + userCursor.getString(userCursor.getColumnIndex(dataBaseHelper.DEVICE_NAME)), Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            }

            catch (IOException e) {
                e.printStackTrace();
            }
        }

    } // END ThreadConnectBTdevice:



    public class ThreadConnected extends Thread {    // Поток - приём и отправка данных

        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        private String sbprint;

        public ThreadConnected(BluetoothSocket socket) {

            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }

            catch (IOException e) {
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }


        @Override
        public void run() { // Приём данных

            while (true) {
                try {
                    byte[] buffer = new byte[1];
                    int bytes = connectedInputStream.read(buffer);
                    String strIncom = new String(buffer, 0, bytes);
                    sb.append(strIncom); // собираем символы в строку
                    int endOfLineIndex = sb.indexOf("\r\n"); // определяем конец строки

                    if (endOfLineIndex > 0) {

                        sbprint = sb.substring(0, endOfLineIndex);
                        sb.delete(0, sb.length());

                        /*runOnUiThread(new Runnable() { // Вывод данных

                            @Override
                            public void run() {

                                switch (sbprint) {

                                    case "D10 ON":
                                        d10.setText(sbprint);
                                        break;

                                    case "D10 OFF":
                                        d10.setText(sbprint);
                                        break;

                                    case "D11 ON":
                                        d11.setText(sbprint);
                                        break;

                                    case "D11 OFF":
                                        d11.setText(sbprint);
                                        break;

                                    case "D12 ON":
                                        d12.setText(sbprint);
                                        break;

                                    case "D12 OFF":
                                        d12.setText(sbprint);
                                        break;

                                    case "D13 ON":
                                        d13.setText(sbprint);
                                        break;

                                    case "D13 OFF":
                                        d13.setText(sbprint);
                                        break;

                                    default:
                                        break;
                                }
                            }
                        });*/
                    }
                } catch (IOException e) {

                    userCursor = db.query(dataBaseHelper.TABLE_CONNECT_DEVICE, new String[]{dataBaseHelper.DEVICE_NAME},
                            null, null, null, null, null);
                    userCursor.moveToFirst();

                    notification = new NotificationCompat.Builder(BluetoothService.this, CHANNEL_ID)
                            .setContentTitle("Отключено от " + userCursor.getString(userCursor.getColumnIndex(dataBaseHelper.DEVICE_NAME)))
                            .addAction(R.mipmap.ic_launcher_round, "Ок", piCloseNotif)
                            .setSmallIcon(R.drawable.ic_launcher_background)
                            .setContentIntent(pendingIntent)
                            .build();

                    startForeground(1, notification);

                    if(!user_close_connect)connect_to_device();

                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

}
