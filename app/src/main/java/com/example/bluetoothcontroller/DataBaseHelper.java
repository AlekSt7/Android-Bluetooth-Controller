package com.example.bluetoothcontroller;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

//Вспомогательный класс для работы с базой данных
public class DataBaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Settings";


    public static final String TABLE_CONNECT_DEVICE = "connect_device"; //Таблица с устройствами, к которым можно подключиться

    public static final String DEVICE_ID = "device_id"; //id устройства
    public static final String DEVICE_NAME = "device_name"; //Имя подключенного устройсства
    public static final String DEVICE_MAC = "device_mac"; //Физический MAC-адрес устройства

    public static final String TABLE_APP_SETTINGS = "table_app_settings"; //Таблица с настройками приложения

    public static final String IS_BACKGROUND = "is_background"; //Разрешена ли работа приложения в фоне
    public static final String AUTO_CONNECT = "auto_connect"; //Разрешено ли автоподключение к устройству



    public DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("create table " + TABLE_CONNECT_DEVICE + //Создаём таблицу с усттройством, к которому должны подключаться
                "(" + DEVICE_ID + " integer," + DEVICE_NAME + " text," + DEVICE_MAC + " text" + ")");

        db.execSQL("create table " + TABLE_APP_SETTINGS + //Создаём таблицу с настройками приложения
                "(" + AUTO_CONNECT + " integer," + IS_BACKGROUND + " integer" + ")");

        ContentValues contentValues = new ContentValues();

        contentValues.put(IS_BACKGROUND, 0); //По умолчанию запрещаем приложению работать в фоне
        contentValues.put(AUTO_CONNECT, 1); //По умолчанию разрешаем приложению автоматически подключаться

        db.insert(DataBaseHelper.TABLE_APP_SETTINGS, null, contentValues); //Записываем дефолтные настройки


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_CONNECT_DEVICE + TABLE_APP_SETTINGS);

        onCreate(db);

    }
}
