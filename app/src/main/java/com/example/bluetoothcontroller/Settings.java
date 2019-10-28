package com.example.bluetoothcontroller;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.example.bluetoothcontroller.Dialogs.BluetoothBlurDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import br.vince.owlbottomsheet.OwlBottomSheet;

import static android.R.layout.simple_list_item_1;

//Activity с настройками приложения
public class Settings extends AppCompatActivity {

    public static final String ACTION = "STACTION";

    ArrayAdapter<String> pairedDeviceAdapter;
    ArrayList<CustomArrayAdapter.Device> pairedDeviceArrayList = new ArrayList<>();

    ListView listViewPairedDevice;

    BluetoothAdapter bluetoothAdapter;

    ArrayList devices_mac; //Для хранения MAC адресов устройств

    OwlBottomSheet mBottomSheet;

    DataBaseHelper dataBaseHelper;
    Switch isBackgroundSwitch;
    Switch autoConnectSwitch;
    SQLiteDatabase db;
    Cursor userCursor;

    int last_element_position; //Запоминает id последнего нажатого элемента в списке

    CustomArrayAdapter adapter;

    Intent intentAction = new Intent(BluetoothService.ACTION);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mBottomSheet = findViewById(R.id.owl_bottom_sheet);

        dataBaseHelper = new DataBaseHelper(this);
        db = dataBaseHelper.getWritableDatabase(); //Открываем базу данных для чтения

        devices_mac = new ArrayList();

        isBackgroundSwitch = findViewById(R.id.switch1);
        autoConnectSwitch = findViewById(R.id.switch2);

        userCursor = db.query(dataBaseHelper.TABLE_APP_SETTINGS, new String[]{dataBaseHelper.IS_BACKGROUND, dataBaseHelper.AUTO_CONNECT},
                null, null, null, null, null);
        userCursor.moveToFirst();

        if(userCursor.getInt(userCursor.getColumnIndex(dataBaseHelper.AUTO_CONNECT)) == 1) { //Если автоподключение разрешено
            autoConnectSwitch.setChecked(true);
        }else if(userCursor.getInt(userCursor.getColumnIndex(dataBaseHelper.AUTO_CONNECT)) == 0){
            autoConnectSwitch.setChecked(false);
        }

        if(userCursor.getInt(userCursor.getColumnIndex(dataBaseHelper.IS_BACKGROUND)) == 1) { //Если работа в фоне разрешена
            isBackgroundSwitch.setChecked(true);
        }else if(userCursor.getInt(userCursor.getColumnIndex(dataBaseHelper.IS_BACKGROUND)) == 0){
            isBackgroundSwitch.setChecked(false);
        }

        userCursor = db.query(dataBaseHelper.TABLE_APP_SETTINGS, new String[]{dataBaseHelper.IS_BACKGROUND, dataBaseHelper.AUTO_CONNECT},
                null, null, null, null, null);
        userCursor.moveToFirst();

        isBackgroundSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!isBackgroundSwitch.isChecked()){ //Включаем работу приложения в фоне
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(dataBaseHelper.IS_BACKGROUND, 0);
                    db.update(DataBaseHelper.TABLE_APP_SETTINGS, contentValues, dataBaseHelper.IS_BACKGROUND + "= ?", new String[]{"1"});
                }else{ //Выключаем работу приложения в фоне
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(dataBaseHelper.IS_BACKGROUND, 1);
                    db.update(DataBaseHelper.TABLE_APP_SETTINGS, contentValues, dataBaseHelper.IS_BACKGROUND + "= ?", new String[]{"0"});
                }
            }
        });

        autoConnectSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!isBackgroundSwitch.isChecked()){ //Включаем автоподключение
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(dataBaseHelper.AUTO_CONNECT, 0);
                    db.update(DataBaseHelper.TABLE_APP_SETTINGS, contentValues, dataBaseHelper.AUTO_CONNECT + "= ?", new String[]{"1"});
                }else{ //Выключаем автоподключение
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(dataBaseHelper.AUTO_CONNECT, 1);
                    db.update(DataBaseHelper.TABLE_APP_SETTINGS, contentValues, dataBaseHelper.AUTO_CONNECT + "= ?", new String[]{"0"});
                }
            }
        });

        //used to calculate some animations
        mBottomSheet.setActivityView(this);
        //icon to show in collapsed sheet
        mBottomSheet.setIcon(R.drawable.ic_bluetooth_black_24dp);

        mBottomSheet.setBottomSheetColor(ContextCompat.getColor(this,R.color.background_dark));

        //view will show in bottom sheet
        mBottomSheet.attachContentView(R.layout.bottom_sheet);

        //getting close button from view shown

        listViewPairedDevice = mBottomSheet.getContentView().findViewById(R.id.pairedlist);

        mBottomSheet.getContentView().findViewById(R.id.comments_sheet_close_button)
                .setOnClickListener(v -> mBottomSheet.collapse());


        BroadcastReceiver br = new BroadcastReceiver() { //Ловит команды с Activity
            // действия при получении сообщений
            @SuppressLint("WrongConstant")
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onReceive(Context context, Intent intent) { //Ловит события из BluetoothService

                    update_list_element(last_element_position, intent.getBooleanExtra("LIST_STATUS", false));

            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intFilt = new IntentFilter(ACTION);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(br, intFilt);


        setup();

    }

    private void setup() { // Создание списка сопряжённых Bluetooth-устройств

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if(pairedDeviceArrayList != null){pairedDeviceArrayList.clear();} //Очищаем лист для обновления устройств

        if (pairedDevices.size() > 0) { // Если есть сопряжённые устройства

            for (BluetoothDevice device : pairedDevices) { // Заполняем список именами сопряжённых устройств

                devices_mac.add(device.getAddress()); // Записываем MAC адрес устройств в отдельный массив
                devices_mac.add(device.getName()); // Записываем именя устройств в отдельный массив

                userCursor.moveToFirst();
                userCursor = db.query(dataBaseHelper.TABLE_CONNECT_DEVICE, new String[] { dataBaseHelper.DEVICE_MAC }, dataBaseHelper.DEVICE_MAC+ "=?",
                        new String[] { String.valueOf(device.getAddress()) }, null, null, null, null); //Смотрим есть ли определённое устройство в базе
                if (userCursor.getCount() > 0) { //Если устройство есть в базе, говорим об этом пользователю
                    if(bluetoothAdapter.getRemoteDevice(device.getAddress()) != null) { //Если устройство подключено, то статус "Подключено"
                        CustomArrayAdapter.Device d = new CustomArrayAdapter.Device(device.getName(), "Подключено");
                        pairedDeviceArrayList.add(d);
                    }else{//Если устройство не подключено, то статус "Сопряжено"
                        CustomArrayAdapter.Device d = new CustomArrayAdapter.Device(device.getName(), "Сопряжено");
                        pairedDeviceArrayList.add(d);
                    }
                }else{
                    CustomArrayAdapter.Device d = new CustomArrayAdapter.Device(device.getName(), null);
                    pairedDeviceArrayList.add(d);
                }
            }

            adapter = new CustomArrayAdapter(this, R.layout.list_bluetooth_item, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(adapter);

            listViewPairedDevice.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { //Долгое нажатие по элементу
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                    userCursor.moveToFirst();
                    userCursor = db.query(dataBaseHelper.TABLE_CONNECT_DEVICE, new String[] { dataBaseHelper.DEVICE_MAC }, dataBaseHelper.DEVICE_MAC+ "=?",
                            new String[] { (String) devices_mac.get(position) }, null, null, null, null); //Смотрим есть ли определённое устройство в базе

                    if (userCursor.getCount() > 0) { //Проверяем существование устройства в базе

                        final android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(Settings.this);
                        LayoutInflater inflater = Settings.this.getLayoutInflater();
                        View dialogView = inflater.inflate(R.layout.dialog_layout1, null);
                        dialogBuilder.setView(dialogView);

                        TextView textView = dialogView.findViewById(R.id.name_text);
                        TextView textView2 = dialogView.findViewById(R.id.mac_text);
                        CustomArrayAdapter.Device device = adapter.getItem(position);
                        textView.setText(device.getName());
                        textView2.setText((String) devices_mac.get(position));

                        android.app.AlertDialog dialog = dialogBuilder.create();

                        BluetoothBlurDialog bluetoothBlurDialog = BluetoothBlurDialog.newInstance(dialog);
                        bluetoothBlurDialog.show(getSupportFragmentManager(), "tag");

                        Button button =  dialogView.findViewById(R.id.del);
                        Button button2 = dialogView.findViewById(R.id.diskonnect);

                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) { //Забываем устройство
                                db.delete(DataBaseHelper.TABLE_CONNECT_DEVICE, dataBaseHelper.DEVICE_ID + "= ?", new String[]{String.valueOf(position)}); //Удаляем устройство из базы
                                setup(); //Обновляем список устройств
                                intentAction.putExtra("TASK", "close_connection"); //Отключаем устройство командой
                                sendBroadcast(intentAction);

                               dialog.dismiss(); //Закрываем диалог
                            }
                        });

                        button2.setOnClickListener(new View.OnClickListener() { //Отключаемся от устройства
                            @Override
                            public void onClick(View v) {
                                intentAction.putExtra("TASK", "close_connection"); //Отключаем устройство командой
                                sendBroadcast(intentAction);

                                dialog.dismiss(); //Закрываем диалог
                            }
                        });
                    }else { //Если устройства в базе нет, просто выводим информацию

                       final android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(Settings.this);
                        LayoutInflater inflater = Settings.this.getLayoutInflater();
                        View dialogView = inflater.inflate(R.layout.dialog_layout2, null);
                        dialogBuilder.setView(dialogView);

                        android.app.AlertDialog dialog1 = dialogBuilder.create();

                        TextView textView = dialogView.findViewById(R.id.name_text);
                        TextView textView2 = dialogView.findViewById(R.id.mac_text);
                        CustomArrayAdapter.Device device = adapter.getItem(position);
                        textView.setText(device.getName());
                        textView2.setText((String) devices_mac.get(position));

                        BluetoothBlurDialog bluetoothBlurDialog = BluetoothBlurDialog.newInstance(dialog1);
                        bluetoothBlurDialog.show(getSupportFragmentManager(), "tag");


                    }

                    return true;
                }
            });

            listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() { // Клик по нужному устройству

                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                        String MAC = (String) devices_mac.get(position); // Получаем MAC-адрес устройства для дальнейшего подключения

                    userCursor.moveToFirst();
                    userCursor = db.query(dataBaseHelper.TABLE_CONNECT_DEVICE, new String[] { dataBaseHelper.DEVICE_MAC }, dataBaseHelper.DEVICE_MAC+ "=?",
                            new String[] { String.valueOf(devices_mac.get(last_element_position)) }, null, null, null, null); //Смотрим есть ли определённое устройство в базе
                    if (userCursor.getCount() > 0) { //Обновляем статус элемента, если устройства нет в базе
                        CustomArrayAdapter.Device d = adapter.getItem(last_element_position);
                        d.setStatus("Сопряжено");
                        d.setProgress(false);
                        pairedDeviceArrayList.set(last_element_position, d);
                    }else{
                        CustomArrayAdapter.Device d = adapter.getItem(last_element_position);
                        d.setStatus(null);
                        d.setProgress(false);
                        pairedDeviceArrayList.set(last_element_position, d);
                    }
                    listViewPairedDevice.setAdapter(adapter); //Обновляем статус элемента

                        //Работа с БД
                        userCursor = db.query(dataBaseHelper.TABLE_CONNECT_DEVICE, new String[]{dataBaseHelper.DEVICE_NAME, dataBaseHelper.DEVICE_MAC},
                                null, null, null, null, null);

                        userCursor.moveToFirst();
                        userCursor = db.query(dataBaseHelper.TABLE_CONNECT_DEVICE, new String[]{dataBaseHelper.DEVICE_MAC}, dataBaseHelper.DEVICE_MAC + "=?",
                                new String[]{String.valueOf(MAC)}, null, null, null, null); //Смотрим есть ли определённое устройство в базе

                        if (DatabaseUtils.longForQuery(db, "SELECT COUNT(" + dataBaseHelper.DEVICE_MAC + ") FROM " + dataBaseHelper.TABLE_CONNECT_DEVICE, null) > 0) { //Проверяем существование устройства в базе
                            last_element_position = position;
                            intentAction.putExtra("TASK", "connect_to_device"); //Подключаемся к устройтсву
                            sendBroadcast(intentAction);
                        } else { //Если устройства нет, то добавляем в базу

                            ContentValues contentValues = new ContentValues();
                            CustomArrayAdapter.Device device = adapter.getItem(position);
                            contentValues.put(dataBaseHelper.DEVICE_NAME, device.getName());
                            contentValues.put(dataBaseHelper.DEVICE_MAC, MAC);
                            contentValues.put(dataBaseHelper.DEVICE_ID, position);
                            db.insert(DataBaseHelper.TABLE_CONNECT_DEVICE, null, contentValues); //Добавляем устройство в таблицу

                            last_element_position = position;
                            intentAction.putExtra("TASK", "connect_to_device"); //Подключаемся к устройтсву
                            sendBroadcast(intentAction);
                        }
                }
            });
        }

    }

    public void update_list_element(int position, boolean status){ //Обновляет элемент списка
        CustomArrayAdapter.Device d = adapter.getItem(position);
        if(status){
            d.setStatus("Подключение");
            d.setProgress(true);
        }else {
            d.setProgress(false);
            d.setStatus("Не удалось подключиться");
        }
        pairedDeviceArrayList.set(position, d);
        listViewPairedDevice.setAdapter(adapter); //Обновляем статус элемента
    }

    // collapse bottom sheet when back button pressed
    @Override
    public void onBackPressed() {
        if (!mBottomSheet.isExpanded())
            super.onBackPressed();
        else
            mBottomSheet.collapse();
    }

}
