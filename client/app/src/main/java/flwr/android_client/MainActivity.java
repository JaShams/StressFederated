package flwr.android_client;

import android.app.Activity;
import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import  flwr.android_client.FlowerServiceGrpc.FlowerServiceStub;
import com.google.protobuf.ByteString;

import org.tensorflow.lite.examples.transfer.api.TransferLearningModel;

import io.grpc.stub.StreamObserver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.opencsv.CSVWriter;


public class MainActivity extends AppCompatActivity {
    private EditText ip;
    private EditText port;
    private Button loadDataButton;
    private Button connectButton;
    private Button trainButton;
    private TextView resultText;
    private EditText device_id;
    private ManagedChannel channel;
    public FlowerClient fc;
    private static final String TAG = "Flower";

    EditText bt,bo,sh,hr;
    Button predict,write,read;
    TextView result;
    private TransferLearningModelWrapper tlModel;
    Context context;

    public TransferLearningModel model;

    int currentQuestion = 1;
    int score = 0;
    TextView questionTextView,scoreTextView;
    RadioGroup radioGroup;
    Button nextButton;

    String csv = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyCsvFile.csv");
    int final_s = 0;
    float avg;
    int state;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=this;
        resultText = (TextView) findViewById(R.id.grpc_response_text);
        resultText.setMovementMethod(new ScrollingMovementMethod());
        device_id = (EditText) findViewById(R.id.device_id_edit_text);
        ip = (EditText) findViewById(R.id.serverIP);
        port = (EditText) findViewById(R.id.serverPort);
        loadDataButton = (Button) findViewById(R.id.load_data);
        connectButton = (Button) findViewById(R.id.connect);
        trainButton = (Button) findViewById(R.id.trainFederated);
        bt = findViewById(R.id.bt);
        bo = findViewById(R.id.bo);
        sh = findViewById(R.id.sh);
        hr = findViewById(R.id.hr);
        result = findViewById(R.id.result);
        predict = (Button) findViewById(R.id.predict);
        fc = new FlowerClient(this);
        tlModel = new TransferLearningModelWrapper(this);

        MainActivity activity = new MainActivity();

        questionTextView = findViewById(R.id.questionTextView);
        radioGroup = findViewById(R.id.radioGroup);
        nextButton = findViewById(R.id.nextButton);

        questionTextView.setText(getString(R.string.question1));

        write = findViewById(R.id.write);
        read = findViewById(R.id.read);

        write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println(csv);
                CSVWriter writer = null;
                try {
                    writer = new CSVWriter(new FileWriter(csv,true));

                    String s=String.valueOf(state);
                    List<String[]> data = new ArrayList<String[]>();
                    data.add(new String[]{bt.getText().toString(), bo.getText().toString(),sh.getText().toString(),hr.getText().toString(),s});
//                    data.add(new String[]{"India", "New Delhi"});
//                    data.add(new String[]{"United States", "Washington D.C"});
//                    data.add(new String[]{"Germany", "Berlin"});

                    writer.writeAll(data); // data is adding to csv
                    System.out.println(writer);
                    writer.close();
                   // callRead();
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                File file = null;
//                try {
//                    file = new File("sampledata/sample.csv"));
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//                try {
//                    // create FileWriter object with file as parameter
//                    FileWriter outputfile = new FileWriter(file);
//
//                    // create CSVWriter object filewriter object as parameter
//                    CSVWriter writer = new CSVWriter (outputfile);
//
//                    // adding header to csv
//                    String[] header = { "Name", "Class", "Marks" };
//                    writer.writeNext(header);
//
//                    // add data to csv
//                    String[] data1 = { "Aman", "10", "620" };
//                    writer.writeNext(data1);
//                    String[] data2 = { "Suraj", "10", "630" };
//                    writer.writeNext(data2);
//
//                    System.out.println("written");
//                    // closing writer connection
//                    writer.close();
//                }
//                catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
            }
        });

        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                int st=0;

                if (selectedId == R.id.option2RadioButton) {
                    score += 1;
                    st=1;
                } else if (selectedId == R.id.option3RadioButton) {
                    score += 2;
                    st=2;
                } else if (selectedId == R.id.option4RadioButton) {
                    score += 3;
                    st=3;
                }
                else if (selectedId == R.id.option5RadioButton) {
                    score += 4;
                    st=4;
                }
                if(currentQuestion == 4 || currentQuestion ==5 || currentQuestion==7 || currentQuestion==8)
                {
                    score=score-st+(4-st);
                }

                currentQuestion++;

                if (currentQuestion > 10) {
                    final_s=score;
                    final_s=final_s/8;
                } else {
                    updateQuestion();
                }
            }
        });
        predict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                float[] arr = {95.36f,94.04f,6.36f,58.4f};
                float[] arr = new float[4];
                arr[0]=Float.parseFloat(bt.getText().toString());
                arr[1]=Float.parseFloat(bo.getText().toString());
                arr[2]=Float.parseFloat(sh.getText().toString());
                arr[3]=Float.parseFloat(hr.getText().toString());


                //Log.e("Predict",fc.predict(arr));

                String pre = fc.predict(arr);
                Log.e("predict",pre);
                String news = pre.split(",")[0];
                String new2 = news.substring(2);
                //Log.e("predict2",news.substring(2));
                result.setText(new2);
                float u;
                if(new2.equals("medium"))
                {
                    u=2;
                }
                else if(new2.equals(("high")))
                {
                    u=4;
                }
                else if(new2.equals(("medium high")))
                {
                    u=3;
                }
                else if(new2.equals(("medium low")))
                {
                    u=1;
                }
                else {
                    u=0;
                }

                float g = (float) final_s;

                avg = Float.sum(u,g);
                if(g!=0) {
                    avg = (float) avg / (float) 2;
                }
                Qlearning reco = new Qlearning(5,4);
                Log.d("2D Array", Arrays.deepToString(reco.qTable));
                state= (int) avg;


                int action = reco.getAction(state);


                reco.takeAction(state,action,context);

