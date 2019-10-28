package com.example.bluetoothcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bluetoothcontroller.Dialogs.BluetoothBlurDialog;


//Основная Activity
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    Intent intentAction = new Intent(BluetoothService.ACTION);

    BluetoothAdapter bluetoothAdapter;

    DataBaseHelper dataBaseHelper;
    SQLiteDatabase db;
    Cursor userCursor;

    FrameLayout ButPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";

        ButPanel = (FrameLayout) findViewById(R.id.ButPanel);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this, "BLUETOOTH NOT support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        dataBaseHelper = new DataBaseHelper(this);
        db = dataBaseHelper.getWritableDatabase(); //Открываем базу данных для чтения

            Intent intent = new Intent(this, BluetoothService.class); //Запускаем сервис по работе с Bluetooth
            startService(intent);

    } // END onCreate

    @Override
    protected void onStart() { // Запрос на включение Bluetooth
        super.onStart();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        //setup();
    }

    @Override
    protected void onDestroy() { // Закрытие приложения
        super.onDestroy();

       userCursor = db.query(dataBaseHelper.TABLE_APP_SETTINGS, new String[]{dataBaseHelper.IS_BACKGROUND},
                null, null, null, null, null);
        userCursor.moveToFirst();

        if(userCursor.getInt(userCursor.getColumnIndex(dataBaseHelper.IS_BACKGROUND)) == 1) { //Если работа в фоне разрешена

        }else{
            stopService(new Intent(this, BluetoothService.class));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) { // Если разрешили включить Bluetooth, тогда void setup()

            if (resultCode == Activity.RESULT_OK) {
                //setup();
            } else { // Если не разрешили, тогда закрываем приложение

                Toast.makeText(this, "BlueTooth не включён", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

BluetoothService bluetoothService = new BluetoothService();

/////////////////// Нажатие кнопок /////////////////////
/////////////////////////D10////////////////////////////

    public void onClickBut9(View v) {

        Intent intent = new Intent(MainActivity.this, Settings.class);
        startActivity(intent);

    }

    public void onClickBut10(View v) {

    }

    public void onClickBut1(View v) {

        intentAction.putExtra("DATA", "a"); //Отправляем данные (символ "a") по Bluetooth
        sendBroadcast(intentAction);
    }
} // END
