package com.myapp.mozc_session;
//
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
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
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
//
import org.mozc.android.inputmethod.japanese.KeycodeConverter;
import org.mozc.android.inputmethod.japanese.MozcLog;
import org.mozc.android.inputmethod.japanese.PrimaryKeyCodeConverter;
import org.mozc.android.inputmethod.japanese.keyboard.ProbableKeyEventGuesser;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig.Config;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands;
import org.mozc.android.inputmethod.japanese.protobuf.ProtoUserDictionaryStorage;
import org.mozc.android.inputmethod.japanese.session.SessionExecutor;
import org.mozc.android.inputmethod.japanese.session.SessionHandlerFactory;
//
import java.util.ArrayList;
import java.util.List;
//
public class MainActivity extends AppCompatActivity {
//
    final String TAG="MOZCsession";
    View mainView;
    TextView key,word,addButton,selButton;
    SessionExecutor sessionExecutor;
    Context context;
    HandlerThread syncDataThread;
    Handler syncDataHandler;
    ProbableKeyEventGuesser guesser;
    PrimaryKeyCodeConverter primaryKeyCodeConverter;
    long sessionId;
    long selectedDictionaryId;
    ProtoUserDictionaryStorage.UserDictionaryStorage storage;
    Config config;
    //
    class SaveTouchEvent implements View.OnTouchListener{
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent){
            return false;
        }
    }
    private class RenderResultCallback implements SessionExecutor.EvaluationCallback {
        @Override public void onCompleted(
                Optional<ProtoCommands.Command> command, Optional<KeycodeConverter.KeyEventInterface> triggeringKeyEvent) {
            Preconditions.checkArgument(Preconditions.checkNotNull(command).isPresent());
            Preconditions.checkNotNull(triggeringKeyEvent);
            ProtoCommands.Output output=command.get().getOutput();
            if(output.hasAllCandidateWords()){
                ProtoCandidates.CandidateList candidates=output.getAllCandidateWords();
                for(int i=0;i<candidates.getCandidatesCount();i++){
                    String candidateWord=candidates.getCandidates(i).getValue();
                Log.d(TAG,"getAllCandidateWords("+output.getAllCandidateWords().getCandidates(i).getId()+")="+candidateWord);
                }
            }
        }
    }
    private class GetResultCallback implements SessionExecutor.EvaluationCallback {
        @Override public void onCompleted(
                Optional<ProtoCommands.Command> response, Optional<KeycodeConverter.KeyEventInterface> triggeringKeyEvent) {
// 確定結果（result）をエディタに反映させる
            String resultText = "NOTHING";
            if (response.get().hasOutput() && response.get().getOutput().hasResult()) {
                resultText = response.get().getOutput().getResult().getValue();
            }
            Log.d(TAG,"EvaluationCallback: "+resultText);
        }
    }
    void convertByMozc(String s) {
        int primaryCode = 0;
        List<ProtoCommands.Input.TouchEvent> touchEventList = new ArrayList<>();
        ProtoCommands.KeyEvent keyEvent = ProtoCommands.KeyEvent.newBuilder()
                .setKeyString(s)
                .build();
        sessionExecutor.sendKey(
                keyEvent,
                primaryKeyCodeConverter.getPrimaryCodeKeyEvent(primaryCode),
                touchEventList,
                new RenderResultCallback()
        );
    }
    private long createSession() {
        ProtoUserDictionaryStorage.UserDictionaryCommand command = ProtoUserDictionaryStorage.UserDictionaryCommand.newBuilder()
                .setType(ProtoUserDictionaryStorage.UserDictionaryCommand.CommandType.CREATE_SESSION)
                .build();
        ProtoUserDictionaryStorage.UserDictionaryCommandStatus status = sessionExecutor.sendUserDictionaryCommand(command);
        if (status.getStatus() != ProtoUserDictionaryStorage.UserDictionaryCommandStatus.Status.USER_DICTIONARY_COMMAND_SUCCESS) {
            throw new IllegalStateException("UserDictionaryCommand session sshsould be created always.");
        }
        return status.getSessionId();
    }
    final private static String userDictionaryName="_USER_DICTIONARY_";
    private boolean renewUserDictionary(){
        ProtoUserDictionaryStorage.UserDictionaryCommand command;
        ProtoUserDictionaryStorage.UserDictionaryCommandStatus status;
        command = ProtoUserDictionaryStorage.UserDictionaryCommand.newBuilder()
                .setType(ProtoUserDictionaryStorage.UserDictionaryCommand.CommandType.LOAD)
                .setSessionId(sessionId)
                .setEnsureNonEmptyStorage(true)
                .build();
        status = sessionExecutor.sendUserDictionaryCommand(command);
        Log.d(TAG,"LOAD:"+status.toString());
        command=ProtoUserDictionaryStorage.UserDictionaryCommand.newBuilder()
                .setType(ProtoUserDictionaryStorage.UserDictionaryCommand.CommandType.GET_USER_DICTIONARY_NAME_LIST)
                .setSessionId(sessionId)
                .build();
        status=sessionExecutor.sendUserDictionaryCommand(command);
        Log.d(TAG,"GET_USER_DICTIONARY_NAME_LIST:"+status.toString());
        if(status.getStatus()!=ProtoUserDictionaryStorage.UserDictionaryCommandStatus.Status.USER_DICTIONARY_COMMAND_SUCCESS) {
            Log.e(TAG,"GET_USER_DICTIONARY_NAME_LIST:"+status.getStatus());
            return false;
        }
        storage=status.getStorage();
        status.getStorage().getDictionariesList().forEach(s->Log.d(TAG,"name="+s.getName()));
        java.util.Optional<ProtoUserDictionaryStorage.UserDictionary> userDictionary_w=
        status.getStorage().getDictionariesList().stream().filter(s->s.getName().equals(userDictionaryName)).findFirst();
        if(userDictionary_w.isPresent()){
            command=ProtoUserDictionaryStorage.UserDictionaryCommand.newBuilder()
                    .setType(ProtoUserDictionaryStorage.UserDictionaryCommand.CommandType.DELETE_DICTIONARY)
                    .setSessionId(sessionId)
                    .setDictionaryId(userDictionary_w.get().getId())
                    .build();
            status=sessionExecutor.sendUserDictionaryCommand(command);
            Log.d(TAG, "DELETE_DICTIONARY:"+status.toString());
        }
        command=ProtoUserDictionaryStorage.UserDictionaryCommand.newBuilder()
                .setType(ProtoUserDictionaryStorage.UserDictionaryCommand.CommandType.CREATE_DICTIONARY)
                .setSessionId(sessionId)
                .setDictionaryName(userDictionaryName)
                .build();
        status=sessionExecutor.sendUserDictionaryCommand(command);
        Log.d(TAG,"CREATE_DICTIONARY:"+status.toString());
        if (status.getStatus()==ProtoUserDictionaryStorage.UserDictionaryCommandStatus.Status.USER_DICTIONARY_COMMAND_SUCCESS){
            selectedDictionaryId=status.getDictionaryId();
        }else{
            Log.e(TAG,"CREATE_DICTIONARY:"+status.getStatus());
            return false;
        }
        command=ProtoUserDictionaryStorage.UserDictionaryCommand.newBuilder()
                .setType(ProtoUserDictionaryStorage.UserDictionaryCommand.CommandType.SET_DEFAULT_DICTIONARY_NAME)
                .setSessionId(sessionId)
                .setDictionaryName(userDictionaryName)
                .build();
        status=sessionExecutor.sendUserDictionaryCommand(command);
        if (status.getStatus()==ProtoUserDictionaryStorage.UserDictionaryCommandStatus.Status.USER_DICTIONARY_COMMAND_SUCCESS){
            sessionExecutor.reload();
            return true;
        }else{
            Log.e(TAG,"SET_DEFAULT_DICTIONARY_NAME:"+status.getStatus());
            return false;
        }
    }
    private boolean addEntry(String word, String reading, ProtoUserDictionaryStorage.UserDictionary.PosType pos){
        ProtoUserDictionaryStorage.UserDictionaryCommand command=ProtoUserDictionaryStorage.UserDictionaryCommand.newBuilder()
                .setType(ProtoUserDictionaryStorage.UserDictionaryCommand.CommandType.ADD_ENTRY)
                .setSessionId(sessionId)
                .setDictionaryId(selectedDictionaryId)
                .setEntry(ProtoUserDictionaryStorage.UserDictionary.Entry.newBuilder()
                        .setKey(reading)
                        .setValue(word)
                        .setPos(pos))
                .build();
        ProtoUserDictionaryStorage.UserDictionaryCommandStatus status=sessionExecutor.sendUserDictionaryCommand(command);
        Log.d(TAG,"ADD_ENTRY:"+status.toString());
        if(status.getStatus()==ProtoUserDictionaryStorage.UserDictionaryCommandStatus.Status.USER_DICTIONARY_COMMAND_SUCCESS){
            return true;
        }else{
            Log.e(TAG,"ADD_ENTRY:"+status.getStatus());
            return false;
        }
    }
    private List<ProtoUserDictionaryStorage.UserDictionary.Entry> getEntries(int beginIndex, int endIndex) {
        ProtoUserDictionaryStorage.UserDictionaryCommand.Builder builder = ProtoUserDictionaryStorage.UserDictionaryCommand.newBuilder()
                .setType(ProtoUserDictionaryStorage.UserDictionaryCommand.CommandType.GET_ENTRIES)
                .setSessionId(sessionId)
                .setDictionaryId(selectedDictionaryId);
        for (int i = beginIndex; i < endIndex; ++i) {
            builder.addEntryIndex(i);
        }
        ProtoUserDictionaryStorage.UserDictionaryCommandStatus status = sessionExecutor.sendUserDictionaryCommand(builder.build());
        if (status.getStatus() != ProtoUserDictionaryStorage.UserDictionaryCommandStatus.Status.USER_DICTIONARY_COMMAND_SUCCESS) {
            MozcLog.e("Unknown failure: " + status.getStatus());
            return new ArrayList<ProtoUserDictionaryStorage.UserDictionary.Entry>();
        }
        return status.getEntriesList();
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
        addButton.setOnClickListener(btn -> {
            ProtoUserDictionaryStorage.UserDictionaryCommand command;
            ProtoUserDictionaryStorage.UserDictionaryCommandStatus status;
            addEntry(word.getText().toString(),key.getText().toString(),ProtoUserDictionaryStorage.UserDictionary.PosType.NOUN);
            command = ProtoUserDictionaryStorage.UserDictionaryCommand.newBuilder()
                .setType(ProtoUserDictionaryStorage.UserDictionaryCommand.CommandType.GET_ENTRY_SIZE)
                .setSessionId(sessionId)
                .setDictionaryId(selectedDictionaryId)
                .build();
            status = sessionExecutor.sendUserDictionaryCommand(command);
            Log.d(TAG,"GET_ENTRY_SIZE:"+status.toString());
            if (status.getStatus()==ProtoUserDictionaryStorage.UserDictionaryCommandStatus.Status.USER_DICTIONARY_COMMAND_SUCCESS) {
                getEntries(0,status.getEntrySize()).forEach(s->Log.d(TAG,"entry="+s.getKey()+","+s.getValue()));
            }
            command=ProtoUserDictionaryStorage.UserDictionaryCommand.newBuilder()
                    .setType(ProtoUserDictionaryStorage.UserDictionaryCommand.CommandType.SAVE)
                    .setSessionId(sessionId)
                    .build();
            status=sessionExecutor.sendUserDictionaryCommand(command);
            if (status.getStatus()!=ProtoUserDictionaryStorage.UserDictionaryCommandStatus.Status.USER_DICTIONARY_COMMAND_SUCCESS) {
                Log.e(TAG,"SAVE:"+status.toString());
            }
            Log.d(TAG,"SAVE:"+status.toString());
            sessionExecutor.reload();// When succeeded, we need to reload the mozc server.
        });
        selButton.setOnClickListener(selBtn -> {
            sessionExecutor.resetContext();
            convertByMozc(key.getText().toString());
        });
//
        context=getApplicationContext();
        guesser=new ProbableKeyEventGuesser(context.getAssets());
        primaryKeyCodeConverter=new PrimaryKeyCodeConverter(context, guesser);
//android.os.Debug.waitForDebugger();
        sessionExecutor=SessionExecutor.getInstanceInitializedIfNecessary(
                new SessionHandlerFactory(com.google.common.base.Optional.<SharedPreferences>absent()), context);
        sessionExecutor.setLogging(true);
        sessionId=createSession();
        renewUserDictionary();
        syncDataThread=new HandlerThread("syncDataThread");// Create MOZC sync thraed
        syncDataThread.start();
        syncDataHandler=new Handler(syncDataThread.getLooper()){
            static final int SYNC_DATA_COMMAND_PERIOD=15*6*1000;
            @Override public void handleMessage(Message msg){
                if(sessionExecutor!=null)sessionExecutor.syncData();
                sendEmptyMessageDelayed(0,SYNC_DATA_COMMAND_PERIOD);
            }
        };
        syncDataHandler.sendEmptyMessage(0);// First SYNC
    }
}