//                bt.setText("");
//                bo.setText("");
//                sh.setText("");
//                hr.setText("");
//                result.setText("");

//                Log.e("Predict);

//                int maxIndex = 0;
//                for (int i = 1; i < array.length; i++) {
//                    if (array[i] > array[maxIndex]) {
//                        maxIndex = i;
//                    }
//                }
//                TransferLearningModel.Prediction[] arr = model.predict(image);
//                String[][] ret = new String[arr.length][2];
//                for(int i=0; i<arr.length; i++){
//                    ret[i][0] = arr[i].getClassName();
//                    ret[i][1] = String.valueOf(arr[i].getConfidence());
//                }
                //System.out.println(Arrays.deepToString(ret));
            }
        });


    }


    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    public void setResultText(String text) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);
        String time = dateFormat.format(new Date());
        resultText.append("\n" + time + "   " + text);
    }

    public void loadData(View view){
        if (TextUtils.isEmpty(device_id.getText().toString())) {
            Toast.makeText(this, "Please enter a client partition ID between 1 and 4 (inclusive)", Toast.LENGTH_LONG).show();
        }
        else if (Integer.parseInt(device_id.getText().toString()) > 10 ||  Integer.parseInt(device_id.getText().toString()) < 1)
        {
            Toast.makeText(this, "Please enter a client partition ID between 1 and 4 (inclusive)", Toast.LENGTH_LONG).show();
        }
        else{
            hideKeyboard(this);
            setResultText("Loading the local training dataset in memory. It will take several seconds.");
            loadDataButton.setEnabled(false);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                private String result;
                @Override
                public void run() {
                    try {
                        fc.loadData(Integer.parseInt(device_id.getText().toString()));
                        result =  "Training dataset is loaded in memory.";
                    } catch (Exception e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        pw.flush();
                        result =  "Training dataset is loaded in memory.";
                    }
                    handler.post(() -> {
                        setResultText(result);
                        connectButton.setEnabled(true);
                    });
                }
            });
        }
    }

    public void connect(View view) {
//        String host = ip.getText().toString();
//        String portStr = port.getText().toString();
        String host = "10.0.2.2";
        String portStr = "8888";
        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(portStr) || !Patterns.IP_ADDRESS.matcher(host).matches()) {
            Toast.makeText(this, "Please enter the correct IP and port of the FL server", Toast.LENGTH_LONG).show();
        }
        else {
            int port = TextUtils.isEmpty(portStr) ? 0 : Integer.parseInt(portStr);
            channel = ManagedChannelBuilder.forAddress(host, port).maxInboundMessageSize(10 * 1024 * 1024).usePlaintext().build();
            hideKeyboard(this);
            trainButton.setEnabled(true);
            connectButton.setEnabled(false);
            setResultText("Channel object created. Ready to train!");
        }
    }

    public void runGRPC(View view){
        MainActivity activity = this;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            private String result;
            @Override
            public void run() {
                try {
                    (new FlowerServiceRunnable()).run(FlowerServiceGrpc.newStub(channel), activity);
                    result =  "Connection to the FL server successful \n";
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    pw.flush();
                    result = "Failed to connect to the FL server \n" + sw;
                }
                handler.post(() -> {
                    setResultText(result);
                    trainButton.setEnabled(false);
                });
            }
        });
    }


    private static class FlowerServiceRunnable{
        protected Throwable failed;
        private StreamObserver<ClientMessage> requestObserver;

        public void run(FlowerServiceStub asyncStub, MainActivity activity) {
             join(asyncStub, activity);
        }

        private void join(FlowerServiceStub asyncStub, MainActivity activity)
                throws RuntimeException {

            final CountDownLatch finishLatch = new CountDownLatch(1);
            requestObserver = asyncStub.join(
                            new StreamObserver<ServerMessage>() {
                                @Override
                                public void onNext(ServerMessage msg) {
                                    handleMessage(msg, activity);
                                }

                                @Override
                                public void onError(Throwable t) {
                                    t.printStackTrace();
                                    failed = t;
                                    finishLatch.countDown();
                                    Log.e(TAG, t.getMessage());
                                }

                                @Override
                                public void onCompleted() {
                                    finishLatch.countDown();
                                    Log.e(TAG, "Done");
                                }
                            });
        }

        private void handleMessage(ServerMessage message, MainActivity activity) {

            try {
                ByteBuffer[] weights;
                ClientMessage c = null;

                if (message.hasGetParametersIns()) {
                    Log.e(TAG, "Handling GetParameters");
                    activity.setResultText("Handling GetParameters message from the server.");

                    weights = activity.fc.getWeights();
                    Log.e(TAG,String.valueOf(weights.length));
                    c = weightsAsProto(weights);
                } else if (message.hasFitIns()) {
                    Log.e(TAG, "Handling FitIns");
                    activity.setResultText("Handling Fit request from the server.");

                    List<ByteString> layers = message.getFitIns().getParameters().getTensorsList();
                    Log.e("layers", layers.toString());
                    Log.e("layers", Arrays.toString(layers.toArray()));

                    Scalar epoch_config = message.getFitIns().getConfigMap().getOrDefault("local_epochs", Scalar.newBuilder().setSint64(1).build());

                    assert epoch_config != null;
                    int local_epochs = (int) epoch_config.getSint64();

                    // Our model has 10 layers
                    ByteBuffer[] newWeights = new ByteBuffer[8] ;
                    for (int i = 0; i < 8; i++) {
                        newWeights[i] = ByteBuffer.wrap(layers.get(i).toByteArray());
                    }

                    Pair<ByteBuffer[], Integer> outputs = activity.fc.fit(newWeights, local_epochs);
                    c = fitResAsProto(outputs.first, outputs.second);
                } else if (message.hasEvaluateIns()) {
                    Log.e(TAG, "Handling EvaluateIns");
                    activity.setResultText("Handling Evaluate request from the server");

                    List<ByteString> layers = message.getEvaluateIns().getParameters().getTensorsList();

                    // Our model has 10 layers
                    ByteBuffer[] newWeights = new ByteBuffer[8] ;
                    for (int i = 0; i < 8; i++) {
                        newWeights[i] = ByteBuffer.wrap(layers.get(i).toByteArray());
                    }
                    Pair<Pair<Float, Float>, Integer> inference = activity.fc.evaluate(newWeights);

                    float loss = inference.first.first;
                    float accuracy = inference.first.second;
                    activity.setResultText("Test Accuracy after this round = " + accuracy);
                    int test_size = inference.second;
                    c = evaluateResAsProto(loss, test_size);
                }
                requestObserver.onNext(c);
                activity.setResultText("Response sent to the server");
            }
            catch (Exception e){
                Log.e(TAG,"Error occurred!");
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private static ClientMessage weightsAsProto(ByteBuffer[] weights){
        List<ByteString> layers = new ArrayList<>();
        for (ByteBuffer weight : weights) {
            layers.add(ByteString.copyFrom(weight));
        }
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.GetParametersRes res = ClientMessage.GetParametersRes.newBuilder().setParameters(p).build();
        return ClientMessage.newBuilder().setGetParametersRes(res).build();
    }

    private static ClientMessage fitResAsProto(ByteBuffer[] weights, int training_size){
        List<ByteString> layers = new ArrayList<>();
        for (ByteBuffer weight : weights) {
            layers.add(ByteString.copyFrom(weight));
        }
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.FitRes res = ClientMessage.FitRes.newBuilder().setParameters(p).setNumExamples(training_size).build();
        return ClientMessage.newBuilder().setFitRes(res).build();
    }

    private static ClientMessage evaluateResAsProto(float accuracy, int testing_size){
        ClientMessage.EvaluateRes res = ClientMessage.EvaluateRes.newBuilder().setLoss(accuracy).setNumExamples(testing_size).build();
        return ClientMessage.newBuilder().setEvaluateRes(res).build();
    }

    private void updateQuestion() {
        if(currentQuestion==2)
            questionTextView.setText(getString(R.string.question2));

        if(currentQuestion==3)
            questionTextView.setText(getString(R.string.question3));

        if(currentQuestion==4)
            questionTextView.setText(getString(R.string.question4));

        if(currentQuestion==5)
            questionTextView.setText(getString(R.string.question5));

        if(currentQuestion==6)
            questionTextView.setText(getString(R.string.question6));

        if(currentQuestion==7)
            questionTextView.setText(getString(R.string.question7));

        if(currentQuestion==8)
            questionTextView.setText(getString(R.string.question8));

        if(currentQuestion==9)
            questionTextView.setText(getString(R.string.question9));

        if(currentQuestion==10)
            questionTextView.setText(getString(R.string.question10));


        radioGroup.clearCheck();
    }
}
