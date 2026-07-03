package com.myapp.mozc_session;
//
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
//
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
//
import com.google.android.apps.inputmethod.libs.mozc.session.MozcJNI;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
//
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow.CandidateWindow;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow.CandidateList;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow.CandidateWord;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Command;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.KeyEvent;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Output;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Request;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.SessionCommand;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Status;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig.Config;
//
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//
public class MainActivity extends AppCompatActivity {
    //
    final String TAG = "MOZCsession";
    View mainView;
    EditText key, word;
    Button sendkeyButton, specialkeyButton, resetButton;
    Context context;
    HandlerThread sendCommandThread,setOutputThread;
    Handler sendCommandHandler,setOutputHandler;
    long sessionId;
    List<String> candidateList=new ArrayList<>();
    long selectedDictionaryId;
    Config config;
    private static final int MOZC_SEND_SPECIAL_KEY=0;
    private static final int MOZC_SEND_KEY=1;
    private static final int MOZC_SEND_SESSION_COMMAND=2;
    private final String mozcDataFile = "mozc.data";
    private final String mozcChildDir = ".mozc";
//
    private class SetOutput implements Runnable{
        Output output;
        public SetOutput(Output output){
            this.output=output;
        }
        @Override public void run(){
            List<String> candidateList=getAllCandidateList(output);
            setComposingText(output);
            Log.d(TAG,"候補数="+candidateList.size());
            Log.d(TAG,"候補="+candidateList.toString());
            key.setText("");
        }
    }
    //
    private Config getConfig() {
        Input input = Input.newBuilder().setType(Input.CommandType.GET_CONFIG).build();
        Command command = Command.newBuilder().setInput(input).build();
        Command response = execute(command);
        if (response != null && response.hasOutput() && response.getOutput().hasConfig()) {
            Config config = response.getOutput().getConfig();
            Log.d(TAG, "Mozc Config: IncognitoMode=" + config.getIncognitoMode());
            Log.d(TAG, "Mozc Config: PreeditMethod=" + config.getPreeditMethod());
            return config;
        }
        return null;
    }

    private Config setConfig() {
        Input input = Input.newBuilder().setType(Input.CommandType.SET_CONFIG)
                .setConfig(Config.newBuilder().setSuggestionsSize(15).build())
                .build();
        Command command = Command.newBuilder().setInput(input).build();
        Command response = execute(command);
        if (response != null && response.hasOutput() && response.getOutput().hasConfig()) {
            Config config = response.getOutput().getConfig();
            Log.d(TAG, "Mozc Config: IncognitoMode=" + config.getIncognitoMode());
            Log.d(TAG, "Mozc Config: PreeditMethod=" + config.getPreeditMethod());
            return config;
        }
        return null;
    }
//
    public long createSession() {
        long sessionId = 0;
//android.os.Debug.waitForDebugger();
        Command command = Command.newBuilder()
                .setInput(Input.newBuilder().setType(Input.CommandType.CREATE_SESSION).build())
                .build();
        Command response = execute(command);
        if (command != null) {
            if (response.hasOutput()) {
                Output output = response.getOutput();
                if (output.getErrorCode() == Output.ErrorCode.SESSION_SUCCESS) {
                    sessionId = output.getId();
                    SessionCommand switchMode = SessionCommand.newBuilder()
                            .setType(SessionCommand.CommandType.SWITCH_COMPOSITION_MODE)
                            .setCompositionMode(ProtoCommands.CompositionMode.HIRAGANA)
                            .build();
                    Input input = Input.newBuilder()
                            .setType(Input.CommandType.SEND_COMMAND)
                            .setId(sessionId)
                            .setCommand(switchMode)
                            .build();
                    command = Command.newBuilder().setInput(input).build();
                    response = execute(command);
                } else {
                    Log.d(TAG, "Session creation failed: " + output.getErrorCode());
                }
            }
        }
        return sessionId;
    }
//
    private Output getStatus() {
        SessionCommand getStatus = SessionCommand.newBuilder()
                .setType(SessionCommand.CommandType.GET_STATUS)
                .build();
        Input input = Input.newBuilder()
                .setType(Input.CommandType.SEND_COMMAND)
                .setId(sessionId)
                .setCommand(getStatus)
                .build();
        Command response = execute(Command.newBuilder().setInput(input).build());
        Output output = response.getOutput();
        if (output.hasMode()) Log.d(TAG, "output.mode: " + output.getMode());
        if (output.hasStatus()) {
            Status status = output.getStatus();
            if (status.hasActivated()) Log.d(TAG, "Activated: " + status.getActivated());
            if (status.hasMode()) Log.d(TAG, "Mode: " + status.getMode());
            if (status.hasComebackMode()) Log.d(TAG, "ComebackMode: " + status.getComebackMode());
            if (status.hasUndoAvailable())
                Log.d(TAG, "UndoAvailable: " + status.getUndoAvailable());
        }
        if (output.hasServerVersion()) {
            Output.VersionInfo versionInfo = output.getServerVersion();
            Log.d(TAG, "MozcVersion: " + versionInfo.getMozcVersion());
            Log.d(TAG, "DataVersion: " + versionInfo.getDataVersion());
        }
        return output;
    }

