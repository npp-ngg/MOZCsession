package com.myapp.mozc_session;
//
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
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
import java.util.List;
//
public class MainActivity extends AppCompatActivity {
//
    final String TAG="MOZCsession";
    View mainView;
    TextView key,word;
    Button sendkeyButton,specialkey1Button,resetButton;
    Context context;
    HandlerThread syncDataThread;
    Handler syncDataHandler;
    long sessionId;
    long selectedDictionaryId;
    Config config;
    private final String mozcDataFile="mozc.data";
    private final String mozcChildDir=".mozc";
    //
    public long createSession() {
        long sessionId=0;
//android.os.Debug.waitForDebugger();
        Command command=Command.newBuilder()
                .setInput(Input.newBuilder().setType(Input.CommandType.CREATE_SESSION).build())
                .build();
        Command response=execute(command);
        if(command!=null){
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
                    response=execute(command);
                } else {
                    Log.d(TAG,"Session creation failed: " + output.getErrorCode());
                }
            }
        }
        return sessionId;
    }
    //
    private Output getStatus(){
        SessionCommand getStatus=SessionCommand.newBuilder()
                .setType(SessionCommand.CommandType.GET_STATUS)
                .build();
        Input input=Input.newBuilder()
                .setType(Input.CommandType.SEND_COMMAND)
                .setId(sessionId)
                .setCommand(getStatus)
                .build();
        Command response=execute(Command.newBuilder().setInput(input).build());
        Output output=response.getOutput();
        if(output.hasMode())Log.d(TAG,"output.mode: "+output.getMode());
        if(output.hasStatus()){
            Status status=output.getStatus();
            if(status.hasActivated())Log.d(TAG,"Activated: "+status.getActivated());
            if(status.hasMode())Log.d(TAG,"Mode: "+status.getMode());
            if(status.hasComebackMode())Log.d(TAG,"ComebackMode: "+status.getComebackMode());
            if(status.hasUndoAvailable())Log.d(TAG,"UndoAvailable: "+status.getUndoAvailable());
        }
        if(output.hasServerVersion()){
            Output.VersionInfo versionInfo=output.getServerVersion();
            Log.d(TAG,"MozcVersion: "+versionInfo.getMozcVersion());
            Log.d(TAG,"DataVersion: "+versionInfo.getDataVersion());
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
                .setCandidatesSizeLimit(50)
                .setMixedConversion(true)
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
    Output sendSpace(){
        Request request = Request.newBuilder()
                .setCandidatesSizeLimit(50)
                .setMixedConversion(true)
                .build();
        Input input = Input.newBuilder()
                .setType(Input.CommandType.SEND_KEY)
                .setId(sessionId)
                .setKey(KeyEvent.newBuilder().setSpecialKey(KeyEvent.SpecialKey.SPACE).build())
                .setRequest(request)
                .build();
        Command command = Command.newBuilder()
                .setInput(input)
                .build();
        Command response = execute(command);
        if(response!=null) if (response.hasOutput()) return response.getOutput();
        return null;
    }
    //
    void resetContext() {
        ProtoCommands.Input input = Input.newBuilder()
                .setType(Input.CommandType.SEND_COMMAND)
                .setId(sessionId)
                .setCommand(ProtoCommands.SessionCommand.newBuilder()
                        .setType(ProtoCommands.SessionCommand.CommandType.RESET_CONTEXT))
                .build();
        Command command = Command.newBuilder().setInput(input).build();
        execute(command);
    }
    // 候補リストを取得するヘルパーメソッド
    private List<String> getCandidateStrings(Output output) {
        List<String> candidates = new ArrayList<>();
        if (output != null && output.hasCandidateWindow()) {
            for (CandidateWindow.Candidate candidate : output.getCandidateWindow().getCandidateList()) {
                candidates.add(candidate.getValue());
            }
        }
        return candidates;
    }
    // JNI呼び出しのラッパー
    private Command execute(Command command) {
        try {
            byte[] responseBytes = MozcJNI.evalCommand(command.toByteArray());
            Command response=Command.parseFrom(responseBytes);
            Log.d(TAG,"Response="+response.toString());
            return response;
        } catch (Exception e) {
            Log.e(TAG,e.getMessage());
            return null;
        }
    }
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
            preedit.getSegmentList().forEach(segment->{fullText.append(segment.getValue());});
            SpannableString compisingText=new SpannableString(fullText.toString());
            int start=0;
            for(ProtoCommands.Preedit.Segment segment:preedit.getSegmentList()){
                int end=start+segment.getValue().length();
                if(end==start)continue;
                compisingText.setSpan(new UnderlineSpan(),start,end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start=end;
            }
            word.setText(compisingText);
        }else
            word.setText("");
        List<CandidateWord> candidates = output.getAllCandidateWords().getCandidatesList();
        for (CandidateWord val : candidates) {
            Log.d(TAG, "候補: " + val.getValue() + ", ID: " + val.getId());
        }
    }
//
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        mainView=findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets)->{
            Insets systemBars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left,systemBars.top,systemBars.right,systemBars.bottom);
            return insets;
        });
        key=mainView.findViewById(R.id.key);
        key.setText("");
        word=mainView.findViewById(R.id.word);
        sendkeyButton=mainView.findViewById(R.id.sendbtn);
        specialkey1Button=mainView.findViewById(R.id.specialkey1btn);
        resetButton=mainView.findViewById(R.id.resetbtn);
        sendkeyButton.setOnClickListener(btn->{
            Output output=null;
            String input=key.getText().toString();
            String[] chars=input.split("");
            for(String s: chars) {
                if(!s.isEmpty()){
                    output = sendKey(s);
                    getStatus();
                }
            }
            if(output!=null){
                setComposingText(output);
                key.setText("");
            }
        });
//
        specialkey1Button.setOnClickListener(btn->{
            Output output=sendSpace();
            if(output!=null){
                setComposingText(output);
                getStatus();
                key.setText("");
            }
        });
//
        resetButton.setOnClickListener(btn->{
            resetContext();
        });
//
//
        context=getApplicationContext();
        ApplicationInfo info = Preconditions.checkNotNull(context).getApplicationInfo();
        File userProfileDirectory = new File(info.dataDir, mozcChildDir);
        if (!userProfileDirectory.exists()) userProfileDirectory.mkdirs();
        File dataFile = new File(userProfileDirectory, mozcDataFile);
        if (!dataFile.exists()) copyFileFromAssets(mozcDataFile, dataFile);
        MozcJNI.load(userProfileDirectory.getAbsolutePath(), dataFile.getAbsolutePath());
        sessionId=createSession();
        getStatus();
    }
}