package com.iview.mirrorclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
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


    private Button pathStartButton;
    private Button pathStopButtonn;

    private Button pathUpButton;
    private Button pathDownButton;
    private Button pathLeftButton;
    private Button pathRightButton;

    private Button pathRunButton;
    private Button pathRunStopButtonn;

    private Button pathSetImageButton;
    private Button pathSetVidButtonn;

    private Button pathPreImageButton;
    private Button pathPreVidButtonn;


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


        pathStartButton = findViewById(R.id.pathStart);
        pathStartButton.setOnClickListener(this);
        pathStopButtonn = findViewById(R.id.pathStop);

        pathUpButton = findViewById(R.id.up);
        pathUpButton.setOnClickListener(this);
        pathDownButton = findViewById(R.id.down);
        pathDownButton.setOnClickListener(this);
        pathLeftButton = findViewById(R.id.left);
        pathLeftButton.setOnClickListener(this);
        pathLeftButton.setOnClickListener(this);
        pathRightButton = findViewById(R.id.right);
        pathRightButton.setOnClickListener(this);

        pathRunButton = findViewById(R.id.planRun);
        pathRunButton.setOnClickListener(this);
        pathRunStopButtonn = findViewById(R.id.planRunStop);
        pathRunStopButtonn.setOnClickListener(this);

        pathSetImageButton = findViewById(R.id.setImgButton);
        pathSetImageButton.setOnClickListener(this);
        pathSetVidButtonn = findViewById(R.id.setVidButton);
        pathSetVidButtonn.setOnClickListener(this);

        pathPreImageButton = findViewById(R.id.preImgButton);
        pathPreImageButton.setOnClickListener(this);
        pathPreVidButtonn = findViewById(R.id.preVidButton);
        pathPreVidButtonn.setOnClickListener(this);
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
            case R.id.pathStart:
                JSONObject jsonObject3 = new JSONObject();
                try {
                    jsonObject3.put("type", "PathPlanning");
                    jsonObject3.put("action", "Start");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg3 = jsonObject3.toString();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg3);
                    }
                });
                break;

            case R.id.pathStop:
                JSONObject jsonObject4 = new JSONObject();
                try {
                    jsonObject4.put("type", "PathPlanning");
                    jsonObject4.put("action", "Stop");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg4 = jsonObject4.toString();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg4);
                    }
                });
                break;
            case R.id.up:
                JSONObject jsonObject5 = new JSONObject();
                try {
                    jsonObject5.put("type", "PathPlanning");
                    jsonObject5.put("action", "Move");

                    JSONObject obj5 = new JSONObject();
                    obj5.put("angle", 90);

                    jsonObject5.put("arg", obj5);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg5 = jsonObject5.toString();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg5);
                    }
                });
                break;
            case R.id.down:
                JSONObject jsonObject6 = new JSONObject();
                try {
                    jsonObject6.put("type", "PathPlanning");
                    jsonObject6.put("action", "Move");

                    JSONObject obj6 = new JSONObject();
                    obj6.put("angle", 270);

                    jsonObject6.put("arg", obj6);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg6 = jsonObject6.toString();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg6);
                    }
                });
                break;
            case R.id.left:
                JSONObject jsonObject7 = new JSONObject();
                try {
                    jsonObject7.put("type", "PathPlanning");
                    jsonObject7.put("action", "Move");

                    JSONObject obj7 = new JSONObject();
                    obj7.put("angle", 180);

                    jsonObject7.put("arg", obj7);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg7 = jsonObject7.toString();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg7);
                    }
                });
                break;
            case R.id.right:
                JSONObject jsonObject8 = new JSONObject();
                try {
                    jsonObject8.put("type", "PathPlanning");
                    jsonObject8.put("action", "Move");

                    JSONObject obj8 = new JSONObject();
                    obj8.put("angle", 0);

                    jsonObject8.put("arg", obj8);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg8 = jsonObject8.toString();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg8);
                    }
                });
                break;

            case R.id.setImgButton:
                JSONObject jsonObject9 = new JSONObject();
                try {
                    jsonObject9.put("type", "PathPlanning");
                    jsonObject9.put("action", "Show");

                    JSONObject obj9 = new JSONObject();
                    obj9.put("rotation", 0);
                    obj9.put("url", "test.jpg");
                    obj9.put("imgTime", 5000);


                    jsonObject9.put("arg", obj9);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg9 = jsonObject9.toString();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg9);
                    }
                });
                break;
            case R.id.preImgButton:
                JSONObject jsonObject10 = new JSONObject();
                try {
                    jsonObject10.put("type", "PathPlanning");
                    jsonObject10.put("action", "Preview");

                    JSONObject obj10 = new JSONObject();
                    obj10.put("rotation", 0);
                    obj10.put("url", "test.jpg");
                    obj10.put("imgTime", 5000);

                    jsonObject10.put("arg", obj10);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg10 = jsonObject10.toString();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg10);
                    }
                });
                break;

            case R.id.planRun:
                JSONObject jsonObject11 = new JSONObject();
                try {
                    jsonObject11.put("type", "PathPlanning");
                    jsonObject11.put("action", "Run");


                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg11 = jsonObject11.toString();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg11);
                    }
                });
                break;
            case R.id.planRunStop:
                JSONObject jsonObject12 = new JSONObject();
                try {
                    jsonObject12.put("type", "PathPlanning");
                    jsonObject12.put("action", "RunStop");


                } catch (Exception e) {
                    e.printStackTrace();
                }

                final String msg12 = jsonObject12.toString();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        tcpClient.send(msg12);
                    }
                });
                break;
        }
    }
}
