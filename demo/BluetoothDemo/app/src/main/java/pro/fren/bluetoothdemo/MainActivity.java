package pro.fren.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private BluetoothAdapter bluetoothAdapter;
    private ListView lvDevices;
    private List<BluetoothBox> boxArray = new ArrayList<BluetoothBox>();

    private ArrayAdapter<BluetoothBox> arrayAdapter;

    private final UUID UID = UUID.fromString("44cad79e-1be0-4ef4-b024-49b221995aa2");
    private final String NAME = "judy";
    private AcceptThread acceptThread;

    private BluetoothSocket clientSocket;
    private BluetoothDevice device;

    private OutputStream outputStream; //客户端往服务端输出

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvDevices = (ListView) findViewById(R.id.lvDevices);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 得到蓝牙配对列表
        Set<BluetoothDevice> paireDevices = bluetoothAdapter.getBondedDevices();
        if (paireDevices.size() > 0) {
            for (BluetoothDevice device : paireDevices) {
                boxArray.add(new BluetoothBox(device.getName(),device.getAddress()));
                System.out.println("c:" + device.getName());
            }
        }
        arrayAdapter = new ArrayAdapter<BluetoothBox>(this ,
                android.R.layout.simple_expandable_list_item_1 , android.R.id.text1,boxArray);
        lvDevices.setAdapter(arrayAdapter);
        lvDevices.setOnItemClickListener(this);

        // 注册搜索接收器[蓝牙搜索是通过广播接收]
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);


        findViewById(R.id.btnOpen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothAdapter.enable();
            }
        });

        findViewById(R.id.btnClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothAdapter.disable();
            }
        });

        findViewById(R.id.btnSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTitle("Search......");
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                bluetoothAdapter.startDiscovery();
                boxArray.clear();
                arrayAdapter.notifyDataSetChanged();
            }
        });

        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 如果搜索到设备
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 如果该设备没有被绑定
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    BluetoothBox box = new BluetoothBox(device.getName(),device.getAddress());
                    boxArray.add(box);
                    arrayAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                setTitle("Search END.");

            }

        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothBox box = arrayAdapter.getItem(position);
        try {
            if (bluetoothAdapter.isDiscovering()){
                bluetoothAdapter.cancelDiscovery();
            }
            // 如果为空,表示该主机没有连接任何蓝牙设备
            if (device == null){
                device = bluetoothAdapter.getRemoteDevice(box.getAddress());
            }
            // 主动请求蓝牙配对
            if (clientSocket == null){
                clientSocket = device.createInsecureRfcommSocketToServiceRecord(UID);
                clientSocket.connect();
                // 客户端到服务端输出文本
                outputStream = clientSocket.getOutputStream();
            }
            outputStream.write("TTTTTTTTTT".getBytes("utf-8"));
        } catch (IOException e){
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private Handler handler = new Handler(){
        public void handleMessage(Message msg){
            Toast.makeText(MainActivity.this , String.valueOf(msg.obj) , Toast.LENGTH_SHORT).show();
            super.handleMessage(msg);
        }
    };

    private class AcceptThread extends Thread{
        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public AcceptThread(){
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME , UID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            try {
                socket = serverSocket.accept();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                while (true){
                    byte[] buffer = new byte[128];
                    int count = inputStream.read(buffer);
                            Message msg = new Message();
                    msg.obj = new String(buffer , 0 , count, "utf-8");
                    handler.sendMessage(msg);
                }
            } catch (Exception e){

            }
        }
    }

    private class BluetoothBox {
        private String name;
        private String address;

        public BluetoothBox(String name , String address){
            this.name = name;
            this.address = address;
        }

        public String getAddress() {
            return address;
        }

        public String getName() {
            return name;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString(){
            return name + ":" + address;
        }
    }

}