    // 2. キー入力の送信（変換）
    Output sendKey(String keyString) {
        // 1. キーイベントを定義
        ProtoCommands.KeyEvent keyEvent = KeyEvent.newBuilder()
                .setKeyString(keyString)
                .build();
        Request request = Request.newBuilder()
                .setCandidatesSizeLimit(30)
                .setMixedConversion(true)
                .setAutoPartialSuggestion(true)
                .build();
        Input input = Input.newBuilder()
                .setType(Input.CommandType.SEND_KEY)
                .setId(sessionId)
                .setKey(keyEvent)
                .setRequest(request)
                .build();
        Command command = Command.newBuilder()
                .setInput(input)
                .build();
        Command response = execute(command);
        if (response != null)
            if (response.hasOutput()) {
                return response.getOutput();
            }
        return null;
    }
//
//
    Output sendSpecialKey(int n) {
        Request request = Request.newBuilder()
                .setCandidatesSizeLimit(50)
                .setMixedConversion(true)
                .build();
        Input input = Input.newBuilder()
                .setType(Input.CommandType.SEND_KEY)
                .setId(sessionId)
                .setKey(KeyEvent.newBuilder().setSpecialKey(KeyEvent.SpecialKey.values()[n]).build())
                .setRequest(request)
                .build();
        Command command = Command.newBuilder()
                .setInput(input)
                .build();
        Command response = execute(command);
        if (response != null) if (response.hasOutput()) return response.getOutput();
        return null;
    }
    //
    Command sendSessionCommand(int n){
        ProtoCommands.Input input=Input.newBuilder()
                .setType(Input.CommandType.SEND_COMMAND)
                .setId(sessionId)
                .setCommand(ProtoCommands.SessionCommand.newBuilder()
                        .setType(ProtoCommands.SessionCommand.CommandType.values()[n]))
                .build();
        Command command=Command.newBuilder().setInput(input).build();
        return execute(command);
    }
    // 候補リストを取得するヘルパーメソッド
    private List<String> getAllCandidateList(Output output) {
        List<String> candidateList = new ArrayList<>();
        if (output == null || !output.hasAllCandidateWords()) return candidateList;
        List<CandidateWord> candidates = output.getAllCandidateWords().getCandidatesList();
        candidates.forEach(c -> candidateList.add(c.getValue()));
        return candidateList;
    }
    // JNI呼び出しのラッパー
    private Command execute(Command command) {
        try {
            byte[] responseBytes = MozcJNI.evalCommand(command.toByteArray());
            Command response = Command.parseFrom(responseBytes);
//            Log.d(TAG,"Response="+response.toString());
            return response;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
//
    private void copyFileFromAssets(String assetFileName, File destFile) {
        try (InputStream is = getAssets().open(assetFileName);
             FileOutputStream os = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024 * 8];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy asset file: " + assetFileName + e.getMessage());
        }
    }
//
    private void setComposingText(Output output){
        if(output.hasPreedit()){
            StringBuilder fullText=new StringBuilder();
            ProtoCommands.Preedit preedit=output.getPreedit();
            int cursor=preedit.getCursor();
            Log.d(TAG,"cursor="+cursor);
            preedit.getSegmentList().forEach(segment->{
                fullText.append(segment.getValue());
            });
            SpannableString compisingText=new SpannableString(fullText.toString());
            int start=0;
            for(ProtoCommands.Preedit.Segment segment:preedit.getSegmentList()){
                int end=start+segment.getValue().length();
                if(end==start)continue;
                compisingText.setSpan(new UnderlineSpan(),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start=end;
            }
            word.setText(compisingText);
        }else
            word.setText("");
    }
//
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        mainView=findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView,(v,insets)->{
            Insets systemBars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left,systemBars.top,systemBars.right,systemBars.bottom);
            return insets;
        });
        key=mainView.findViewById(R.id.key);
        key.setText("");
        word=mainView.findViewById(R.id.word);
        sendkeyButton=mainView.findViewById(R.id.sendbtn);
        specialkeyButton=mainView.findViewById(R.id.specialkey1btn);
        resetButton=mainView.findViewById(R.id.resetbtn);
        sendkeyButton.setOnClickListener(btn->{
            String word=key.getText().toString();
            for(int i=0;i<word.length();i++)
                sendCommandHandler.sendMessage(sendCommandHandler.obtainMessage(MOZC_SEND_KEY,(int)(word.charAt(i)),0));
            key.setText("");
        });
//
        specialkeyButton.setOnClickListener(btn->{
            sendCommandHandler.sendMessage(
                    sendCommandHandler.obtainMessage(
                        MOZC_SEND_SPECIAL_KEY,Integer.parseInt(key.getText().toString()),0));
            key.setText("");
        });
//
        resetButton.setOnClickListener(btn->{
            sendCommandHandler.removeCallbacksAndMessages(null);
            sendCommandHandler.sendMessage(
                    sendCommandHandler.obtainMessage(
                        MOZC_SEND_SESSION_COMMAND,Integer.parseInt(key.getText().toString()),0));
            key.setText("");
        });
//
//
        context = getApplicationContext();
        ApplicationInfo info = Preconditions.checkNotNull(context).getApplicationInfo();
        File userProfileDirectory = new File(info.dataDir, mozcChildDir);
        if (!userProfileDirectory.exists()) userProfileDirectory.mkdirs();
        File dataFile = new File(userProfileDirectory, mozcDataFile);
        if (!dataFile.exists()) copyFileFromAssets(mozcDataFile, dataFile);
        MozcJNI.load(userProfileDirectory.getAbsolutePath(), dataFile.getAbsolutePath());
//        MozcJNI.load(userProfileDirectory.getAbsolutePath(), null);
        sessionId = createSession();
        setConfig();
        getConfig();
        getStatus();
        sendCommandThread=new HandlerThread("sendCommandThread");
        sendCommandThread.start();
        sendCommandHandler=new Handler(sendCommandThread.getLooper()){
            @Override public void handleMessage(Message msg){
                switch(msg.what){
                    case MOZC_SEND_KEY:{
                        Output output=sendKey(String.valueOf((char)msg.arg1));
                        if(output!=null)setOutputHandler.sendMessage(setOutputHandler.obtainMessage(0,output));
                        break;
                    }
                    case MOZC_SEND_SPECIAL_KEY:{
                        Output output=sendSpecialKey(msg.arg1);
                        if(output!=null)setOutputHandler.sendMessage(setOutputHandler.obtainMessage(0,output));
                        break;
                    }
                    case MOZC_SEND_SESSION_COMMAND:
                        sendSessionCommand(msg.arg1);
                        break;
                    default:
                }
            }
        };
        setOutputThread=new HandlerThread("setOutputThread");
        setOutputThread.start();
        setOutputHandler=new Handler(setOutputThread.getLooper()){
            @Override public void handleMessage(Message msg){
                Output output=(Output)msg.obj;
                if(output!=null)runOnUiThread(new SetOutput((Output)msg.obj));
            }
        };
    }
}