package flwr.android_client;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.ConditionVariable;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import com.opencsv.CSVReader;

import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
//import com.opencsv.CSVReader;
public class FlowerClient {

    String csv = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyCsvFile.csv");

    private TransferLearningModelWrapper tlModel;
    private static final int LOWER_BYTE_MASK = 0xFF;
    private MutableLiveData<Float> lastLoss = new MutableLiveData<>();
    private Context context;
    private final ConditionVariable isTraining = new ConditionVariable();
    private static String TAG = "Flower";
    private int local_epochs = 1;

    public FlowerClient(Context context) {
        this.tlModel = new TransferLearningModelWrapper(context);
        this.context = context;
    }

    public ByteBuffer[] getWeights() {
        return tlModel.getParameters();
    }

    public Pair<ByteBuffer[], Integer> fit(ByteBuffer[] weights, int epochs) {
        this.local_epochs = epochs;
        tlModel.updateParameters(weights);
        isTraining.close();
        tlModel.train(this.local_epochs);
        tlModel.enableTraining((epoch, loss) -> setLastLoss(epoch, loss));
        Log.e(TAG ,  "Training enabled. Local Epochs = " + this.local_epochs);
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
            String line[];
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
            Log.d(TAG, "loadData: IOexception occured");
            ex.printStackTrace();
        }
    }

    private void addSample(String[] params, Boolean isTraining) throws IOException {
        String sampleClass = null;
        // get rgb equivalent and class
        float[] fparams = new float[4];
        for(int i=0; i<4 ; i++){
            fparams[i] = Float.parseFloat(params[i]);
        }

        if(params[params.length - 1].equals("0")) sampleClass = "low";
        else if(params[params.length - 1].equals("1")) sampleClass = "medium low";
        else if(params[params.length - 1].equals("2")) sampleClass = "medium";
        else if(params[params.length - 1].equals("3")) sampleClass = "medium high";
        else if(params[params.length - 1].equals("4")) sampleClass = "high";
        else {
            Log.e(TAG, "Stress value not recognized");
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

    public String predict(float data[]){
        return Arrays.deepToString(this.tlModel.predict(data));
    }

    public String get_class(String path) {
        String label = path.split("/")[2];
        return label;
    }

    /**
     * Normalizes a camera image to [0; 1], cropping it
     * to size expected by the model and adjusting for camera rotation.
     */
    private static float[] prepareImage(Bitmap bitmap)  {
        int modelImageSize = TransferLearningModelWrapper.IMAGE_SIZE;

        float[] normalizedRgb = new float[modelImageSize * modelImageSize * 3];
        int nextIdx = 0;
        for (int y = 0; y < modelImageSize; y++) {
            for (int x = 0; x < modelImageSize; x++) {
                int rgb = bitmap.getPixel(x, y);

                float r = ((rgb >> 16) & LOWER_BYTE_MASK) * (1 / 255.0f);
                float g = ((rgb >> 8) & LOWER_BYTE_MASK) * (1 / 255.0f);
                float b = (rgb & LOWER_BYTE_MASK) * (1 / 255.0f);

                normalizedRgb[nextIdx++] = r;
                normalizedRgb[nextIdx++] = g;
                normalizedRgb[nextIdx++] = b;
            }
        }

        return normalizedRgb;
    }
}
