package flwr.android_client;

import android.content.Context;
import android.os.ConditionVariable;
import android.util.Log;
import android.util.Pair;

import com.opencsv.CSVReader;

import androidx.lifecycle.MutableLiveData;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FlowerClient {

    private TransferLearningModelWrapper tlModel;
    private MutableLiveData<Float> lastLoss = new MutableLiveData<>();
    private Context context;
    private final ConditionVariable isTraining = new ConditionVariable();
    private static final String TAG = "Flower";
    private int local_epochs = 1;
    private List<Integer> weightSize;
    private int flag = 0;

    public FlowerClient(Context context) {
        this.tlModel = new TransferLearningModelWrapper(context);
        this.context = context;
        this.weightSize = Arrays.asList(16000, 4000, 2000000, 2000, 600000, 1200, 6000, 20);
    }

    public ByteBuffer[] getWeights() {
        return tlModel.getParameters();
    }

    public void restoreWeights() {
        try (FileInputStream fileInputStream = context.openFileInput("weights.txt")) {
            ByteBuffer[] weights = new ByteBuffer[8];
            for (int i = 0; i < 8; i++) {
                weights[i] = ByteBuffer.allocate(weightSize.get(i));
            }
            FileChannel fc = fileInputStream.getChannel();
            fc.read(weights);
            for (int i = 0; i < 8; i++) {
                weights[i].rewind();
            }
            tlModel.updateParameters(weights);
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void storeWeights() {
        try (FileOutputStream fileOutputStream = context.openFileOutput("weights.txt", Context.MODE_PRIVATE)) {
            FileChannel fc = fileOutputStream.getChannel();
            ByteBuffer[] weights = tlModel.getParameters();
            for (int i = 0; i < weights.length; i++) weights[i].flip();
            long b = fc.write(weights);
            Log.e(TAG, b + " Weights Stored");
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Pair<ByteBuffer[], Integer> fit(ByteBuffer[] weights, int epochs) {
        this.local_epochs = epochs;
        tlModel.updateParameters(weights);
        isTraining.close();
        tlModel.train(this.local_epochs);
        tlModel.enableTraining((epoch, loss) -> setLastLoss(epoch, loss));
        Log.e(TAG, "Training enabled. Local Epochs = " + this.local_epochs);
        isTraining.block();
        return Pair.create(getWeights(), tlModel.getSize_Training());
    }

    public Pair<Pair<Float, Float>, Integer> evaluate(ByteBuffer[] weights) {
        tlModel.updateParameters(weights);
        tlModel.disableTraining();
        return Pair.create(tlModel.calculateTestStatistics(), tlModel.getSize_Testing());
    }

    public void setLastLoss(int epoch, float newLoss) {
        if (epoch == this.local_epochs - 1) {
            Log.e(TAG, "Training finished after epoch = " + epoch);
            lastLoss.postValue(newLoss);
            tlModel.disableTraining();
            isTraining.open();
        }
    }

    public void loadData(int device_id) {
        try {
            CSVReader reader = new CSVReader(new InputStreamReader(this.context.getAssets().open("data/dataset"+ String.valueOf(device_id)+".csv")));
            //CSVReader reader = new CSVReader(new FileReader(csv));

            //CSVReader = new CSVReader(new FileWriter(csv));
            String[] line;
            int i = 0;
            while ((line = reader.readNext()) != null) {
                i++;
                Log.e(TAG, i + "th training example loaded");
                addSample(line, true);
            }
            reader.close();

            i = 0;
            reader = new CSVReader(new InputStreamReader(this.context.getAssets().open("data/dataset"+ String.valueOf(device_id)+".csv")));
//            reader = new CSVReader(new FileReader(csv));
            while ((line = reader.readNext()) != null) {
                i++;
                addSample(line, false);
                Log.e(TAG, i + "th test example loaded");
            }
            reader.close();

        } catch (IOException ex) {
            Log.d(TAG, "loadData: IOException occurred");
            ex.printStackTrace();
        }
    }

    private void addSample(String[] params, Boolean isTraining) throws IOException {
        String sampleClass = null;
        // get rgb equivalent and class
        float[] fparams = new float[4];
        for (int i = 0; i < 4; i++) {
            fparams[i] = Float.parseFloat(params[i]);
        }

        switch (params[params.length - 1]) {
            case "0":
                sampleClass = "low";
                break;
            case "1":
                sampleClass = "medium low";
                break;
            case "2":
                sampleClass = "medium";
                break;
            case "3":
                sampleClass = "medium high";
                break;
            case "4":
                sampleClass = "high";
                break;
            default:
                Log.e(TAG, "Stress value not recognized");
                break;
        }

        // add to the list.
        try {
            this.tlModel.addSample(fparams, sampleClass, isTraining).get();
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to add sample to model", e.getCause());
        } catch (InterruptedException e) {
            // no-op
        }
    }

    public String predict(float[] data) {
        return Arrays.deepToString(this.tlModel.predict(data));
    }


}
