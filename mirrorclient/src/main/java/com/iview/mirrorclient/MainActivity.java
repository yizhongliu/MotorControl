package com.iview.mirrorclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static String TAG = "MainActivity";

    private EditText serverIpEdit;
    private EditText serverPortEdit;

    private Button startButton;
    private Button sendButton;

    private Button imgButton;
    private Button vidButton;

    private TcpClient tcpClient = null;

    ExecutorService exec = Executors.newCachedThreadPool();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        serverIpEdit = findViewById(R.id.serverIpEdit);
        serverPortEdit = findViewById(R.id.serverPortEdit);

        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(this);
        sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);

        imgButton = findViewById(R.id.imgButton);
        imgButton.setOnClickListener(this);
        vidButton = findViewById(R.id.vidButton);
        vidButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.startButton:
                String serverip = serverIpEdit.getText().toString();
                int serverPort = Integer.parseInt(serverPortEdit.getText().toString());
                tcpClient = new TcpClient(serverip, serverPort);
                exec.execute(tcpClient);
                break;
            case R.id.sendButton:
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send("test tcp client");
                    }
                });
                break;
            case R.id.imgButton:
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", "Control");
                    jsonObject.put("action", "ShowImage");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg = jsonObject.toString();

                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg);
                    }
                });
                break;
            case R.id.vidButton:
                JSONObject jsonObject2 = new JSONObject();
                try {
                    jsonObject2.put("type", "Control");
                    jsonObject2.put("action", "ShowVideo");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg2 = jsonObject2.toString();

                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg2);
                    }
                });
                break;
        }
    }
}
