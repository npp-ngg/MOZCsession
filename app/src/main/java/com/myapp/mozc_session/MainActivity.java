package com.myapp.mozc_session;
//
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
//
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
//
import com.google.android.apps.inputmethod.libs.mozc.session.MozcJNI;
import com.google.common.base.Preconditions;
//
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Command;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.KeyEvent;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Output;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig.Config;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow.CandidateWindow;
//
import java.io.File;
import java.util.List;
//
public class MainActivity extends AppCompatActivity {
//
    final String TAG="MOZCsession";
    View mainView;
    TextView key,word,addButton,selButton;
    Context context;
    HandlerThread syncDataThread;
    Handler syncDataHandler;
    long sessionId;
    long selectedDictionaryId;
    Config config;
    //
    class SaveTouchEvent implements View.OnTouchListener{
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent){
            return false;
        }
    }
    public long createSession() {
        long sessionId=0;
        Command command=Command.newBuilder()
                .setInput(Input.newBuilder()
                        .setType(Input.CommandType.CREATE_SESSION)
                        .build())
                .build();
        Command response=execute(command);
        if(command!=null){
            if (response.hasOutput()) {
                Output output = response.getOutput();
                if (output.getErrorCode() == Output.ErrorCode.SESSION_SUCCESS) {
                    sessionId = output.getId();
                } else {
                    System.err.println("Session creation failed: " + output.getErrorCode());
                }
            }
        }
        return sessionId;
    }
    // 2. キー入力の送信（変換）
    Output sendKey(String keyString) {
        // 1. キーイベントを定義
        ProtoCommands.KeyEvent keyEvent = KeyEvent.newBuilder()
                .setKeyString(keyString)
                .build();
        Input input = Input.newBuilder()
                .setType(Input.CommandType.SEND_KEY)
                .setId(sessionId) // ★ Inputに対してIDをセットする
                .setKey(keyEvent)
                .build();
        Command command = Command.newBuilder()
                .setInput(input)
                .build();
        Command response = execute(command);
        if(response!=null)
            if (response.hasOutput()) {
                return response.getOutput();
            }
        return null;
    }
    Output sendSpace(){
        Input input = Input.newBuilder()
                .setType(Input.CommandType.SEND_KEY)
                .setId(sessionId)
                .setKey(KeyEvent.newBuilder().setSpecialKey(KeyEvent.SpecialKey.SPACE).build())
                .build();
        Command command = Command.newBuilder()
                .setInput(input)
                .build();
        Command response = execute(command);
        if(response!=null)
            if (response.hasOutput()) {
                return response.getOutput();
            }
        return null;
    }
    // JNI呼び出しのラッパー
    private Command execute(Command command) {
        try {
            byte[] responseBytes = MozcJNI.evalCommand(command.toByteArray());
            Command response=Command.parseFrom(responseBytes);
            Log.d(TAG,"Response="+response.toString());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        mainView=findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets)->{
            Insets systemBars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        key=mainView.findViewById(R.id.key);
        key.setText("よみ");
        word=mainView.findViewById(R.id.word);
        addButton=mainView.findViewById(R.id.addbtn);
        selButton=mainView.findViewById(R.id.selbtn);
        selButton.setOnClickListener(selBtn -> {
            Output output=sendKey(key.getText().toString());
            CandidateWindow candidates=output.getCandidateWindow();
            //Log.d(TAG,"candidates="+candidates.toString());
            if (output.hasCandidateWindow()) {
                CandidateWindow window = output.getCandidateWindow();
                List<CandidateWindow.Candidate> candidateList = window.getCandidateList();
                for (CandidateWindow.Candidate candidate : candidateList) {
                    String value = candidate.getValue();
                    int id = candidate.getId();
                    int index = candidate.getIndex();
                    Log.d(TAG,"候補: " + value + " (ID: " + id + ")");
                }
            }
            output=sendSpace();
            if (output.hasCandidateWindow()) {
                CandidateWindow window = output.getCandidateWindow();
                List<CandidateWindow.Candidate> candidateList = window.getCandidateList();
                for (CandidateWindow.Candidate candidate : candidateList) {
                    String value = candidate.getValue(); // 漢字やかななどの表示文字列
                    int id = candidate.getId();          // 選択時のID
                    int index = candidate.getIndex();    // リスト内の位置
                    Log.d(TAG,"候補: " + value + " (ID: " + id + ")");
                }
            }
        });
//
        context=getApplicationContext();
        ApplicationInfo info = Preconditions.checkNotNull(context).getApplicationInfo();
        File userProfileDirectory = new File(info.dataDir, ".mozc");
        MozcJNI.load(userProfileDirectory.getAbsolutePath(), null);
        sessionId=createSession();
    }
}