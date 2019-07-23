package com.senjucao.android_serialport_com;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.senjucao.serialport_api.serialport.SerialPort;
import com.senjucao.serialport_api.serialport.SerialPortFinder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static String[] baundRates = {"9600", "19200", "38400", "57600", "115200"};
    private final static String[] dataBits = {"8", "7", "6", "5"};
    private final static String[] stopBits = {"1", "2"};
    private final static String[] paritys = {"None", "Odd", "Even"};

    private Spinner spinner_endpoint;
    private Spinner spinner_bandRate;
    private Spinner spinner_dataBits;
    private Spinner spinner_stopBit;
    private Spinner spinner_parity;
    private CheckedTextView ctv_send_ascii;
    private CheckedTextView ctv_send_hex;
    private CheckedTextView ctv_receive_ascii;
    private CheckedTextView ctv_receive_hex;
    private RecyclerView rcy_log;
    private EditText edt_send;
    private Button bt_send;
    private Button bt_open;
    private Button bt_clear;
    private LogAdapter logAdapter;
    private ArrayList<String> logs;

    private SerialPortFinder serialPortFinder;
    private SerialPort serialPort;

    private String serialPath = "";
    private int bandRate = 9600;
    private int dataBit = 8;
    private int parity = 0;
    private int stopBit = 1;

    private boolean isSendHex = true;
    private boolean isReceiveHex = true;

    private Handler writerHandler;
    private HandlerThread handlerThread;
    private ReadThread readThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serialPortFinder = new SerialPortFinder();
        initView();

        handlerThread = new HandlerThread("writeHandler");
        handlerThread.start();
        writerHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (serialPort != null) {
                    try {
                        String obj = (String) msg.obj;
                        byte[] data = obj.getBytes();
                        if (isSendHex) {
                            data = HexData.stringTobytes(obj);
                        }
                        serialPort.getOutputStream().write(data);
                    } catch (IOException e) {
                        logs.add("发送失败。。。");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshList();
                            }
                        });
                        e.printStackTrace();
                    }
                } else {
                    logs.add("串口未打开...");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshList();
                        }
                    });
                }
            }
        };

    }

    private void initView() {
        spinner_endpoint = findViewById(R.id.spinner_endpoint);
        spinner_bandRate = findViewById(R.id.spinner_bandRate);
        spinner_dataBits = findViewById(R.id.spinner_dataBits);
        spinner_stopBit = findViewById(R.id.spinner_stopBit);
        spinner_parity = findViewById(R.id.spinner_parity);
        ctv_send_ascii = findViewById(R.id.ctv_send_ascii);
        ctv_send_hex = findViewById(R.id.ctv_send_hex);
        ctv_receive_ascii = findViewById(R.id.ctv_receive_ascii);
        ctv_receive_hex = findViewById(R.id.ctv_receive_hex);
        rcy_log = findViewById(R.id.rcy_log);
        edt_send = findViewById(R.id.edt_send);
        bt_send = findViewById(R.id.bt_send);
        bt_open = findViewById(R.id.bt_open);
        bt_clear = findViewById(R.id.bt_clear);
        bt_clear.setOnClickListener(this);

        spinner_bandRate.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, baundRates));
        spinner_dataBits.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataBits));
        spinner_parity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, paritys));
        spinner_stopBit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, stopBits));
        logs = new ArrayList<>();
        logAdapter = new LogAdapter(logs);
        rcy_log.setLayoutManager(new LinearLayoutManager(this));
        rcy_log.setAdapter(logAdapter);

        final String[] endpoints = serialPortFinder.getAllDevicesPath();
        spinner_endpoint.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, endpoints));
        if (endpoints.length > 0)
            serialPath = endpoints[0];

        bt_send.setOnClickListener(this);
        bt_open.setOnClickListener(this);

        spinner_endpoint.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                bt_open.setEnabled(true);
                serialPath = endpoints[position];
                if (serialPort != null) {
                    serialPort.close();
                }
                if (readThread != null) {
                    readThread.setKeep(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner_bandRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        bandRate = 9600;
                        break;
                    case 1:
                        bandRate = 19200;
                        break;
                    case 2:
                        bandRate = 38400;
                        break;
                    case 3:
                        bandRate = 57600;
                        break;
                    case 4:
                        bandRate = 115200;
                        break;
                }
                if (serialPort != null)
                    serialPort.close();
                if (readThread != null) {
                    readThread.setKeep(false);
                }
                bt_open.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner_dataBits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        dataBit = 8;
                        break;
                    case 1:
                        dataBit = 7;
                        break;
                    case 2:
                        dataBit = 6;
                        break;
                    case 3:
                        dataBit = 5;
                        break;
                }
                if (serialPort != null)
                    serialPort.close();
                if (readThread != null) {
                    readThread.setKeep(false);
                }
                bt_open.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner_parity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        parity = 0;
                        break;
                    case 1:
                        parity = 1;
                        break;
                    case 2:
                        parity = 2;
                        break;
                }
                if (serialPort != null)
                    serialPort.close();
                if (readThread != null) {
                    readThread.setKeep(false);
                }
                bt_open.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner_stopBit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        stopBit = 1;
                        break;
                    case 1:
                        stopBit = 2;
                        break;
                }
                if (serialPort != null)
                    serialPort.close();
                if (readThread != null) {
                    readThread.setKeep(false);
                }
                bt_open.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ctv_send_ascii.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSendHex) {
                    ctv_send_hex.toggle();
                    ctv_send_ascii.toggle();
                    isSendHex = false;
                }
            }
        });
        ctv_send_hex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSendHex) {
                    ctv_send_ascii.toggle();
                    ctv_send_hex.toggle();
                    isSendHex = true;
                }
            }
        });
        ctv_receive_ascii.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isReceiveHex) {
                    ctv_receive_hex.toggle();
                    ctv_receive_ascii.toggle();
                    isReceiveHex = false;
                }
            }
        });

        ctv_receive_hex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isReceiveHex) {
                    ctv_receive_ascii.toggle();
                    ctv_receive_hex.toggle();
                    isReceiveHex = true;
                }
            }
        });

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_send:
                if (edt_send.getText().toString().isEmpty()) {
                    logs.add("先填数据吧");
                    refreshList();
                    return;
                }
                if (!serialPort.isOpen()) {
                    logs.add("串口没打开");
                    refreshList();
                    return;
                }
                String data = edt_send.getText().toString();
                Message message = writerHandler.obtainMessage(0, data);
                writerHandler.sendMessage(message);

                break;
            case R.id.bt_open:
                if (serialPath.isEmpty()) return;
                try {
                    serialPort = new SerialPort(new File(serialPath), bandRate, parity, dataBit, stopBit, 0);
                } catch (Exception e) {
                    logs.add("打开串口失败.请检测路径或者权限。");
                    refreshList();
                    e.printStackTrace();
                    return;
                }
                logs.add("打开串口成功");
                refreshList();
                bt_open.setEnabled(false);
                readThread = new ReadThread();
                readThread.start();
                break;
            case R.id.bt_clear:
                logs.clear();
                refreshList();
                break;
        }
    }

    public void refreshList(){
        logAdapter.notifyDataSetChanged();
        rcy_log.scrollToPosition(logs.size() -1);
    }


    private class ReadThread extends Thread {
        private AtomicBoolean keep = new AtomicBoolean(true);

        @Override
        public void run() {
            try {
                InputStream is = serialPort.getInputStream();
                int available;
                int first;
                while (!isInterrupted()
                        && is != null
                        && (first = is.read()) != -1) {
                    do {
                        available = is.available();
                        SystemClock.sleep(10);
                    } while (available != is.available());

                    available = is.available();
                    byte[] bytes = new byte[available + 1];
                    is.read(bytes, 1, available);
                    bytes[0] = (byte) (first & 0xFF);
                    Log.d("ReceviceData: ", HexData.hexToString(bytes));
                    if (isReceiveHex)
                        logs.add(HexData.hexToString(bytes));
                    else
                        logs.add(new String(bytes));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshList();
                        }
                    });

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void setKeep(boolean keep) {
            this.keep.set(keep);
        }
    }

    class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHodler> {
        List<String> logs;

        LogAdapter(List<String> logs) {
            this.logs = logs;
        }

        @NonNull
        @Override
        public LogViewHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
            return new LogViewHodler(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHodler holder, int position) {
            holder.textView.setText(logs.get(position));
        }


        @Override
        public int getItemCount() {
            return logs.size();
        }


        public class LogViewHodler extends RecyclerView.ViewHolder {
            TextView textView;

            public LogViewHodler(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.tv_message);
            }
        }


    }

}
